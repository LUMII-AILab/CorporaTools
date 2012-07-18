#!/usr/bin/env perl

# Abstract: a test tool for testing the perl API for PML (esp. schema and instance loader and writer)

use Data::Dumper;
use Data::Compare;

use Pod::Usage;

use Getopt::Long;
Getopt::Long::Configure ("bundling");
my %opts;
GetOptions(\%opts,
	'quiet|q',
	'debug|D',
	'debug-level|d=i',
	'keep|k',
	'iterate|i=i',
	'load-only|l',
	'validate-cdata',
	'paths|p=s',
	'benchmark|B',
	'fsfile|F',
	'no-knit',
	'no-references',
	'no-trees',
	'no-diff',
	'use-config|C',
	'config|c=s',
	'help|h',
	'usage|u',
	'man',
       ) or $opts{usage}=1;

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

use constant TRED => q(/home/pajas/tred-devel);
use constant TREDLIB => 
  TRED.'/tredlib'; #
  # q(/home/pajas/projects/pdtsc-med);
use Benchmark;
use lib (TREDLIB.'/libs/pml-base', TREDLIB.'/libs/fslib');

use PMLInstance;

$Fslib::resourcePath="$ENV{HOME}/.tred.d:".TRED.'/resources';
$Fslib::resourcePath=$opts{paths}.':'.$Fslib::resourcePath if defined $opts{paths};
$Fslib::Debug = $opts{'debug'} || $opts{'debug-level'} || 0;
$PMLInstance::DEBUG=$opts{'debug-level'} || 0;

{
my $config;
sub configure {
  return unless $opts{'use-config'} || defined($opts{'config'});
  return $config if defined $config;
  return 0 unless eval {
    require XML::LibXSLT;
  };
  my $config_file = Fslib::FindInResources($opts{'config'} || 'pmlbackend_conf.xml');
  if (-f $config_file) {
    print STDERR "config file: $config_file\n" if $opts{debug};
    $config = PMLInstance->load({filename => $config_file});
  }
  return $config;
}
}
my $iter = $opts{iterate}||1;
while (@ARGV) {
  my $filename = shift;
  for my $iteration (1..$iter) {
    print "Loading $filename (iteration $iteration)\n" unless $opts{quiet};
    my $t0 = new Benchmark;
    my $load_opts = {
		     filename => $filename,
		     no_trees => $opts{'no-trees'} ? 1 : 0,
		     no_knit => $opts{'no-knit'} ? 1 : 0,
		     no_references => $opts{'no-references'} ? 1 : 0,
		     validate_cdata => $opts{'validate-cdata'} ? 0 : 1,
		     config => configure(),
		    };
    my $pml = PMLInstance->load($load_opts);
    if ($opts{benchmark}) {
      my $t1 = new Benchmark;
      my $time = timediff($t1, $t0) if ($t1 and $t0);
      print "PMLInstance->load: ",timestr($time),"\n";
    }
    if ($opts{fsfile}) {
      my $fsfile = $pml->convert_to_fsfile;
      $pml = PMLInstance->convert_from_fsfile($fsfile);
    }
    next if $opts{'load-only'};
#
#print Dumper($pml);

#for my $tree (@{$pml->{trees}}) {
#  print Dumper($tree->firstson->{'#content'}),"\n";
#}

#print Dumper($pml->{trees});

  #FSNode::set_attr($pml->get_root(),'meta',Fslib::Struct->new);
  #print Dumper($pml->get_root);
  
    $|=1;
    print "Saving $filename\n" unless $opts{quiet};
    $pml->save({filename => $filename.'.out',
		no_trees => $opts{'no-trees'} ? 1 : 0,
		write_single_LM => 0,
	       });
    my $pml2;
    unless ($opts{'no-diff'}) {
      print "Loading $filename.out\n" unless $opts{quiet};
      $pml2 = PMLInstance->load({%$load_opts,
				    use_classes => 1,
				    filename => $filename.'.out'});
    }
    unlink $filename.'.out' unless $opts{keep};
    cmp_dumps($pml,$pml2) unless $opts{'no-diff'};
  } 
}

sub cmp_dumps {
  print "Dumping\n" unless $opts{quiet};
  $Data::Dumper::Sortkeys = 1;
  my @seqs = map {
#    %{$_->get_schema} = ();
    [
      grep { !/ => undef,?$/ }
      map{ /'$/ ? $_.',' : $_ }
      split /\s*\n\s*/,
      Dumper( [$_->get_root, $_->get_trees, $_->get_trees_prolog, $_->get_trees_epilog ] )
     ] } @_;

  use utf8;
  require Algorithm::Diff::XS;
  print "Comparing\n" unless $opts{quiet};
  my $diff = Algorithm::Diff::XS->new( @seqs );
  
  $diff->Base( 1 );   # Return line numbers, not indices
  my $identical = 1;
  while(  $diff->Next()  ) {
    next   if  $diff->Same();
    $identical = 0;
    my $sep = '';
    if(  ! $diff->Items(2)  ) {
      printf "%d,%dd%d\n",
	$diff->Get(qw( Min1 Max1 Max2 ));
    } elsif(  ! $diff->Items(1)  ) {
      printf "%da%d,%d\n", $diff->Get(qw( Max1 Min2 Max2 ));
    } else {
      $sep = "---\n";
      printf "%d,%dc%d,%d\n", $diff->Get(qw( Min1 Max1 Min2 Max2 ));
    }
    print "< $_\n" for $diff->Items(1);
    print $sep;
    print "> $_\n" for $diff->Items(2);
  }
  print "NOT " unless $identical;
  print "IDENTICAL\n";
  #print STDERR @{$seqs[0]},"\n" unless $identical;
}
