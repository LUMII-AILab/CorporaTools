# -*- cperl -*-

#ifndef instConll2007
#define instConll2007

package instConll2007;
use strict;

BEGIN {
  import TredMacro;
  import ConllBackend;
  import PML;
  import Treex::PML::IO;
}

AddBackend(Treex::PML::ImportBackends('ConllBackend'));

#sub get_backends_hook
#{
#	print 'Izsauc huuku.';
#	return ('ConllBackend', ConllBackend->new, @_);
#}

push @TredMacro::AUTO_CONTEXT_GUESSING, sub
{
  my ($hook)=@_;
  my $resuming = ($hook eq 'file_resumed_hook');
  my $current = CurrentContext();
  if (((PML::SchemaName()||'') eq 'conlldata'))
  {
    SetCurrentStylesheet('conll') if $resuming;
    return 'CONLL_2007';
  }
  return;
};

# set correct stylesheet when entering this annotation mode
sub switch_context_hook
{
  SetCurrentStylesheet('conll');
  Redraw() if GUI();
}


#binding-context CONLL_2007
#bind Redraw_All to Alt+r menu Redraw


#endif instConll2007
