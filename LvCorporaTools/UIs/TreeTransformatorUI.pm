package LvCorporaTools::UIs::TreeTransformatorUI;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processDir collect ord unnest dep red knit conll fold);

#use Carp::Always;	# Print stack trace on die.

use File::Copy;
use File::Path;
use IO::Dir;
use IO::File;

use LvCorporaTools::PMLUtils::AUtils;
use LvCorporaTools::TreeTransf::UnnestCoord;
use LvCorporaTools::TreeTransf::Hybrid2Dep;
use LvCorporaTools::TreeTransf::RemoveReduction;
use LvCorporaTools::PMLUtils::Knit;
use LvCorporaTools::FormatTransf::DepPml2Conll;
use LvCorporaTools::DataSelector::SplitTreebank;

use LvCorporaTools::GenericUtils::UIWrapper;

use Data::Dumper;

###############################################################################
# Interface for Latvian Treebank PML transformations. See documentation in
# &_printMan().
# Invocation example for Windows:
# perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect --unnest --dep xpred=BASELEM coord=ROW pmc=BASELEM root=0 phdep=1 na=0 subrt=0 --red label=0 --knit --conll label=1 cpostag=FIRST postag=FULL conll09=0 --fold p=1
#
# TODO: control sentence omiting, when converting to conll.
#
# Works with A level schema v.2.14.
# Input files - utf8.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2013
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
sub _printMan
{
	print <<END;
Unified interface for transformation scripts.
Input files should be provided as UTF-8.
Usage:
  TreeTransformatorUI --flag1 param11=value11 param12=value12
                      --flag2 param21=value21 ...
                        ...
  Flags, parameter names and values are case sensitive.
  --dir and at least one processing step is mandatory.
  
Params:
  General
    --dir      input directory (single value)

  Preprocessing
    --collect  collect all .w + .m + .a from input folder and it's
               folder - use this if data files are given in some subfolder
               structure (no params)

  Main flow
    --unnest   convert multi-level coordinations to single level
               params:
                 ord - do input data have all nodes ordered [0 (default) / 1]
    --dep      convert to dependencies
               params:
                 xpred - x-Pred mode [BASELEM / DEFAULT (default)],
                 coord - Coord mode [ROW / ROW_NO_CONJ / 3_LEVEL / DEFAULT
                         (default)],
                 pmc   - PMC mode [BASELEM / DEFAULT (default)],
                 root  - label root node with distinct label [0 / 1 (default)],
                 phdep - label phrase dependents with different role prefix [0
                         (default) / 1],
                 na    - allow 'N/A' to be part of longer labels [0 (default)
                         / 1],
                 subrt - label new roots of all phrases as members of
                         corresponding phrases [0 (only some) / 1 (default)],
                 ord   - do input data have all nodes ordered [0 (default) / 1]
    --red      remove reductions
               params:
                 label - label ommisions of empty nodes [0 / 1 (default)],
                 ord - do input data have all nodes ordered [0 (default) / 1]
    --knit     convert .w + .m + .a to a single .pml file
               params:
                 path - directory of PML schemas [default =
                        'TrEd extension/lv-treebank/resources']
    --conll    convert .pml to conll
               params:
                 label   - label output tree arcs [0/1],
                 cpostag - CPOSTAG mode [PURIFY / FIRST / NONE (default)],
                 postag  - POSTAG mode [FULL (default) / PURIFY], 
                 conll09 - do "large" CoNLL-2009 output [0 (default) / 1]
    --fold     create development/test or cross-validation datasets
               params:
                 p    - probability (0;1), or cross-validation part count {3;
                        4; 5; ...}, or 1 for concatenating all files,
                 seed - seed [default = nothing pased to srand]

  Additional stand-alone transformations
    --ord      recalculate 'ord' fields
               params:
                 mode - reordering mode [TOKEN/NODE]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
}

# Process treebank folder. This should be used as entry point, if this module
# is used standalone.
sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 3)
	{
		&_printMan;
		exit 1;
	}
	my @flags = @_;

	# Parse parameters: at first find flags indicating steps and parameters of
	# according steps.
	my %params = ();
	my $lastFlag;
	for my $f (@flags)
	{
		if ($f =~ /^--/)
		{
			$lastFlag = $f;
			$params{$f} = [];
		} elsif (defined $lastFlag)
		{
			$params{$lastFlag} = [@{$params{$lastFlag}}, $f];
		} else
		{
			&_printMan;
			exit 1;
		}
	}
	
	# Then parse parameters of each step.
	for my $step (keys %params)
	{
		my %attrValues = ();
		for my $attrValSt (@{$params{$step}})
		{
			my @splitted = split('=', $attrValSt, 2);
			push (@splitted, '1') if (@splitted == 1);
			%attrValues = (%attrValues, @splitted);
		}
		$params{$step} = \%attrValues;
	}
	
	# Get directories.
	if (not $params{'--dir'} or keys(%{$params{'--dir'}}) > 1)
	{
		&_printMan;
		exit 1;
	}

	my $dirPrefix = (keys(%{$params{'--dir'}}))[0];
	my $source = (keys(%{$params{'--dir'}}))[0];
		
	# Collecting data recursively.
	if ($params{'--collect'})
	{
		$source = &collect($source, "$dirPrefix/collected");
	}
	
	# Recalculating ord fields.
	$source = &ord($source, "$dirPrefix/ord", $params{'--ord'})
		if ($params{'--ord'});
		
	# Unnest coordinations.
	$source = &unnest($source, "$dirPrefix/unnest", $params{'--unnest'})
		if ($params{'--unnest'});
	
	
	# Converting to dependencies.
	$source = &dep($source, "$dirPrefix/dep", $params{'--dep'})
		if ($params{'--dep'});
	
	# Removing reductions.
	$source = &red($source, "$dirPrefix/red", $params{'--red'})
		if ($params{'--red'});
	
	# Knitting-in.
	$source = &knit($source, "$dirPrefix/knitted", $params{'--knit'})
		if ($params{'--knit'});
		
	# Converting to CoNLL.
	$source = &conll($source, "$dirPrefix/conll", $params{'--conll'})
		if ($params{'--conll'});
	
	# Folding data sets for training.
	$source = &fold($source, "$dirPrefix/fold", $params{'--fold'})
		if ($params{'--fold'});
	
	print "\n==== Successful finish =======================================\n";
}

# Collect data recursively.
# collect(source data directory, destination directory)
# return folder with step results.
sub collect
{
	my ($source, $dest) = @_;
	print "\n==== Recursive data collecting ===============================\n";
		
	my $fileCounter = 0;
	my @todoDirs = ();
	my $current = $source;
	mkpath ($dest);
		
	# Traverse subdirectories.
	while ($current)
	{
		my $dir = IO::Dir->new($current) or die "Can't open folder $!";
		while (defined(my $item = $dir->read))
		{
			# Treebank file
			if ((-f "$current/$item") and ($item =~ /.[amw]$/))
			{
				copy("$current/$item", $dest);
				$fileCounter++;
			}
			elsif (-d "$current/$item" and $item !~ /^\.\.?$/ and $item ne "collected")
			{
				# If copy source and dest is the same, result under Unix is empty file.
				push @todoDirs, "$current/$item";
			}
		}
	}
	continue
	{
		$current = shift @todoDirs;
	}
		
		print "Found $fileCounter files.\n";
		return $dest;
}

# Recalculate ord fields.
# ord(source data directory, destination directory, pointer to parameter array)
# return folder with step results.
sub ord
{
	my ($source, $dest, $params) = @_;
	print "\n==== Recalculating ord fields ================================\n";
	die "Invalid argument ".$params->[0]." for --ord $!"
		if ($params->{'mode'} ne 'TOKEN' and $params->{'mode'} ne 'NODE');
	
	# Definition how to process a single tree.
	my $treeProc = sub
	{
		$params->{'mode'} eq 'NODE' ?
			LvCorporaTools::PMLUtils::AUtils::renumberNodes(@_):
			LvCorporaTools::PMLUtils::AUtils::renumberTokens(@_);
	};
	
	# Definition how to process a single file.
	my $fileProc = sub
	{
		LvCorporaTools::GenericUtils::UIWrapper::transformAFile(
			$treeProc, 0, 0, '', '', @_);
	};
	
	# Process contents of source folder.
	LvCorporaTools::GenericUtils::UIWrapper::processDir(
		$fileProc, "^.+\\.a\$", '-ord.a', 1, 0, $source);
	
	# Move files to correct places.
	move("$source/res", $dest)
		|| warn "Moving $source/res to $dest failed: $!";
	my @files = glob("$source/*.m $source/*.w");
	for (@files)
	{
		copy($_, $dest);
	}
	
	return $dest;
}

# Unnest coordinations.
# unnest(source data directory, destination directory, pointer to parameter
#		 array)
# return folder with step results.
sub unnest
{
	my ($source, $dest, $params) = @_;
	print "\n==== Unnesting coordinations =================================\n";
		
	# Convert.
	LvCorporaTools::TreeTransf::UnnestCoord::processDir($source, $params->{'ord'});
		
	# Move files to correct places.
	move("$source/res", $dest)
		|| warn "Moving $source/res to $dest failed: $!";
	move("$source/warnings", "$dest/warnings")
		|| warn "Moving $source/warnings to $dest/warnings failed: $!";
	my @files = glob("$source/*.m $source/*.w");
	for (@files)
	{
		copy($_, $dest);
	}
		
	return $dest;
}


# Convert to dependencies.
# dep(source data directory, destination directory, pointer to parameter array)
# return folder with step results.
sub dep
{
	my ($source, $dest, $params) = @_;
	print "\n==== Converting to dependencies ==============================\n";
		
	# Set parameters.
	$LvCorporaTools::TreeTransf::Hybrid2Dep::XPRED = $params->{'xpred'}
		if ($params->{'xpred'});
	$LvCorporaTools::TreeTransf::Hybrid2Dep::COORD = $params->{'coord'}
		if ($params->{'coord'});
	$LvCorporaTools::TreeTransf::Hybrid2Dep::PMC = $params->{'pmc'}
		if ($params->{'pmc'});
	$LvCorporaTools::TreeTransf::Hybrid2Dep::LABEL_ROOT = $params->{'root'}
		if (defined $params->{'root'});
	$LvCorporaTools::TreeTransf::Hybrid2Dep::LABEL_PHRASE_DEP = $params->{'phdep'}
		if (defined $params->{'phdep'});
	$LvCorporaTools::TreeTransf::Hybrid2Dep::LABEL_DETAIL_NA = $params->{'na'}
		if (defined $params->{'na'});
	$LvCorporaTools::TreeTransf::Hybrid2Dep::LABEL_SUBROOT = $params->{'subrt'}
		if (defined $params->{'subrt'});
		
	# Convert.
	LvCorporaTools::TreeTransf::Hybrid2Dep::processDir($source, $params->{'ord'});
		
	# Move files to correct places.
	move("$source/res", $dest)
		|| warn "Moving $source/res to $dest failed: $!";
	move("$source/warnings", "$dest/warnings")
		|| warn "Moving $source/warnings to $dest/warnings failed: $!";
	my @files = glob("$source/*.m $source/*.w");
	for (@files)
	{
		copy($_, $dest)
			|| warn "Copying $_ to $dest failed: $!";
	}
		
	return $dest;
}

# Remove reductions.
# red(source data directory, destination directory, pointer to parameter array)
# return folder with step results.
sub red
{
	my ($source, $dest, $params) = @_;
	print "\n==== Removing reductions =====================================\n";
		
	# Set parameters.
	$LvCorporaTools::TreeTransf::RemoveReduction::LABEL_EMPTY = $params->{'label'}
		if (defined $params->{'label'});
		
	# Convert.
	LvCorporaTools::TreeTransf::RemoveReduction::processDir(
		$source, $params->{'ord'});
		
	# Move files to correct places.
	move("$source/res", $dest)
		|| warn "Moving $source/res to $dest failed: $!";
	my @files = glob("$source/*.m $source/*.w");
	for (@files)
	{
		copy($_, $dest)
			|| warn "Copying $_ to $dest failed: $!";
	}
		
	return $dest;
}

# Knit-in.
# knit(source data directory, destination directory, pointer to parameter
#	  array)
# return folder with step results.
sub knit
{
	my ($source, $dest, $params) = @_;
	print "\n==== Knitting-in =============================================\n";
	
	# Set parameters.
	my $schemaDir = $params->{'path'};
	$schemaDir = 'TrEd extension/lv-treebank/resources' unless $schemaDir;
	
	# Convert.
	LvCorporaTools::PMLUtils::Knit::processDir($source, 'a', $schemaDir);
	move("$source/res", $dest)
		|| warn "Moving $source/res to $dest failed: $!";
		
	return $dest;
}

# Convert to CoNLL format.
# conll(source data directory, destination directory, pointer to parameter
#	  array)
# return folder with step results.
sub conll
{
	my ($source, $dest, $params) = @_;
	print "\n==== Converting to CoNLL =====================================\n";
	
	# Set parameters.
	$LvCorporaTools::FormatTransf::DepPml2Conll::CPOSTAG = 
		$params->{'cpostag'} if ($params->{'cpostag'});
	$LvCorporaTools::FormatTransf::DepPml2Conll::POSTAG =
		$params->{'postag'} if ($params->{'postag'});
		
	# Convert.
	LvCorporaTools::FormatTransf::DepPml2Conll::processDir(
		$source, $params->{'label'}, $params->{'conll09'});
	move("$source/res", $dest)
		|| warn "Moving $source/res to $dest failed: $!";
		
	return $dest;
}

# Fold data sets for training.
# fold(source data directory, destination directory, pointer to parameter
#	  array)
# return folder with step results.
sub fold
{
	my ($source, $dest, $params) = @_;
	print "\n==== Folding datasets ========================================\n";
	
	LvCorporaTools::DataSelector::SplitTreebank::splitCorpus(
		$source, $params->{'p'}, $params->{'seed'});
	move("$source/res", $dest)
		|| warn "Moving $source/res to $dest failed: $!";

	return $dest;
	
}

# This ensures that when module is called from shell (and only then!)
# processDir is envoked.
&processDir(@ARGV) unless caller;

1;
