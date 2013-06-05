#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::PMLUtils::CheckLvPmlBatch;

use strict;
use warnings;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(checkLvPmlBatch);

use LvCorporaTools::PMLUtils::CheckLvPml qw(checkLvPml);

use IO::File;
use IO::Dir;

###############################################################################
# Batch processing for LvCorporaTools::PMLUtils::CheckLvPml - if single
# argument provided, treat it as directory and process all files in it.
# Otherwise pass all arguments to CheckLvPml.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
sub checkLvPmlBatch
{
	if (@_ == 1)
	{

		my $dir_name = $_[0];
		my $dir = IO::Dir->new($dir_name) or die "dir $!";

		while (defined(my $in_file = $dir->read))
		{
			if ((! -d "$dir_name/$in_file") and ($in_file =~ /^(.+)\.w$/))
			{
				checkLvPml ($dir_name, $1, "$1-errors.txt");
			}
		}

	}
	else
	{
		checkLvPml (@_);
	}
}
1;