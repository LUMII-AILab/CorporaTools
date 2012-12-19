#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::PMLUtils::CheckW;

use strict;
use warnings;
use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(checkW);

#use Data::Dumper;
use XML::Simple;  # XML handling library
use IO::File;
use File::Path;

###############################################################################
# This programm checks PML W file agaist the original TXT file. Spaces and
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


sub checkW
{
	#my $errFile = IO::File->new('err.txt', ">");
	autoflush STDOUT 1;
	if (not @_ or @_ le 1)
	{
		print <<END;
Script verifies .w file against original plain-text and adds mising spaces and
paragraph borders, if necessary. Input files should be provided as UTF-8.

Params:
   directory prefix
   .w file
   plain text file [opt, "original.txt" used otherwise]

Latvian Treebank project, LUMII, 2011, provided under GPL
END
		exit 1;
	}
	
	# Input paramaters. 
	my $dirPrefix = shift @_;
	my $pml = shift @_;
	my $txt = (shift @_ or 'original.txt');
	
	# Statistics.
	my $addedSpaces = 0;
	my $deletedSpaces = 0;
	my $movedPara = 0;

	mkpath("$dirPrefix/res/");
	my $errFile = IO::File->new("$dirPrefix/res/$pml-errors.txt", ">") or die "$pml-errors.txt: $!";
	
	my $txtIn = IO::File->new("$dirPrefix/$txt", "< :encoding(UTF-8)") or die "TXT file $txt: $!";
	my $wIn = IO::File->new("$dirPrefix/$pml", "< :encoding(UTF-8)") or die "W file $pml: $!";
	my $xmlString = join '', <$wIn>;
	$xmlString =~ /^\s*(\Q<?\E.*?\Q?>\E)/;
	my $xmlHeader = $1;
	#$xmlString = encode_utf8($xmlString);
	#print $xmlString;
	my $xmlW = XML::Simple->new();
	my $lvwdata = $xmlW->XMLin(
		$xmlString,
		'KeyAttr' => [],
		'ForceArray' => ['para', 'w', 'schema'],
		'ForceContent' => 1);

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
				my $wElem = @{$lvwdata->{'doc'}->{'para'}[$currentPara]->{'w'}}[$tokenId];
				# Text paragraph ends, xml paragraph continues.
				if ($line =~ /^\s*$/)
				{
					my $curParW = $lvwdata->{'doc'}->{'para'}[$currentPara]->{'w'};
					
					my @newPara = @$curParW[$tokenId..@$curParW-1];
					splice @{$curParW}, $tokenId, @$curParW - $tokenId;
					# Insert new paragraph in xml tree.
					#splice (@{$lvwdata->{'doc'}->{'para'}}, $currentPara + 1, 0, {'w' => @newPara});
					unshift @{$lvwdata->{'doc'}->{'para'}[$currentPara + 1]->{'w'}}, @newPara;
					#$currentPara++;
					$movedPara++;
					 #Finish porcesing current xml paragraph, as there is nothing left to process.
					last;
				}
				my $token = $wElem->{'token'}->{'content'};
				
				print $errFile "No such token $wElem->{'id'}:\"$token\" in \"$line\" āčēģīķļņōŗšūž" and
				die "No such token \"$token\" in \"$line\""
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

	# Close files.
	$txtIn->close();
	$wIn->close();

	# Print result in new PML file.
	my $wOut = IO::File->new("$dirPrefix/res/$pml", "> :encoding(UTF-8)") or die "w file $pml: $!";
	my $xmlResult = $xmlW->XMLout($lvwdata,
		'AttrIndent' => 1,
		'RootName' => 'lvwdata',
		'OutputFile' => $wOut,
		'SuppressEmpty' => 1,
		'NoSort' => 1,
		'XMLDecl' => $xmlHeader,
		'NoEscape' => 1
		);
	$wOut->close();

	# Print statistics on the screen.
	print "Added $addedSpaces spaces.\n";
	print "Deleted $deletedSpaces spaces.\n";
	print "Moved $movedPara paragraphs.\n";
	print "CheckW has finished.\n";

	$errFile->close();
}

1;
