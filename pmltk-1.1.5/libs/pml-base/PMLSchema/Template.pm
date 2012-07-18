package PMLSchema::Template;
# pajas@ufal.ms.mff.cuni.cz          25 led 2008

use strict;
use warnings;
no warnings 'uninitialized';
use Carp;
use PMLSchema::Constants;
use base qw(PMLSchema::XMLNode);

sub get_decl_type     { return(PML_TEMPLATE_DECL); }
sub get_decl_type_str { return('template'); }

sub simplify {
  my ($template,$opts)=@_;
  for my $c (
    sort {$a->{'-#'} <=> $b->{'-#'}}
      ((map { @{$template->{$_}} } grep {exists $template->{$_} } qw(copy import)),
       (map { values %{$template->{$_}} } grep {exists $template->{$_} } qw(template derive)))) {
    #    print STDERR "Processing <$c->{-xml_name}>\n";
    $c->simplify($opts);
  }
  delete $template->{template} unless $opts->{'preserve_templates'};
  delete $template->{copy} unless $opts->{'no_copy'};
  for (qw(derive import)) {
    if ($template->get_decl_type == PML_TEMPLATE_DECL) {
      delete $template->{$_} unless $opts->{'no_template_'.$_};
    } else {
      delete $template->{$_} unless $opts->{'no_'.$_};
    }
  }
}
sub for_each_decl {
  my ($self,$sub)=@_;
  $sub->($self);
  for my $d (qw(template type)) {
    if (ref $self->{$d}) {
      foreach (values %{$self->{$d}}) {
	$_->for_each_decl($sub);
      }
    }
  }
}



1;
__END__
# Below is stub documentation for your module. You'd better edit it!

=head1 NAME

PMLSchema::Template - a class representing templates in a PMLSchema

=head1 SYNOPSIS

use PMLSchema::Template;

=head1 DESCRIPTION

This class represents templates in a PMLSchema and is also a base for
PMLShema class itself.

=head2 EXPORT

None by default.

=head1 SEE ALSO

L<PMLSchema>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2008 by Petr Pajas

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.8.2 or,
at your option, any later version of Perl 5 you may have available.

=head1 BUGS

None reported... yet.

=cut

