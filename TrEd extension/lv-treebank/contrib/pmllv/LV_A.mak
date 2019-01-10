# -*- cperl -*-

#ifndef LV_A
#define LV_A

#include <contrib/pml/PML.mak>

package LV_A;
use strict;
use MorphoTags;
use SyntaxChecker;
use LemmaChecker;

BEGIN { import TredMacro; import PML; }

# Switch on/of vertical layout, if appropriate stylesheet is provided.
sub swich_styles_vert
{
  my $st = GetCurrentStylesheet();
  if ($st =~ /-vert/) 
  {
	$st =~ s/-vert//;
  } else
  {
	if ($st =~ /-ord/)
	{
	  $st =~ s/-ord/-vert-ord/;
	} else
	{
	  $st = $st.'-vert-ord';
	}
  }
  SetCurrentStylesheet($st) if StylesheetExists($st);
}

sub switch_mode
{
  my $mode = CurrentContext();
  SwitchContext('LV_A_View') if ($mode eq 'LV_A_Edit');
  SwitchContext('LV_A_Edit') if ($mode eq 'LV_A_View');
}

sub switch_styles_compact
{
  my $st = GetCurrentStylesheet();
  if ($st =~ /-compact/) 
  {
	$st =~ s/-compact//;
  } else
  {
    $st =~ s/-vert// if ($st =~ /-vert/);
	if ($st =~ /-ord/)
	{
	  $st =~ s/-ord/-compact-ord/;
	} else
	{
	  $st = $st.'-compact';
	}
  }
  SetCurrentStylesheet($st) if StylesheetExists($st);

}

# Switch on/of print-layout, if appropriate stylesheet is provided.
sub swich_styles_full
{
  my $st = GetCurrentStylesheet();
  if ($st =~ /-full/) 
  {
	$st =~ s/-full//;
  } else
  {
	$st =~ s/^(lv-a(-edit)?)/$1-full/;
  }
  SetCurrentStylesheet($st) if StylesheetExists($st);
}

# Switch on/of orderedlayout, if appropriate stylesheet is provided.
sub swich_styles_ord
{
  my $st = GetCurrentStylesheet();
  if ($st =~ /-ord$/) 
  {
	$st =~ s/-ord$//;
  } else
  {
	$st = $st.'-ord';
  }
  SetCurrentStylesheet($st) if StylesheetExists($st);
}


sub get_extendend_morpho
{
	return MorphoTags::getAVPairsFromAnyTag(@_);
}

sub get_tag_errors
{
	return MorphoTags::checkAnyTag(@_);
}

sub get_tag_for_xType_errors
{
	return MorphoTags::isSubTagAllowedForPhraseType(@_);
}

sub get_lemma_errors
{
	return LemmaChecker::checkLemmaByTag(@_);
}

sub get_all_morphomorpho_errors
{
   my $node = shift;
   my @res = ();
   push @res, @{get_lemma_errors($node->attr('m/lemma'), $node->attr('m/tag'))};
   push @res, @{get_tag_errors($node->attr('m/tag'))};
   return \@res;
}

sub get_all_morphosynt_errors
{
   my $node = shift;
   my @res = ();
   push @res, @{get_tag_errors($node->attr('tag'))};
   push @res, @{get_tag_errors($node->attr('reduction'))};
   push @res, @{get_tag_for_xType_errors($node->attr('tag'),$node->attr('xtype')) }
	 if ($node->attr('#name') eq 'xinfo');
   return \@res;
}

sub get_structural_errors
{
	return SyntaxChecker::get_structural_errors(@_);
}

sub get_all_errors
{
   my $node = shift;
   my @res = ();
   push @res, @{get_all_morphomorpho_errors($node)};
   push @res, @{get_all_morphosynt_errors($node)};
   push @res, @{get_structural_errors($node)};
   return \@res;
}

sub has_nondep_child
{
	return PMLLVHelpers::has_nondep_child(@_);
}

#sub is_unfinished
#{
#	return SyntaxChecker::is_unfinished(@_);
#}

#sub is_allowed_for_parent
#{
#	return SyntaxChecker::is_role_allowed_for_parent(@_, $root);
#}

#sub is_wrong_empty_node
#{
#	return not SyntaxChecker::is_allowed_to_be_empty(@_);
#}


# Determine wether the given node (xinfo or pmcinfo) is apropriate for it's
# children hiding - for dependency-only stylesheet.
sub hidable_type
 {
  my $node = shift;
  if ($node->{'#name'} eq 'xinfo')
  {
	return 1;
	
  } elsif ($node->{'#name'} eq 'pmcinfo')
  {
    return 1 if ($node->{'pmctype'} eq 'quot' or
	             $node->{'pmctype'} eq 'abbr' or
				 $node->{'pmctype'} eq 'address' or
				 $node->{'pmctype'} eq 'numPmc' or
				 $node->{'pmctype'} eq 'spcPmc' or
				 $node->{'pmctype'} eq 'interj' or
				 $node->{'pmctype'} eq 'particle');
	return 0;
  }
  return 0;
}

# Status line mesidge.
sub get_status_line_hook
{
  # get_status_line_hook may either return a string
  # or a pair [ field-definitions, field-styles ]
  return unless $this;
  my @mas = ();
  if ($this->{id})
  {
	push(@mas, "     id: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->{id});
	push(@mas, [qw({id} value)]);
  } else
  {
	push(@mas, "     ");
	push(@mas, [qw(label)]);
	push(@mas, $this->{'#name'});
	push(@mas, [qw({#name} value)]);
  }
  if ($this->attr('m/lemma'))
  {
	push(@mas, "     m/lemma: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->attr('m/lemma'));
	push(@mas, [qw({m/lemma} value)]);
  }
  if ($this->attr('m/tag'))
  {
	push(@mas, "     m/tag: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->attr('m/tag'));
	push(@mas, [qw({m/tag} value)]);
  }
  if ($this->attr('tag'))
  {
	push(@mas, "     tag: ");
	push(@mas, [qw(label)]);
	push(@mas, $this->attr('tag'));
	push(@mas, [qw({tag} value)]);
  }
  return [\@mas,
      [
	   "label" => [-foreground => 'black' ],
	   "value" => [-underline => 1 ],
	  ]
	 ];
}

# Check if the current file is standard Latvian Treebank file.
sub is_lva_file
{
  return (((PML::SchemaName()||'') eq 'lvadata') ? 1 : 0);
}

push @TredMacro::AUTO_CONTEXT_GUESSING, sub
{
  my ($hook)=@_;
  my $resuming = ($hook eq 'file_resumed_hook');
  my $current = CurrentContext();
  if (LV_A::is_lva_file())
  {
    SetCurrentStylesheet('lv-a') if $resuming;
    return 'LV_A_View';
  }
  return;
};

# do not use this annotation mode for other files
sub allow_switch_context_hook
{
  return 'stop' if (not LV_A::is_lva_file);
}

1;

#endif LV_A

