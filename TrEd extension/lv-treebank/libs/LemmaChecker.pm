package LemmaChecker;

use utf8;
use strict;

# TODO: maybe we need to use MorphoTags.getAVPairsFromAnyTag() here?
sub checkLemmaByTag
{
	my $lemma = shift;
	my $tag = shift;
	return [] unless ($tag and $lemma);
	return [] if ($tag =~ m#^[Nn]/[Aa]$#);
	my @errors = ();
	
	# TODO update accordingly linguist feedback.
	push @errors, 'Lemma should contain an upercase letter?' if ($tag =~ /^np.*/ and $lemma !~ /\p{Lu}/);
	push @errors, 'Lemma should contain a punctuation symbol?' if ($tag =~ /^z.*/ and $lemma !~ /\p{P}/);
	push @errors, 'Lemma must be lowercase!' if ($tag =~ /^(nc|i).*/ and $lemma !~ /^-?\p{Ll}+(-\p{Ll}+)*-?$/);
	push @errors, 'Lemma must be lowercase!' if ($tag =~ /^(?!n|x|y|z|i).*/ and $lemma !~ /^\p{Ll}+$/);
	# Needed for UD
	#push @errors, 'Lemma must be lowercase!' if ($tag =~ /^s.*/ and $lemma !~ /^\p{Ll}+$/);
	# Needed for UD
	#push @errors, 'Lemma must be lowercase!' if ($tag =~ /^rr.*/ and $lemma !~ /^\p{Ll}+$/);

	# Needed for UD
	push @errors, 'Lemma must start with \'ne\'!' if ($tag =~ /^v..[^p].{6}y.*/ and $lemma !~ /^ne/);
	# Needed for UD
	push @errors, 'Lemma must start with \'ne\'!' if ($tag =~ /^v..p.{8}y.*/ and $lemma !~ /^ne/);
	
	push @errors, 'Wrong verb lemma ending!' if ($tag =~ /^v.*/ and $lemma !~ /t(ies)?$/);
	push @errors, 'Wrong adjective lemma ending!' if ($tag =~ /^a.*/ and $lemma !~ /[sšāiou]$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n..[svp].1.*/ and $lemma !~ /[sš]$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n..[svp].2.*/ and $lemma !~ /([^aeu]is|(akm|asm|rud|ūd|zib)ens|mēness|uguns|suns|sāls)$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n..[d].[12].*/ and $lemma !~ /i$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n..[svp].3.*/ and $lemma !~ /us$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n.m[d].3.*/ and $lemma !~ /i$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n.f[d].3.*/ and $lemma !~ /us$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n..[svp].4.*/ and $lemma !~ /a$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n..[d].4.*/ and $lemma !~ /as$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n..[svp].5.*/ and $lemma !~ /e$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n..[d].5.*/ and $lemma !~ /es$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n..[svp].6.*/ and $lemma !~ /[^aeiuo]s$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n..[d].6.*/ and $lemma !~ /is$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n....r.*/ and $lemma !~ /(šanās|umies|ājies)$/);
	push @errors, 'Wrong noun lemma ending!' if ($tag =~ /^n....g.*/ and $lemma !~ /(u|a|as|es)$/);
	
	return \@errors;
	
}

1;