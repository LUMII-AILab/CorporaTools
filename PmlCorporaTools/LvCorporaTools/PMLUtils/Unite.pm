#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::PMLUtils::Unite;

use strict;
use warnings;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(unite);

use LvCorporaTools::PMLUtils::NormalizeIds qw(load process doOutput);

use IO::File;
use IO::Dir;
use Data::Dumper;

###############################################################################
# This program unite multiple PML format files. Header from first file is used.
#
# Input files - utf8.
# Output file can have diferent XML element order. To obtain standard order
# resave file with TrEd.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012-2017
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
sub unite
{
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for uniting multiple PML dataset. To do this, IDs are recalculated if
necessary. Files are united in their alphanumerical order. Input files should be
provided as UTF-8.

Params:
   directory where all data to be concatenated is stored
   new file name stub
   new source id [opt, file name used otherwise]
   ID of the first paragraph [opt, int, 1 used otherwise]
   ID of the first sentence [opt, int, 1 used otherwise]
   ID of the first token [opt, int, 1 used otherwise]

Latvian Treebank project, LUMII, 2012-2017, provided under GPL
END
		exit 1;
	}
	
	my $dirName = shift @_;
	my $fileName = shift @_;
	my $source_id = (shift @_ or $fileName);
	my $firstPara = (shift @_ or 1);
	my $firstSent = (shift @_ or 1);
	my $firstWord = (shift @_ or 1);

	my $dir = IO::Dir->new($dirName) or die "Could not use folder $dirName $!";
	
	my $xmlData;
	my $isFirstFile = 1;
	
	while (defined(my $inFile = $dir->read))
	{
		if ((! -d "$dirName\$inFile") and ($inFile =~ /^(.+)\.w$/))
		{
			my $doc_id = $1;
			my $xmls = load ($dirName, $doc_id);
			my $res = process (
				$fileName, $source_id, $xmls->{'w'}->{'xml'}, $xmls->{'m'}->{'xml'},
				$xmls->{'a'}->{'xml'}, $firstPara, $firstSent, $firstWord);
				
			if ($isFirstFile)
			{
				$xmlData = $xmls;
				$isFirstFile = 0;
			} else
			{
				# Unite w.
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
			print "$doc_id loaded!\n";
		}
	}
	
	doOutput($dirName, $fileName, $xmlData);

}

1;