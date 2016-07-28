package MorphoTags;

### Most of the code complexity arise from the fact that while the base tags
### used for morphology use only one character for each value, tags used to
### add aditional information for phrase annotaton can use longer segments.

use utf8;
use strict;
use Data::Dumper;

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
		'p' => 'Passive'}],
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
		'p' => 'Passive'}],
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

my $xPredHelper = [
['Tense', {
	's' => 'Simple',
	'p' => 'Perfect'}],
['Gender', {
	'm' => 'Masculine',
	'f' => 'Feminine'}],
['Case', {
	'n' => 'Nominative',
	'g' => 'Genitive',
	'd' => 'Dative'}],
];

my $xPrepHelper = [
['Place adverbial', {
	'y' => 'Is possible',
	'n' => 'Not possible'}],
];

our %phraseTags = (
#xPred table
'act' => ['Active voice', $xPredHelper],
'pass' => ['Pasive voice', $xPredHelper],
'subst' => ['Substantival predicate', $xPredHelper],
'adj' => ['Adjectival predicate', $xPredHelper],
'pronom' => ['Pronominal predicate', $xPredHelper],
'modal' => ['Modal predicate', $xPredHelper],
'phase' => ['Phasal predicate', $xPredHelper],
'expr' => ['Expressional predicate', $xPredHelper],
'adv' => ['Adverbial predicate', $xPredHelper],
#xSimile table
'spk' => ['Secondary predicative component', [
	['Subtype', {
		'sd' => 'Similative part'}],
	]],
#xPrep table
'pre' => ['Preposition', $xPrepHelper],
'post' => ['Postposition', $xPrepHelper],
'rel' => ['Relative adverb', $xPrepHelper],
);

our $notRecognized = 'NOT RECOGNIZED';
our $missing = 'MISSING VALUE';

our %generic = (
	'0' => 'Not applicable',
	'_' => $missing,
);

# Process the tag part which use multiple letters per attribute value.
sub getAVPairsFromTagPhrasePart
{
	my $tag = shift;
	return 0 unless $tag;
	$tag =~ s/^\s*(.*?)\s*$/$1/;
	return 0 unless $tag;

	return [['Type', $missing]] if ($tag =~ m#^N/[Aa]$#);
	
	# Find the type.
	my $nextPosition = 0;
	my $properties;
	for my $l (1..length($tag))
	{
		$nextPosition++;
		my $part = substr($tag, 0, $l);
		$properties = $phraseTags{$part};
		last if $properties;
	}
	return [['Type', $notRecognized]] unless $properties;
	
	my @result = (['Type', $properties->[0]]);
	$properties = $properties->[1];
	
	# Process other features.
	my $featureNumber = 0;
	while ($nextPosition < length($tag))
	{

		my $currentPosition = $nextPosition;
		my $fValue;
		# Find next feature.
		for my $l (1..length($tag)-$currentPosition)
		{
			$nextPosition++;
			my $part = substr($tag, $currentPosition, $l);
			$fValue = $properties->[$featureNumber][1]{$part};
			$fValue = $generic{$part} unless $fValue;
			last if ($fValue);
		}
		# Add to the results.
		if ($fValue)
		{
			push(@result, [$properties->[$featureNumber][0], $fValue]);
			$featureNumber++;
		}
		else
		{
			push(@result, [$properties->[$featureNumber][0], $notRecognized]);
		}
	}
	
	# Process missing features.
	for my $i ($featureNumber+1..@$properties-1)
	{
		push(@result, [$properties->[$i][0], $missing]);
	}
	
	return \@result;
};

# Process the tag part which use one letter per attribute value.
sub getAVPairsFromSimpleTag
{
	my $tag = shift;
	return 0 unless $tag;
	$tag =~ s/^\s*(.*?)\s*$/$1/;
	return 0 unless $tag;

	# Determine POS.
	return [['POS', $missing]] if ($tag =~ m#^N/[Aa]$#);
	$tag =~ /^(.)(.*)$/;
	my $tagTail = $2;
	my $properties = $tags{$1};
	$properties = $tags{'v..p'} if ($tag =~ /^v..p.*/);
	return [['POS', $notRecognized]] unless $properties;
	
	my @result = (['POS', $properties->[0]]);
	$properties = $properties->[1];
	
	# Process other features.
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
	# Process missing features.
	for my $i ($last+1..@$properties-1)
	{
		push(@result, [$properties->[$i][0], $missing]);
	}
	
	return \@result;
};

sub getAVPairsFromAnyTag
{
	my $tag = shift;
	return 0 unless $tag;
	
	# Phrase tags.
	if ($tag =~ /^([^\[\]\(\)]*)\[([^\[\]\(\)]*)\]$/)
	{
		my ($simple, $phrase) = ($1, $2);
		return [getAVPairsFromSimpleTag($simple), getAVPairsFromTagPhrasePart($phrase)];
	}
	
	# Reduction tags.
	if ($tag =~ /^([^\[\]\(\)]*)\(([^\[\]\(\)]*)\)$/)
	{
		my ($simple, $elipsis) = ($1, $2);
		return [getAVPairsFromSimpleTag($simple), [['Ellipted', $elipsis]]];
	};
		
	# All other tags.
	return [getAVPairsFromSimpleTag($tag)];
};

sub checkAnyTag
{
	my $tag = shift;
	my @avPairs = map {@$_} @{ getAVPairsFromAnyTag($tag) };
	my @errors = ();
	
	push @errors, 'Missing tag values!' if (0 < grep {$_->[1] eq $missing} @avPairs);
	push @errors, 'Unrecognized tag values!' if (0 < grep {$_->[1] eq $notRecognized} @avPairs);
	
	return \@errors;
};

sub checkSimpleTag
{
	my $tag = shift;
	my @avPairs = @{ getAVPairsFromSimpleTag($tag) };
	my @errors = ();
	
	push @errors, 'Missing tag values!' if (0 < grep {$_->[1] eq $missing} @avPairs);
	push @errors, 'Unrecognized tag values!' if (0 < grep {$_->[1] eq $notRecognized} @avPairs);
	
	return \@errors;
};

1;