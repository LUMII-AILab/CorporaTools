package PMLSchema::Element;
# pajas@ufal.ms.mff.cuni.cz          25 led 2008

use strict;
use warnings;
no warnings 'uninitialized';
use Carp;

use PMLSchema::Constants;
use base qw( PMLSchema::Decl );

=head1 NAME

PMLSchema::Element - implements declaration of an element of a
sequence.

=head1 INHERITANCE

This class inherits from L<PMLSchema::Decl>.

=head1 METHODS

See the super-class for the complete list.

=over 3

=item $decl->get_decl_type ()

Returns the constant PML_ELEMENT_DECL.

=item $decl->get_decl_type_str ()

Returns the string 'element'.

=item $decl->get_name ()

Return name of the element.

=item $decl->get_parent_sequence ()

Return the sequence declaration the member belongs to.

=back

=cut

sub is_atomic { undef }
sub get_decl_type { return PML_ELEMENT_DECL; }
sub get_decl_type_str { return 'element'; }
sub get_name { return $_[0]->{-name}; }
*get_parent_sequence = \&PMLSchema::Decl::get_parent_decl;

sub validate_object {
  shift->get_content_decl->validate_object(@_);
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

