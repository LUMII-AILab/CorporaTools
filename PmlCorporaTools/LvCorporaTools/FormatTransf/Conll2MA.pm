#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::Conll2MA;

use warnings;
use utf8;
use strict;

use IO::File;
use IO::Dir;
use LvCorporaTools::GenericUtils::SimpleXmlIo;
use Data::Dumper;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processDir processFileSet);

our $vers = 0.1;
our $progname = "CoNLL automātiskais konvertors, $vers";

###############################################################################
# This program creates PML M and A files, if CONLL file containing morphology
# and w files are provided. All input files must be UTF-8.
#
# Input parameters: conll dir, w dir, otput dir.
#
# Developed on Strawberry Perl
# Latvian Treebank project, 2017
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
# TODO: drukāt m un a failus ar simpleXML?
sub processDir
{
	if (not @_ or @_ < 3)
	{
		print <<END;
Script for batch creating PML M and A files, if CONLL files and w files are
provided. Currently only morphology is used. All input files must be UTF-8.
Corresponding files must have corresponding filenames.

Params:
   w files directory (.w files)
   morphology directory (.conll files)
   output directory

Latvian Treebank project, LUMII, 2017, provided under GPL
END
		exit 1;
	}
	my $wDirName = shift;
	my $morphoDirName = shift;
	my $outDirName = shift;

	my $wDir = IO::Dir->new($wDirName) or die "$!";
	mkdir($outDirName);

	while (defined(my $inWFile = $wDir->read))
	{
		if (! -d "$wDirName/$inWFile")
		{
			my $coreName = $inWFile =~ /^(.*)\.w*$/ ? $1 : $inWFile;
			&processFileSet($coreName, $outDirName, "$wDirName/$inWFile", "$morphoDirName/$coreName.conll")
		}
	}

}

sub processFileSet
{
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for creating PML M and A files, if and w file and (optional) CoNLL file
are provided. Currently only morphology is used. All input files must be UTF-8.
Corresponding files must have corresponding filenames.

Params:
   file name stub for output
   output folder
   .w file name [opt, stub + .w used otherwise]
   .conll file name [opt, stub + .conll used otherwise]

Latvian Treebank project, LUMII, 2017, provided under GPL
END
		exit 1;
	}

	my $nameStub = shift;
	my $outDirName = shift;
	my $wName = (shift or "$nameStub.w");
	my $conllName = (shift or "$nameStub.conll");

	my $w = LvCorporaTools::GenericUtils::SimpleXmlIo::loadXml($wName, ['para', 'w', 'schema'], []);
	my $conllIn = IO::File->new($conllName, '< :encoding(UTF-8)')
		or die "Could not open file $conllName: $!";

	my $mOut = IO::File->new("$outDirName/$nameStub.m", '> :encoding(UTF-8)');
	&_printMBegin($mOut, $nameStub);
	my $aOut = IO::File->new("$outDirName/$nameStub.a", '> :encoding(UTF-8)');
	&_printABegin($aOut, $nameStub);

	my $insideOfSent = 0;
	my $paraId = 1;
	my $sentCounter = 0;
	my $wordCounter = 0;
	my @unusedWIds = ();
	my $unusedTokens = '';
	my $unusedConll = '';

	# A and M files are made by going through W file.
	for my $wPara (@{$w->{'xml'}->{'doc'}->{'para'}})
	{
		for my $wTok (@{$wPara->{w}})
		{
			# Read a new CoNLL line, if previous one has been used.
			my $line = ($unusedConll or <$conllIn>);
			# Process empty lines, if there any.
			while ($line and $line !~ /^(\d+)\t(\S+)\t(\S+)\t(\S+)\t/)
			{
				if ($insideOfSent)
				{
					&_printMSentEnd($mOut);
					&_printASentEnd($aOut);
					$insideOfSent = 0;
					$wordCounter = 0;
				}
				$unusedConll = '';
				$line = <$conllIn>;
			}

			#print Dumper($wTok);
			$wTok->{'id'} =~ /-p(\d+)w\d+$/;
			$paraId = $1;
			if ($line =~ /^(\d+)\t(\S+)\t(\S+)\t(\S+)\t(\S+)\s/)
			{
				my ($conllToken, $lemma, $simpleTag, $tag) = ($2, $3, $4, $5);
				$conllToken =~ s/_/ /g;
				$lemma =~ s/_/ /g;
				unless($insideOfSent)
				{
					$insideOfSent = 1;
					$sentCounter++;
					&_printMSentBegin($mOut, $nameStub, $paraId, $sentCounter);
					&_printASentBegin($aOut, $nameStub, $paraId, $sentCounter);
				}
				push @unusedWIds, $wTok->{'id'};
				$unusedTokens = $unusedTokens . $wTok->{'token'}->{'content'};
				$unusedTokens = "$unusedTokens " unless ($wTok->{'no_space_after'});
				$unusedTokens =~ /^\s*(.*?)\s*$/;
				#print 'CoNLL ' . Dumper($conllToken);
				#print 'W-W-W ' . Dumper($unusedTokens);
				if ($1 eq $conllToken)
				{
					$wordCounter++;
					&_printMDataNode($mOut, $nameStub, $paraId, $sentCounter,
						$wordCounter, \@unusedWIds, $conllToken, $lemma, $tag);
					&_printADataSimple($aOut, $nameStub, $paraId, $sentCounter,
						$wordCounter, $conllToken);
					@unusedWIds = ();
					$unusedTokens = '';
					$unusedConll = '';
				}
				else
				{
					$unusedConll = $line;
				}
			}
		}
		# Process unused CoNLL lines in the end of the paragraph and warn
		if ($unusedConll =~ /^(\d+)\t(\S+)\t(\S+)\t(\S+)\t/)
		{
			my ($conllToken, $lemma, $tag) = ($2, $3, $4);
			die "CoNLL token $conllToken and W tokens $unusedTokens found unused after the end of paragraph! $!";
			$conllToken =~ s/_/ /g;
			$lemma =~ s/_/ /g;
			unless($insideOfSent)
			{
				$insideOfSent = 1;
				$sentCounter++;
				&_printMSentBegin($mOut, $nameStub, $paraId, $sentCounter);
				&_printASentBegin($aOut, $nameStub, $paraId, $sentCounter);
			}
			$unusedTokens =~ /^\s*(.*?)\s*$/;
			if ($1 eq $conllToken)
			{
				$wordCounter++;
				&_printMDataNode($mOut, $nameStub, $paraId, $sentCounter,
					$wordCounter, ${@unusedWIds}, $conllToken, $lemma, $tag);
				&_printADataSimple($aOut, $nameStub, $paraId, $sentCounter,
					$wordCounter, $conllToken);
				@unusedWIds = ();
				$unusedTokens = '';
				$unusedConll = '';
			}
		}

	}
	if ($insideOfSent)
	{
		&_printMSentEnd($mOut);
		&_printASentEnd($aOut);
	}

	&_printMEnd($mOut);
	$mOut->close();
	&_printAEnd($aOut);
	$aOut->close();
}

sub _printMBegin
{
	my ($output, $docId) = @_;
	my $timeNow = localtime time;
	print $output <<END;
<?xml version="1.0" encoding="utf-8"?>
<lvmdata xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
	<head>
		<schema href="lvmschema.xml" />
		<references>
			<reffile id="w" name="wdata" href="$docId.w" />
		</references>
	</head>
	<meta>
		<lang>lv</lang>
		<annotation_info id="semi-automatic">$progname,  $timeNow</annotation_info>
	</meta>

END
}

sub _printMEnd
{
	my $output = shift @_;
	print $output <<END;
</lvmdata>
END

}

sub _printMSentBegin
{
	my ($output, $docId, $parId, $sentId) = @_;
	print $output <<END;
	<s id="m-${docId}-p${parId}s${sentId}">
END
}

sub _printMSentEnd
{
	my $output = shift @_;
	print $output <<END;
	</s>
END
}

sub _printMDataNode
{
	my ($output, $docId, $parId, $sentId, $tokId, $wIds, $token, $lemma, $tag) = @_;
	$lemma = 'N/A' unless ($lemma and $lemma !~ /^\s*$/);
	$tag = 'N/A' unless ($tag and $tag !~ /^\s*$/);
	my $wIdString = '';
	if (@$wIds > 1)
	{
		$wIdString = '<LM>w#' . join('</LM><LM>w#', @$wIds) . '</LM>';
	}
	elsif (@$wIds == 1)
	{
		$wIdString = "w#@$wIds[0]";
	}
	print $output <<END;
		<m id="m-$docId-p${parId}s${sentId}w$tokId">
			<src.rf>$docId</src.rf>
END
	if ($wIdString)
	{
		print $output <<END;
			<w.rf>$wIdString</w.rf>
END
	}
	if (@$wIds > 1)
	{
		print $output <<END;
			<form_change>union</form_change>
END
	}
	print $output <<END;
			<form>$token</form>
			<lemma>$lemma</lemma>
			<tag>$tag</tag>
		</m>
END
}

sub _printABegin
{
	my ($output, $docId) = @_;
	my $timeNow = localtime time;
	print $output <<END;
<?xml version="1.0" encoding="utf-8"?>

<lvadata xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
	<head>
		<schema href="lvaschema.xml" />
		<references>
			<reffile id="m" name="mdata" href="$docId.m" />
			<reffile id="w" name="wdata" href="$docId.w" />
		</references>
	</head>
	<meta>
		<annotation_info>
			<desc>$progname, $timeNow</desc>
		</annotation_info>
	</meta>

	<trees>
END
}

sub _printAEnd
{
	my $output = shift @_;
	print $output <<END;
	</trees>
</lvadata>
END
}

sub _printASentBegin
{
	my ($output, $docId, $parId, $sentId) = @_;
	print $output <<END;

		<LM id="a-${docId}-p${parId}s${sentId}">
			<s.rf>m#m-${docId}-p${parId}s${sentId}</s.rf>
			<children>
				<pmcinfo>
					<pmctype>sent</pmctype>
					<children>
END
}

sub _printASentEnd
{
	my $output = shift;
	print $output <<END;
					</children>
				</pmcinfo>
			</children>
		</LM>
END
}

sub _printADataSimple
{
	my ($output, $docId, $parId, $sentId, $tokId, $token) = @_;
	print $output <<END;
						<node id="a-${docId}-p${parId}s${sentId}w$tokId">\t<!-- $token -->
							<m.rf>m#m-${docId}-p${parId}s${sentId}w$tokId</m.rf>
							<role>N/A</role>
							<ord>$tokId</ord>
						</node>
END
}

1;
