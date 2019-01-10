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
		'v' => 'Vocative'}],
	['Declension', {
		'1' => '1',
		'2' => '2',
		'3' => '3',
		'4' => '4',
		'5' => '5',
		'6' => '6',
		'g' => 'Genitiveling',
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
		'y' => 'Yes'}],
	['Degree', {
		'p' => 'Positive',
		'c' => 'Comparative',
		's' => 'Superlative'}],
	['Negative', {
		'n' => 'No',
		'y' => 'Yes'}],
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
	]],
'i' => ['Interjection', [
	]],
'q' => ['Particle', [
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
	['Type', {
		'n' => 'Common noun',
		'p' => 'Proper noun',
		'a' => 'Adjectival',
		'v' => 'Verbal',
		'r' => 'Adverbial',
		'd' => 'Discourse marker'}],
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
	'd' => 'Dative',
	'a' => 'Accusative',
	'l' => 'Locative'}],
];

my $xSimileHelper = [
['Gramaticalized', {
	'y' => 'Yes',
	'n' => 'No'}],
];

our %phraseTags = (
#xPred table
'act'    => ['Active voice', $xPredHelper],
'pass'   => ['Pasive voice', $xPredHelper],
'subst'  => ['Substantival predicate', $xPredHelper],
'adj'    => ['Adjectival predicate', $xPredHelper],
'pronom' => ['Pronominal predicate', $xPredHelper],
'modal'  => ['Modal predicate', $xPredHelper],
'phase'  => ['Phasal predicate', $xPredHelper],
'expr'   => ['Expressional predicate', $xPredHelper],
'adv'    => ['Adverbial predicate', $xPredHelper],
'inf'    => ['Infinitive predicate', $xPredHelper],
'num'    => ['Numeral predicate', $xPredHelper],
#xSimile table
'sim'  => ['Similative construction', $xSimileHelper],
'comp' => ['Comparative construction', $xSimileHelper],
#xPrep table
'pre'  => ['Preposition', []],
'post' => ['Postposition', []],
'rel'  => ['Relative adverb', []],
#xParticle table
'aff' => ['Affirmative', []],
'neg' => ['Negative', []],
#xApp
'agr' => ['Agreed apposition', []],
'non' => ['Non-agreed apposition', []],
#subrAnal
'vv'   => ['Pronominal phrase', []],
'ipv'  => ['Pronomen with adjective', []],
'skv'  => ['Pronomen with nominal', []],
'set'  => ['Selection construction', []],
'sal'  => ['Gramaticalized comparative', []],
'part' => ['Multiword particle', []],
);

our %phraseTypes2TagParts = (
'xPred'     => ['act', 'pass', 'subst', 'adj', 'pronom', 'modal', 'phase', 'expr', 'adv', 'inf', 'num',],
'xSimile'   => ['sim', 'comp',],
'xPrep'     => ['pre', 'post', 'rel',],
'xParticle' => ['aff', 'neg',],
'xApp'      => ['agr', 'non',],
'subrAnal'  => ['vv', 'ipv', 'skv', 'set', 'sal'],
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
	for my $i ($featureNumber .. (@$properties - 1))
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
	my $last = -1;
	for my $i (0 .. (length($tagTail)-1))
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
	for my $i (($last + 1) .. (@$properties - 1))
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
	return [] unless ($tag);
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

sub isSubTagAllowedForPhraseType
{
	my $tag = shift;
	my $xType = shift;
	return 0 unless $xType;
	return 0 unless $tag;
	my @errors = ();

	if ($tag =~ /^([^\[\]\(\)]*)\[([^\[\]\(\)]*)\]$/)
	{
		my ($simpleTag, $phraseTag) = ($1, $2);
		if (defined $phraseTypes2TagParts{$xType})
		{
			my $foundPref = 0;
			for my $pref (@{$phraseTypes2TagParts{$xType}})
			{
				$foundPref++ if ($phraseTag =~/^$pref/);
			}
			push @errors, 'Wrong bracket-tag!' unless $foundPref;
		}
		else
		{
			push @errors, 'Unneeded bracket-tag!';
		}
		#return [getAVPairsFromSimpleTag($simple), getAVPairsFromTagPhrasePart($phrase)];
	} else
	{
		push @errors, 'Missing bracket-tag!' if (defined $phraseTypes2TagParts{$xType});
	}
	
	return \@errors;
}

1;