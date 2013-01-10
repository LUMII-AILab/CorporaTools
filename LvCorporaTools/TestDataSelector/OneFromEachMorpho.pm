#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::TestDataSelector::OneFromEachMorpho;

use utf8;
use strict;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(selectOneFromEach);

use IO::File;
use IO::Dir;

#use Data::Dumper;

###############################################################################
# Select one sentence from each file in provided morphocorpus. Input data must
# be UTF-8 PML M.
#
# Developed on Strawberry Perl 5.12.3.0
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv, 2012
# Licenced under GPL.
###############################################################################
sub selectOneFromEach
{
	autoflush STDOUT 1;
	if (not @_ or @_ le 1)
	{
		print <<END;
Script for choosing one sentence from each data file.
Input files should be provided as PML M files in UTF-8 encoding.

Params:
   input data directory
   result file name [opt, "test-corp.xml" used otherwise]

LUMII, 2012, provided under GPL
END
		exit 1;
	}

	my $in_dir_name = shift @_;
	my $out_name = (shift @_ or "test-corp.xml");

	my $in_dir = IO::Dir->new($in_dir_name) or die "dir $!";

	my $out = IO::File->new;
	open $out, ">:encoding(UTF-8)", "$out_name"
		or die "Could not open file $out_name: $!";

	while (defined(my $in_file = $in_dir->read))
	{
		if (! -d "$in_dir_name\\$in_file")
		{
			my $in = IO::File->new;
			open $in, "<:encoding(UTF-8)", "$in_dir_name\\$in_file"
				or die "Could not open file $in_file: $!";
			print "Processing $in_file.\n";
			
			my $buffer = '';
			my @sentences = ();
			
			while (<$in>)
			{
				$buffer = $buffer."$_";
				if ($buffer =~ m#</\s*s>#is)
				{
					my @ss = $buffer =~ m#<s(?:\s|>).*?</\s*s>#gis;
					$buffer =~ s#<s(?:\s|>).*?</\s*s>##gis;
					#print Dumper(\@ss);
					push @sentences, @ss;#$buffer =~ s#<s(?:\s|>).*?</\s*s>##gis;
				}
				if ($buffer =~ m#(<s(\s|>).*)$#is) { $buffer = $1; }
			}
			my $rnd_ind = int(rand @sentences);
			my $size = @sentences;
			print "Sentence No. ".($rnd_ind+1)." has been shosen from $size total.\n";
			#print "".$sentences[$rnd_ind]."\n";
			print $out "".$sentences[$rnd_ind]."\n";
			
			$in->close;
			$out->flush;
		}
	}
	$in_dir->close;
	$out->close;
}
1;