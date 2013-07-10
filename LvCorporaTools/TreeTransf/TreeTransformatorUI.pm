package LvCorporaTools::TreeTransf::TreeTransformatorUI;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processDir);

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

use Data::Dumper;

###############################################################################
# Interface for Latvian Treebank PML transformations. See documentation in
# &_printMan().
# Invocation example for Windows:
# perl LvCorporaTools/TreeTransf/TreeTransformatorUI.pm --dir data --collect --dep BASELEM ROW BASELEM 0 0 0 --red 0 --knit --conll 1 FIRST FULL 0 --fold 1
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
Usage
  TreeTransformatorUI --flag1 value11 value12 --flag2 value21 ...
  --dir and at least one processing step is mandatory
  
Params:
  General
    --dir      input directory (single value)

  Preprocessing
    --collect  collect all .w + .m + .a from input folder and it's
               folder - use this if data files are given in some subfolder
               structure (no values)

  Main flow
    --coord    un-nest coordinations so that 
    --dep      convert to dependencies
               args: (*) x-Pred mode [BASELEM / DEFAULT (default)]
                     (*) Coord mode [ROW / DEFAULT (default)]
                     (*) PMC mode [BASELEM / DEFAULT (default)]
                     (*) label root node with distinct label [0 / 1 (default)]
                     (*) allow 'N/A' to be part of longer labels
                         [0 (default) / 1]
                     (*) input data have all nodes ordered [0 / 1 (default)]
               no later than before: --knit
    --red      remove reductions
               args: (*) label ommisions of empty nodes [0 / 1 (default)]
               pre-req: --dep
               no later than before: --knit
    --knit     convert .w + .m + .a to a single .pml file
               args: (*) directory of PML schemas [default =
                     'TrEd extension/lv-treebank/resources']
               no later than before: --conll
    --conll    convert .pml to conll
               args: (*) label output tree arcs [0/1]
                     (*) CPOSTAG mode [PURIFY / FIRST / NONE (default)]
                     (*) POSTAG mode [FULL (default) / PURIFY]
                     (*) is "large" CoNLL-2009 output needed [0 (default) / 1]
               pre-req: --knit
               no later than before: --fold
    --fold     create development/test or cross-validation datasets
               args: (*) probability (0;1), or cross-validation part count {3; 4;
                         {3; 4; 5; ...}, or 1 for concatenating all files
                     (*) seed [default = nothing pased to srand]
               pre-req: --conll

  Additional stand-alone transformations
    --ord      recalculate 'ord' fields
               args: (*) reordering mode[TOKEN/NODE]
               no later than before: --knit

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

	# Parse parameters.
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
	
	# Get directories.
	if (not $params{'--dir'} or @{$params{'--dir'}} > 1)
	{
		&_printMan;
		exit 1;
	}
	my $dirPrefix = $params{'--dir'}[0];
	my $source = $params{'--dir'}[0];
		
	# Collecting data recursively.
	if ($params{'--collect'})
	{
		$source = &_collect($source, $dirPrefix);
		#$dirPrefix = $source;
	}
	
	# Recalculating ord fields.
	$source = &_ord($source, $dirPrefix, $params{'--ord'})
		if ($params{'--ord'});
	
	
	# Converting to dependencies.
	$source = &_dep($source, $dirPrefix, $params{'--dep'})
		if ($params{'--dep'});
	
	# Removing reductions.
	$source = &_red($source, $dirPrefix, $params{'--red'})
		if ($params{'--red'});
	
	# Knitting-in.
	$source = &_knit($source, $dirPrefix, $params{'--knit'})
		if ($params{'--knit'});
		
	# Converting to CoNLL.
	$source = &_conll($source, $dirPrefix, $params{'--conll'})
		if ($params{'--conll'});
	
	# Folding data sets for training.
	$source = &_fold($source, $dirPrefix, $params{'--fold'})
		if ($params{'--fold'});
	
	print "\n==== Successful finish =======================================\n";
}

# Collect data recursively.
# _collect(source data directory, global working directory)
# return adress to step results.
sub _collect
{
	my ($source, $dirPrefix) = @_;
	print "\n==== Recursive data collecting ===========================\n";
		
	my $fileCounter = 0;
	my @todoDirs = ();
	my $current = $source;
	mkpath ("$dirPrefix/collected");
		
	# Traverse subdirectories.
	while ($current)
	{
		my $dir = IO::Dir->new($current) or die "Can't open folder $!";
		while (defined(my $item = $dir->read))
		{
			# Treebank file
			if ((-f "$current/$item") and ($item =~ /.[amw]$/))
			{
				copy("$current/$item", "$dirPrefix/collected/");
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
		return "$dirPrefix/collected";
}

# Recalculate ord fields.
# return adress to step results.
sub _ord
{
	my ($source, $dirPrefix, $params) = @_;
	print "\n==== Recalculating ord fields ================================\n";
	die "Invalid argument ".$params->[0]." for --ord $!"
		if ($params->[0] ne 'TOKEN' and $params->[0] ne 'NODE');
	
	my $dir = IO::Dir->new($source) or die "dir $!";
	mkpath("$dirPrefix/ord");
	
	# Process each file.
	while (defined(my $inFile = $dir->read))
	{
		if (-f "$source/$inFile" and ($inFile =~ /^(.+)\.a$/))
		{
			my $newName = "$1-ord\.a";
			my $parser = XML::LibXML->new('no_blanks' => 1);
			my $doc = $parser->parse_file("$source/$inFile");
			my $xpc = XML::LibXML::XPathContext->new();
			$xpc->registerNs('pml', 'http://ufal.mff.cuni.cz/pdt/pml/');
			$xpc->registerNs('fn', 'http://www.w3.org/2005/xpath-functions/');
	
			# Process each tree.
			foreach my $tree ($xpc->findnodes('//pml:trees/pml:LM', $doc))
			{
				$params->[0] eq 'NODE' ?
					LvCorporaTools::PMLUtils::AUtils::renumberNodes($xpc, $tree):
					LvCorporaTools::PMLUtils::AUtils::renumberTokens($xpc, $tree);
					
			}
			my $outFile = IO::File->new("$dirPrefix/ord/$newName", ">")
				or die "Output file opening: $!";
			print $outFile $doc->toString(1);
			$outFile->close();
			print "Processing $inFile finished!\n";
		}
	}
	
	# Add .m and .w files.
	my @files = glob("$source/*.m $source/*.w");
	for (@files)
	{
		copy($_, "$dirPrefix/ord/");
	}
	return "$dirPrefix/ord";
}

# Convert to dependencies.
# return adress to step results.
sub _dep
{
	my ($source, $dirPrefix, $params) = @_;
	print "\n==== Converting to dependencies ==============================\n";
		
	# Set parameters.
	$LvCorporaTools::TreeTransf::Hybrid2Dep::XPRED = $params->[0]
		if ($params->[0]);
	$LvCorporaTools::TreeTransf::Hybrid2Dep::COORD = $params->[1]
		if ($params->[1]);
	$LvCorporaTools::TreeTransf::Hybrid2Dep::PMC = $params->[2]
		if ($params->[2]);
	$LvCorporaTools::TreeTransf::Hybrid2Dep::LABEL_ROOT = $params->[3]
		if (defined $params->[3]);
	$LvCorporaTools::TreeTransf::Hybrid2Dep::LABEL_DETAIL_NA = $params->[4]
		if (defined $params->[4]);
		
	# Convert.
	LvCorporaTools::TreeTransf::Hybrid2Dep::processDir($source, $params->[5]);
		
	# Move files to correct places.
	move("$source/res", "$dirPrefix/dep");
	move("$source/warnings", "$dirPrefix/dep/warnings");
	my @files = glob("$source/*.m $source/*.w");
	for (@files)
	{
		copy($_, "$dirPrefix/dep/");
	}
		
	return "$dirPrefix/dep";
}

# Remove reductions.
# return adress to step results.
sub _red
{
	my ($source, $dirPrefix, $params) = @_;
	print "\n==== Removing reductions =====================================\n";
		
	# Set parameters.
	$LvCorporaTools::TreeTransf::RemoveReduction::LABEL_EMPTY = $params->[0]
		if (defined $params->[0]);
		
	# Convert.
	LvCorporaTools::TreeTransf::RemoveReduction::processDir($source);
		
	# Move files to correct places.
	move("$source/res", "$dirPrefix/red");
	my @files = glob("$source/*.m $source/*.w");
	for (@files)
	{
		copy($_, "$dirPrefix/red/");
	}
		
	return "$dirPrefix/red";
}

# Knit-in.
# return adress to step results.
sub _knit
{
	my ($source, $dirPrefix, $params) = @_;
	print "\n==== Knitting-in =============================================\n";
	
	# Set parameters.
	my $schemaDir = $params->[0];
	$schemaDir = 'TrEd extension/lv-treebank/resources' unless $schemaDir;
	
	# Convert.
	LvCorporaTools::PMLUtils::Knit::processDir($source, 'a', $schemaDir);
	move("$source/res", "$dirPrefix/knitted");
		
	return "$dirPrefix/knitted";
}

# Convert to CoNLL format.
# return adress to step results.
sub _conll
{
	my ($source, $dirPrefix, $params) = @_;
	print "\n==== Converting to CoNLL =====================================\n";
	
	# Set parameters.
	$LvCorporaTools::FormatTransf::DepPml2Conll::CPOSTAG = 
		$params->[1] if ($params->[1]);
	$LvCorporaTools::FormatTransf::DepPml2Conll::POSTAG =
		$params->[2] if ($params->[2]);
		
	# Convert.
	LvCorporaTools::FormatTransf::DepPml2Conll::processDir(
		$source, $params->[0], $params->[3]);
	move("$source/res", "$dirPrefix/conll");
		
	return "$dirPrefix/conll";
}

# Fold data sets for training.
# return adress to step results.
sub _fold
{
	my ($source, $dirPrefix, $params) = @_;
	print "\n==== Folding datasets ========================================\n";
	
	LvCorporaTools::DataSelector::SplitTreebank::splitCorpus(
		$source, @{$params});
	move("$source/res", "$dirPrefix/fold");

	return "$dirPrefix/fold";
	
}

# This ensures that when module is called from shell (and only then!)
# processDir is envoked.
&processDir(@ARGV) unless caller;

1;
