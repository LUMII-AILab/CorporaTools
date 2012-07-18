#!C:\strawberry\perl\bin\perl -w
package LvTreeBank::Utils::Unite;

use strict;
use warnings;

use Exporter();
our @EXPORT_OK = qw(unite);

use LvTreeBank::Utils::NormalizeIds;# qw(load process doOutput);

use IO::File;
use IO::Dir;
use Data::Dumper;
#print Dumper(qw (a b c));

###############################################################################
# This program unite multiple PML format files. Header from first file is used.
#
# Input files - utf8.
# Output file can have diferent XML element order. To obtain standard order
# resave file with TrEd.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
sub unite
{
	if (@_ le 1)
	{
		print <<END;
Script for uniting multiple PML dataset. To do this, IDs are recalculated if
necessary. Input files should be provided as UTF-8.

Params:
   directory prefix
   new file name
   ID of the first paragraph [opt, int, 1 used otherwise]
   ID of the first sentence [opt, int, 1 used otherwise]
   ID of the first token [opt, int, 1 used otherwise]

Latvian Treebank project, LUMII, 2012, provided under GPL
END
		exit 1;
	}
	
	my $dirName = shift @ARGV;
	my $fileName = shift @ARGV;
	
	my $firstPara = (shift @_ or 1);
	my $firstSent = (shift @_ or 1);
	my $firstWord = (shift @_ or 1);

	my $dir = IO::Dir->new($dirName) or die "dir $!";
	
	my $xmlData;
	my $isFirstFile = 1;
	
	while (defined(my $inFile = $dir->read))
	{
		if ((! -d $inFile) and ($inFile =~ /^(.+)\.w$/))
		{
			my $id = $1;
			my $xmls = LvTreeBank::Utils::NormalizeIds::load ($dirName, $id, $fileName);
			my $res = LvTreeBank::Utils::NormalizeIds::process (
				$fileName, $xmls->{'w'}->{'xml'}, $xmls->{'m'}->{'xml'},
				$xmls->{'a'}->{'xml'}, $firstPara, $firstSent, $firstWord);
				
			if ($isFirstFile)
			{
				$xmlData = $xmls;
				$isFirstFile = 0;
			} else
			{
				# Unite w.
				#print Dumper(@{$res->{'w'}->{'doc'}->{'para'}});
				push @{$xmlData->{'w'}->{'xml'}->{'doc'}->{'para'}},
					@{$res->{'w'}->{'doc'}->{'para'}};
				# Unite m.
				push @{$xmlData->{'m'}->{'xml'}->{'s'}}, @{$res->{'m'}->{'s'}};
				# Unite a.
				push @{$xmlData->{'a'}->{'xml'}->{'trees'}->{'LM'}},
					@{$res->{'a'}->{'trees'}->{'LM'}};
			}
			
			$firstPara = $res->{'nextPara'};
			$firstSent = $res->{'nextSent'};
			$firstWord = 1;
			print "$id loaded!\n";
		}
	}
	
	LvTreeBank::Utils::NormalizeIds::doOutput $dirName, $fileName, $xmlData;

}

1;