package LvCorporaTools::DataSelector::SplitTrainDevTest;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(makeTDT);

#use Data::Dumper;
use File::Copy;
use IO::File;
use IO::Dir;

###############################################################################
# This program splits given files into a train, dev and test sets. No splitting
# in the middle of the file is done. File describing TDT split is assumed to be
# UTF-8.
#
# Latvian Treebank project, 2017
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

sub makeTDT
{
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for splitting a data set according to given train-dev-test split.
Malformed TDT lines and lines referencing non-existing files give warnings.
Data folder is checked for files not mentioned in TDT file, but these files do
not give warnings.

Params:
   directory where all data to be split is stored
   file describing TDT split in tab-separated format with following columns
     1) where to put: train/dev/test/skip
     2) PML file set name without extension
        (extensions .w, .m, .a, .pml, .txt, .conll, .conllu and no extension
         will be checked)
     3) corpus type, e.g., 'Morphocorpus'
     4) any other columns will be ignored
   directory name where to put result folders [opt]
   ommit warnings on missing Morphocorpus files [opt, false by default]

Result:
   folders train, dev, test, skip, not-mentioned

Latvian Treebank project, LUMII, 2017, provided under GPL
END
		exit 1;
	}

	my $sourceDirName = shift @_;
	my $tdtFileName = shift @_;
	my $destDirName = (shift @_ or $sourceDirName);
	my $ommitMorphoWarns = shift;

	# Go through TDT file and move mentioned here
	my %usedFiles = ();
	my $tdtFile = IO::File->new("$tdtFileName", "< :encoding(UTF-8)") or die "Can't open $tdtFileName: $!";
	my $sourceDir = IO::Dir->new($sourceDirName) or die "Can't access $sourceDirName: $!";
	while (my $line = <$tdtFile>)
	{
		if ($line =~ /^(train|dev|test|skip)\t([^\s]+)(.*)/)
		{
			my $folder = $1;
			my $fileStub = $2;
			my $otherInfo = $3;
			mkdir("$destDirName/$folder") unless(-d "$destDirName/$folder");
			my @potentialFiles = (
				"$fileStub.w",
				"$fileStub.m",
				"$fileStub.a",
				"$fileStub.pml",
				"$fileStub.txt",
				"$fileStub.conll",
				"$fileStub.conllu",
				"$fileStub");
			my $found = 0;
			for my $fileName (@potentialFiles)
			{
				if (-f "$sourceDirName/$fileName")
				{
					File::Copy::copy  "$sourceDirName/$fileName",  "$destDirName/$folder/$fileName";
					$usedFiles{$fileName} = 1;
					$found++;
				}
			}

			warn "Nothing like $fileStub found " if (!$found and (!$ommitMorphoWarns or $otherInfo !~ /^\tMorphocorpus([\t\r\n]|$)/));
		}
		elsif ($line =~ /^(\s*.+?)\r?\n?$/)
		{
			warn "Malformed line in TDT file \"$1\"";
		}
	}

	#Now check if all files ended up used somewhere
	while (defined(my $fileName = $sourceDir->read))
	{
		if (-f "$sourceDirName/$fileName")
		{
			unless ($usedFiles{$fileName})
			{
				mkdir("$destDirName/not-mentioned") unless(-d "$destDirName/not-mentioned");
				File::Copy::copy  "$sourceDirName/$fileName",  "$destDirName/not-mentioned/$fileName";
			}
		}
	}
}

1;
