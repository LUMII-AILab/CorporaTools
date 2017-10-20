#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::PMLUtils::CheckW;

use strict;
use warnings;
use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(checkW processDir);

#use Data::Dumper;
use IO::Dir;
use IO::File;
use File::Path;
use XML::Simple;  # XML handling library
use LvCorporaTools::GenericUtils::SimpleXmlIo qw(loadXml printXml);
use LvCorporaTools::GenericUtils::UIWrapper;

###############################################################################
# This programm checks PML W file agaist the original .txt file. Spaces and
# paragraph placement are adjusted automaticaly. If one of the files contains
# some alphanumeric sequence the other one does not, fatal error arrises.
#
# Input files - utf8.
# Output file can have diferent XML element order. To obtain standard order
# resave file with TrEd.
#
# Developed on ActivePerl 5.10.1.1007, tested on Strawberry Perl 5.12.3.0
# Latvian Treebank project, Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# 2011-12
# Licenced under GPL.
###############################################################################

# Perform error-chacking in multiple files. This can be used as entry point, if
# this module is used standalone.
sub processDir
{
	if (not @_ or @_ < 1)
	{
		print <<END;
Script verifies .w files against original plain-text and adds mising spaces
and paragraph borders, if necessary. Adds paragraph IDs, if there is none.
Input files should be provided as UTF-8.

Params:
   data directory
Returns:
   count of failed files

Latvian Treebank project, LUMII, 2017, provided under GPL
END
		exit 1;
	}
	my $problems = LvCorporaTools::GenericUtils::UIWrapper::processDir(
		\&checkW, "^.+\\.w\$", '', 0, 0, @_);
	if ($problems)
	{
		print "$problems files failed.\n";
	}
	else
	{
		print "All finished.\n";
	}
	return $problems;
}

# Perform error-chacking in single file. This can be used as entry point, if
# this module is used standalone.
sub checkW
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 2)
	{
		print <<END;
Script verifies .w file against original plain-text and adds mising spaces and
paragraph borders, if necessary. Adds paragraph IDs, if there is none. Input
files should be provided as UTF-8.

Params:
   directory prefix
   .w file
   plain text file [opt, file_name.txt used otherwise]

Latvian Treebank project, LUMII, 2011, provided under GPL
END
		exit 1;
	}
	
	# Input paramaters. 
	my $dirPrefix = shift @_;
	my $pml = shift @_;
	my $txt = shift @_;
	unless ($txt)
	{
		$txt = $pml =~ /^(.+)\.w$/ ? "$1.txt" : "$pml.txt";
	}

	# Statistics.
	my $addedSpaces = 0;
	my $deletedSpaces = 0;
	my $movedPara = 0;
	my $addedParaID = 0;

	mkpath("$dirPrefix/res/");
	my $logFile = IO::File->new("$dirPrefix/res/$pml-log.txt", ">") or die "Can't create $pml-log.txt: $!";
	
	my $txtIn = IO::File->new("$dirPrefix/$txt", "< :encoding(UTF-8)") or die "Can't read text file $txt: $!";
	my $wXml = loadXml ("$dirPrefix/$pml", ['para', 'w', 'schema', 'title', 'source', 'author', 'authorgender', 'published', 'genre', 'keywords', 'msc']);
	my $lvwdata = $wXml->{'xml'};
	my $docId = $lvwdata->{'doc'}->{'id'};

	my $currentPara = 0;
	# For each paragraph in the incoming text...
	while (my $line = <$txtIn>)
	{
		#$line = encode_utf8($line);
		my $tokenId = 0;
		while ($line !~ /^\s*$/)
		{
			$line =~ s/^\s*(.*?)\s*$/$1 /; # Remove leading and trailing spaces.
			# Process current xml paragraph.
			while($tokenId < +@{$lvwdata->{'doc'}->{'para'}[$currentPara]->{'w'}})
			{
				unless ($lvwdata->{'doc'}->{'para'}[$currentPara]->{'id'})
				{
					$lvwdata->{'doc'}->{'para'}[$currentPara]->{'id'} = "w-$docId-p".($currentPara+1);
					$addedParaID++;
				}
				my $wElem = @{$lvwdata->{'doc'}->{'para'}[$currentPara]->{'w'}}[$tokenId];
				# Text paragraph ends, xml paragraph continues.
				if ($line =~ /^\s*$/)
				{
					my $curParW = $lvwdata->{'doc'}->{'para'}[$currentPara]->{'w'};
					
					my @newPara = @$curParW[$tokenId..@$curParW-1];
					splice @{$curParW}, $tokenId, @$curParW - $tokenId;
					# Insert new paragraph in xml tree.
					unshift @{$lvwdata->{'doc'}->{'para'}[$currentPara + 1]->{'w'}}, @newPara;
					$movedPara++;
					 #Finish porcesing current xml paragraph, as there is nothing left to process.
					last;
				}
				my $token = $wElem->{'token'}->{'content'};
				
				print $logFile "No such token $wElem->{'id'}:\"$token\" in \"$line\" āčēģīķļņōŗšūž" and
				die "Error while processing $pml: no such token \"$token\" in \"$line\""
					if ($line !~ /^\Q$token\E(.*)$/);
				$line =~ s/^\Q$token\E(.*)$/$1/;
				
				# Analize space after token.
				if ($line =~ /^\s+/)
				{
					no warnings;
					if ($wElem->{'no_space_after'} and
						$wElem->{'no_space_after'}->{'content'} eq 1)
					{
						$wElem->{'no_space_after'} = undef;
						$deletedSpaces++;
					}
				} elsif (not $wElem->{'no_space_after'}->{'content'})
				{
						$wElem->{'no_space_after'}->{'content'} = 1;
						$addedSpaces++;
				}
				$line =~ s/^\s*(.*)$/$1/;
				
			} continue
			{
				$tokenId++;
			}
			
			# XML paragraph ends, text paragraph continues.
			if ($line !~ /^\s*$/)
			{
				my @tmp = @{$lvwdata->{'doc'}->{'para'}[$currentPara + 1]->{'w'}};
				push(@{$lvwdata->{'doc'}->{'para'}[$currentPara]->{'w'}}, @tmp);
				splice @{$lvwdata->{'doc'}->{'para'}}, $currentPara + 1, 1;
				#$currentPara++;
				$movedPara++;
			}
			else
			{
				$currentPara++;
			}
		}
	}

	# Close text file.
	$txtIn->close();

	# Print result in new PML file.
	printXml ("$dirPrefix/res/$pml", $wXml->{'handler'},
		$wXml->{'xml'}, 'lvwdata', $wXml->{'header'});

	# Print statistics on the screen.
	print $logFile "Added $addedSpaces spaces.\n";
	print $logFile "Deleted $deletedSpaces spaces.\n";
	print $logFile "Moved $movedPara paragraphs.\n";
	print $logFile "Added $addedParaID paragraph IDs.\n";
	print "CheckW has finished procesing \"$pml\".\n";

	$logFile->close();
	return 0;
}

1;
