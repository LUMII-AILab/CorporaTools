package LvCorporaTools::DataSelector::SplitConll2Para;
use strict;
use warnings;
#use utf8;

use IO::File;
use LvCorporaTools::GenericUtils::UIWrapper;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(transformFile processDir);

###############################################################################
# Script for splitting well-formed multi-paragraph CoNLL-U files into smaller
# well-formed single-paragraph CoNLL-U files.
# Input files - UTF-8.
#
# Developed on Strawberry Perl 5.16.x
# Latvian Treebank project, 2017
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 1)
	{
		print <<END;
Script for batch transfoming multi-paragraph CoNLL-U files to one paragraph per
file CoNLL-U. Input files must have newdoc and newpar lines according to
http://universaldependencies.org/format.html.
Input files should be provided as UTF-8.

Params:
   data directory
   don't fail processing input file, if some paragraph file already exists
     [opt, 0 (failing) by default]

Latvian Treebank project, LUMII, 2017, provided under GPL
END
		exit 1;
	}

	my $dirName = shift @_;
	my $failed = LvCorporaTools::GenericUtils::UIWrapper::processDir(
		\&transformFile, "^.+\\.conllu\$", '', 0, 0, $dirName, @_);
	print "$failed files failed.\n" if ($failed);
	return $failed;
}

sub transformFile
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for splitting a CoNLL-U file to one paragraph per file. Result files are
named according to paragraph IDs. Input file must have newdoc and newpar lines
according to http://universaldependencies.org/format.html.
Input files should be provided as UTF-8.

Params:
   directory prefix
   file name (.conllu)
   allow to overwrite existing paragraph files [opt, 0 (dying) by default]

Latvian Treebank project, LUMII, 2017, provided under GPL
END
		exit 1;
	}

	my $dirPrefix = shift @_;
	my $fileName = shift @_;
	my $noEasyDie = shift @_;

	mkdir("$dirPrefix/res/");
	my $in = IO::File->new(
		"$dirPrefix/$fileName", "< :encoding(UTF-8)")
		or die "Could not open file $dirPrefix/$fileName: $!";
	my $docId = $fileName;
	$docId =~ s/\.conllu$//;

	my $out;
	my $parId = 0;

	while (my $line = <$in>)
	{
		if ($line =~ /^#\s*newdoc\s*id\s*=\s*(.*?)\s*$/)
		{
			$docId = $1;
		}
		elsif ($line =~ /^#\s*newpar\s*id\s*=\s*(.*?)\s*$/)
		{
			$parId = $1;
			$out->close() if ($out);
			if (-f "$dirPrefix/res/$parId.conllu")
			{
				$noEasyDie ?
					print "Overwriting file $dirPrefix/res/$parId.conllu.\n" :
					die "Duplicate file creation for name $dirPrefix/res/$parId.conllu $!";
			}

			$out = IO::File->new(
				"$dirPrefix/res/$parId.conllu", "> :encoding(UTF-8)")
				or die "Could not open file $dirPrefix/res/$parId.conllu $!";
			print $out "# newdoc id = $docId\n";
			print $out "# newpar id = $parId\n";
		}
		else
		{
			print $out $line;
		}
	}
	$out->close() if ($out);
}

1;