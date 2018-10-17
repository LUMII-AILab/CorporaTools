package LemmaChecker;

use utf8;
use strict;

# TODO: maybe we need to use MorphoTags.getAVPairsFromAnyTag() here?
sub checkLemmaByTag
{
	my $lemma = shift;
	my $tag = shift;
	return 0 unless ($tag and $lemma);
	return 0 if ($tag =~ m#^[Nn]/[Aa]$#);
	my @errors = ();
	
	# TODO update accordingly linguist feedback.
	push @errors, 'Lemma should contain an upercase letter?' if ($tag =~ /^np.*/ and $lemma !~ /\p{Lu}/);
	push @errors, 'Lemma should contain a punctuation symbol?' if ($tag =~ /^z.*/ and $lemma !~ /\p{P}/);
	push @errors, 'Lemma must be lowercase!' if ($tag =~ /^(?!np|x|y|z).*/ and $lemma !~ /^\p{Ll}+$/);
	# Needed for UD
	#push @errors, 'Lemma must be lowercase!' if ($tag =~ /^s.*/ and $lemma !~ /^\p{Ll}+$/);
	# Needed for UD
	#push @errors, 'Lemma must be lowercase!' if ($tag =~ /^rr.*/ and $lemma !~ /^\p{Ll}+$/);

	# Needed for UD
	push @errors, 'Lemma must start with \'ne\'!' if ($tag =~ /^v..[^p].{6}y.*/ and $lemma !~ /^ne/);
	# Needed for UD
	push @errors, 'Lemma must start with \'ne\'!' if ($tag =~ /^v..p.{8}y.*/ and $lemma !~ /^ne/);
	
	push @errors, 'Lemma must end with \'t\' or \'ties\'!' if ($tag =~ /^v.*/ and $lemma !~ /t(ies)?$/);
	
	return \@errors;
	
}

1;