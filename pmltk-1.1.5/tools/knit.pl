#!/usr/bin/env perl
# knit.pl     pajas@ufal.mff.cuni.cz     2006/09/26 12:24:08

# Abstract: load a PML instance and save it back preserving all knitted material embedded

use warnings;
use strict;
$|=1;

use Getopt::Long;
use Pod::Usage;
Getopt::Long::Configure ("bundling");
my %opts;
GetOptions(\%opts,
	'debug|D',
	'help|h',
	'usage|u',
        'version|V',
	'man',
       ) or $opts{usage}=1;

if ($opts{usage}) {
  pod2usage(-msg => 'foreach_match.pl');
}
if ($opts{help}) {
  pod2usage(-exitstatus => 0, -verbose => 1);
}
if ($opts{man}) {
  pod2usage(-exitstatus => 0, -verbose => 2);
}
if ($opts{version}) {
  #print "$VERSION\n";
  print "1.1.5 hacked\n";
  exit;
}

use constant TRED => q(/home/pajas/tred-devel);
use lib (map TRED.'/tredlib/libs/'.$_, qw(fslib pml-base));

use PMLInstance;

$Fslib::resourcePath="$ENV{HOME}/.tred.d:".TRED.'/resources';

$Fslib::Debug = 1 if $opts{debug};

my $infile = shift || '-';
my $outfile = shift || '-';
my $pml = PMLInstance->load({filename => $infile});
$pml->save({filename => $outfile, keep_knit => 1, refs_save => {}});


__END__

=head1 NAME

knit.pl - load a PML instance and save it back preserving all knitted material embedded

=head1 SYNOPSIS

  knit.pl [-d|--debug] input.pml output.pml

or

  knit.pl --help|-h | --usage|-u | --man | --version|-V

=head1 DESCRIPTION

Loads a PML instance obeying the #KNIT role (used to embed referenced
data) and saves the instance back keeping all knited content embedded
in the single PML instance.

=head1 AUTHOR

Petr Pajas, E<lt>pajas@sup.ms.mff.cuni.czE<gt>

=head1 COPYRIGHT AND LICENSE

Copyright (C) 2006 by Petr Pajas

This program is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.8.2 or,
at your option, any later version of Perl 5 you may have available.

=head1 BUGS

None reported... yet.

=cut
