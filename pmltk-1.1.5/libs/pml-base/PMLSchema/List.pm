package PMLSchema::List;
# pajas@ufal.ms.mff.cuni.cz          25 led 2008

use strict;
use warnings;
no warnings 'uninitialized';
use Carp;

use PMLSchema::Constants;
use base qw( PMLSchema::Decl );

=head1 NAME

PMLSchema::List - implements declaration of a list.

=head1 INHERITANCE

This class inherits from L<PMLSchema::Decl>.

=head1 METHODS

See the super-class for the complete list.

=over 3

=item $decl->get_decl_type ()

Returns the constant PML_LIST_DECL.

=item $decl->get_decl_type_str ()

Returns the string 'list'.

=item $decl->get_content_decl ()

Return type declaration of the list members.

=item $decl->is_ordered ()

Return 1 if the list is declared as ordered.

=back

=cut

sub is_atomic { 0 }
sub get_decl_type { return PML_LIST_DECL; }
sub get_decl_type_str { return 'list'; }
sub is_ordered { return $_[0]->{ordered} }

sub init {
  my ($self,$opts)=@_;
  $self->{-parent}{-decl} = 'list';
}


sub validate_object {
  my ($self, $object, $opts) = @_;

  my ($path,$tag,$flags);
  my $log = [];
  if (ref($opts)) {
    $flags = $opts->{flags};
    $path = $opts->{path};
    $tag = $opts->{tag};
    $path.="/".$tag if $tag ne q{};
  }
  if (ref($object) eq 'Fslib::List') {
    my $lm_decl = $self->get_knit_content_decl;
    for (my $i=0; $i<@$object; $i++) {
      $lm_decl->validate_object($object->[$i], {
	flags => $flags,
	path=> $path,
	tag => "[".($i+1)."]",
	log => $log,
      });
    }
  } else {
    push @$log, "$path: unexpected content of a list: $object";
  }
  if ($opts and ref($opts->{log})) {
    push @{$opts->{log}}, @$log;
  }
  return @$log ? 0 : 1;
}

1;
__END__

=head1 SEE ALSO

L<PMLSchema::Decl>, L<PMLSchema>.

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2008 by Petr Pajas

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.8.2 or,
at your option, any later version of Perl 5 you may have available.

=head1 BUGS

None reported... yet.

=cut

