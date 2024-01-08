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
  SetCurrentStylesheet('lv-a-full-compact-ord');
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

#bind Redraw_All menu Redraw
#bind switch_mode to Ctrl+m menu Switch to Edit mode
#bind swich_styles_vert to Ctrl+h menu Switch horizontal/vertical layout
#bind swich_styles_full to Ctrl+t menu Switch on/off tags
#bind switch_styles_compact to Ctrl+k menu Switch on/off compact layout

1;

#endif LV_A_View
