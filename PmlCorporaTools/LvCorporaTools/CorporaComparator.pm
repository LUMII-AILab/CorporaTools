package LvCorporaTools::CorporaComparator;

use warnings;
use utf8;
use strict;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(compareFiles);

use IO::File;

sub compareFiles
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for comparing two CoNLL files. Output: (1) counts of matching tokens and
(2) differently annotated tokens. Matching empty rows are ignored (not counted
towards matching row score).

Params:
   file 1
   file 2

Latvian Treebank project, LUMII, 2014-now, provided under GPL
END
		exit 1;
	}
	
	my $file1 = shift @_;
	my $file2 = shift @_;
	
	my $in1 = IO::File->new($file1, "< :encoding(UTF-8)")
		or die "Could not open file $file1: $!";
	my $in2 = IO::File->new($file2, "< :encoding(UTF-8)")
		or die "Could not open file $file2: $!";
	
	my $equalRows = 0;
	my $diffRows = 0;
	while (my $row1 = <$in1>)
	{
		my $row2;
		unless ($row2 = <$in2>)
		{
			print "File 2 is shorter than file 1!\n";
			last;
		}
		
		$row1 =~ s/^\s*(.*?)\s*$/$1/;
		$row2 =~ s/^\s*(.*?)\s*$/$1/;
		
		if ($row1 eq $row2 and $row1 ne '')
		{
			$equalRows++;
		} elsif ($row1 ne $row2)
		{
			$diffRows++;
		}
	}

	print "File 1 is shorter than file 2!\n" if (my $row2 = <$in2>);
	
	print "$equalRows equal nonempty rows.\n";
	print "$diffRows different rows (at least one of the pair is not empty).\n";
	
}

# This ensures that when module is called from shell (and only then!)
# compareFiles is envoked.
&compareFiles(@ARGV) unless caller;

1;