# -*- cperl -*-

#ifndef LV_A
#define LV_A

package LV_A;

use MorphoTags;

BEGIN { import TredMacro; import PML; }

# Switch on/of vertical layout, if appropriate stylesheet is provided.
sub swich_styles_vert
{
  my $st = GetCurrentStylesheet();
  if ($st =~ /-vert/) 
  {
	$st =~ s/-vert//;
  } else
  {
	if ($st =~ /-ord/)
	{
	  $st =~ s/-ord/-vert-ord/;
	} else
	{
	  $st = $st.'-vert-ord';
	}
  }
  SetCurrentStylesheet($st) if StylesheetExists($st);
}

sub switch_mode
{
  my $mode = CurrentContext();
  SwitchContext('LV_A_View') if ($mode eq 'LV_A_Edit');
  SwitchContext('LV_A_Edit') if ($mode eq 'LV_A_View');
}

sub switch_styles_compact
{
  my $st = GetCurrentStylesheet();
  if ($st =~ /-compact/) 
  {
	$st =~ s/-compact//;
  } else
  {
    $st =~ s/-vert// if ($st =~ /-vert/);
	if ($st =~ /-ord/)
	{
	  $st =~ s/-ord/-compact-ord/;
	} else
	{
	  $st = $st.'-compact';
	}
  }
  SetCurrentStylesheet($st) if StylesheetExists($st);

}

# Switch on/of print-layout, if appropriate stylesheet is provided.
sub swich_styles_full
{
  my $st = GetCurrentStylesheet();
  if ($st =~ /-full/) 
  {
	$st =~ s/-full//;
  } else
  {
	$st =~ s/^lv-a/lv-a-full/;
  }
  SetCurrentStylesheet($st) if StylesheetExists($st);
}

# Switch on/of orderedlayout, if appropriate stylesheet is provided.
sub swich_styles_ord
{
  my $st = GetCurrentStylesheet();
  if ($st =~ /-ord$/) 
  {
	$st =~ s/-ord$//;
  } else
  {
	$st = $st.'-ord';
  }
  SetCurrentStylesheet($st) if StylesheetExists($st);
}

# Check on various structural errors
sub get_structural_errors
{
  my $node = shift;
  my @errors = ();
  push @errors, 'Root must have one PMC!'
    if ($node eq $root and count_pmc_child($node) != 1);
  push @errors, 'Only one phrase per dependency node allowed!' 
    if (count_nondep_child($node) > 1);
  push @errors, 'Empty field!'
    if (is_unfinished($node));
  push @errors, 'Role unsuitable for this parent!'
    if (!is_allowed_for_parent($node));
  push @errors, 'Node must have reduction, morphology or phrase!'
    if (is_wrong_empty_node($node) and not is_phrase_node($node));
  push @errors, 'Phrase are not allowed under node with morphology!'
    if (($node->{'#name'} eq 'xinfo' or $node->{'#name'} eq 'coordinfo' or $node->{'#name'} eq 'pmcinfo')
	  and ($node->parent)->attr('m/id'));
  push @errors, 'Phrase are not allowed under node with reduction!'
    if (($node->{'#name'} eq 'xinfo' or $node->{'#name'} eq 'coordinfo' or $node->{'#name'} eq 'pmcinfo')
	  and ($node->parent)->attr('reduction'));
  push @errors, 'Phrase node must have children!'
    if (is_wrong_empty_node($node) and is_phrase_node($node));

  if ($node->{'xtype'} eq 'xPred')
  {
    my $basElemCount = count_children_with_with_role($node, 'basElem');
    my $modCount = count_children_with_with_role($node, 'mod');
    my $auxVerbCount = count_children_with_with_role($node, 'auxVerb');
    push @errors, 'xPred must have one basElem!'
      if ($basElemCount != 1);
	push @errors, 'xPred shouldn\'t have multiple mod!'
      if ($modCount > 1);
	push @errors, 'xPred shouldn\'t have auxVerb with mod!'
	  if ($modCount > 0 and $auxVerbCount > 0);
  }
  
  if ($node->{'xtype'} eq 'xPrep')
  {
    my $basElemCount = count_children_with_with_role($node, 'basElem');
    my $prepCount = count_children_with_with_role($node, 'prep');
	push @errors, 'xPrep must have one basElem!'
      if ($basElemCount != 1);
	push @errors, 'xPrep must have one prep!'
      if ($prepCount != 1);
  }
  
  if ($node->{'xtype'} eq 'xSimile')
  {
    my $basElemCount = count_children_with_with_role($node, 'basElem');
    my $prepCount = count_children_with_with_role($node, 'conj');
	push @errors, 'xSimile must have one basElem!'
      if ($basElemCount != 1);
	push @errors, 'xSimile must have one conj!'
      if ($prepCount != 1);
  }
  
  if ($node->{'xtype'} eq 'xParticle')
  {
    my $basElemCount = count_children_with_with_role($node, 'basElem');
    my $prepCount = count_children_with_with_role($node, 'no');
	push @errors, 'xSimile must have one basElem!'
      if ($basElemCount != 1);
	push @errors, 'xSimile must have one no!'
      if ($prepCount != 1);
  }
  
  if ($node->{'xtype'} eq 'xNum' or $node->{'xtype'} eq 'xApp'
    or $node->{'xtype'} eq 'namedEnt' or $node->{'xtype'} eq 'unstruct'
    or $node->{'xtype'} eq 'subrAnal' or $node->{'xtype'} eq 'coordAnal'
    or $node->{'xtype'} eq 'phrasElem')
  {
    my $basElemCount = count_children_with_with_role($node, 'basElem');
	push @errors, 'Why would '.$node->{'xtype'}.' have so few basElem?'
      if ($basElemCount < 2);
  }
  
  if ($node->{'#name'} eq 'coordinfo')
  {
    my $crdPartCount = count_children_with_with_role($node, 'crdPart');
	push @errors, 'Why would '.$node->{'coordtype'}.' have so few crdPart?'
      if ($crdPartCount < 2);
  }
  
  if ($node->{'pmctype'} eq 'sent' or $node->{'pmctype'} eq 'dirSpPmc'
    or $node->{'pmctype'} eq 'mainCl' or $node->{'pmctype'} eq 'subrCl'
    or $node->{'pmctype'} eq 'quot' or $node->{'pmctype'} eq 'insPmc')
  {
    my $basElemCount = count_children_with_with_role($node, 'basElem');
    my $predCount = count_children_with_with_role($node, 'pred');
	push @errors, $node->{'pmctype'}.' must have one pred or basElem!'
	  if ($basElemCount + $predCount != 1);
  }
  elsif ($node->{'pmctype'} eq 'utter')
  {
    my $basElemCount = count_children_with_with_role($node, 'basElem');
	push @errors, $node->{'pmctype'}.' must have basElem!'
      if ($basElemCount < 1);
  }
  elsif ($node->{'#name'} eq 'pmcinfo')
  {
    my $basElemCount = count_children_with_with_role($node, 'basElem');
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
sub is_allowed_for_parent
{
  my $node = shift;
  my $p = $node->parent;
  return 1 unless ($node and $p);
  
  # root children.
  if ($node->{'pmctype'} eq 'sent' or
      $node->{'pmctype'} eq 'utter')
  {
    return 1 if ($p eq $root);
	return 1 if ($p->parent->{'pmctype'} eq 'dirSpPmc');
	return 1 if ($p->parent->{'pmctype'} eq 'quot');
	return 0;
  }
  
  if ($p eq $root)
  {
	return 1 if ($node->{'pmctype'} eq 'sent' or
                 $node->{'pmctype'} eq 'utter' or
				 $node->{'pmctype'} eq 'quot' or
				 $node->{'pmctype'} eq 'dirSpPmc');
	return 0 if ($node->{'role'} eq 'punct' or
                 $node->{'role'} eq 'conj' or
                 $node->{'role'} eq 'pred' or
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
  # modal werbs and aux.werbs must be below xPred
  if ($node->{'role'} eq 'mod' or
      $node->{'role'} eq 'auxVerb')
  {
    return 1 if ($p->{'xtype'} eq 'xPred');
	return 0;
  }
  
  # punctuation must be below coordination or pmc or used for reduction.
  if ($node->{'role'} eq 'punct')
  {
    return 0 if ($p eq $root);
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
  # subjects, objects, atributes etc. can be either in dependency or in
  # insertion or in parenthesis
  foreach (@normalRoles, @clRoles) #@redRoles, 
  {
	if ($node->{'role'} eq $_)
	{
	  return 1 if ($p->{'#name'} eq 'node' or
				   $p->{'s.rf'} or
				   #$p->{'pmctype'} eq 'sent' or
				   $p->{'pmctype'} eq 'utter' or # Parcelaati.
				   $p->{'pmctype'} eq 'ins');
	  return 0;
	}
  }
  # insertions, determinants and situants must be in dependency
  foreach (@detRoles) 
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
	return 0;
  }
  return 1;
}

# Count children with given role.
sub count_children_with_with_role
{
  my $node = shift;
  my $role = shift;
  
  my $count = 0;
  foreach $ch ($node->children)
  {
	$count++ if ($ch->{'role'} eq $role);
  }
  return $count;
}

sub get_extendend_morpho
{
	return MorphoTags::getAVPairsFromAnyTag(@_);
}

sub get_tag_errors
{
	return MorphoTags::checkAnyTag(@_);
}

# Determine wether the given node (xinfo or pmcinfo) is apropriate for it's
# children hiding - for dependency-only stylesheet.
sub hidable_type
 {
  my $node = shift;
  if ($node->{'#name'} eq 'xinfo')
  {
	return 1;
	
  } elsif ($node->{'#name'} eq 'pmcinfo')
  {
    return 1 if ($node->{'pmctype'} eq 'quot' or
	             $node->{'pmctype'} eq 'abbr' or
				 $node->{'pmctype'} eq 'address' or
				 $node->{'pmctype'} eq 'numPmc' or
				 $node->{'pmctype'} eq 'spcPmc' or
				 $node->{'pmctype'} eq 'interj' or
				 $node->{'pmctype'} eq 'particle');
	return 0;
  }
  return 0;
}

# Finds, if node has x-node or pmc-node or coord-node as children.
sub has_nondep_child
{
  my $node = shift;
  foreach $ch ($node->children)
  {
	if (&is_phrase_node($ch))
	{
	  return $ch;
	}
  }
  return '';
}

# Finds, if node has x-node or pmc-node or coord-node as children.
sub count_nondep_child
{
  my $node = shift;
  my $count = 0;
  foreach $ch ($node->children)
  {
	if (&is_phrase_node($ch))
	{
	  $count++;
	}
  }
  return $count;
}

# Finds, if node has x-node or pmc-node or coord-node as children.
sub count_pmc_child
{
  my $node = shift;
  my $count = 0;
  foreach $ch ($node->children)
  {
	if ($ch->{'#name'} eq 'pmcinfo')
	{
	  $count++;
	}
  }
  return $count;
}

# Finds, if node has x-node or pmc-node or coord-node as children.
sub is_wrong_empty_node
{
  my $node = shift;
  return 0 if (&is_phrase_node($node) and $node->children);
  return 0 if ($node->{'reduction'} or $node->{'m'});
  return 0 if (&has_nondep_child($node));
  return 1;
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


# Status line mesidge.
sub get_status_line_hook
{
  # get_status_line_hook may either return a string
  # or a pair [ field-definitions, field-styles ]
  return unless $this;
  my @mas = ();
  if ($this->{id})
  {
	push(@mas, "     id: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->{id});
	push(@mas, [qw({id} value)]);
  } else
  {
	push(@mas, "     ");
	push(@mas, [qw(label)]);
	push(@mas, $this->{'#name'});
	push(@mas, [qw({#name} value)]);
  }
  if ($this->attr('m/lemma'))
  {
	push(@mas, "     m/lemma: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->attr('m/lemma'));
	push(@mas, [qw({m/lemma} value)]);
  }
  if ($this->attr('m/tag'))
  {
	push(@mas, "     m/tag: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->attr('m/tag'));
	push(@mas, [qw({m/tag} value)]);
  }
  if ($this->attr('tag'))
  {
	push(@mas, "     tag: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->attr('tag'));
	push(@mas, [qw({tag} value)]);
  }
  return [\@mas,
      [
	   "label" => [-foreground => 'black' ],
	   "value" => [-underline => 1 ],
	  ]
	 ];
}

# Check if the current file is standard Latvian Treebank file.
sub is_lva_file
{
  return (((PML::SchemaName()||'') eq 'lvadata') ? 1 : 0);
}

# Check if the current file is Latvian Treebank dependency-only file.
sub is_lvadep_file
{
  return (((PML::SchemaName()||'') eq 'lvadepdata') ? 1 : 0);
}


push @TredMacro::AUTO_CONTEXT_GUESSING, sub
{
  my ($hook)=@_;
  my $resuming = ($hook eq 'file_resumed_hook');
  my $current = CurrentContext();
  if (LV_A::is_lva_file())
  {
    SetCurrentStylesheet('lv-a') if $resuming;
    return 'LV_A_View';
  }
  if (LV_A::is_lvadep_file())
  {
    SetCurrentStylesheet('lv-a-dep-ord') if $resuming;
    return 'LV_A_PureDependency';
  }
  return;
};

# do not use this annotation mode for other files
sub allow_switch_context_hook
{
  return 'stop' if (not LV_A::is_lva_file and not LV_A::is_lvadep_file);
}
#endif LV_A









