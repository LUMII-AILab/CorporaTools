# -*- cperl -*-

#ifndef LV_A_Edit
#define LV_A_Edit

#include "LV_A.mak"

package LV_A_Edit;
use strict;

BEGIN { import LV_A; import PML; }

# set correct stylesheet when entering this annotation mode
sub switch_context_hook
{
  SetCurrentStylesheet('lv-a-full-ord');
  Redraw() if GUI();
}

# Recalculate ord values, based on the word order.
# Deletes ords of the "empty" nodes.
sub normalize_m_ords
{
  my @nodes = GetNodes;
  SortByOrd(\@nodes);
  my $npk = 1;
  foreach my $n (@nodes)
  {
	if ($n->{'m'})
	{
	  $n->{'ord'} = $npk;
	  $npk++;
	} else
	{
	  $n->{'ord'} = undef;
	}
  }
}

# Recalculate ord values for all trees, based on the word order.
# Deletes ords of the "empty" nodes.
sub normalize_m_ords_all_trees
{
  my $tree = CurrentTreeNumber;
  for (my $i = 1; $i <= GetTrees(); $i++)
  {
    GotoTree($i);
	&normalize_m_ords;
  }
  GotoTree($tree + 1);
}

# Recalculate ord values, and give ords to the empty nodes.
sub normalize_n_ords
{
  &normalize_m_ords;
  &_give_ord_everybody;
  my @nodes = GetNodes;
  SortByOrd(\@nodes);
  my $npk = 1;
  foreach my $n (@nodes)
  {
	$n->{'ord'} = $npk;
	$npk++;
  }
}
# Recalculate ord values and give ords to the empty nodes for all trees.
sub normalize_n_ords_all_trees
{
  my $tree = CurrentTreeNumber;
  for (my $i = 1; $i <= GetTrees(); $i++)
  {
    GotoTree($i);
	&normalize_n_ords;
  }
  GotoTree($tree + 1);
}


# Give "ord" tags to the "empty" nodes.
sub _give_ord_everybody
{
  my @nodes = GetNodes;
  SortByOrd(\@nodes);
  for (my $i = 0; $nodes[$i]->attr('ord') < 1; $i++)
  {
    # The ord for this node can't be calculated from it's children.
    if (not $nodes[$i]->firstson)
	{
	  my $ord_giver = $nodes[$i]->rbrother;
	  $ord_giver = $ord_giver->rbrother
  	    while ($ord_giver and $ord_giver->attr('ord') < 1);
	  if (not $ord_giver)
	  {
	    $ord_giver = $nodes[$i]->lbrother;
	    $ord_giver = $ord_giver->lbrother
		  while ($ord_giver and $ord_giver->attr('ord') < 1);
	  }
	  if ($ord_giver)
	  {
	    my $ord = $ord_giver->attr('ord');
	    &_rise_ords($ord);
		$nodes[$i]->set_attr('ord', $ord);
	  }
	  # The order for this node can't be calculated from it's brothers.
	  else
	  {
	    $ord_giver = $nodes[$i]->parent;
	    $ord_giver = $ord_giver->parent
		  while ($ord_giver and $ord_giver->attr('ord') < 1);
		
	    if ($ord_giver)
	    {
	      my $ord = $ord_giver->attr('ord') + 1;
	      &_rise_ords($ord);
		  $nodes[$i]->set_attr('ord', $ord);
	    }
	  }
	}
  }
  
  # Calculate ords for the nodes from their children.
  for (my $i = 1; $i <= GetNodes; $i++)
  {
    #writeln($i);
    my @nodes = GetNodes;
    SortByOrd(\@nodes);
	last if (@nodes[0]->attr('ord') > 0);
	my $node_id = 0;
	$node_id++ while (@nodes[$node_id]->attr('ord') < $i);#$node_id < GetNodes and 
	#last if ($node_id = GetNodes);
	my $ancestor = @nodes[$node_id]->parent;
	my $change_anc = undef;
	while ($ancestor)
	{
		$change_anc = $ancestor if ($ancestor->attr('ord') < 1);
		$ancestor = $ancestor->parent;
	}
	#writeln($change_anc);
	if ($change_anc)
	{
	  &_rise_ords($i);
	  #writeln($change_anc);
	  #foreach my $n (@nodes)
	  #{
	  #  $n->set_attr('ord', $n->attr('ord') + 1) if ($n->attr('ord') >= $i);
	  #}
	  $change_anc->set_attr('ord', $i);
	}
  }
}

# Support function.
# Adds +1 to each ord, which is equal or greater than integer recieved as
# first parameter.
sub _rise_ords
{
  my $param = shift;
  my @nodes = GetNodes;
  foreach my $n (@nodes)
  {
    $n->set_attr('ord', $n->attr('ord') + 1) if ($n->attr('ord') >= $param);
  }
}

# Incorporate given nodes token in the parent and delete node.
sub incorp_in_parent
{
  if ((not $this->parent) or ($this->parent->{'#name'} ne 'node'))
  {
    stderr 'Can\'t incorporate in parent, active node has no appropriate parent!';
	return;	
  }
  if ($this->firstson)
  {
    stderr 'Can\'t incorporate in parent, active node has children!';
	return;	
  }
  foreach my $member (TredMacro::ListV($this->attr('m/w')))
  {
    TredMacro::AddToList($this->parent, 'm/w', $member);
  }
  $this->parent->set_attr('m/form', &_get_form_from_tokens($this->parent));
  $this->set_attr('m/deleted', '1');
  TredMacro::AddToListUniq($this->parent, 'm/form_change', 'union');
  &delete_node;
  return;
}

# Create new x-word node.
sub new_xinfo_node
{
  if ($this->{'#name'} ne 'node')
  {
    stderr 'X node is not allowed here!';
	return;
  }
  my $n = Treex::PML::Factory->createTypedNode(
	'a-xinfo.type', $root->type->schema); 
  PasteNode($n, $this);
  $n->{'#name'}='xinfo';
  $n->{'xtype'}='N/A';
  $n->{'tag'}='N/A';
  $this = $n;
#  TredMacro::PlainNewSon($this);
#  $this->{'#name'}='xinfo';
#  $this->{'xtype'}='N/A';
#  $this->{'tag'}='N/A';
}

# Create new PMC node.
sub new_pmcinfo_node
{
  if ($this->{'#name'} ne 'node')
  {
    stderr 'PMC node is not allowed here!';
	return;
  }
  my $n = Treex::PML::Factory->createTypedNode(
	'a-pmcinfo.type', $root->type->schema); 
  PasteNode($n, $this);
  $n->{'#name'}='pmcinfo';
  $n->{'pmctype'}='N/A';
  $this = $n;
  #TredMacro::PlainNewSon($this);
  #$this->{'#name'}='pmcinfo';
  #$this->{'pmctype'}='N/A';
}

# Create new coordination node.
sub new_coordinfo_node
{
  if ($this->{'#name'} ne 'node')
  {
    stderr 'Coordination node is not allowed here!';
	return;
  }
  my $n = Treex::PML::Factory->createTypedNode(
	'a-coordinfo.type', $root->type->schema); 
  PasteNode($n, $this);
  $n->{'#name'}='coordinfo';
  $n->{'coordtype'}='N/A';
  $this = $n;

  #TredMacro::PlainNewSon($this);
  #$this->{'#name'}='coordinfo';
  #$this->{'coordtype'}='N/A';
}

# Create backbone structure for coordinated clauses.
sub new_coordcl_struct
{
  # Ask, how many children we should have.
  my $chN;
  my $mw = GUI()->{'framegroup'}->{'top'};
  my $scheme = $this->type->schema;
  #use Data::Dumper;
  #print Dumper(keys %$mw);
  my $d = $mw->DialogBox(-title => 'Number of Children',
		-buttons => ['OK', 'Cancel'], -default_button => 'OK');
  $d->add('LabEntry', -textvariable => \$chN, -width => 20, 
         -label => 'Number of Children', -labelPack => [-side => 'left'])->pack;
    #$d->BindReturn( $d, 1 );
    #$d->BindEscape();
  $d->resizable( 0, 0 );
  $d->BindButtons;
  my $answer = $d->Show();
  
  return unless ($answer eq "OK");
  $chN = 0 if $chN lt 1;
  
  #print Dumper($this->type);
  my $sentid = $root->{'id'};

  # Create basElem.
  &new_child_node;
  $this->{'role'}='basElem';
  # Create coord node.
  &new_coordinfo_node();
  my $coordCl = $this;
  $this->{'coordtype'}='coordCl';
  
  my $clauseType = '';
  if ($chN ge 1)
  {
    # Create 1st crdPart.
	&new_child_node;
    $this->{'role'}='crdPart';

    # Create clause under 1st crdPart.
	&new_pmcinfo_node;
    $this->{'#name'} = 'pmcinfo';
    # Prompt user for give order number for newly created node.
    EditAttribute($this, 'pmctype');
	$clauseType = $this->{'pmctype'};
	
	for (2 .. $chN)
	{
      # Create next crdPart.
	  $this = $coordCl;
	  &new_child_node;
      $this->{'role'}='crdPart';
      # Create clause under crdPart.
	  &new_pmcinfo_node;
      $this->{'#name'} = 'pmcinfo';
      $this->{'pmctype'} = $clauseType;
	}
  }
}

# Create new ordinary empty node.
sub new_child_node
{
  #TredMacro::PlainNewSon($this);
  my $sentid = $root->{'id'};
  my $n = Treex::PML::Factory->createTypedNode(
	'a-node.type', $root->type->schema); 
  PasteNode($n, $this);
  $n->{'#name'} = 'node';
  $n->{'id'} = $sentid . 'x' . &_get_next_id('x');;
  $n->{'role'}='N/A';
  $this = $n;

#  $sentid .= 'x' . &_get_next_id('x');
#  $this->{'#name'} = 'node';
#  $this->{'id'} = $sentid;
#  $this->{'role'}='N/A';
}

# Create new ordinary node with "place" for morphology.
# Really hacky code, forces save on all files, can't be undone.
#TODO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
sub new_m_node
{
  my $context = CurrentContext();
  my $stylesheet = GetCurrentStylesheet();
  my $sentid = $root->{'id'};
  my $nid = $sentid . 'w' . &_get_next_id('w');
  my @old_nodes = GetNodes;
  SortByOrd(\@old_nodes);

  # Create new a-level node.
  my $n = Treex::PML::Factory->createTypedNode(
	'a-node.type', $root->type->schema); 
  PasteNode($n, $this);
  $this = $n;
  # Fill a-level fields for newly created node.  
  $this->{'#name'} = 'node';
  $this->{'id'} = $nid;
  $this->{'role'} = 'N/A';

  # Prompt user for give order number for newly created node.
  EditAttribute($this, 'ord');
  my $ordnr = $this->{'ord'};
  # Increase order numbers for nodes after the newly created node.
  my $bro = 0;
  for (my $i = @old_nodes - 1; $i >= 0; $i--)
  {
	if ($old_nodes[$i]->{'ord'} < $ordnr)
	{
	  $bro = $old_nodes[$i]->{'m'}{'id'};
	  last;
	}
	$old_nodes[$i]->{'ord'} = $old_nodes[$i]->{'ord'} + 1;
  }
  
  # Save changes without questions.
  CurrentFile()->save(FileName());

  # Create new m-level node. This invokes switching to the m-file, editing it,
  # saving it and switching back again.
  my $node = $this;
  my $m_id = &_write_new_m($bro);
  $this = $node;

  # Map a-node to m-node.
  $this->{'m'}{'#knit_prefix'} = 'm';
  $this->{'m'}{'id'} = $m_id;
    
  # When switching to m file and back again, current context is set to
  # TredMacro. That is why we need to re-set it.
  SwitchContext($context);
  
  # Hack-style save: saves only a file without saving m file (if m file is
  # saved here, previously created m-node is lost).
  #SaveAs({'filename' => FileName(), 'update_refs' => 'noooo'});
  #Save();
  my $tmp = CurrentFile()->appData('ref');
  CurrentFile()->changeAppData ('ref',{});
  CurrentFile()->save(FileName());
  CurrentFile()->changeAppData ('ref',$tmp);
  
  # Reload file to force TrEd  "knit in" the new data from m level.
  ReloadCurrentFile();
  SetCurrentStylesheet($stylesheet);
}

# Create new m-level node. This this is done by switching to the coresponding
# m-file, finding the coresponding tree (linear time) editing it, saving it and
# switching back again.
sub _write_new_m
{
  my $bro_id = shift;
  # Open m-file and find the necessary tree (sentence).
  my $m_sent_id = $root->attr('s.rf');
  $m_sent_id =~ s/^.+#//;
  my $a_file = CurrentFile();
  my $ref_id = $a_file->referenceNameHash->{'mdata'};
  my $ref_file = $a_file->referenceURLHash->{$ref_id};
  print "$ref_id, $ref_file.\n";
  #my $m_file = Open($ref_file, ('-keep' => 1)); #Doesn't work for TrEd 2.x
  my $m_file = Open($ref_file);
  NextTree() while ($root->attr('id') ne $m_sent_id);
  
  # Create new m-node.
  PlainNewSon($root);
  
  # Move it to the appropriate position.
  if ($bro_id)
  {
    my @nodes = GetNodes;
    for (my $i = 0; $i < @nodes; $i++)
    {
	  if ($nodes[$i]->{'#content'}{'id'} eq $bro_id)
	  {
		PasteNodeAfter($this, $nodes[$i]);
	    last;
	  }
    }
  }
  
  # Fill the m-nodes fields with initial values.
  my $m_id = $m_sent_id . 'w' . &_get_next_id('w', 1);
  $this->{'#name'} = 'm';
  $this->{'#content'}{'id'} = $m_id;
  $this->{'#content'}{'form'} = 'N/A';
  $this->{'#content'}{'lemma'} = 'N/A';
  $this->{'#content'}{'tag'} = 'N/A';
  $this->{'#content'}{'src.rf'} = 'manual';
  $this->{'#content'}{'form_change'}[0] = 'insert';
  
  # Save m-file, close it and resume the file, which was opened in the begining
  # of this function.
  #SaveAs({update_refs => 'nooooooo', update_filelist => 'noooo'});
  CurrentFile()->save(FileName());
  CloseFile($m_file);
  ResumeFile($a_file);
  # Return newly created node's ID.
  return $m_id;
}


#Delete active node without affecting ORD values.
#TODO
sub delete_node
{
  $this->{'m/deleted'} = 1;
  TredMacro::PlainDeleteNode($this);
}

#Supporting function: find first free id of given type. Types supported: x, w.
sub _get_next_id
{
  my ($type, $prefix) = @_;
  my @nodes = GetNodes($root);
  my %ids = ();
  foreach my $n (@nodes)
  {
	if ($prefix)
	{
	  if ($n->{'#content'}{'id'} =~ /$type(\d+)$/)
  	  {
	    $ids{$1} = 1;
	  }
	} else
	{
	  if ($n->{'id'} =~ /$type(\d+)$/)
  	  { $ids{$1} = 1 };
	}
  }
  my $newid = 1;
  foreach my $key (sort {$a <=> $b} keys(%ids))
  {
    last if ($key > $newid);
	$newid++ if ($key == $newid);
  }
  return $newid;
}

# Makes 'm/form' value from 'm/w/token' values.
sub _get_form_from_tokens
{
  my $node = shift;
  my $result;
  foreach my $w (TredMacro::ListV($node->attr('m/w')))
  {
    $result .= $w->{'token'};
    $result .= ' ' unless $w->{'no_space_after'};	
  }
  $result =~ /^(.*?)\s*$/;
  $result = $1;
  return $result;
}

#binding-context LV_A_Edit
#bind GotoTree to Alt+g menu Goto Tree

#bind new_xinfo_node to x menu New X-word Node
#bind new_pmcinfo_node to p menu New PMC Node
#bind new_coordinfo_node to c menu New Coordination Node
#bind new_child_node to n menu New Ordinary Node
#bind new_m_node to m menu New Node with Morphology (forces save, can't be undone)
#bind new_coordcl_struct to C menu New Coordination Construction

#bind incorp_in_parent to Ctrl+Up menu Incorporate in Parent
#bind delete_node to Delete menu Delete Leaf Node

#bind normalize_m_ords to Ctrl+w menu Recalculate Word Order (delete empty nodes' ords)
#insert normalize_m_ords_all_trees menu Recalculate Word Order for All Trees
#bind normalize_n_ords to Ctrl+n menu Recalculate Node Order (give ords for empty nodes)
#insert normalize_n_ords_all_trees menu Recalculate Node Order for All Trees

#bind Redraw_All to Alt+r menu Redraw
#bind swich_styles_vert to Alt+v menu Switch On/Off Vertical Layout
#bind swich_styles_full to Alt+f menu Switch On/Off Full-info Layout
#bind swich_styles_ord to Alt+o menu Switch On/Off Ordered Layout
#bind switch_mode to Alt+m menu Switch to View Mode

1;

#endif LV_A_Edit
