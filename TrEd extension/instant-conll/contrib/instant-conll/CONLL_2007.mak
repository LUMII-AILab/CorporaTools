# -*- cperl -*-

#ifndef CONLL_2007
#define CONLL_2007

package CONLL_2007;
use strict;

BEGIN { import TredMacro; import PML; }

use ConllBackend;
use Treex::PML::IO;

AddBackend(Treex::PML::ImportBackends('ConllBackend'));

push @TredMacro::AUTO_CONTEXT_GUESSING, sub
{
  my ($hook)=@_;
  #my $resuming = ($hook eq 'file_resumed_hook');
  my $current = CurrentContext();
  if (&is_conll_2007_file)
  {
    SetCurrentStylesheet('conll');# if $resuming;
    return 'CONLL_2007';	#Must much package name.
  }
  return;
};

# Set correct stylesheet when entering this annotation mode.
sub switch_context_hook
{
  SetCurrentStylesheet('conll');
  Redraw() if GUI();
}

# Do not use this mode for other files.
sub allow_switch_context_hook
{
  return 'stop' if (not &is_conll_2007_file);
}

# Check (by schema) if the file opened is suitable for this mode.
sub is_conll_2007_file
{
	return ((PML::SchemaName() or '') eq 'conlldata');
}


#binding-context CONLL_2007
#bind Redraw_All to Alt+r menu Redraw


#endif CONLL_2007
