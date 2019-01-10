# -*- cperl -*-

#ifndef LV_A_Edit
#define LV_A_Edit

#include "LV_A.mak"

package LV_A_Edit;
use strict;
use Treex::PML;

BEGIN { import LV_A; import PML; }

# set correct stylesheet when entering this annotation mode
sub switch_context_hook
{
  SetCurrentStylesheet('lv-a-edit-full-ord');
  Redraw() if GUI();
}

# Recalculate ord values, based on the word order.
# Deletes ords of the "empty" nodes.
sub normalize_m_ords
{
  my @nodes = GetNodes;
  @nodes = sort { $a->attr('ord') <=> $b->attr('ord') } @nodes;
  #SortByOrd (\@nodes); # This makes warnings with nodes with no ords in Tred 2.5049
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
  print 'Recalculating token ords for all trees... ';
  my $tree = CurrentTreeNumber;
  my $node = $this;
  for (my $i = 1; $i <= GetTrees(); $i++)
  {
    GotoTree($i);
	&normalize_m_ords;
  }
  GotoTree($tree + 1);
  $this = $node;
  print "Finished!\n"
}

# Recalculate ord values, and give ords to the empty nodes.
sub normalize_n_ords
{
  &normalize_m_ords;
  &_give_ord_everybody($root);
}
# Recalculate ord values and give ords to the empty nodes for all trees.
sub normalize_n_ords_all_trees
{
  print 'Recalculating node ords for all trees... ';
  my $tree = CurrentTreeNumber;
  my $node = $this;
  for (my $i = 1; $i <= GetTrees(); $i++)
  {
    GotoTree($i);
	&normalize_n_ords;
  }
  GotoTree($tree + 1);
  $this = $node;
  print "Finished!\n"
}

# Give "ord" tags to the "empty" nodes.
sub _give_ord_everybody
{
	my $tree = shift;
	my $smallerSibOrd = shift or 0;
	
	# Currently best found ID for root node.
	my $new_id = 0;
	# Does this node have phrase children with unreduced constituents (these
	# are more inportant than dependants).
	my $has_num_phrase_ch = 0;

	# Process children.
	if ($tree->children)
	{
		# Seperate children with nonzero ords.
		my (@processFirst, @postponed) = ((), ());
		for my $ch ($tree->children)
		{
			&_has_ord_child($ch) ? 
				push @processFirst, $ch :
				push @postponed, $ch;
		}
		
		# Process children recursively.
		push @processFirst, @postponed;
		for my $ch (@processFirst)
		{
			# Find smallest sibling ord.
			my @sibOrds = grep {($_->attr('ord') and $_->attr('ord') > 0)}
							   $tree->children;
			@sibOrds = sort (map {$_->attr('ord')} @sibOrds);
			my $min = $sibOrds[0] ? $sibOrds[0] : $smallerSibOrd;
			&_give_ord_everybody($ch, $min);
		}
		
		return if ($tree->attr('ord') > 0);
			
		# Obtain new id if given node have children. 
			# Now we can find new ord safely, because children ords don't
			# change anymore.
		for my $ch ($tree->children)
		{
			my $ch_ord = $ch->attr('ord');
			
			# If node has phrase children with numbered constituents, it is 
			# them, who determine ord of the parent, not dependants.
			if ($ch_ord)
			{
				my $type = $ch->attr('#name');
				if (($type ne 'node') and
					($ch_ord < $new_id or $new_id <= 0 or not $has_num_phrase_ch))
				{
					$new_id = $ch_ord;
					$has_num_phrase_ch = 1;
				}
				elsif (not $has_num_phrase_ch and ($ch_ord < $new_id or $new_id <= 0))
				{
					$new_id = $ch_ord;
				} 
			}
		}
	}
	return if ($tree->attr('ord'));
	
	# Obtain new id if given node has no children.
	$new_id = $smallerSibOrd if ($new_id <= 0);
	# Obtain new id if given node has no children and no siblings.	
	print "Node ID was not obtained from other nodes.\n" if ($new_id <= 0);
	$new_id++;
	
	# Give ord to root of the subtree.
	&_rise_ords($new_id);
	$tree->set_attr('ord', $new_id);
}

# Support function.
# Determines wether given subtree contains at least one node that corresponds
# to a token from sentence (has non-automatically assigned ord value).
sub _has_ord_child
{
	my $tree = shift;
	return 1 if ($tree->attr('m/form'));
	for my $ch ($tree->children)
	{
		return 1 if (&_has_ord_child($ch));
	}
	return 0;
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
  $this->{'coordtype'}='crdClauses';
  
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
  my $sentid = $root->{'id'};
  my $n = Treex::PML::Factory->createTypedNode(
	'a-node.type', $root->type->schema); 
  PasteNode($n, $this);
  $n->{'#name'} = 'node';
  $n->{'id'} = $sentid . 'x' . &_get_next_id('x');;
  $n->{'role'}='N/A';
  $this = $n;

}

# Create new ordinary node with "place" for morphology.
# Really hacky code, forces save on all files, can't be undone.
# Kinda TODO - authors said, it can't be done much better with knitted files.
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
  my $incrementIds = 1;
  my $bro = 0;
  for (my $i = @old_nodes - 1; $i >= 0; $i--)
  {
	if ($old_nodes[$i]->{'ord'} < $ordnr)
	{
	  $incrementIds = 0;
	  $bro = $old_nodes[$i]->{'m'}{'id'};
	  last if ($bro);
	}
	$old_nodes[$i]->{'ord'} = $old_nodes[$i]->{'ord'} + 1 if ($incrementIds);
  }
  
  # Save changes without questions.
  foreach (GetSecondaryFiles())
  {
	$_->save();
  }
  CurrentFile()->save(FileName());


  # Create new m-level node. This invokes switching to the m-file, editing it,
  # saving it and switching back again.
  my $node = $this;
  my $m_id = &_write_new_m($bro);
  if ($m_id)
  {
	  $this = $node;

	  # Map a-node to m-node.
	  $this->{'m'}{'#knit_prefix'} = 'm';
	  $this->{'m'}{'id'} = $m_id;
  }
  else
  {
  }
    
  # When switching to m file and back again, current context is set to
  # TredMacro. That is why we need to re-set it.
  SwitchContext($context);
  if ($m_id)
  {
    # Hack-style save: saves only a file without saving m file (if m file is
    # saved here, previously created m-node is lost).
    #SaveAs({'filename' => FileName(), 'update_refs' => 'noooo'});
    #Save();
    my $tmp = CurrentFile()->appData('ref');
    CurrentFile()->changeAppData ('ref',{});
    CurrentFile()->save(FileName());
    CurrentFile()->changeAppData ('ref',$tmp);
  }
  
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
  print "Opening $ref_id, $ref_file.\n";
  #my $m_file = Open($ref_file, {'-keep' => 1}); #Doesn't work for TrEd 2.x?
  my $m_file = Open($ref_file, {'-keep' => 0});
  #my $m_file = Open($ref_file);
  GotoTree(1);
  print "Serching $m_sent_id...\n";
  print "Looking at ".$root->attr('id').".\n";
  my $foundSent = $root->attr('id') eq $m_sent_id;
  
  while (not $foundSent and NextTree())
  {
    print "Looking at ".$root->attr('id').".\n";
    $foundSent = 1 if ($root->attr('id') eq $m_sent_id);
  };
  
  unless ($foundSent)
  {
    print "Could not find sentence $m_sent_id.\n";
	ErrorMessage("Could not find M sentence with ID $m_sent_id! M node was not created.");
	CloseFile($m_file);
    print "Switching back to a.\n";
    ResumeFile($a_file);
	return;
  }
  print "Found sentence $m_sent_id.\n";

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
		print "Added correctly placed m-node.\n";
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
  #use TrEd::FileLock qw(remove_lock);
  #TrEd::FileLock::remove_lock($m_file, $ref_file, 1);
  print "Switching back to a.\n";
  ResumeFile($a_file);
  CloseFile($m_file); # It is important to switch and then close, otherewise the m file dangles in the memory and voids the editions done through a file.
  # Return newly created node's ID.
  return $m_id;
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

#Delete active node without affecting ORD values.
#TODO
sub delete_node
{
  $this->{'m/deleted'} = 1;
  TredMacro::PlainDeleteNode($this);
}

#Rename to object passive voice subject if it is sibling of passive voice's
#xPred.
sub passive_subj_to_obj
{
  print "Renaming passive voice subjects for all trees...\n";
  my @changedIds = ();
  my $tree = CurrentTreeNumber;
  for (my $i = 1; $i <= GetTrees(); $i++)
  {
    GotoTree($i);
	# Find all passiive voice xPred.
	my @nodes = $root->descendants;
	my @passives = grep {
		$_->attr('#name') eq 'xinfo' and $_->attr('xtype') eq 'xPred'
		and $_->attr('tag') =~ /^v.*?\[pass/} @nodes;
	# Process each passive.
	for my $p (@passives)
	{
		# Find all related subjects.
		my @sibs = $p->parent->children;
		my $grandp = $p->parent->parent;
		# Coordination or xParticle can be between subject and passive. Or
		# multiple of them.
		while ($grandp->attr('#name') eq 'xinfo'
			and $grandp->attr('xtype') eq 'xParticle'
			or $grandp->attr('#name') eq 'coordinfo')
		{
			@sibs = (@sibs, $grandp->parent->children);
			$grandp = $grandp->parent->parent;
		}
		my @subjects = grep {
			$_->attr('#name') eq 'node' and $_->attr('role') eq 'subj'}
			@sibs;
		# Rename each subject.
		for my $subj (@subjects)
		{
			print $subj->{'id'}."\n";
			push @changedIds, $subj->{'id'};
			$subj->{'role'} = 'obj';
		}
	}
  }
  GotoTree($tree + 1);
  print "Finished!\n";
  my $mes = @changedIds ?
	@changedIds." node(s) has changed:\n". join("\n", @changedIds):
	"No nodes has changed!\n";
  InfoMessage($mes);

}

#Remove namedEnt and phrasElem having only one child.
sub remove_single_child_x
{
  print "Removing namedEnt and phrasElem having 1 child from all trees...\n";
  my @changedIds = ();
  my $tree = CurrentTreeNumber;
  for (my $i = 1; $i <= GetTrees(); $i++)
  {
    GotoTree($i);
	# Find all namedEnt and phrasElem with less than two children.
	my @nodes = $root->descendants;
	my @forRemove = grep {$_->attr('#name') eq 'xinfo' and
		($_->attr('xtype') eq 'namedEnt' or $_->attr('xtype') eq 'phrasElem') and
		not $_->parent->attr('m/id') and $_->children < 2} @nodes;
	# Process each removable.
	for my $n (@forRemove)
	{
		my $parent = $n->parent;
		my $grandp = $parent->parent;
		my @ch = $n->children;
		if (@ch and @ch gt 0)
		{
			$ch[0]->{'role'} = $parent->{'role'};
			my @movables = grep {$_ != $ch[0]} $parent->children;
			for my $m (@movables)
			{
				$m->cut->paste_on($ch[0]);
			}
			$ch[0]->cut->paste_before($parent);
		}
		TredMacro::PlainDeleteNode($n);
		TredMacro::PlainDeleteNode($parent);
		my $id = $grandp->{'id'} ? $grandp->{'id'} : $grandp->parent->{'id'};
		print $id."\n";
		push @changedIds, $id;
	}
  }
  GotoTree($tree + 1);
  print "Finished!\n";
  my $mes = @changedIds ?
	@changedIds." node(s) has changed:\n". join("\n", @changedIds):
	"No nodes has changed!\n";
  InfoMessage($mes);
}

# Set 'FIXME' for this tree.
sub set_fixme
{
  my $oldComent = $root->attr('comment');
  return if ($oldComent =~ '^FIXME');
  unless ($oldComent)
  {
    $root->{'comment'} = 'FIXME';
  }
  elsif ($oldComent =~ '^AUTO')
  {
    $root->{'comment'} = "$oldComent FIXME";
  }
  else
  {
    $root->{'comment'} = "FIXME $oldComent";
  }
}

# Set 'AUTO' for this file.
sub set_auto
{
  my $tree = CurrentTreeNumber;
  my $node = $this;
  GotoTree(1);
  my $oldComent = $root->attr('comment');
  unless ($oldComent =~ '^AUTO')
  {
    if ($oldComent)
    {
      $root->{'comment'} = "AUTO $oldComent";
    }
    else
    {
      $root->{'comment'} = 'AUTO';
    }
  }
  GotoTree($tree + 1);
  $this = $node;
}

#binding-context LV_A_Edit

#bind new_xinfo_node to x menu New X-word Node
#bind new_pmcinfo_node to p menu New PMC Node
#bind new_coordinfo_node to c menu New Coordination Node
#bind new_child_node to n menu New Ordinary Node
#insert new_m_node menu New Node with Morphology (forces save, can't be undone)
#bind new_coordcl_struct to C menu New Coordination Construction

#bind set_fixme to f menu Add "FIXME"
#bind set_auto to a menu Add "AUTO"

#bind incorp_in_parent to Ctrl+Up menu Incorporate in Parent
#bind delete_node to Delete menu Delete Leaf Node

#bind normalize_m_ords to Ctrl+w menu Recalculate Word Order (delete empty nodes' ords)
#bind normalize_m_ords_all_trees to Ctrl+W menu Recalculate Word Order for All Trees (might take some time)
#bind normalize_n_ords to Ctrl+n menu Recalculate Node Order (give ords for empty nodes)
#bind normalize_n_ords_all_trees to Ctrl+N menu Recalculate Node Order for All Trees (might take some time)

#bind Save to Ctrl+s menu Save
#bind PerlSearch to Ctrl+h menu Perl-Search
#bind PerlSearchNext to Ctrl+H menu Perl-Search Next

#bind Redraw_All to Ctrl+r menu Redraw
#bind switch_mode to Ctrl+m menu Switch to View Mode
#bind swich_styles_full to Ctrl+t menu Switch on/off Tags
#bind swich_styles_ord to Ctrl+o menu Switch on/off Ordered Layout

#insert passive_subj_to_obj menu Change Passive Voice subj to obj
#insert remove_single_child_x menu Remove Single-childed namedEnt and phrasElem

1;

#endif LV_A_Edit
