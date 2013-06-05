#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::PMLUtils::CheckWBatch;

use strict;
use warnings;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(checkWBatch);

use LvCorporaTools::PMLUtils::CheckW qw(checkW);

use IO::File;
use IO::Dir;

###############################################################################
# Batch processing for LvCorporaTools::PMLUtils::CheckW - if single argument
# provided, treat it as directory and process all files in it. Otherwise pass
# all arguments to CheckW.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
sub checkWBatch
{
	if (@_ == 1)
	{

		my $dir_name = $_[0];
		my $dir = IO::Dir->new($dir_name) or die "dir $!";

		while (defined(my $in_file = $dir->read))
		{
			if ((! -d "$dir_name/$in_file") and ($in_file =~ /^(.+)\.w$/))
			{
				checkW ($dir_name, "$1.w", "$1.txt");
			}
		}

	}
	else
	{
		checkW (@_);
	}
}
1;