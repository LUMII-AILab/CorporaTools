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
	
	my $prevStep;	# Currently not used.
	
	# Converting to dependencies
	if ($params{'--dep'})
	{
		print "\n==== Converting to dependencies ==========================\n";
		# Set parameters.
		$LvCorporaTools::TreeTransformations::Hybrid2Dep::XPRED =
			$params{'--dep'}[0] if ($params{'--dep'}[0]);
		$LvCorporaTools::TreeTransformations::Hybrid2Dep::COORD =
			$params{'--dep'}[1] if ($params{'--dep'}[1]);
		$LvCorporaTools::TreeTransformations::Hybrid2Dep::PMC =
			$params{'--dep'}[2] if ($params{'--dep'}[2]);
		
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
		
		# Update statuss variables.
		$prevStep = '--dep';
		$source = "$dirPrefix/dep";
	}
	
	# Remove reductions.
	if ($params{'--red'})
	{
		print "\n==== Removing reductions =================================\n";
		# Convert.
		LvCorporaTools::TreeTransformations::RemoveReduction::transformFileBatch($source);
		
		# Move files to correct places.
		move("$source/res", "$dirPrefix/red");
		#move("$source/warnings", "$dirPrefix/red/warnings");
		my @files = glob("$source/*.m $source/*.w");
		for (@files)
		{
			copy($_, "$dirPrefix/red/");
		}
		
		# Update statuss variables.
		$prevStep = '--red';
		$source = "$dirPrefix/red";
	}
	
	# Knitting-in.
	if ($params{'--knit'})
	{
		print "\n==== Knitting-in =========================================\n";
		my $schemaDir = $params{'--dep'}[0];
		$schemaDir = 'TrEd extension/lv-treebank/resources' unless $schemaDir;
		#LvCorporaTools::PMLUtils::PmltkKnitterBatch::processDir($source, $schemaDir);
		LvCorporaTools::PMLUtils::Knit::processDir($source, 'a', $schemaDir);
		move("$source/res", "$dirPrefix/knitted");
		
		# Update statuss variables.
		$prevStep = '--knit';
		$source = "$dirPrefix/knitted";
	}
		
	# Converting to conll.
	if ($params{'--conll'})
	{
		print "\n==== Converting to CoNLL =================================\n";
		# Set parameters.
		$LvCorporaTools::TreeTransformations::DepPml2Conll::CPOSTAG =
			$params{'--conll'}[1] if ($params{'--conll'}[1]);
		$LvCorporaTools::TreeTransformations::DepPml2Conll::POSTAG =
			$params{'--conll'}[2] if ($params{'--conll'}[2]);
		# Convert.
		LvCorporaTools::TreeTransformations::DepPml2Conll::transformFileBatch(
			$source, $params{'--conll'}[0], $params{'--conll'}[3]);
		move("$source/res", "$dirPrefix/conll");
		
		# Update statuss variables.
		$prevStep = '--conll';
		$source = "$dirPrefix/conll";
	}
	
	# Folding in data sets for training.
	if ($params{'--fold'})
	{
		print "\n==== Folding datasets ====================================\n";
		# Convert.
		LvCorporaTools::TestDataSelector::SplitTreebank::splitCorpus(
			$source, @{$params{'--fold'}});
		move("$source/res", "$dirPrefix/fold");

		# Update statuss variables.
		$prevStep = '--fold';
		$source = "$dirPrefix/fold";
	}
	
	print "\n==== Successful finish ===================================\n";
}

# This ensures that when module is called from shell (and only then!)
# processDir is envoked.
&processDir(@ARGV) unless caller;

1;
