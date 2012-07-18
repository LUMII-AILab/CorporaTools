package PMLSchema::Root;
# pajas@ufal.ms.mff.cuni.cz          25 led 2008

package PMLSchema::Root;
use strict;
use warnings;
no warnings 'uninitialized';
use Carp;
use PMLSchema::Constants;
use base qw( PMLSchema::Decl );

=head1 NAME

PMLSchema::Root - implements root PML-schema declaration

=head1 INHERITANCE

This class inherits from L<PMLSchema::Decl>.

=head1 METHODS

See the super-class for the complete list.

=over 3

=item $decl->get_name ()

Returns the declared PML root-element name.

=item $decl->get_decl_type ()

Returns the constant PML_ROOT_DECL.

=item $decl->get_decl_type_str ()

Returns the string 'root'.

=item $decl->get_content_decl ()

Returns declaration of the content type.

=cut

sub is_root { 1 }
sub is_atomic { undef }
sub get_decl_type { return PML_ROOT_DECL; }
sub get_decl_type_str { return 'root'; }
sub get_name { return $_[0]->{name}; }
sub validate_object {
  my $self = shift;
  $self->get_content_decl->validate_object(@_);
}

=back

=cut


1;

=head1 SEE ALSO

L<PMLSchema::Decl>, L<PMLSchema>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2008 by Petr Pajas

This library is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.8.2 or,
at your option, any later version of Perl 5 you may have available.

=head1 BUGS

None reported... yet.

=cut

