# -*- cperl -*-

#ifndef CONLL_U
#define CONLL_U

package CONLL_U;
use strict;

BEGIN { import TredMacro; import PML; }

use ConllBackend;
use Treex::PML::IO;

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

# Set correct stylesheet when entering this annotation mode.
sub switch_context_hook
{
  SetCurrentStylesheet('conllu');
  Redraw() if GUI();
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


#binding-context CONLL_U
#bind Redraw_All to Alt+r menu Redraw


#endif CONLL_U
