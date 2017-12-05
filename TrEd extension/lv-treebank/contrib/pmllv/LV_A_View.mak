# -*- cperl -*-

#ifndef LV_A_View
#define LV_A_View

#include "LV_A.mak"

package LV_A_View;
use strict;

BEGIN { import TredMacro; import LV_A; }

sub node_release_hook     { 'stop' };
sub enable_attr_hook      { 'stop' };
sub enable_edit_node_hook { 'stop' };

# set correct stylesheet when entering this annotation mode
sub switch_context_hook {
  SetCurrentStylesheet('lv-a-compact-ord');
  disable_node_menu_items() if GUI();
  Redraw() if GUI();
}

# enable menus when leaving this mode
sub pre_switch_context_hook {
  my ($prev,$current)=@_;
  return if $prev eq $current;
  enable_node_menu_items() if GUI();
}

#binding-context LV_A_View

#bind Redraw_All to Alt+r menu Redraw
#bind swich_styles_vert to Alt+v menu Switch On/Off Vertical Layout
#bind swich_styles_full to Alt+f menu Switch On/Off Full-info Layout
#bind swich_styles_ord to Alt+o menu Switch On/Off Ordered Layout
#bind switch_styles_compact to Alt+c menu Switch On/Off Compact Layout
#bind switch_mode to Alt+m menu Switch to Edit Mode

1;

#endif LV_A_View
