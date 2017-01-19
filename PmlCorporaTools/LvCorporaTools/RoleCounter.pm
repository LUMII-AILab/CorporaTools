package LvCorporaTools::RoleCounter;

use strict;
use warnings;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processFile);

#use Carp::Always;	# Print stack trace on die.

use IO::File;

sub processFile
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for counting roles in CONLL file.

Params:
   data directory
   input file

Latvian Treebank project, LUMII, 2013-now, provided under GPL
END
		exit 1;
	}
	my $dirName = shift @_;
	my $file = shift @_;
	my $in = IO::File->new("$dirName/$file", "< :encoding(UTF-8)")
		or die "Could not open file $file: $!";
	my @rows = <$in>;
	$in->close();
	
	my @roles = map( /^(?:.+?\t){7}(.+?)\t/, grep( !/^\s*$/, @rows));
	my %counts = ();
	$counts{$_}++ for @roles;
	
	my $out = IO::File->new("$dirName/counts.txt", "> :encoding(UTF-8)")
		or die "Could not open file counts.txt: $!";
	@roles = sort {$counts{$a} == $counts{$b} ? $a cmp $b : $counts{$a}<=>$counts{$b}}
		keys(%counts);
	
	print $out @roles."\n";
	print $out $counts{$_}."\t$_\n" for @roles;
	$out->close;

}

# This ensures that when module is called from shell (and only then!)
# processDir is envoked.
&processFile(@ARGV) unless caller;

1;
