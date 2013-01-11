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

use File::Path;
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
	my $dirName = shift @_;
	my $prob = shift @_;

	# Open output files.
	my $dir = IO::Dir->new($dirName) or die "dir $dirName $!";
	my $devOut = IO::File->new("$dirName/res/dev.conll", "> :encoding(UTF-8)")
		or die "Output file opening: $!";	
	my $testOut = IO::File->new("$dirName/res/test.conll", "> :encoding(UTF-8)")
		or die "Output file opening: $!";
	# Sentence counters (statistics).
	my ($devCount, $testCount) = (0, 0);

	#Process data.
	while (defined(my $file = $dir->read))
	{
		if (! -d "$dirName/$file")
		{
			my $in = IO::File->new("$dirName/$file", "< :encoding(UTF-8)")
				or die "Input file opening: $!";
			my $sent = &_readSentence($in);
			while (defined $sent)
			{
				next if ($sent =~ /^\s*$/);
				my $coin = rand;
				if ($coin >= $prob)
				{
					print $devOut "$sent\n";
					$devCount++;
				} else
				{
					print $testOut "$sent\n";
					$testCount++;
				}
				#my $destOut = $coin ge $prob ? $devOut : $testOut;
				#print $destOut $sent;
			}
			continue
			{
				$sent = &_readSentence($in);
			}
			$in->close();
		}
	}
	
	#Close output files.
	$devOut->close();
	$testOut->close();
	print "Development set contains $devCount sentences; test set - $testCount sentences.\n"
}
# Splits treebank for N-fold cross-validation.
# _splitForCV (data directory, part count N)
sub _splitForCV
{
	my $dirName = shift @_;
	my $partCount = shift @_;
	
	my @outputs;
	my @stats;
	# Initialization for otput files and stats counters.
	for (my $i = 0; $i < $partCount; $i++)
	{
		$outputs[$i][0] = IO::File->new("$dirName/res/train$i.conll", "> :encoding(UTF-8)")
			or die "Output file opening: $!";
		$outputs[$i][1] = IO::File->new("$dirName/res/val$i.conll", "> :encoding(UTF-8)")
			or die "Output file opening: $!";
		$stats[$i][0] = 0;
		$stats[$i][1] = 0;
	}
	
	# Process data.
	my $dir = IO::Dir->new($dirName) or die "dir $dirName $!";	
	while (defined(my $file = $dir->read))
	{
		if (! -d "$dirName/$file")
		{
			my $in = IO::File->new("$dirName/$file", "< :encoding(UTF-8)")
				or die "Input file opening: $!";
			my $sent = &_readSentence($in);
			while (defined $sent)
			{
				next if ($sent =~ /^\s*$/);
				my $coin = rand;
				for (my $i = 0; $i < $partCount; $i++)
				{
					if ($coin >= $i /$partCount and $coin < ($i + 1) /$partCount)
					{
						my $tmp = $outputs[$i][1];
						print $tmp "$sent\n";
						$stats[$i][1]++;
					}
					else
					{
						my $tmp = $outputs[$i][0];
						print $tmp "$sent\n";
						$stats[$i][0]++;
					}
				}
			}
			continue
			{
				$sent = &_readSentence($in);
			}
			$in->close();
		}
	}
	
	#Close output files and print statistics.
	print "Statistics about created data sets:\n";
	for (my $i = 0; $i < $partCount; $i++)
	{
		$outputs[$i][0]->close();
		$outputs[$i][1]->close();
		print "  $i. Training set: $stats[$i][0] sentences, validation set: $stats[$i][1].\n"
	}
}

# Read single sentence from input stream.
# _readSentence (input stream)
# FIXME
sub _readSentence
{
	my $in = shift @_;
	my $res = '';
	while (1)	#my $rinda = <$in> and $res !~ /^\s*$/)
	{
		my $rinda = <$in>;
		return undef if (not defined $rinda and $res =~/^\s*$/);
		return $res if ($rinda =~/^\s*$/);
		$res .= $rinda;
	}
	undef;
}