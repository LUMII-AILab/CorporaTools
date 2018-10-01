package PMLLVHelpers;

use utf8;
use strict;

# Finds, if node has x-node or pmc-node or coord-node as children.
sub has_nondep_child
{
  my $node = shift;
  foreach my $ch ($node->children)
  {
	return $ch if (is_phrase_node($ch));
  }
  return '';
}


# Finds, if node has x-node or pmc-node or coord-node as children.
sub is_phrase_node
{
  my $node = shift;
  return 1 if (($node->{'#name'} eq 'xinfo'
	 or $node->{'#name'} eq 'pmcinfo'
	 or $node->{'#name'} eq 'coordinfo'));
  return 0;
}

1;