#!/usr/bin/env perl
# sinica2pml.pl     pajas@ufal.mff.cuni.cz     2009/12/18 10:17:47

our $VERSION="0.1";

use warnings;
use strict;
$|=1;

use File::Basename qw(basename dirname);
use File::Path qw(mkpath);
use Getopt::Long;
use Pod::Usage;

Getopt::Long::Configure ("bundling");
my %opts = (
  'id-prefix' => 's',
  'encoding' => 'big5-hkscs',
);
GetOptions(\%opts,
#	'debug|D',
#	'quiet|q',
	'help|h',
	'usage|u',
        'version|V',
	'id-prefix|I=s',
	'encoding|e=s',
	'max-sentences|m=i',
	'out-prefix|o=s',
	'man',
       ) or $opts{usage}=1;

if ($opts{usage}) {
  pod2usage(-msg => 'sinica2pml.pl');
}
if ($opts{help}) {
  pod2usage(-exitstatus => 0, -verbose => 1);
}
if ($opts{man}) {
  pod2usage(-exitstatus => 0, -verbose => 2);
}
if ($opts{version}) {
  print "$VERSION\n";
  exit;
}

use strict;
use utf8;

my $max_sentences = $opts{'max-sentences'} || 100;
my $prefix = $opts{'out-prefix'} || 'out';
my $id_prefix = $opts{'id-prefix'} || 's';

my $prefix_dir = dirname($prefix);
if ( length($prefix_dir) and ! -d $prefix_dir ) {
  mkpath($prefix_dir);
}

binmode STDERR, ':utf8';

my $input_file = $ARGV[0] || '-';

if ($input_file eq '-') {
  *I=\*STDIN;
} else {
  open( I,'<:encoding('.$opts{encoding}.')',$input_file )
    or die "Error opening file $input_file: $!";
}

my $sentence_in_file=0;
my $sentences=0;
my $sent_no = 0;
my $id_base = $opts{'id-prefix'};
my $node_no = 0;

while (<I>) {
  chomp;
  my $line = $_;
  next if ($line=~/^%/);
  if ($line=~s{^(#(\S+))? }{ }) {
    my $orig_id = $2 if $1;
    start_out_file() if ($sentence_in_file==0);
    $sentence_in_file++;
    $sent_no++;
    print STDERR "\r$sentence_in_file                       ";
    my $root=SNode->new('#name'=>'root', orig_id => $orig_id, id=>$id_base.$sent_no);
    if ($line=~s{#(.)\((\w+)\)\s*$}{}) {
      $root->{ending}=$1;
      $root->{'ending-type'}=$2;
    }
    print O (node2sinica_pml(sinica_line_to_tree($line, $root),'  '));
    end_out_file() if ($sentence_in_file>=$max_sentences);
  }
}
end_out_file() if ($sentence_in_file>0);
print STDERR "Done\n";

my $fileno = 0;
sub end_out_file {
  print O <<'EOF';
 </trees>
</sinica>
EOF
  close O;
  $sentence_in_file=0;
}

sub start_out_file {
  my $filename = $prefix.sprintf('_%04d.pml',$fileno++);
  open(O, '>:utf8', $filename) or die "Cannot open $filename for writing: $!";
  print STDERR "Writing $filename\n";

  print O <<'EOF';
<?xml version="1.0" encoding="utf-8"?>
<sinica xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
 <head><schema href="sinica_schema.xml"/></head>
 <trees>
EOF
}
sub sinica_line_to_tree {
  my ($line,$root) = @_;
  my $cnt=1;
#  print STDERR "original line: $line\n\n";
  my @stack;
  my %cnt2node;

  my $nonterminal;
  while ($line =~ s/([A-Z][^():|]*)\(([^\(\)]+)\)/[$cnt]/) {
    my $phrasetype = $1;
    my $rest = $2;
    $nonterminal = SNode->new('#name'=>'nonterminal',id=>$id_base.$sent_no.'n'.($node_no++));
#    print STDERR "reduced line: $line\n";
    $nonterminal->{cat} = $phrasetype;
#    print STDERR "creating nonterminal cnt=$cnt  phrase=$phrasetype\n";

    $root->appendChild($nonterminal);

    $cnt2node{$cnt} = $nonterminal;

    foreach my $child (split /\|/,$rest) {
      if ($child=~/([\w\d]+):\[(\d+)\]/) {   # ditetem je uz zredukovany neterminal
#	print STDERR "neterminal role=$1 index=$2\n";
	my $child = $cnt2node{$2};
	if ($child) {
	  $child->{role} = $1;
	  $nonterminal->appendChild($child->unbindNode);
	}
	else {
#	  print STDERR "Nenasel se rodic $1!!!\n";
	}
      }
      elsif ($child=~/^(.+):([\w\d]+):([\w\d]+)$/) { # ditetem je terminal
	my ($role,$pos,$form) = ($1,$2,$3);
#	print STDERR "terminal role=$1 pos=$2 form=$3\n";
	my $terminal_node = SNode->new('#name'=>'terminal',id=>$id_base.$sent_no.'n'.($node_no++));
	$terminal_node->{form} = $form;
	$terminal_node->{role} = $role;
	$terminal_node->{pos} = $pos;
	$nonterminal->appendChild($terminal_node);
      }
      else {
#	print STDERR "nerozeznano: $rest\n"
      }
    }
    $cnt++;
  }
  return $root;
}
sub node2sinica_pml {
  my ($node,$indent)  = @_;
  return '' unless $node;
  my $output;
  if ($node->{'#name'} eq 'root') { # root
    $output = $indent.qq{<root id="$node->{id}"}
      .($node->{orig_id} ? ' orig_id="'.$node->{orig_id}.'"' : '' )
      .($node->{ending} ? ' ending="'.$node->{ending}.'"' : '' )
      .($node->{'ending-type'} ? ' ending-type="'.$node->{'ending-type'}.'"' : '' )
      .">\n"
      .(join "",map{node2sinica_pml($_,$indent.' ')} $node->childNodes)
      .$indent."</root>\n";
  } elsif ($node->{'#name'} eq 'nonterminal') {
    $output = $indent.q{<nonterminal}
      .($node->{id} ? qq{ id="$node->{id}"} : '')
      .($node->{cat} ? qq{ cat="$node->{cat}"} : '')
      .($node->{role} ? qq{ role="$node->{role}"} : '')
      .qq{>\n}
      .(join "",map{node2sinica_pml($_,$indent.' ')} $node->childNodes)
      .$indent."</nonterminal>\n"
  } else { # terminal
    $output = $indent.qq{<terminal id="$node->{id}" role="$node->{role}" pos="$node->{pos}" form="$node->{form}" />\n}
  }
  return $output;
}


package SNode;
use Scalar::Util qw(weaken);

sub new { my $class = shift;return bless({@_}, ref($class) || $class); }
sub appendChild {
  my ($node,$child)=@_;
  $child->unbindNode($child) if ($child->{'#parent'});
  push @{$node->{'#children'}}, $child;
  weaken( $child->{'#parent'}=$node );
  return $child;
}
sub unbindNode {
  my ($node)=@_;
  my $parent = $node->{'#parent'};
  if ($parent) {
    @{$parent->{'#children'}} = grep $_!=$node, @{$parent->{'#children'}};
    delete $node->{'#parent'};
  }
  return $node;
}
sub childNodes {
  my ($node)=@_;
  return @{$node->{'#children'} || []};
}


__END__

=head1 NAME

sinica2pml.pl

=head1 SYNOPSIS

sinica2pml.pl [ -I id-prefix ] input-file  output-file
or
  sinica2pml.pl -u          for usage
  sinica2pml.pl -h          for help
  sinica2pml.pl --man       for the manual page
  sinica2pml.pl --version   for version

=head1 DESCRIPTION

Convert Sinica Treebank files to PML. The output is split
according to --max-sentences (defaults to 100) into files named
C<basename_XXXX.pml> where C<basename> is a filename prefix specified
with --out-prefix (defaults to 'out') and XXXX is a four digit
0-padded integer starting from 0000.

=over 5

=item B<--out-prefix|-o> C<basename>

Output filename prefix.

=item B<--max-sentences|-m> C<N>

Try to create output with maximum C<N> sentences per file. (If some of
the sentences produce more than one tree, then the real number of
trees in the file may be sightly higher).

=item B<--id-prefix|-I> prefix

Use a given string as the prefix for tree IDs

=item B<--usage|-u>

Print a brief help message on usage and exits.

=item B<--help|-h>

Prints the help page and exits.

=item B<--man>

Displays the help as manual page.

=item B<--version>

Print program version.

=back

=head1 AUTHOR

Petr Pajas, E<lt>pajas@sup.ms.mff.cuni.czE<gt>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2009 by Petr Pajas

This program is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.8.2 or,
at your option, any later version of Perl 5 you may have available.

=head1 BUGS

None reported... yet.

=cut
