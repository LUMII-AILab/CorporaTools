#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::TagTransformations::TagTransformator;
use utf8;
use strict;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(VeestnesisTagToKamols VeestnesisDirToKamols EsTagToKamols EsDirToKamols);

###############################################################################
# This module performs conversation between different tag formats. Input data
# format must have no more than one morphotag per line delimited with
# <tag>...</tag> (e.g. approprietly indented PML M file).
#
# Supported transformations:
#	Latvijas Vēstnesis->SemTi-Kamols
#	ES baltā grāmata->SemTi-Kamols
#
# Developed on ActivePerl 5.10.1.1007, tested on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2011-2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

use IO::File;
use IO::Dir;

###############################################################################
# Convert a single "Latvijas Vēstnesis" tag to SemTi-Kamols tagset.
# 
# Input parameter: tag.
###############################################################################
sub VeestnesisTagToKamols
{
	my $tag = shift;
	
	# Process verbs.
	if ($tag =~ /^v..[^p].{8}$/i)
	{
		# Remove perfect.
		$tag =~ s/^(v.{5}).(.*)$/$1$2/i;
	}
	
	# Process adjectives.
	if ($tag =~ /^a/i)
	{
		# Degree
		$tag =~ s/^(a.{5})n(.*)$/$1p$2/i;
	}
	
	# Process pronouns.
	if ($tag =~ /^p/i)
	{
		# Type, negation
		$tag =~ s/^(p)z(.{4})(.*)$/$1_$2y$3/i;
		$tag =~ s/^(p[^z].{4})(.*)$/$1n$2/i;
	}

	# Process adverbs.
	if ($tag =~ /^r/i)
	{
		# Degree
		$tag =~ s/^(r)n(.*)$/$1_$2/i;
	}

	# Process numerals.
	if ($tag =~ /^m/i)
	{
		# Defininitness
		$tag =~ s/^(m.{5}).(.*)$/$1$2/i;
	}
	
	return $tag;
}

###############################################################################
# Convert a single "ES baltā grāmata" tag to SemTi-Kamols tagset.
# 
# Input parameter: tag.
###############################################################################
sub EsTagToKamols
{
	my $tag = shift;
	
	# Process /.
	if ($tag =~ /^zl$/i)
	{
	$tag =~ s/^zl$/zx/i;
	}
	
	return $tag;
}

###############################################################################
# Convert tags used in "ES baltā grāmata" corpus to SemTi-Kamols tagset.
# Perform conversation for all files in directory.
# 
# Input parameters: data dir, output dir.
###############################################################################
sub EsDirToKamols
{
	_ioShell(\&EsTagToKamols, shift, shift);
}

###############################################################################
# Convert tags used in "Latvijas Vēstnesis" corpus to SemTi-Kamols tagset.
# Perform conversation for all files in directory.
# 
# Input parameters: data dir, output dir.
###############################################################################
sub VeestnesisDirToKamols
{
	_ioShell(\&VeestnesisTagToKamols, shift, shift);
}

# Helper function: perform I/O operations and call appropriate transformator.
# Input parameters: pointer to transformator funtion, data dir, output dir.
sub _ioShell
{
	if (not @_ or @_ le 2)
	{
		print <<END;
Script for converting tags between varios tagsets in PML M file (or any other
file with no more than one morphotag per line, where each morphotag is denoted
by <tag>...</tag> delimiters).
For supported tagsets see available function calls.

Params:
   input directory
   output directory

Latvian Treebank project, LUMII, 2011-2012, provided under GPL
END
		exit 1;
	}
	my $transFunc = shift;
	my $inDirName = shift; #$ARGV[0] ? $ARGV[0] : "Veestnesis";
	my $outDirName = shift; #$ARGV[1] ? $ARGV[1] : "Veestnesis-dir";

	my $inDir = IO::Dir->new($inDirName) or die "dir $!";
	mkdir $outDirName;
	my $outDir = IO::Dir->new($outDirName);

	while (defined(my $inFile = $inDir->read))
	{
		if (not -d "$inDirName\\$inFile")
		{
			my $in = IO::File->new("$inDirName\\$inFile", "<")
				or die "Could not open file $inFile: $!";
			my $out = IO::File->new("$outDirName\\$inFile", ">")
				or die "Could not open file $inFile: $!";
			
			print "Processing $inFile.\n";
			while (<$in>)
			{
				my $begin = '';
				my $end = $_;
				while($end =~ m#^(.*?<tag>)(.*?)(</tag>.*)$#s)
				{
					$begin = $begin.$1;
					my $tag = $2;
					$end = $3;
					
					$tag = &$transFunc($tag);
					$begin = $begin.$tag;
				}
				print $out "$begin$end";				
			}
			
			$in->close;
			$out->close;
		}
	}
	$inDir->close;
	$outDir->close;
}

1;

