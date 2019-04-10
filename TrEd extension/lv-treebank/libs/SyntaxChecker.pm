package SyntaxChecker;

use utf8;
use strict;

use PMLLVHelpers;

# Check on various structural errors
sub get_structural_errors
{
  my $node = shift;
  my $root = shift;
  my @errors = ();

  push @errors, 'Root must have one PMC!'
    if ($node eq $root and _count_pmc_child($node) != 1);
  push @errors, 'Only one phrase per dependency node allowed!' 
    if (_count_nondep_child($node) > 1);
  push @errors, 'Empty field!'
    if (is_unfinished($node));
  push @errors, 'Role unsuitable for this parent!'
    unless (is_role_allowed_for_parent($node));
  push @errors, 'Node must have reduction, morphology or phrase!'
    unless (is_allowed_to_be_empty($node) or PMLLVHelpers::is_phrase_node($node));
  push @errors, 'Phrase node must have children!'
    if (not is_allowed_to_be_empty($node) and PMLLVHelpers::is_phrase_node($node));
  push @errors, 'Phrase are not allowed under node with morphology!'
    if (($node->{'#name'} eq 'xinfo' or $node->{'#name'} eq 'coordinfo' or $node->{'#name'} eq 'pmcinfo')
	  and ($node->parent)->attr('m/id'));
  push @errors, 'Phrase are not allowed under node with reduction!'
    if (($node->{'#name'} eq 'xinfo' or $node->{'#name'} eq 'coordinfo' or $node->{'#name'} eq 'pmcinfo')
	  and ($node->parent)->attr('reduction'));
  
  if ($node->{'xtype'} eq 'xPred')
  {
    my $basElemCount = _count_children_with_with_role($node, 'basElem');
    my $modCount = _count_children_with_with_role($node, 'mod');
    my $auxVerbCount = _count_children_with_with_role($node, 'auxVerb');
    push @errors, 'xPred must have one basElem!'
      if ($basElemCount != 1);
	push @errors, 'xPred shouldn\'t have multiple mod!'
      if ($modCount > 1);
	push @errors, 'xPred shouldn\'t have auxVerb with mod!'
	  if ($modCount > 0 and $auxVerbCount > 0);
  }
  
  if ($node->{'xtype'} eq 'xPrep')
  {
    my $basElemCount = _count_children_with_with_role($node, 'basElem');
    my $prepCount = _count_children_with_with_role($node, 'prep');
	push @errors, 'xPrep must have one basElem!'
      if ($basElemCount != 1);
	push @errors, 'xPrep must have one prep!'
      if ($prepCount != 1);
  }
  
  if ($node->{'xtype'} eq 'xSimile')
  {
    my $basElemCount = _count_children_with_with_role($node, 'basElem');
    my $prepCount = _count_children_with_with_role($node, 'conj');
	push @errors, 'xSimile must have one basElem!'
      if ($basElemCount != 1);
	push @errors, 'xSimile must have one conj!'
      if ($prepCount != 1);
  }
  
  if ($node->{'xtype'} eq 'xParticle')
  {
    my $basElemCount = _count_children_with_with_role($node, 'basElem');
    my $prepCount = _count_children_with_with_role($node, 'no');
	push @errors, 'xSimile must have one basElem!'
      if ($basElemCount != 1);
	push @errors, 'xSimile must have one no!'
      if ($prepCount != 1);
  }
  
  if ($node->{'xtype'} eq 'xNum' or $node->{'xtype'} eq 'xApp'
    or $node->{'xtype'} eq 'xFunctor' or $node->{'xtype'} eq 'unstruct'
    or $node->{'xtype'} eq 'namedEnt' or $node->{'xtype'} eq 'phrasElem'
    or $node->{'xtype'} eq 'subrAnal' or $node->{'xtype'} eq 'coordAnal')
  {
    my $basElemCount = _count_children_with_with_role($node, 'basElem');
	push @errors, 'Why would '.$node->{'xtype'}.' have so few basElem?'
      if ($basElemCount < 2);
  }
  
  if ($node->{'#name'} eq 'coordinfo')
  {
    my $crdPartCount = _count_children_with_with_role($node, 'crdPart');
	push @errors, 'Why would '.$node->{'coordtype'}.' have so few crdPart?'
      if ($crdPartCount < 2);
  }
  
  if ($node->{'pmctype'} eq 'sent' or $node->{'pmctype'} eq 'dirSpPmc'
    or $node->{'pmctype'} eq 'mainCl' or $node->{'pmctype'} eq 'subrCl'
    or $node->{'pmctype'} eq 'quot' or $node->{'pmctype'} eq 'insPmc')
  {
    my $basElemCount = _count_children_with_with_role($node, 'basElem');
    my $predCount = _count_children_with_with_role($node, 'pred');
	push @errors, $node->{'pmctype'}.' must have one pred or basElem!'
	  if ($basElemCount + $predCount != 1);
  }
  elsif ($node->{'pmctype'} eq 'utter')
  {
    my $basElemCount = _count_children_with_with_role($node, 'basElem');
	push @errors, $node->{'pmctype'}.' must have basElem!'
      if ($basElemCount < 1);
  }
  elsif ($node->{'#name'} eq 'pmcinfo')
  {
    my $basElemCount = _count_children_with_with_role($node, 'basElem');
	push @errors, $node->{'pmctype'}.' must have one basElem!'
      if ($basElemCount != 1);
  }
  return \@errors;
}

# Determine wether the given node has 'N/A' values in any field.
sub is_unfinished
{
  my $node = shift;
  
  (($node->{'role'} eq 'N/A')
    or ($node->{'xtype'} eq 'N/A')
	or ($node->{'coordtype'} eq 'N/A')
	or ($node->{'pmctype'} eq 'N/A')
	or ($node->{'tag'} eq 'N/A')
	or ($node->{'reduction'} eq 'N/A')
	or ($node->attr('m/form') eq 'N/A')
	or ($node->attr('m/tag') eq 'N/A')
	or ($node->attr('m/lemma') eq 'N/A')
  ) ? 1 : 0;
}

# Determine wether the given node is logicaly apropriate for it's parent
# N/A values gives positive answer.
sub is_role_allowed_for_parent
{
  my $node = shift;
  #my $root = shift;
  my $p = $node->parent;

  return 1 unless ($node and $p);
  #return 1 if ($node eq $root);
  
  
  # root children.
  if ($node->{'pmctype'} eq 'sent' or
      $node->{'pmctype'} eq 'utter')
  {
    #return 1 if ($p eq $root);
	return 1 unless ($p->parent);
	return 1 if ($p->parent->{'pmctype'} eq 'dirSpPmc');
	return 1 if ($p->parent->{'pmctype'} eq 'quot');
	return 0;
  }
  
  #if ($p eq $root)
  unless ($p->parent)
  {
	return 1 if ($node->{'pmctype'} eq 'sent' or
                 $node->{'pmctype'} eq 'utter' or
				 $node->{'pmctype'} eq 'quot' or
				 $node->{'pmctype'} eq 'dirSpPmc');
	return 0 if ($node->{'role'} eq 'punct' or
                 $node->{'role'} eq 'conj' or
                 $node->{'role'} eq 'pred' or
                 $node->{'role'} eq 'subj' or
				 $node->{'role'} eq 'basElem');
	return 1 if ($node->{'#name'} eq 'node');
	return 0;
  }

  my @normalRoles = qw(subj attr obj adv app spc sit det);
  my @detRoles = qw(ins sit det);
  #my @redRoles = qw(redSubj redAttr redObj redAdv redApp redSpc redSit);
  my @clRoles = qw(subjCl predCl attrCl objCl appCl placeCl timeCl manCl degCl causCl purpCl condCl cnsecCl compCl cncesCl motivCl quasiCl);
  my @clauses = qw(sent mainCl subrCl insPmc report dirSp utter);
  
  # preposition's parent must be xPrep
  if ($node->{'role'} eq 'prep')
  {
    return 1 if ($p->{'xtype'} eq 'xPrep');
	return 0;
  }
  # conjunction's parent must be some kind of clause or coordination node.
  if ($node->{'role'} eq 'conj')
  {
    return 1 if ($p->{'#name'} eq 'coordinfo' 
				 or $p->{'xtype'} eq 'xSimile');
    foreach (@clauses)
	{
	  return 1 if ($p->{'pmctype'} eq $_);
	}
	return 0;
  }
  # coordinated parts
  if ($node->{'role'} eq 'crdPart')
  {
    return 1 if ($p->{'#name'} eq 'coordinfo');
	return 0;
  }
  # modal werbs and aux.verbs must be below xPred
  if ($node->{'role'} eq 'mod' or
      $node->{'role'} eq 'auxVerb')
  {
    return 1 if ($p->{'xtype'} eq 'xPred');
	return 0;
  }
  
  # punctuation must be below coordination or pmc or used for reduction.
  if ($node->{'role'} eq 'punct')
  {
    #return 0 if ($p eq $root);
    return 0 unless ($p->parent);
    return 1 if ($p->{'#name'} eq 'coordinfo' or
				 $p->{'#name'} eq 'pmcinfo' or
				 $node->{'reduction'});
	return 0;
  }
  
  # basElem must be below pmc or x-word.
  if ($node->{'role'} eq 'basElem')
  {
    return 0 if ($p->{'#name'} eq 'node' or
				 $p->{'#name'} eq 'coordinfo' or
				 $p->{'pmctype'} eq 'mainCl' or
				 $p->{'pmctype'} eq 'subrCl');
	return 1;
  }
  
  # "no" elements is allowed below ordinary nodes or below pmc or below xParticle
  if ($node->{'role'} eq 'no')
  {
    return 1 if ($p->{'#name'} eq 'node' or
				 $p->{'#name'} eq 'pmcinfo' or
				 $p->{'xtype'} eq 'xParticle');
	return 0;
  }
  
  # subject's parent is something predicative
  if ($node->{'role'} eq 'subj')
  {
	return 1 if ($p->{'role'} eq 'pred' or
                 $p->{'role'} eq 'spc' or
                 $p->{'role'} eq 'basElem' or
				 $p->{'role'} eq 'crdPart' or
				 $p->{'pmctype'} eq 'utter' or # Parcelaati.
				 $p->{'pmctype'} eq 'ins');
	return 0;
  }
  
  # subject clause's parent is either subject or something predicative
  if ($node->{'role'} eq 'subjCl')
  {
	return 1 if ($p->{'role'} eq 'subj' or
                 $p->{'role'} eq 'pred' or
                 $p->{'role'} eq 'spc' or
                 $p->{'role'} eq 'basElem' or
				 $p->{'role'} eq 'crdPart' or
				 $p->{'pmctype'} eq 'utter' or # Parcelaati.
				 $p->{'pmctype'} eq 'ins');
	return 0;
  }

  # objects, atributes etc. can be either in dependency or in
  # insertion or in parenthesis
  # NB! subjects are already procesed before this!
  foreach (@normalRoles, @clRoles) #@redRoles, 
  {
	if ($node->{'role'} eq $_)
	{
	  return 1 if ($p->{'#name'} eq 'node' or
				   $p->{'s.rf'} or
				   $p->{'pmctype'} eq 'utter' or # Parcelaati.
				   $p->{'pmctype'} eq 'ins');
	  return 0;
	}
  }
  # insertions, determinants and situants must be in dependency
  foreach (@detRoles, 'repeat') 
  {
	if ($node->{'role'} eq $_)
	{
	  return 1 if ($p->{'#name'} eq 'node' or
				   $p->{'s.rf'});
	  return 0;
	}
  }
  # predicate's parent is some kind of clause
  if ($node->{'role'} eq 'pred') # or $node->{'role'} eq 'redPred')
  {
    foreach (@clauses)
	{
	  return 1 if ($p->{'pmctype'} eq $_);
	}
	return 1 if ($p->{'pmctype'} eq 'quot');
	return 0;
  }
  
  return 1;
}

# Finds, if node has x-node or pmc-node or coord-node as children.
sub is_allowed_to_be_empty
{
  my $node = shift;
  return 1 if (PMLLVHelpers::is_phrase_node($node) and $node->children);
  return 1 if ($node->{'reduction'} or $node->{'m'});
  return 1 if (PMLLVHelpers::has_nondep_child($node));
  return 0;
}

# Count children with given role.
sub _count_children_with_with_role
{
  my $node = shift;
  my $role = shift;
  
  my $count = 0;
  foreach ($node->children)
  {
	$count++ if ($_->{'role'} eq $role);
  }
  return $count;
}


# Count, how many phrase children node has.
sub _count_nondep_child
{
  my $node = shift;
  my $count = 0;
  foreach my $ch ($node->children)
  {
	$count++ if (PMLLVHelpers::is_phrase_node($ch));
  }
  return $count;
}

# Count, how many PMC children node has.
sub _count_pmc_child
{
  my $node = shift;
  my $count = 0;
  foreach my $ch ($node->children)
  {
	$count++ if ($ch->{'#name'} eq 'pmcinfo');
  }
  return $count;
}


1;