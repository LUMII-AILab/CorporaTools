# -*- cperl -*-

#ifndef LV_A_PureDependency
#define LV_A_PureDependency

#include "LV_A.mak"

package LV_A_PureDependency;
use strict;

#binding-context LV_A_PureDependency

BEGIN { import TredMacro; import LV_A; }

sub node_release_hook     { 'stop' };
sub enable_attr_hook      { 'stop' };
sub enable_edit_node_hook { 'stop' };

# set correct stylesheet when entering this annotation mode
sub switch_context_hook {
  SetCurrentStylesheet('lv-a-dep-ord');
  disable_node_menu_items() if GUI();
  Redraw() if GUI();
}

# enable menus when leaving this mode
sub pre_switch_context_hook {
  my ($prev,$current)=@_;
  return if $prev eq $current;
  enable_node_menu_items() if GUI();
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
  if (LV_A_PureDependency::is_lvadep_file())
  {
    SetCurrentStylesheet('lv-a-dep-ord') if $resuming;
    return 'LV_A_PureDependency';
  }
  return;
};

# do not use this annotation mode for other files
sub allow_switch_context_hook
{
  return 'stop' if (not LV_A_PureDependency::is_lvadep_file);
}


#bind Redraw_All to Alt+r menu Redraw
#bind swich_styles_full to Alt+f menu Switch On/Off Full-info Layout
#bind swich_styles_ord to Alt+o menu Switch On/Off Ordered Layout

1;

#endif LV_A_PureDependency
