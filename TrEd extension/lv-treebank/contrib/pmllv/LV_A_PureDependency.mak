# -*- cperl -*-

#ifndef LV_A_PureDependency
#define LV_A_PureDependency

#include "LV_A.mak"

package LV_A_PureDependency;

#binding-context LV_A_PureDependency

BEGIN { import LV_A; }

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

#bind GotoTree to Alt+g menu Goto Tree

#bind Redraw_All to Alt+r menu Redraw
#bind swich_styles_full to Alt+f menu Switch On/Off Full-info Layout
#bind swich_styles_ord to Alt+o menu Switch On/Off Ordered Layout

1;

#endif LV_A_PureDependency
