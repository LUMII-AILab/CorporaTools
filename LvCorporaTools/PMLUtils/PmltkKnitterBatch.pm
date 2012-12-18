#!C:\strawberry\perl\bin\perl -w
package LvTreeBank::Utils::PmltkKnitterBatch;

use strict;
use warnings;

use IO::File;
use IO::Dir;

###############################################################################
# Batch processing interface for pmltk-1.1.5 knitter. WinXP specific. Exits
# after first failed file processing.
#
# Input: directory with files to process,
#		 directory where PML Toolkit is (default is 'pmltk-1.1.5').
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
sub runPmltkKnitterBatch
{
	autoflush STDOUT 1;
	if (not @_ or @_ lt 1)
	{
		print <<END;
Batch processing interface for PML Toolkit knitter. Tested on v-1.1.5. WinXP
specific.
Input files should be provided as UTF-8.

Params:
   directory with files to process
   directory with PML Toolkit [opt, 'pmltk-1.1.5' used otherwise]

Latvian Treebank project, LUMII, 2012, provided under GPL
END
		exit 1;
	}
	my $dir_name = shift @ARGV;
	my $pmltk_path = shift @ARGV;
	$pmltk_path = 'pmltk-1.1.5' if (not $pmltk_path);
	my $dir = IO::Dir->new($dir_name) or die "dir $!";

	while (defined(my $in_file = $dir->read))
	{
		if ((! -d "$dir_name/$in_file") and ($in_file =~ /^(.+)\.a$/))
		{
			my $fail = system ("perl $pmltk_path/tools/knit.pl $dir_name/$1.a $dir_name/$1.pml");
			if (not $fail) 
			{
				print "Processing $1 finished!\n";
			} else
			{
				print "Something failed while processing $1!";
				exit 1;
			}
		}
	}
}

1;