#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::Plaintext2W;

use utf8;
use strict;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(transformFile processDir);

use IO::File;
use LvCorporaTools::GenericUtils::UIWrapper;


###############################################################################
# This program creates PML W file from the original text file. 
# Input files - utf8.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2011-now
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

# Process all .pml and .xml files in given folder. This can be used as entry
# point, if this module is used standalone.
sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for batch transfoming .txt files to Latvian Treebank PML W format.
Additionally vertical file with tokenization can be provided.

Params:
   data directory
   text file extension [opt, txt used by default]
   tokenization file extension or 0/'' to use brute tokenization (formats: 
     CoNLL with spaces inside token replaced with '_' format or one token per
      line)[opt, brute tokenization used otherwise] THIS IS JET TODO.
   input data encoding [opt, UTF-8 used by default]
   metadata file (metadata must be UTF--8 encoded one-liner) or string [opt]

Latvian Treebank project, LUMII, 2012-now, provided under GPL
END
		exit 1;
	}

	my $dirName = shift @_;
	my $textExt = shift @_;
	my $tokExt = (shift @_ or 0);
	my @otherPrams = @_;
	
	my $wrapper = sub {
		my $dirName = shift @_;
		my $fileNameStub = shift @_;
		my $outFile = shift @_;
		my @otherPrams = @_;
		
		my $tokFile = $tokExt ? "$fileNameStub.$tokExt" : 0;
		return &transformFile (
			$dirName, "$fileNameStub.$textExt", $outFile, $tokFile,
			@otherPrams);
	};
	
	LvCorporaTools::GenericUtils::UIWrapper::processDir(
		$wrapper, "^.+\\.txt\$", ".w", 1, 1, $dirName, @otherPrams);

}

# Process single plain-text file. This can be used as entry point, if this
# module is used standalone.
sub transformFile
{
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for creating PML W file from the plain text file. Additionally vertical
file with tokenization can be provided.

Params:
   directory prefix
   text file name
   new file name [opt, current file name used otherwise]
   tokenization file name or 0/'' to use brute tokenization (formats: CoNLL 
      with spaces inside token replaced with '_' format or one token per line)
      [opt, brute tokenization used otherwise] THIS IS JET TODO.
   input data encoding [opt, UTF-8 used by default]
   metadata file (metadata must be UTF--8 encoded one-liner) or string [opt]

Latvian Treebank project, LUMII, 2011-now, provided under GPL
END
		exit 1;
	}

	# Input paramaters.
	my $dirPrefix = shift @_;
	my $oldName = shift @_;
	my $newName = (shift @_ or $oldName);
	my $tokFile = (shift @_ or 0);
	my $encoding = (shift @_ or 'UTF-8');
	my $metaSource = shift @_;

	my $metainfo = 'Nezināms avots.';

	# Get metainfo.
	if (-e "$dirPrefix/$metaSource")
	{
		my $metaFlow = IO::File->new(
			"$dirPrefix/$metaSource", "< :encoding(UTF-8)")
			or die "Could not open file $dirPrefix/$metaSource: $!";
		$metainfo = <$metaFlow>;
		$metaFlow->close;
	} elsif ($metaSource)
	{
		$metainfo = $metaSource;
	}

	# Open input file.
	my $in = IO::File->new("$dirPrefix/$oldName", "< :encoding(UTF-8)")
		or die "Could not open file $oldName: $!";
	# Open output file.
	File::Path::mkpath("$dirPrefix/res/");
	my $out = IO::File->new("$dirPrefix/res/$newName", "> :encoding(UTF-8)")
		or die "Could not create file $newName: $!";

	$oldName =~ /^(.*)\..*?$/;
	my $sourceId = $1;
	$newName =~ /^(.*)\..*?$/;
	my $docId = $1;
	
	&_printHeader ($out, $docId, $sourceId, $metainfo);
	
	# Process each paragraph.
	my $parId = 0;
	while (<$in>)
	{
		$parId++;
		if (! /^(\s)*$/)
		{
			my $tokId = 0;
			print $out "\t\t<para>\n";
			
			# Process each token.
			while (m#(\d+|\p{L}+|\.+|!+|\?+|\S)#g)
			#while (m#(\w+|\.+|!+|\S)#g)
			{
				$tokId++;
				print $out "\t\t\t<w id=\"w-$docId-p${parId}w$tokId\">\n\t\t\t\t<token>$1</token>\n";
				if ($' !~ /^\s/)
				{
					print $out "\t\t\t\t<no_space_after>1</no_space_after>\n";
				}
				print $out "\t\t\t</w>\n";
			}
			print $out "\t\t</para>\n";
		}
	}
			
	&_printFooter($out);
			
	$in->close;
	$out->close;
}

sub _printHeader
{
	my ($output, $docid, $sourceid, $metainfo) = @_;
	#my $timeNow = localtime time;
	print $output <<END;
<?xml version="1.0" encoding="utf-8"?>
<lvwdata xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
	<head>
		<schema href="lvwschema.xml"/>
	</head>
	<meta>-</meta>
	<doc id="$docid" source_id="$sourceid">
		<docmeta>$metainfo</docmeta>
END
}

sub _printFooter
{
	my $output = shift @_;
	print $output <<END;
	</doc>
</lvwdata>
END
}

1;
