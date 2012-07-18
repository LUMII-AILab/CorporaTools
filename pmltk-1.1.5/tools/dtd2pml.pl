#!/usr/bin/perl

use IO;
use SGML::DTDParse::DTD;
use XML::Writer;

use constant {
  PML_NS => "http://ufal.mff.cuni.cz/pdt/pml/",
  SCHEMA_NS => "http://ufal.mff.cuni.cz/pdt/pml/schema/",
  PML_VERSION => 1.1,
};

use Pod::Usage;
use Getopt::Long;
Getopt::Long::Configure ("bundling");
my %opts = (
 suffix=>'.type',
 prefix=>'',
);
GetOptions(\%opts,
	'debug|D',
	'suffix|s=s',
	'prefix|p=s',
	'help|h',
	'usage|u',
	'man',
       ) or $opts{usage}=1;


pod2usage(-msg => '('.$0.')') if @ARGV!=3;

my ($dtd_file,$pml_file,$root_element) = @ARGV;


my $DEBUG = $opts{debug};
sub _DEBUG { print STDERR @_ if $DEBUG }

if ($opts{usage}) {
  pod2usage(-msg => '('.$0.')');
}
if ($opts{help}) {
  pod2usage(-exitstatus => 0, -verbose => 1);
}
if ($opts{man}) {
  pod2usage(-exitstatus => 0, -verbose => 2);
}


$dtd = SGML::DTDParse::DTD->new(
  'Verbose'             => 0,
  'Debug'               => 0,
  'SgmlCatalogFilesEnv' => 0,
  'UnexpandedContent'   => 0,
  'SourceDtd'           => $dtd_file,
  'Xml'                 => 1,
);
$dtd->parse($dtd_file || '-');

open my $out, '>:utf8', $pml_file if $pml_file ne '';
my $w =  new XML::Writer(OUTPUT => $out || \*STDOUT,
			 DATA_MODE => 1, 
			 DATA_INDENT => 1);
use Data::Dumper;
#print STDERR join ",",keys %$dtd,"\n";


#print Dumper([ keys %$dtd ]);
#print Dumper([ grep { ref $_ } @{$dtd->{DECLS}} ]);
#print Dumper($dtd->{DECLS});

sub type_name { $opts{prefix}.$_[0].$opts{suffix} }

$w->xmlDecl("utf-8");
$w->startTag('pml_schema', 
	     xmlns => SCHEMA_NS, 
	     version => PML_VERSION);

if ($root_element) {
  $w->emptyTag('root', name => $root_element, type => type_name($root_element));
}

foreach my $element (grep { UNIVERSAL::isa($_, 'SGML::DTDParse::DTD::ELEMENT') } 
		     $dtd->declarations) {
  my $name = $element->name;
  my $content = $element->content_model;
  my $attributes = $dtd->{'ATTR'};
  my $attlist = $attributes->{$name};
  my @attrs = ($attlist ? $attlist->attribute_list() : ());

  $w->startTag('type', name => type_name($name));
  my $container_type = $content=~/[+*]|\#PCDATA|^EMPTY/ ? 'container' : 'structure';
  my $container = 0;
  if (@attrs or $content =~ /^EMPTY/ or ($container_type eq 'structure')) {
    my @attribute_tag = $container_type eq 'structure' ? ('member', as_attribute=>1) : ('attribute');
    $container = 1;
    $w->startTag($container_type);
    foreach my $attr (@attrs) {
      if ($attr=~/^xmlns/) {
	warn "Ignoring attribute declaring a namespace - $attr\n";
	next;
      }
      my $required = ($attlist->attribute_type($attr) eq '#REQUIRED') ? 1 :  0;
      my $vals = $attlist->attribute_values($attr);
      $w->startTag(@attribute_tag,
		   name => $attr,
		   ( $vals eq 'ID'  ? ( role => '#ID' ) : ()),
		   ( $required ? ( 'required' => 1) : () )
		  );
      my $format;
      if ($vals =~ /^(?:ID|IDREFS|NMTOKEN|NMTOKENS)$/) {
	$w->emptyTag('cdata', format => $vals);
      } elsif ($vals eq 'IDREF') { # approx
	$w->emptyTag('cdata', format => 'PMLREF');
      } elsif ($vals =~ /^\((.*)\)$/) {
	my $choice = $1;
	$w->startTag('choice');
	for my $value (split /\|/, $choice) {
	  $w->startTag('value');
	  $w->characters($value);
	  $w->endTag('value');
	}
	$w->endTag('choice');
      } elsif ($vals eq 'CDATA') {
	$w->emptyTag('cdata', format => 'any');
      } else {
	warn("Unsupported attribute value type $vals for attribute $attr of $name; defaulting to format='any'\n");
	$w->emptyTag('cdata', format => 'any');
      }
      $w->endTag($attribute_tag[0]);
    }
  }
  if ($content =~ /^EMPTY/) {
  } elsif ($content =~ /^\(/) {
    if ($content eq '(#PCDATA)') {
      $w->emptyTag('cdata', format => 'any');
    } else {
      $content =~ s/\#PCDATA/\#TEXT/g;
      my $seq = 0;
      my $element_type;
      if ($container_type eq 'structure') {
	$element_type = 'member';
      } else {
	$seq=1;
	$element_type = 'element';
	$w->startTag('sequence', content_pattern => $content);
      }
      my $names = $content; $names =~ s/[(),|+*?]/ /g;
      my %content = map { $_ => 1 } grep { $_ ne '' } split /\s+/, $names;
      my $text = delete $content{'#TEXT'};
      if ($text) {
	$w->emptyTag('text');
      }
      foreach my $element (keys %content) {
	$w->emptyTag($element_type, name => $element, type => type_name($element));
      }
      $w->endTag('sequence') if $seq;
    }
  } else {
    warn "Unrecognized content model for element '$name': $content\n";
  }
  $w->endTag($container_type) if $container;
  $w->endTag('type');
}
$w->endTag('pml_schema');

__END__

=head1 NAME

dtd2pml.pl

=head1 SYNOPSIS

dtd2pml.pl [-p string] [-s string] dtd_in.dtd   pml_schema_out.xml   root_element_name 
or
  dtd2pml.pl -u          for usage
  dtd2pml.pl -h          for help
  dtd2pml.pl --man       for the manual page
  dtd2pml.pl --version   for version

=head1 DESCRIPTION

dtd2pml.pl is a tool that automatically converts XML DTD's to PML
schemas. The command takes three arguments: the filename of an input
DTD, a filename for the resulting PML Schema file and name of the
element declared in the DTD that should be considered as the root
element of the PML Schema.

The convertor creates a named type for every element declared in the
DTD.  If the DTD contains an element named 'foo', a type named
'PREFIXfooSUFFIX' will be created in the PML Schema, where PREFIX
and SUFFIX are specified using the B<-p> and B<-s> options.
PREFIX defaults to the empty string, SUFFIX defaults to '.type'.

A XML document conforming to a given DTD whose root element name is
root_element_name (specified during conversion) can be translated to a
document conforming to the resulting PML Schema by:

=over 5

=item removing a <!DOCTYPE ...> declaration

=item adding a xmlns="http://ufal.ms.mff.cuni.cz/pdt/pml/" attribute to the root element. See LIMITATIONS.

=back

=head2 OPTIONS

=over 5

=item B<--prefix|-p> string

Prepend given prefix string to every type name declared in the PML
Schema. Defaults to ''.

=item B<--suffix|-s> string

Prepend given suffix string to every type name declared in the PML Schema.
Defaults to '.type'.

=item B<--usage|-u>

Print a brief help message on usage and exits.

=item B<--help|-h>

Prints the help page and exits.

=item B<--man>

Displays the help as manual page.

=item B<--version>

Print program version.

=back

=head1 LIMITATIONS

Since PML does not (yet) support non-PML namespaces in PML instances,
this program works correctly only for DTDs describing documents that
do not use namespaces and documents whose all elements are in a
default.

=head1 BUGS

An attribute value declared in <!ATTLIST ...> list containing dashes
(e.g. ---), can be incorrectly translated (e.g. into three same values
of a choice type: '-','-','-'). This seems to be a bug in the DTD
parser module (SGML::DTDParse).

=head1 AUTHOR

Petr Pajas, E<lt>pajas@sup.ms.mff.cuni.czE<gt>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2007 by Petr Pajas

This program is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.8.2 or,
at your option, any later version of Perl 5 you may have available.

=head1 BUGS

None reported... yet.

=cut
