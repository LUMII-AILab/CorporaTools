#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::TreeTransformations::DepPml2ConllBatch;

use strict;
use warnings;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(transformFileBatch);

use LvCorporaTools::TreeTransformations::DepPml2Conll qw(transformFile);
use IO::File;
use IO::Dir;

###############################################################################
# Batch processing for LvCorporaTools::TreeTransformations::DepPml2Conll - if 5
# arguments (mode, directory name, cpostag mode, postag mode, whether do output
# in CoNLL-2009 format) provided, treat it as directory and process all files
# in it.
# Otherwise pass all arguments to DepPml2Conll.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
sub transformFileBatch
{
	if (@ARGV eq 5)
	{
		my $mode = shift @ARGV;
		my $dir_name = shift @ARGV;
		my $cpostag = shift @ARGV;
		my $postag = shift @ARGV;
		my $conll2009 = shift @ARGV;
		my $dir = IO::Dir->new($dir_name) or die "dir $!";
		my $infix = $mode ? "nored" : "unlabeled";
		
		while (defined(my $in_file = $dir->read))
		{
			if ((! -d "$dir_name/$in_file") and ($in_file =~ /^(.+)\.(pml|xml)$/))
			{
				transformFile ($mode, $dir_name, $in_file, $cpostag, $postag,
					"$1-$infix.conll", $conll2009);
			}
		}

	}
	else
	{
		transformFile (@ARGV);
	}
}
1;