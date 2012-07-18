package PMLSchema::Constant;
# pajas@ufal.ms.mff.cuni.cz          25 led 2008

use strict;
use warnings;
no warnings 'uninitialized';
use Carp;

use PMLSchema::Constants;
use base qw( PMLSchema::Decl );

=head1 NAME

PMLSchema::Constant - implements constant declaration.

=head1 INHERITANCE

This class inherits from L<PMLSchema::Decl>.

=head1 METHODS

See the super-class for the complete list.

=over 3

=item $decl->get_decl_type ()

Returns the constant PML_CONSTANT_DECL.

=item $decl->get_decl_type_str ()

Returns the string 'constant'.

=item $decl->get_value ()

Return the constant value.

=item $decl->get_values ()

Returns a singleton list consisting of the constant value (for
compatibility with choice declarations).

=back

=cut


sub is_atomic { 1 }
sub get_decl_type { return PML_CONSTANT_DECL; }
sub get_decl_type_str { return 'constant'; }
sub get_content_decl { return(undef); }
sub get_value { return $_[0]->{value}; }
sub get_values { my @val=($_[0]->{value}); return @val; }
sub init {
  my ($self,$opts)=@_;
  $self->{-parent}{-decl} = 'constant';
}

sub validate_object {
  my ($self, $object, $opts) = @_;
  my $const = $self->{value};
  my $ok = ($object eq $const) ? 1 : 0;
  if (!$ok and ref($opts) and ref($opts->{log})) {
    my $path = $opts->{path};
    my $tag = $opts->{tag};
    $path.="/".$tag if $tag ne q{};
    push @{$opts->{log}}, "$path: invalid constant, should be '$const', got: '$object'";
  }
  return $ok;
}

sub serialize_get_children {
  my ($self,$opts)=@_;
  my $writer = $opts->{writer} || croak __PACKAGE__."->serialize: missing required option 'writer'!\n";
  return [undef, $self->{value}];
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

