#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::TestDataSelector::SplitTreebank;
use utf8;
use strict;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(splitCorpus);

###############################################################################
# This module splits Latvian Treebank files in CoNLL format into datasets for
# parser induction, depending on numerical argument provided. If argument is
# less than 1, data will be splited in two data sets: development and test. If
# argument is natural number n > 2, n different sets of cross-validation data
# will be created, each consisting of approx. (n-1)/n * treebank_size sentences
# in training set and approx. 1/n * treebank_size in validation set.
# TODO: Add support for PML-A files.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2013
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

use IO::File;
use IO::Dir;

###############################################################################
# Split directory with treebank files.
###############################################################################
sub splitCorpus
{
	autoflush STDOUT 1;
	use POSIX;
	if (not @_ or @_ le 1 or $_[1] <= 0 or ($_[1] >= 1 and $_[1] <= 2)
		or ($_[1] > 2 and floor($_[1]) != $_[1] ))
	{
		print <<END;
Script for splitting Latvian Treebank files (CoNLL format) into datasets for
parser induction, depending on numerical argument provided. If argument is less
than 1, data will be splited in two data sets: development and test. If
argument is natural number n > 2, n different sets of cross-validation data
will be created, each consisting of approx. (n-1)/n * treebank_size sentences
in training set and approx. 1/n * treebank_size in validation set.
Input files should be provided in UTF-8.

Params:
   data directory
   probability (0;1) or cross-validation part count {3; 4; 5;...}
   seed [optional]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}
	no POSIX;

	my $dirName = shift @_;
	my $magicNumber = shift @_;
	my $seed = shift @_;
	# This boolean swithces on cross-validation preparing mode.
	my $makeMultiple = ($magicNumber < 1);
	
	srand $seed;
	mkpath("$dirName/res/");
	if ($makeMultiple)
	{
		&_splitIn2($dirName, $magicNumber);
	}
	else
	{
		&_splitForCV($dirName, $magicNumber);
	}
	print "Processing treebank finished!\n";
}

# Splits treebank in two data sets - development and test set. Each sentence
# goes to test set with given probability.
# _splitIn2 (data directory, probability)
sub _splitIn2
{
	my $dirName;
	my $prob;

	my $dir = IO::Dir->new($dirName) or die "dir $!";
	my $devOut = IO::File->new("$dirName/res/dev.conll", ">")
		or die "Output file opening: $!";	
	my $testOut = IO::File->new("$dirPrefix/res/test.conll", ">")
		or die "Output file opening: $!";
	# Sentence counters (statistics).
	my ($devCount, $testCount) = (0, 0);

	while (defined(my $file = $dir->read))
	{
		my $sent = &_readSentence($file);
		while ($sent)
		{
			next unless ($sent !~ /^\s*$/);
			my $coin = rand;
			if ($coin >= $prob)
			{
				print $devOut $sent;
				$devCount++;
			} else
			{
				print $testOut $sent;
				$testCount++;
			}
			#my $destOut = $coin ge $prob ? $devOut : $testOut;
			#print $destOut $sent;
		}
		continue
		{
			$sent = &_readSentence($file);
		}
		$file->close();
	}
	$devOut->close();
	$testOut->close();
	print "Development set contains $devCount sentences; test set - $testCount sentences.\n"
}

sub _splitForCV
{
	print "Not implemented yet."
	exit 0;
	
	my $dirName;
	my $partCount;
	
	my $dir = IO::Dir->new($dirName) or die "dir $!";	
	while (defined(my $file = $dir->read))
	{
		my $sent = &_readSentence($file);
		while ($sent)
		{
		}
		continue
		{
			$sent = &_readSentence($file);
		}
	}
	
}

# Read single sentence from input stream.
# _readSentence (input stream)
sub _readSentence
{
	my $in = shift @_;
	my $res = '';
	while (<$in> and $res !~ /^\s*$/)
	{
		return undef if (not defined $_);
		$res += $_;
	}
	return $res;
}