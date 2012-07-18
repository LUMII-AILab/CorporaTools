#!/usr/bin/env perl
# foreach_match.pl     pajas@ufal.mff.cuni.cz     2007/10/26 14:28:26

# Abstract: print or process data by attribute paths

our $VERSION="0.1";

use warnings;
use strict;
$|=1;

use open qw(:locale :std);
use URI;
use URI::file;
use Getopt::Long;
use Pod::Usage;
Getopt::Long::Configure ("bundling");
my %opts;
GetOptions(\%opts,
	'eval|e=s',
	'path|p=s@',
	'save|S',
	'out-dir|o=s',
	'dump-refs|d',
	'print-match|m',
	'print-path|P',
	'print-types|t',
	'no-types|T',
	'print-type-paths|y',
	'print-file-name|f',
	'no-references|R',
	'debug|D',
	'quiet|q',
	'silent|s',
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
  print "$VERSION\n";
  exit;
}

use FindBin;
use lib (glob(File::Spec->catfile(${FindBin::RealBin},'..','libs','*','')));
use PMLInstance qw(:all);
use Fslib;
use Data::Dumper;

$Fslib::Debug=1 if $opts{debug};
Fslib::AddResourcePath(@{$opts{path}});

my $expression  = shift;
while (@ARGV) {
  my $filename = shift;
  print STDERR "Loading $filename\n" unless $opts{quiet};
  my $pml = PMLInstance->load({filename => $filename,
			       validate_cdata => 0,
			       no_references => $opts{'no-references'},
			       no_trees => 1,
			      });
  print STDERR "done.\n" unless $opts{quiet};
  $pml->for_each_match(
    { map {
      $_ => [sub {
	       my ($match,$path)=@_;
	       my $val = $match->{value};
	       if ($opts{'dump-refs'} and ref($val)) {
		 $val = Dumper($val);
	       }
	       if ($opts{'print-file-name'}) {
		 print "$filename: ";
	       }
	       if ($opts{'print-path'}) {
		 print "$match->{path}: ";
	       }
	       if ($opts{'print-match'}) {
		 print "matches $path: ";
	       }
	       if ($opts{'print-types'}) {
		 my $type = $match->{type};
		 $type = (defined($type)&&$type->get_decl_type_str)
		   ||'UNKNOWN_TYPE';
		 print "$type: ";
	       }
	       if ($opts{'print-type-paths'}) {
		 my $type = $match->{type};
		 $type = (defined($type)&&$type->get_decl_path)
		   ||'#UNKNOWN_TYPE';
		 print "$type: ";
	       }
	       print $val,"\n" unless $opts{silent};
	       if ($opts{eval}) {
		 local $_ = $match;
		 eval $opts{eval};
		 die $@ if $@;
	       }
	     }, $_
	    ]
    } split /\s+/,$expression
   },
   {
     $opts{'no-types'} ? ( type => undef ) : ()
   }
 );
  if ($opts{save}) {
    my $outfile = $filename;
    if ($opts{'out-dir'}) {
      my (undef,undef,$basename) = File::Spec->splitpath($outfile);
      $outfile = File::Spec->catfile($opts{'out-dir'},$basename);
    }
    print STDERR "Saving $filename as $outfile\n" unless $opts{quiet};
    $pml->save({filename => $outfile,
		       no_references => $opts{'no-references'},
		       no_trees => 1,
		      });
  }
}


__END__

=head1 NAME

foreach_match.pl - print or process data by attribute paths

=head1 SYNOPSIS

  foreach_match.pl [ options ] expression file.pml [...]

or

  foreach_match.pl -u          for usage
  foreach_match.pl -h          for help
  foreach_match.pl --man       for the manual page
  foreach_match.pl --version   for version

=head1 DESCRIPTION

Print, dump or otherwise process data in the source PML instances selectively using
one or more attribute paths to select the interesting parts of the document.

The expression

=head2 OPTIONS

=over 5

=item B<--eval|-e> code

Evaluate given Perl code on each match. The match is described in the $_
variable which is a HASH reference with the following key=>value pairs:

   path  => path to the matched data,
   value => the matched data
   type  => object representing the corresponding PML data type

=item B<--save|-S> 

Save the (possibly modified by --eval) document.

=item B<--out-dir|-o> directory_name

Use given directory for all output files.

=item B<--dump-refs|-d>

Use Data::Dumper to print matches that are a reference.

=item B<--print-match|-m>

Print the attribute path that matched the data.

=item B<--print-path|-P>

Print actual attribute path to the data.

=item B<--print-types|-t>

Print the PML type of the matched data.

=item B<--no-types|-T>

This flag indicates that the processor should not bother to track data types.

=item B<--print-type-paths|-y>

Print the PML type of the matched data as an attribute path.

=item B<--print-file-name|-f>

Print file name for each match.

=item B<--no-references|-R>

Do not load references.

=item B<--debug|-D>

Print various debugging messages.

=item B<--quiet|-q>

Suppress information about files being opened and saved.

=item B<--silent|-s>

Suppress default output (the matched value).

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

Copyright (C) 2007 by Petr Pajas

This program is free software; you can redistribute it and/or modify
it under the same terms as Perl itself, either Perl version 5.8.2 or,
at your option, any later version of Perl 5 you may have available.

=head1 BUGS

None reported... yet.

=cut
