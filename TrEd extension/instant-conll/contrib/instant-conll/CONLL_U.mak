# -*- cperl -*-

#ifndef CONLL_U
#define CONLL_U

package CONLL_U;
use strict;

#include <contrib/support/arrows.inc>
#include <contrib/pml/PML.mak>

BEGIN { import TredMacro; import PML; }

use ConllBackend;
use Treex::PML::IO;
use Data::Dumper;

AddBackend(Treex::PML::ImportBackends('ConlluBackend'));

push @TredMacro::AUTO_CONTEXT_GUESSING, sub
{
  my ($hook)=@_;
  #my $resuming = ($hook eq 'file_resumed_hook');
  my $current = CurrentContext();
  if (&is_conll_u_file)
  {
    SetCurrentStylesheet('conllu');# if $resuming;
    return 'CONLL_U';	#Must much package name.
  }
  return;
};

# No edditing in this mode for now.
sub node_release_hook     { 'stop' }
# No edditing in this mode for now.
sub enable_attr_hook      { 'stop' }
# No edditing in this mode for now.
sub enable_edit_node_hook { 'stop' }


# Set correct stylesheet when entering this annotation mode.
sub switch_context_hook
{
  SetCurrentStylesheet('conllu');
  disable_node_menu_items() if GUI(); # No edditing in this mode for now.
  Redraw() if GUI();
}

# No edditing in this mode for now.
sub pre_switch_context_hook {
  my ($prev,$current)=@_;
  return if $prev eq $current;
  enable_node_menu_items() if GUI();
}

# Do not use this mode for other files.
sub allow_switch_context_hook
{
  return 'stop' if (not &is_conll_u_file);
}

# Check (by schema) if the file opened is suitable for this mode.
sub is_conll_u_file
{
	return ((PML::SchemaName() or '') eq 'conlludata');
}

sub root_style_hook
{
    DrawArrows_init();
}

sub after_redraw_hook
{
    DrawArrows_cleanup();
}

## TODO: REDRAW on edit.

sub node_style_hook {
  my ($node, $styles)=@_;
  if ($node->{'deps'})
  {
    # draw enhanced dependencies
    my %common = (
      -arrow => 'first',
      -smooth => 1,
      -arrowshape => '16,18,3',
      -width => 2,
      -dash => '',
      -frac => 0.05,
      -fill => 'SaddleBrown',
	);
	my @edges = ();
	for my $dep (@{$node->{'deps'}})
	{
		my %edge = (
		    '-target' => SearchNodeById($dep->attr('head'), $node->root),
			'-tag' => $dep->attr('label'),
			'-hint' => $dep->attr('label'),
		    );
		if ($dep->attr('head') eq '0')
		{
			$edge{'-target'} = $node->root;
		}
		push @edges,{%edge, ()};
	}
	DrawArrows($node,$styles,\@edges,\%common);
	
  }
  return 1;
}

sub SearchNodeById {
	my ($id, $root) = @_;
	my @todo = ($root);
	while (@todo)
	{
		my $node = shift @todo;
		return $node if ($node->attr('id') eq $id);
		push @todo, $node->children;
	}
}

#binding-context CONLL_U
#bind Redraw_All to Alt+r menu Redraw


#endif CONLL_U
