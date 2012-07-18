#!/usr/bin/perl

# Abstract: convert files from the Penn Arabic Treebank to PML
# The input must be in the AG XML format

use strict;
use Carp;

$|=1;   # flush on write

use Fslib qw(ImportBackends AddResourcePath);
use FindBin;
use lib "$FindBin::RealBin/../libs";
use Pod::Usage;
my $VERSION = '1.1';

use Getopt::Long;
Getopt::Long::Configure ("bundling");
my %opts = ('output-dir' => '.');
GetOptions(\%opts,
	'output-dir|o',
	'quiet|q',
	'help|h',
	'usage|u',
        'version|V',
	'man',
       ) or $opts{usage}=1;


if (@ARGV==0 or $opts{usage}) {
  pod2usage(-msg => 'pml_simplify');
}
if ($opts{help}) {
  pod2usage(-exitstatus => 0, -verbose => 1);
}
if ($opts{man}) {
  pod2usage(-exitstatus => 0, -verbose => 2);
}
if ($opts{version}) {
  print "pml_simplify version: $VERSION\n";
  exit 0;
}

AddResourcePath("$FindBin::RealBin/../resources");

my @backends = ImportBackends(qw(AG2PML PMLBackend));
for my $f (@ARGV) {
  my $out = $f.'.pml';
  my (undef,undef,$base)=File::Spec->splitpath($out);
  $out = File::Spec->catfile($opts{'output-dir'}, $base);

  print "$f => $out\n" unless $opts{quiet};
  my $doc = FSFile->load($f,{
    backends=>\@backends,
  });
  if ( -f $out ) {
    if ( -f $out.'~' ) {
      unlink $out.'~';
    }
    rename($out, $out.'~') or warn "Cannot rename $out to $out~: $!\n";
  }
  $doc->changeFilename($out);
  $doc->changeBackend('PMLBackend');
  $doc->save();
}

############################################################

__END__

=head1 atb2pml.pl

atb2pml.pl - convert a modular PML schema to a simplified PML schema

=head1 SYNOPSIS


atb2pml.pl [ --output-dir path ] [options] file.xml [...]

Get help:

  atb2pml.pl  -u|--usage          for usage (synopsis)
  atb2pml.pl  -h|--help           for help
  atb2pml.pl  --man               for the manual page

=head1 DESCRIPTION

This program converts files from the Penn Arabic Treebank 2.0 format
to PML.  The input is an AG XML file (the related *.txt and *.sgm
files are loaded automatically).

The base-names of the output files are the same as those of the input
files with the suffix '.pml' appended. Use the C<--output-dir> option
to specify the target directory (defaults to the current working
directory) .

=head1 OPTIONS

=over 4

=item B<--output-dir|-o> path

Write output files to a given directory.

=item B<--quiet>

Suppress output of the progress information (source-file => target-file).

=item B<--debug|-D>

Print lots of debugging messages on the standard error output.

=item B<--version|-V>

Print this program's version number and quit.

=item B<--help|-h>

Print this help and exit.

=item B<--man>

Show this help as a manual page and exit.


=item B<--usage>

Print usage and exit.

=back

=head1 AUTHOR

Petr Pajas, E<lt>pajas@ufal.ms.mff.cuni.czE<gt>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2006 by Petr Pajas

This program is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.8.2 or,
at your option, any later version of Perl 5 you may have available.

=head1 BUGS

None reported... yet.

=cut
