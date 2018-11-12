# -*- cperl -*-

#ifndef LV_M
#define LV_M

#include <contrib/pml/PML.mak>

package LV_M;
use strict;
use MorphoTags;
use LemmaChecker;

BEGIN { import TredMacro; import PML; }

# Set correct stylesheet when entering this annotation mode.
sub switch_context_hook
{
  SetCurrentStylesheet('lv-m');
  Redraw() if GUI();
}

# Status line message.
sub get_status_line_hook
{
  # get_status_line_hook may either return a string
  # or a pair [ field-definitions, field-styles ]
  return unless $this;
  my @mas = ();
  
  if ($this->attr('id'))
  {
	push(@mas, "     id: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->attr('id'));
	push(@mas, [qw({id} value)]);
  }
  elsif ($this->attr('#content/id'))
  {
	push(@mas, "     id: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->attr('#content/id'));
	push(@mas, [qw({id} value)]);
  }
  else
  {
	push(@mas, "     ");
	push(@mas, [qw(label)]);
	push(@mas, $this->{'#name'});
	push(@mas, [qw({#name} value)]);
  }
  
  if ($this->attr('#content/lemma'))
  {
	push(@mas, "     lemma: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->attr('#content/lemma'));
	push(@mas, [qw({lemma} value)]);
  }
  if ($this->attr('#content/tag'))
  {
	push(@mas, "     tag: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->attr('#content/tag'));
	push(@mas, [qw({tag} value)]);
  }
  return [\@mas,
      [
	   "label" => [-foreground => 'black' ],
	   "value" => [-underline => 1 ],
	  ]
	 ];
}

# Insert new node. Mode ('BEFORE' or 'AFTER') identifies, whether to insert
# before or after current node.
sub _new_node
{
  my $mode = shift;
  my $sentid = $root->{'id'};
  my $anchor = $this;
  PlainNewSon($root);
 # Treex::PML::Factory->createTypedNode(
#	'm-m.type', $root->type->schema);
  if ($anchor != $root and $mode eq 'AFTER')
  {
	CutPasteAfter($this, $anchor);
  }
  elsif ($anchor != $root and $mode eq 'BEFORE')
  {
	CutPasteBefore($this, $anchor);
  }
  
  $this->{'#content'} = Treex::PML::Factory->createContainer(0, {
	'id' => $sentid . 'w' . &_get_next_id,
	'form' => 'N/A',
	'tag' => 'N/A',
	'lemma' => 'N/A',
	'src.rf' => 'manual',
	}, 1);

  $this->{'#content'}{'form_change'} = Treex::PML::Factory->createList(['insert'], 1);

  #Redraw_All;
}

sub new_brother_before
{
	&_new_node('BEFORE')
}

sub new_brother_after
{
	&_new_node('AFTER')
}

sub delete_node
{
  TredMacro::PlainDeleteNode($this);
}


# Get next free word ID.
sub _get_next_id
{
  my @nodes = GetNodes($root);
  my %ids = ();
  foreach my $n (@nodes)
  {
	  $ids{$1} = 1
		if ($n->{'#content'}{'id'} =~ /w(\d+)$/);
  }
  my $newid = 1;
  foreach my $key (sort {$a <=> $b} keys(%ids))
  {
    last if ($key > $newid);
	$newid++ if ($key == $newid);
  }
  return $newid;
}


# Check if the current file is standard Latvian Treebank M file.
sub is_lvm_file
{
  return (((PML::SchemaName()||'') eq 'lvmdata') ? 1 : 0);
}


# Add context guesser.
push @TredMacro::AUTO_CONTEXT_GUESSING, sub
{
  my ($hook)=@_;
  my $resuming = ($hook eq 'file_resumed_hook');
  my $current = CurrentContext();
  if (LV_M::is_lvm_file)
  {
    SetCurrentStylesheet('lv-m') if $resuming;
    return 'LV_M';
  }
  return;
};

# do not use this annotation mode for other files
sub allow_switch_context_hook
{
  return 'stop' if (not LV_M::is_lvm_file);
}

# Do not allow to move nodes.
sub node_release_hook
{
  return 'stop';
}

sub get_extendend_morpho
{
	return MorphoTags::getAVPairsFromSimpleTag(@_);
}

sub get_tag_errors
{
	return MorphoTags::checkSimpleTag(@_);
}

sub get_lemma_errors
{
	return LemmaChecker::checkLemmaByTag(@_);
}


#binding-context LV_M

#bind Save to Ctrl+s menu Save
#bind Redraw_All to Ctrl+r menu Redraw

#bind new_brother_after to a menu New Node After Current
#bind new_brother_before to b menu New Node Before Current
#bind delete_node to Delete menu Delete Leaf Node

1;

#endif LV_M
