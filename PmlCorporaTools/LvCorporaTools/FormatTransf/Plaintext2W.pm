#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::Plaintext2W;

use warnings;
use utf8;
use strict;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(transformFile processDir escape);

use IO::File;
use LvCorporaTools::GenericUtils::UIWrapper;

our $vers = 0.2;
our $progname = "LVTB teksta/PML-W konvertors, $vers";


###############################################################################
# This program creates PML W file from the original text file. 
# Input files - utf8.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2011-now
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

# Convert all .txt files in given folder to PML-W files. This can be used as
# entry point, if this module is used standalone.
sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for batch transfoming .txt files to Latvian Treebank PML W format.
Assumes paragraph numbering from 1.

Params:
   data directory
   text file extension [opt, txt used by default]
   metadata file extension [opt, dummy-data used by default]
   input data encoding [opt, UTF-8 used by default]

Latvian Treebank project, LUMII, 2012-2017, provided under GPL
END
		exit 1;
	}

	my $dirName = shift @_;
	my $textExt = shift @_;
	my $metaExt = shift @_;
	my $encoding = shift @_;
	
	my $wrapper = sub {
		my $dirName = shift @_;
		my $fileNameStub = shift @_;

		return &transformFile (
			$dirName, "$fileNameStub.$textExt", $fileNameStub, "$fileNameStub.$metaExt",
			1, $fileNameStub, $encoding);
	};
	
	LvCorporaTools::GenericUtils::UIWrapper::processDir(
		$wrapper, "^.+\\.txt\$", '', 0, 1, $dirName);

}

# Convert single plain-text file to PML-W file. This can be used as entry
# point, if this module is used standalone.
sub transformFile
{
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for creating PML W file from the plain text file.

Params:
   directory prefix
   text file name
   source document ID [opt, current file name without .txt used otherwise]
   metadata file or string [opt], preferably from LVK; metadata must be
      headless XML in following form:
      <docmeta>
        <title>Mandatory document name.</title>
        <source>Optional document source.</source>
        <author>Author is also optional.</author>
        <authorgender>Author gender is optional.</authorgender>
        <published>Publication date is optional.</published>
        <genre>Genre is optional, but nice to have.</genre>
        <keywords>
          <LM>first keyword</LM>
          <LM>second keyword</LM>
          <LM>all keywords and element itself is optional</LM>
        </keywords>
        <misc>Any additional information that does not fit above.</misc>
      </docmeta>
   first paragraph ID [opt, shuld be numerical, 1 used by default]
   new PML fileset ID [opt, <source_doc id>_p<paragraph id> used otherwise]
   input data encoding [opt, UTF-8 used by default]

Latvian Treebank project, LUMII, 2011-2017, provided under GPL
END
		exit 1;
	}

	# Input paramaters.
	my $dirPrefix = shift @_;
	my $textFileName = shift @_;
	$textFileName =~ /(.*?)(.\txt)?/;
	my $sourceId = (shift @_ or $1);
	my $metaSource = shift @_;
	my $firstPara = (shift @_ or 1);
	my $docId = (shift @_ or "${sourceId}_p$firstPara");
	my $encoding = (shift @_ or 'UTF-8');

	my $metainfo = '<docmeta><title>Nezināms avots.</title></docmeta>';

	# Get metainfo.
	if (-e "$dirPrefix/$metaSource")
	{
		my $metaFlow = IO::File->new(
			"$dirPrefix/$metaSource", "< :encoding(UTF-8)")
			or die "Could not open file $dirPrefix/$metaSource: $!";
		local $/ = undef;
		$metainfo = <$metaFlow>;
		$metaFlow->close;
	} elsif ($metaSource)
	{
		$metainfo = $metaSource;
	}

	# Open input file.
	my $in = IO::File->new("$dirPrefix/$textFileName", "< :encoding($encoding)")
		or die "Could not open file $textFileName: $!";
	# Open output file.
	File::Path::mkpath("$dirPrefix/res/");
	my $out = IO::File->new("$dirPrefix/res/$docId.w", "> :encoding(UTF-8)")
		or die "Could not create file $docId.w: $!";

	&_printHeader ($out, $docId, $sourceId, $metainfo);
	
	# Process each paragraph.
	# TODO use XML library.
	my $parId = $firstPara;
	while (<$in>)
	{
		if (! /^(\s)*$/)
		{
			my $tokId = 0;
			print $out "\t\t<para id=\"w-$sourceId-p${parId}\">\n";
			
			# Process each token.
			while (m#(\d+|\p{L}+|\.+|!+|\?+|\S)#g)
			{
				$tokId++;
				my $escTok = escape($1);
				print $out "\t\t\t<w id=\"w-$sourceId-p${parId}w$tokId\">\n\t\t\t\t<token>$escTok</token>\n";
				print $out "\t\t\t\t<no_space_after>1</no_space_after>\n" if ($' !~ /^\s/);
				print $out "\t\t\t</w>\n";
			}
			print $out "\t\t</para>\n";
			$parId++;
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
	<meta>$progname</meta>
	<doc id="$docid" source_id="$sourceid">
		$metainfo
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

sub escape
{
	my $data = shift @_;
	if ($data)
	{
		$data =~ s/&/&amp;/g;
		$data =~ s/</&lt;/g;
		$data =~ s/>/&gt;/g;
	}
	return $data;
}

1;
