package MorphoTags;

use utf8;
use strict;
#use Data::Dumper;

our %tags = (
'n' => ['Noun', [
	['Type', {
		'c' => 'Common',
		'p' => 'Proper'}],
	['Gender', {
		'm' => 'Masculine',
		'f' => 'Feminine'}],
	['Number', {
		's' => 'Singular',
		'p' => 'Plural',
		'v' => 'Singularia tantum',
		'd' => 'Pluralia tantum'}],
	['Case', {
		'n' => 'Nominative',
		'g' => 'Genitive',
		'd' => 'Dative',
		'a' => 'Accusative',
		'l' => 'Locative',
		'v' => 'Vocative',
		's' => 'Genitiveling'}],
	['Declension', {
		'1' => '1',
		'2' => '2',
		'3' => '3',
		'4' => '4',
		'5' => '5',
		'6' => '6',
		'r' => 'Reflexive'}],
	]],
'v' => ['Verb', [
	['Type', {
		'm' => 'Main',
		'g' => '"nebūt", "trūkt", "pietikt"',
		'o' => 'Modal',
		'p' => 'Phasal',
		'e' => 'Expressional',
		'c' => 'Auxilliary "būt"',
		't' => 'Auxilliary "tikt", "tapt", "kļūt"'}],
	['Reflexive', {
		'n' => 'No',
		'y' => 'Yes'}],
	['Mood', {
		'i' => 'Indicative',
		'r' => 'Relative',
		'c' => 'Conditional',
		'd' => 'Debitive/necessitative',
		'm' => 'Imperative',
		'n' => 'Infinitive'}],
	['Tense', {
		'p' => 'Present',
		'f' => 'Future',
		's' => 'Past'}],

	['Transitivity', {
		't' => 'Transitive',
		'i' => 'Intransitive'}],
	['Conjugation', {
		'1' => '1',
		'2' => '2',
		'3' => '3',
		'i' => 'Irregular'}],
	['Person', {
		'1' => '1',
		'2' => '2',
		'3' => '3'}],
	['Number', {
		's' => 'Singular',
		'p' => 'Plural'}],
	['Voice', {
		'a' => 'Active',
		'p' => 'Pasive'}],
	['Negative', {
		'n' => 'No',
		'y' => 'Yes'}],
	]],
'v..p' => ['Participle', [
	['Type', {
		'm' => 'Main',
		'g' => '\"nebūt\", \"trūkt\", \"pietikt\"',
		'o' => 'Modal',
		'p' => 'Phasal',
		'e' => 'Expressional',
		'c' => 'Auxilliary \"būt\"',
		't' => 'Auxilliary \"tikt\", \"tapt\", \"kļūt\"'}],
	['Reflexive', {
		'n' => 'No',
		'y' => 'Yes'}],
	['Mood', {
		'p' => 'Participle'}],
	['Flexibility', {
		'd' => 'Declinable',
		'p' => 'Partially declinable',
		'u' => 'Indeclinable'}],
	['Gender', {
		'm' => 'Masculine',
		'f' => 'Feminine'}],
	['Number', {
		's' => 'Singular',
		'p' => 'Plural'}],
	['Case', {
		'n' => 'Nominative',
		'g' => 'Genitive',
		'd' => 'Dative',
		'a' => 'Accusative',
		'l' => 'Locative',
		'v' => 'Vocative'}],
	['Voice', {
		'a' => 'Active',
		'p' => 'Pasive'}],
	['Tense', {
		'p' => 'Present',
		's' => 'Past'}],
	['Definite', {
		'n' => 'No',
		'y' => 'Yes'}]
	]],
'a' => ['Adjective', [
	['Type', {
		'f' => 'Qualificative',
		'r' => 'Relative'}],
	['Gender', {
		'm' => 'Masculine',
		'f' => 'Feminine'}],
	['Number', {
		's' => 'Singular',
		'p' => 'Plural'}],
	['Case', {
		'n' => 'Nominative',
		'g' => 'Genitive',
		'd' => 'Dative',
		'a' => 'Accusative',
		'l' => 'Locative',
		'v' => 'Vocative'}],
	['Definite', {
		'n' => 'No',
		'y' => 'Yes'}],
	['Degree', {
		'p' => 'Positive',
		'c' => 'Comparative',
		's' => 'Superlative'}],
	]],
'm' => ['Numeral', [
	['Type', {
		'c' => 'Cardinal',
		'o' => 'Ordinal',
		'f' => 'Fractal'}],
	['Make up', {
		's' => 'Simple',
		'c' => 'Compound',
		'j' => 'Multi-word'}],
	['Gender', {
		'm' => 'Masculine',
		'f' => 'Feminine'}],
	['Number', {
		's' => 'Singular',
		'p' => 'Plural'}],
	['Case', {
		'n' => 'Nominative',
		'g' => 'Genitive',
		'd' => 'Dative',
		'a' => 'Accusative',
		'l' => 'Locative',
		'v' => 'Vocative'}],
	]],
'p' => ['Pronoun', [
	['Type', {
		'p' => 'Personal',
		'x' => 'Reflexive/reciprocal',
		's' => 'Possesive',
		'd' => 'Demonstrative',
		'i' => 'Indefinite',
		'q' => 'Interrogative',
		'r' => 'Relative',
		'g' => 'Definite/total'}],
	['Person', {
		'1' => '1',
		'2' => '2',
		'3' => '3'}],
	['Gender', {
		'm' => 'Masculine',
		'f' => 'Feminine'}],
	['Number', {
		's' => 'Singular',
		'p' => 'Plural'}],
	['Case', {
		'n' => 'Nominative',
		'g' => 'Genitive',
		'd' => 'Dative',
		'a' => 'Accusative',
		'l' => 'Locative'}],
	['Negative', {
		'n' => 'No',
		'y' => 'Yes'}],
	]],
'r' => ['Adverb', [
	['Degree', {
		'r' => 'Relative',
		'p' => 'Positive',
		'c' => 'Comparative',
		's' => 'Superlative'}],
	['Semantic group', {
		'q' => 'Quantitative',
		'm' => 'Manner',
		'p' => 'Place',
		't' => 'Time',
		'c' => 'Causative'}],
	]],
's' => ['Preposition', [
	['Position', {
		'p' => 'Pre',
		't' => 'Post'}],
	['Number', {
		's' => 'Singular',
		'p' => 'Plural'}],
	['Used with', {
		'g' => 'Genitive',
		'd' => 'Dative',
		'a' => 'Accusative'}],
	]],
'c' => ['Conjunction', [
	['Type', {
		'c' => 'Coordinating',
		's' => 'Subordinating'}],
	['Make up', {
		's' => 'Simple',
		'c' => 'Compound',
		'd' => 'Double',
		'r' => 'Repetit'}],
	]],
'i' => ['Interjection', [
	['Make up', {
		's' => 'Simple',
		'c' => 'Compound'}],
	]],
'q' => ['Particle', [
	['Make up', {
		's' => 'Simple',
		'c' => 'Compound'}],
	]],
'z' => ['Punctuation', [
	['Type', {
		'c' => 'Comma',
		'q' => 'Quote',
		's' => 'Full stop',
		'b' => 'Bracket',
		'd' => 'Hyphen/dash',
		'o' => 'Colon',
		'x' => 'Other'}],
	]],
'y' => ['Abbreviation', [
	]],
'x' => ['Residual', [
	['Type', {
		'f' => 'Foreign',
		'n' => 'Cardinal number',
		'o' => 'Ordinal number',
		'u' => 'URI',
		'x' => 'Other'}],
	]],
);

our %generic = (
	'0' => 'Not applicable',
	'_' => 'MISSING',
);

our $notRecognized = 'NOT RECOGNIZED';

sub getAVPairs
{
	my $tag = shift;
	return 0 unless $tag;
	return 0 if ($tag =~ m#^N/[Aa]$#);
	$tag =~ /^(.)(.*)$/;
	my $tagTail = $2;
	my $properties = $tags{$1};
	$properties = $tags{'v..p'} if ($tag =~ /^v..p.*/);
	return [['POS', $notRecognized]] unless $properties;
	
	my @result = (['POS', $properties->[0]]);
	$properties = $properties->[1];
	
	my $last = 0;
	for my $i (0..length($tagTail)-1)
	{
		my $tagChar = substr($tagTail, $i, 1) or '_';
		if ($properties->[$i])
		{
			my $val = $properties->[$i][1]{$tagChar};
			$val = $generic{$tagChar} unless $val;
			$val = $notRecognized unless $val;
			push(@result, [$properties->[$i][0], $val]);
		}
		else
		{
			push(@result, [$tagChar, $notRecognized]);
		}
		$last = $i;
	}
	for my $i ($last+1..@$properties-1)
	{
		push(@result, [$properties->[$i][0], $notRecognized]);
	}
	
	return \@result;
}


1;