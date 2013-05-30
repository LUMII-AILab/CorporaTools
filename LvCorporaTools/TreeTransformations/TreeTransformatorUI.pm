package LvCorporaTools::TreeTransformations::TreeTransformatorUI;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(transfromDir);

use Carp::Always;	# Print stack trace on die.

use File::Copy;
use File::Path;
use IO::Dir;
use IO::File;

use LvCorporaTools::TreeTransformations::Hybrid2Dep;
use LvCorporaTools::TreeTransformations::RemoveReduction;
use LvCorporaTools::PMLUtils::Knit;
use LvCorporaTools::TreeTransformations::DepPml2Conll;
use LvCorporaTools::TestDataSelector::SplitTreebank;

use Data::Dumper;

# TODO: renumuration

sub _printMan
{
	print <<END;
Unified interface for transformation scripts.
Input files should be provided as UTF-8.
Usage
  TreeTransformatorUI --flag1 value11 value12 --flag2 value21 ...
  --dir and at least one processing step is mandatory
  
Params:
     --dir       input directory (single value)
	 --collect   collect all .w + .m + .a from data directory
     --dep       convert to dependencies (values: (*) x-Pred mode, (*) Coord
                 mode, (*) PMC mode)
     --red       remove reductions (no values)
     --knit      convert .w + .m + .a to a single .pml file (value: directory
                 of PML schemas)
     --conll     convert .pml to conll (values: (*) label output tree arcs
                 [0/1], (*) CPOSTAG mode [purify/first/none], (*) POSTAG mode
                 [full/purify], (*) is "large" CoNLL-2009 output needed)
     --fold      create development/test or cross-validation datasets (values:
                 (*) probability (0;1), or cross-validation part count
                 {3; 4; 5;...}, or 1 for concatenating all files, (*) seed)
				 
Latvian Treebank project, LUMII, 2013, provided under GPL
END
}

sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 3)
	{
		#print 1;
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
			#print 2;
			&_printMan;
			exit 1;
		}
	}
	
	# Get directories.
	unless ($params{'--dir'} or @{$params{'--dir'}} > 1)
	{
		#print 3;
		&_printMan;
		exit 1;
	}
	my $dirPrefix = $params{'--dir'}[0];
	my $source = $params{'--dir'}[0];
		
	# Collecting data recursively.
	if ($params{'--collect'})
	{
		$source = &_collect($source, $dirPrefix);
		$dirPrefix = $source;
	}
	
	# Converting to dependencies.
	$source = &_dep($source, $dirPrefix, $params{'--dep'})
		if ($params{'--dep'});
	
	# Removing reductions.
	$source = &_red($source, $dirPrefix)
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
	
	print "\n==== Successful finish ===================================\n";
}

# Collect data recursively.
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
			elsif (-d "$current/$item" and $item !~ /^\.\.?$/)
			{
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

# Convert to dependencies.
# return adress to step results.
sub _dep
{
	my ($source, $dirPrefix, $params) = @_;
	print "\n==== Converting to dependencies ==========================\n";
		
	# Set parameters.
	$LvCorporaTools::TreeTransformations::Hybrid2Dep::XPRED = $params->[0]
		if ($params->[0]);
	$LvCorporaTools::TreeTransformations::Hybrid2Dep::COORD = $params->[1]
		if ($params->[1]);
	$LvCorporaTools::TreeTransformations::Hybrid2Dep::PMC = $params->[2]
		if ($params->[2]);
		
	# Convert.
	LvCorporaTools::TreeTransformations::Hybrid2Dep::transformFileBatch($source);
		
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
	my ($source, $dirPrefix) = @_;
	print "\n==== Removing reductions =================================\n";
		
	# Convert.
	LvCorporaTools::TreeTransformations::RemoveReduction::transformFileBatch($source);
		
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
	print "\n==== Knitting-in =========================================\n";
	
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
	print "\n==== Converting to CoNLL =================================\n";
	
	# Set parameters.
	$LvCorporaTools::TreeTransformations::DepPml2Conll::CPOSTAG = 
		$params->[1] if ($params->[1]);
	$LvCorporaTools::TreeTransformations::DepPml2Conll::POSTAG =
		$params->[2] if ($params->[2]);
		
	# Convert.
	LvCorporaTools::TreeTransformations::DepPml2Conll::transformFileBatch(
		$source, $params->[0], $params->[3]);
	move("$source/res", "$dirPrefix/conll");
		
	return "$dirPrefix/conll";
}

# Fold data sets for training.
# return adress to step results.
sub _fold
{
	my ($source, $dirPrefix, $params) = @_;
	print "\n==== Folding datasets ====================================\n";
	
	LvCorporaTools::TestDataSelector::SplitTreebank::splitCorpus(
		$source, @{$params});
	move("$source/res", "$dirPrefix/fold");

	return "$dirPrefix/fold";
	
}

# This ensures that when module is called from shell (and only then!)
# processDir is envoked.
&processDir(@ARGV) unless caller;

1;
