package FormChecker;

use utf8;
use strict;
#use Data::Dumper;

# TODO: maybe we need to use MorphoTags.getAVPairsFromAnyTag() here?
sub checkFormByTag
{
	my $form = shift;
	my $tag = shift;
	return [] unless ($tag and $form);
	return [] if ($tag =~ m#^[Nn]/[Aa]$#);
	my @errors = ();
	
	# TODO update accordingly linguist feedback.
	push @errors, 'Form must start with \'ne\'!' if ($tag =~ /^v..[^p].{6}y.*/ and $form !~ /^ne/);
	push @errors, 'Form must start with \'ne\'!' if ($tag =~ /^v..p.{8}y.*/ and $form !~ /^ne/);
	
	#print Dumper ({'Form' => $form, 'Tag' => $tag, 'Result' => \@errors});
	
	return \@errors;
	
}

1;