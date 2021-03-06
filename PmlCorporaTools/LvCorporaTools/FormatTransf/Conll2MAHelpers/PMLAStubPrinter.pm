#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::Conll2MAHelpers::PMLAStubPrinter;
use strict;
use warnings;
use LvCorporaTools::FormatTransf::Plaintext2W qw(escape);

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK =
	qw(printAFileBegin printAFileEnd printASentBegin printASentEnd
		printAPhraseBegin printAPhraseEnd printANodeStart printANodeEnd printALeaf);

#TODO rework in proper OOP way.
# Mostly these functions just print stuff in output stream to create PML-A files.

# PML A file header.
sub printAFileBegin
{
	my ($output, $docId, $annotationDesc) = @_;
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
			<desc>$annotationDesc</desc>
		</annotation_info>
	</meta>

	<trees>
END
}

# PML-A file footer.
sub printAFileEnd
{
	my $output = shift @_;
	print $output <<END;
	</trees>
</lvadata>
END
}

# PML-A file sentence header without PMC node.
sub printASentBegin
{
	my ($output, $aSentId, $mSentId, $comment) = @_;
	print $output <<END;

		<LM id="$aSentId">
			<s.rf>m#$mSentId</s.rf>
END
	if ($comment)
	{
		my $escComment = escape($comment);
		print $output <<END;
			<comment>$escComment</comment>
END
	}
	print $output <<END;
			<children>
END
}

# PML-A file sentence footer without PMC.
sub printASentEnd
{
	my $output = shift;
	print $output <<END;
			</children>
		</LM>
END
}

# PML-A file pmcinfo, xinfo or coordinfo node header.
sub printAPhraseBegin
{
	my ($output, $phraseType, $phraseSubType) = @_;
	print $output <<END;
				<${phraseType}info>
					<${phraseType}type>$phraseSubType</${phraseType}type>
END
	if ($phraseType eq 'x')
	{
		print $output <<END;
					<tag>N/A</tag>
END
	}
	print $output <<END;
					<children>
END
}

# PML-A file pmcinfo, xinfo or coordinfo node footer.
sub printAPhraseEnd
{
	my ($output, $phraseType) = @_;
	print $output <<END;
					</children>
				</${phraseType}info>
END
}

# Single PML-A data node - leaf in the tree.
sub printALeaf
{
	my ($output, $aId, $role, $mId, $ord, $token) = @_;
	$role = 'N/A' unless $role;
	my $escToken = escape($token);
	print $output <<END;
						<node id="$aId">\t<!-- $escToken -->
							<m.rf>m#$mId</m.rf>
							<role>$role</role>
							<ord>$ord</ord>
						</node>
END
}

# Header and the data for single PML-A data node - non-leaf node.
sub printANodeStart
{
	my ($output, $aId, $role, $mId, $ord, $token) = @_;
	$role = 'N/A' unless $role;
	my $escToken = escape($token);
	print $output <<END;
						<node id="$aId">
							<role>$role</role>
END
	if ($mId)
	{
		print $output <<END;
							<!-- $escToken -->
							<m.rf>m#$mId</m.rf>
							<ord>$ord</ord>
END
	}
	print $output <<END;
							<children>
END
}

# Footer for single PML-A data node - non-leaf node.
sub printANodeEnd
{
	my $output = shift;
	print $output <<END;
							</children>
						</node>
END
}

1;