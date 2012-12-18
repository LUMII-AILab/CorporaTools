#!C:\strawberry\perl\bin\perl -w
package LvTreeBank::Transformations::DepPml2ConllBatch;

use strict;
use warnings;

use LvTreeBank::Transformations::DepPml2Conll;
use IO::File;
use IO::Dir;

###############################################################################
# Batch processing for LvTreeBank::Transformations::DepPml2Conll - if 4
# arguments (mode, directory name, cpostag mode, postag mode) provided, treat
# it as directory and process all files in it.
# Otherwise pass all arguments to DepPml2Conll.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
sub transformFileBatch
{
	if (@ARGV eq 4)
	{
		my $mode = shift @ARGV;
		my $dir_name = shift @ARGV;
		my $cpostag = shift @ARGV;
		my $postag = shift @ARGV;
		my $dir = IO::Dir->new($dir_name) or die "dir $!";
		my $infix = $mode ? "nored" : "unlabeled";
		
		while (defined(my $in_file = $dir->read))
		{
			if ((! -d "$dir_name/$in_file") and ($in_file =~ /^(.+)\.(pml|xml)$/))
			{
				LvTreeBank::Transformations::DepPml2Conll::transformFile (
					$mode, $dir_name, $in_file, $cpostag, $postag, "$1-$infix.conll");
			}
		}

	}
	else
	{
		LvTreeBank::Transformations::DepPml2Conll::transformFile (@ARGV);
	}
}
1;