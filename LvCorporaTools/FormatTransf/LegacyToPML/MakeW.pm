#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::LegacyToPML::MakeW;

use utf8;
use strict;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(makeW metainfo);


our $vers = 0.2;
our $metainfo = 'Nezināms korpuss';
our $progname = "w līmeņa auto-marķētājs, $vers";

###############################################################################
# This program creates PML W file from the original text file.
#
# Input parameters: data dir, output dir, [metainfo file], [encoding].
#
# Developed on ActivePerl 5.10.1.1007, tested on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2011-2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
sub makeW
{
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for creating PML W file from the original text file.

Params:
   input directory
   output directory (UTF-8 always)
   metadata file (metadata must be oneliner) [opt]
   input data encoding [opt, UTF-8 used by default]

Latvian Treebank project, LUMII, 2011-2012, provided under GPL
END
		exit 1;
	}

	# Input paramaters.
	my $corpus = shift; #$ARGV[0] ? $ARGV[0] : 'dati';
	my $outDir = shift; #$ARGV[1] ? $ARGV[1] : 'wrez';
	if (open META, "<:encoding(UTF-8)", shift)
	{
		$metainfo = <META>;
		close META;
	};
	my $encoding = (shift or 'UTF-8');	

	opendir(DIR, $corpus) or die "Input directory error: $!";
	mkdir $outDir;
	while (defined(my $inFile = readdir(DIR)))
	{
		# do something with "$dirname/$file"
		if (! -d "$corpus\\$inFile")
		{
			$inFile =~ /^(.*)\..*?$/;
			my $docid = $1;
			#open INFLOW, "<:encoding(windows-1257)", "$corpus\\$inFile"
			#open INFLOW, "<:encoding(UTF-8)", "$corpus\\$inFile"
			open INFLOW, "<:encoding($encoding)", "$corpus\\$inFile"
				or warn "Input file error $inFile: $!";
			open OUTFLOW, ">:encoding(UTF-8)", "$outDir\\$docid.w"
				or warn "Output file error: $!";
			_printBegin (\*OUTFLOW, $docid, $inFile);
			my $parid = 0;
			while (<INFLOW>)
			{
				$parid++;
				if (! /^(\s)*$/)
				{
					my $vid = 0;
					print OUTFLOW "\t\t<para>\n";
					while (m#(\d+|\p{L}+|\.+|!+|\?+|\S)#g)
					#while (m#(\w+|\.+|!+|\S)#g)
					{
						$vid++;
						print OUTFLOW "\t\t\t<w id=\"w-$docid-p${parid}w$vid\">\n\t\t\t\t<token>$1</token>\n";
						if ($' !~ /^\s/) {
							print OUTFLOW "\t\t\t\t<no_space_after>1</no_space_after>\n";
						}
					print OUTFLOW "\t\t\t</w>\n";
					}
					#print OUTFLOW $_;
					print OUTFLOW "\t\t</para>\n";
				}
			}
			_printEnd(\*OUTFLOW);
			close INFLOW;
			close OUTFLOW;
		}
	}
	closedir(DIR);
}

sub _printBegin
{
	my ($output, $docid, $fileID) = @_;
	my $timeNow = localtime time;
	print $output <<END;
<?xml version="1.0" encoding="utf-8"?>
<lvwdata xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
	<head>
		<schema href="lvwschema.xml"/>
	</head>
	<meta>Eksperimentālais fails, automātiski marķēts ($progname), $timeNow</meta>
	<doc id="$docid" source_id="$fileID">
		<docmeta>$metainfo</docmeta>
END
}

sub _printEnd
{
	my $output = shift;
	print $output <<END;
	</doc>
</lvwdata>
END
}

1;
