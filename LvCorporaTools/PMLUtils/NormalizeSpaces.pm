package LvCorporaTools::PMLUtils::NormalizeSpaces;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processFile processDir);

use File::Path;
use IO::File;
use IO::Dir;
use XML::LibXML;

###############################################################################
# Script for normalizing spaces in PML .m file acording to .w file. Only m
# elements with multiple w.rf are tested for changes.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2013
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

# Process all .m files in given folder. This can be used as entry point, if
# this module is used standalone.
sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 1)
	{
		print <<END;
Script for normalizing spaces (lemmas and forms) in all .m files in given
folder according to spacing in .w files
Input files should be provided as UTF-8.

Params:
   data directory 
   normalize lemmas, too? [opt, 0/1, 1(yes) assumed by default, not implemented yet]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}

	my $dirName = shift @_;
	my $doLemmas = (shift @_ or 1);
	my $dir = IO::Dir->new($dirName) or die "dir $!";

	while (defined(my $inFile = $dir->read))
	{
		if ((! -d "$dirName/$inFile") and ($inFile =~ /^(.+)\.m$/))
		{
			&processFile ($dirName, $1, $doLemmas);
		}
	}
}

# Process single PML m file consulting w file. This can be used as entry point,
# if this module is used standalone.
sub processFile
{
	autoflush STDOUT 1;
		if (not @_ or @_ < 2)
	{
		print <<END;
Script for normalizing spaces in .m file (lemmas and forms) according to
spacing in .w file
Input files should be provided as UTF-8.

Params:
   directory prefix
   file name without extension
   normalize lemmas, too? [opt, 0/1, 1(yes) assumed by default, not implemented yet]
   new file name [opt, current file name used otherwise]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}

	my $dirPrefix = shift @_;
	my $oldName = shift @_;
	my $doLemmas = (shift @_ or 1);
	my $newName = (shift @_ or "$oldName.m");
	my $changed = 0;

	mkpath("$dirPrefix/res/");
	mkpath("$dirPrefix/logs/");
	my $logFile = IO::File->new("$dirPrefix/logs/$oldName-log.txt", ">")
		or die "$newName-warnings.txt: $!";
	print "Processing $oldName started...\n";

	my $parser = XML::LibXML->new('no_blanks' => 1);
	my $mDoc = $parser->parse_file("$dirPrefix/$oldName.m");
	my $wDoc = $parser->parse_file("$dirPrefix/$oldName.w");
	
	my $xpc = XML::LibXML::XPathContext->new();
	$xpc->registerNs('pml', 'http://ufal.mff.cuni.cz/pdt/pml/');
	$xpc->registerNs('fn', 'http://www.w3.org/2005/xpath-functions/');
	
	# Process each morpheme in m file.
	foreach my $m ($xpc->findnodes('/pml:lvmdata/pml:s/pml:m', $mDoc))
	{
		#print "M ir te\n";
		# Obtain w.rf (multiple, single or no values posible)
		my @wrf = $xpc->findnodes('pml:w.rf/pml:LM', $m);
		my $mId = $xpc->findvalue('@id', $m);
		
		if (@wrf and @wrf > 1)
		{
			my @refs = map {$_->textContent} @wrf;
			my $wString;
			for my $ref (@refs)
			{
				# Obtain corresponding w element.
				my $refCore = ($ref =~ m/^w#(.*)$/) ? $1 : $ref;
				my @wNodes = $xpc->findnodes(
					"/pml:lvwdata/pml:doc/pml:para/pml:w[\@id=\'$refCore\']", $wDoc);
				die "ID $refCore not found in $oldName.w $!" unless (@wNodes);
				die "ID $refCore found multiple times in $oldName.w $!"
					if (@wNodes > 1);
				
				$wString .= $xpc->findvalue('pml:token', $wNodes[0]);
				$wString .= ' ' unless ($xpc->findvalue('pml:no_space_after', $wNodes[0]));
			}
			my @forms = $xpc->findnodes('pml:form', $m);
			
			if ($forms[0]->textContent ne $wString)
			{
				# Set new form.
				$forms[0]->removeChild($forms[0]->firstChild);
				$forms[0]->appendText($wString);
				print $logFile "$mId changed.\n";
				$changed++;
				
				if ($doLemmas)
				{
					# Set new lemma.
					# HOW??????????????????????????????????????????????????????
					
				}
			}

		}
		
	}
	# Print the XML.
	my $outFile = IO::File->new("$dirPrefix/res/$newName", ">")
		or die "Output file opening: $!";	
	print $outFile $mDoc->toString(1);
	$outFile->close();
	
	print "Processing $oldName finished! $changed m elements changed.\n";
	print $logFile "Processing $oldName finished!\n";
	$logFile->close();
}

1;
