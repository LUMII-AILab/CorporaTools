#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::TagTransformations::Tag2FeatureList;
use utf8;
use strict;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(parseTagSet decodeTag);

use Data::Dumper;
use IO::File;
use XML::Simple;

###############################################################################
# This module contains functionality for decoding SemTi-Kamols morphological
# tags to feature-value list.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2013
# Lauma Pretkalniòa, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

# parseTagSet (tagset file(optional))
# Parse given TagSet.xml to hashmap in form:
# {
#	0 =>
#	{
#		NAME => Name of 0th tag
#		Tag letter => Category name
#		Tag letter => Category name
#		...
#	}
#	Category name =>
#	{
#		Position in tag =>
#		{
#			NAME => Attribute name
#			Tag letter => Value name	
#			Tag letter => Value name	
#			...
#		}
#	}
#	Category name =>
#	{
#		...
#	}
#	...
# }
# Note that this hashmap contains aditional category "Divdabis".
# Function returns name of 0th tag and reference to this hashmap.
sub parseTagSet
{
	my $inFile = shift;
	
	# Default TagSet.xml is in the same directory where this script.
	use File::Basename;
	my $dirname = dirname(__FILE__);
	$inFile = "$dirname/TagSet.xml" unless $inFile;
	no File::Basename;
	
	# Read file.
	my $in = IO::File->new($inFile, "< :encoding(UTF-8)")
		or die "Could not open file $inFile: $!";
	my $xmlString = join '', <$in>;
	my $sxml = XML::Simple->new();
	my $data = $sxml->XMLin(
		$xmlString,
		'KeyAttr' => ['Tag'],
		'ForceContent' => 1,
		);
	$in->close();
	
	#print Dumper($data);
	my $res = {};
	# POS tags (position 0).
	my $tmpPos = $data->{'Attribute'}[0];
	$res->{0}->{'NAME'} = $tmpPos->{'LV'};
	#my $posName = $tmpPos->{'LV'};
	for my $tag (keys %{$tmpPos->{'Value'}})
	{
		$res->{0}->{$tag} = $tmpPos->{'Value'}->{$tag}->{'LV'};
	}
	# Other tags.
	for my $attr (@{$data->{'Attribute'}})
	{
		if ($attr->{'PartOfSpeech'} and $attr->{'MarkupPos'} > 0)
		{
			my $new = {};
			$new->{'NAME'} = $attr->{'LV'};
			for my $tag (keys %{$attr->{'Value'}})
			{
				$new->{$tag} = $attr->{'Value'}->{$tag}->{'LV'};
			}
			$res->{$attr->{'PartOfSpeech'}}->{$attr->{'MarkupPos'}} = $new;#$attr->{'Value'};#
		}
	}
	
	#print Dumper($res);
	# Return name of 0th tag and tag name hashmap.
	return $res;
}

# decodeTag (tag to decode, tag decoding hashmap obtained from parseTagSet().
# Returns array with tuples consisting of feature name and value name.
sub decodeTag
{
	my $tag = shift;
	my $tagHash = shift;
	my @tagChars = split //, $tag;
	
	# Part Of Speech.
	my $pos = $tagHash->{0}->{$tagChars[0]};
	# Magical treatment for participles.
	$pos = 'Divdabis' if ($tagHash->{$pos}->{3} eq 'Divdabis');
	my @res = (($tagHash->{0}->{'NAME'}, $pos));
	
	for (my $ind = 1; $ind < @tagChars; $ind++)
	{
		my $tmp = $tagHash->{$pos}->{$ind};
		push @res, ($tmp->{'NAME'}, $tmp->{$tagChars[$ind]});
	}
	return \@res;
}

1;