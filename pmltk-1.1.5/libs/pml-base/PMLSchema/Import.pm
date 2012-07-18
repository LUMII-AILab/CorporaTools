package PMLSchema::Import;
# pajas@ufal.ms.mff.cuni.cz          25 led 2008

use strict;
use warnings;
no warnings 'uninitialized';
use Carp;
use URI;
use PMLSchema::Constants;

use base qw(PMLSchema::XMLNode);

sub get_decl_type     { return(PML_IMPORT_DECL); }
sub get_decl_type_str { return('import'); }

sub schema {
  my ($self)=@_;
  $self=$self->{-parent} while $self->{-parent};
  return $self;
}

sub simplify {
  my ($import,$opts)=@_;
  my $target = $import->schema;
  my $base_url = $target->{URL}||'';
  my $parent = $import->{-parent}; # FIXME: for templates
  return if
    ($parent->get_decl_type == PML_TEMPLATE_DECL and $opts->{no_template_import} or
     $parent->get_decl_type == PML_SCHEMA_DECL and $opts->{no_import});
  die "Missing 'schema' attribute on element  <import> in $base_url!" unless $import->{schema};

  $opts->{schemas}||={};
  my $url = URI->new($import->{schema});

  my $schema = PMLSchema->new({
    (map { ($_=>$opts->{$_}) } qw(schemas use_resources validate)),
    filename => $url,
    base_url => $base_url,
    imported => 1,
    (map {
      exists($import->{$_}) ? ( $_ => $import->{$_} ) : ()
    } qw(revision minimal_revision maximal_revision)),
    revision_error => "Error importing schema %f to $base_url - revision mismatch: %e"
   });
  if ((!exists($import->{type}) and
       !exists($import->{template}) and
       !exists($import->{root})
      ) or defined($import->{type}) and $import->{type} eq '*') {
#    print STDERR "IMPORTING *\n";
    if (ref $schema->{type}) {
      $parent->{type}||={};
      foreach my $name (keys(%{$schema->{type}})) {
	unless (exists $parent->{type}{$name}) {
	  $parent->{type}{$name}=$parent->copy_decl($schema->{type}{$name});
	}
      }
    }
  } else {
    my $name = $import->{type};
#    print STDERR "IMPORTING $name\n";
    if (ref($schema->{type})) {
      $import->_import_type($parent,$schema,$name);
    }
  }
  if ((!exists($import->{type}) and
       !exists($import->{template}) and
       !exists($import->{root})
      ) or defined($import->{template}) and $import->{template} eq '*') {
    if (ref $schema->{template}) {
      $parent->{template}||={};
      foreach my $name (keys(%{$schema->{template}})) {
	unless (exists $parent->{template}{$name}) {
	  $parent->{template}{$name}=$parent->copy_decl($schema->{template}{$name});
	}
      }
    }
  } else {
    my $name = $import->{template};
    if (ref($schema->{template})) {
      unless (exists $parent->{template}{$name}) {
	$parent->{template}{$name}=$parent->copy_decl($schema->{template}{$name});
      }
    }
  }
  if (((!exists($import->{type}) and
       !exists($import->{template}) and
       !exists($import->{root})
      ) or defined($import->{root}) and $import->{root} eq '1') and !exists($parent->{root}) and $schema->{root}) {
    $parent->{root} = $parent->copy_decl($schema->{root});
  }
  return $schema;
}

sub _import_type {
  my ($self,$target,$src_schema, $name) = @_;
  unless (exists $src_schema->{type}{$name}) {
    croak "Cannot import type '$name' from '$src_schema->{URL}' to '$target->{URL}': type not declared in the source schema\n";
  }
  my $type = $src_schema->{type}{$name};
  my %referred = ($name => $type);
  $src_schema->_get_referred_types($type,\%referred);
  foreach my $n (keys %referred) {
    unless (exists $target->{type}{$n}) {
      $target->{type}{$n}=$target->copy_decl($referred{$n});
    } else {
#      print STDERR "already there\n";
    }
  }
}


1;
__END__

=head1 NAME

PMLSchema::Import - a class representing import instructions in a PMLSchema

=head1 DESCRIPTION

This is an auxiliary class  representing import instructions in a PMLSchema.
Note that all import instructions are removed from the schema during parsing.

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
