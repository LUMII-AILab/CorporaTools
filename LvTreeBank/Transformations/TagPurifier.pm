#!C:\strawberry\perl\bin\perl -w
package LvTreeBank::Transformations::TagPurifier;

use strict;
use warnings;
###############################################################################
# This program takes single Semti-Kamols morphotag and removes nonsyntactical
# features.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

sub purifyKamolsTag
{
	my $tag = shift @_;
	#$tag =~ tr/\[\]//d;
	if ($tag =~ /^n.{5}$/i) # Process nouns.
	{
		$tag =~ s/^n.(...).$/n_$1_/i;
	} elsif ($tag =~ /^v.{10}$/i) # Process verbs.
	{
		$tag =~ s/^v(.).([^p].)..(...).$/v$1_$2__$3_/i;	# For verbs.
		$tag =~ s/^v(.).(p....).(.).$/v$1_$2_$3_/i;		# For participles.
	} elsif ($tag =~ /^a.{6}/i) # Process adjectives.
	{
		$tag =~ s/^a.(...)..$/a_$1__/i;
	} elsif ($tag =~ /^p.{6,7}$/i) # Process pronouns.
	{
		$tag =~ s/^p.(....)..?$/p_$1_/i;
	} elsif ($tag =~ /^r..$/i) # Process adverbs.
	{
		$tag =~ s/^rr.$/rr_/i; # For relative adverbs.
		$tag =~ s/^r[^r].$/r__/i; # For other adverbs.
	} elsif ($tag =~ /^c..$/i) # Process conjunctions.
	{
		$tag =~ s/^c(.).$/c$1_/i;
	} elsif ($tag =~ /^m.{5,6}$/i) # Process numerals.
	{
		$tag =~ s/^m..(...).?$/m__$1_/i;
	} elsif ($tag =~ /^[iq].?$/i) # Process interjections and particles.
	{
		$tag =~ s/^([iq]).?$/$1_/i;
	} elsif ($tag =~ /^[szyx]/i) # Process prepositions, punctuation, abbrevations, residuals.
	{
		# Do nothing.
	} elsif ($tag =~ m#^(N/A|_)$#i) # Process prepositions, punctuation, abbrevations, residuals.
	{
		$tag = "_";
	} else
	{
		print "Tag $tag was not recognized.\n";
	}

	return $tag;
}

1;