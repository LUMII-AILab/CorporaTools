#!C:\strawberry\perl\bin\perl -w
package LvMorphoCorpus::TestDataSelector::SplitData;
use utf8;
use strict;
###############################################################################
# This module splits PML M files into two data sets, whose size depends on
# given probability. All new M files reference the same W files as the old
# ones.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

use File::Path;
use IO::File;
use XML::LibXML;

###############################################################################
# Split single M file.
###############################################################################
sub splitFile
{
	autoflush STDOUT 1;
	if (not @_ or @_ le 2)
	{
		print <<END;
Script for splitting PML M file into two data sets, whose size depends on given
probability..
Input files should be provided as UTF-8.

Params:
   directory prefix
   file name
   probability
   seed [optional]

Latvian Treebank project, LUMII, 2012, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $fileName = shift @_;
	my $prob = shift @_;
	my $seed = shift @_;

	# Load the XML.
	my $parser = XML::LibXML->new('no_blanks' => 1);
	my $data = $parser->parse_file("$dirPrefix/$fileName");
	
	my $xpc = XML::LibXML::XPathContext->new();
	$xpc->registerNs('pml', 'http://ufal.mff.cuni.cz/pdt/pml/');
	#$xpc->registerNs('fn', 'http://www.w3.org/2005/xpath-functions/');
	
	# Create DOMs for result files.
	my $devSet = &_copyHeader($xpc, $data);
	my $testSet = &_copyHeader($xpc, $data);
	
	srand $seed;
	# Process each sentence.
	foreach my $sent ($xpc->findnodes('/pml:lvmdata/pml:s', $data))
	{
		my $coin = rand;
		my $destSet = $coin ge $prob ? $devSet : $testSet;
		my $destRoot = $destSet->documentElement;
		$destRoot->appendChild($sent->cloneNode(1));
	}

	# Print out all the results.
	mkpath("$dirPrefix/dev/");
	mkpath("$dirPrefix/test/");
	my $outFile = IO::File->new("$dirPrefix/dev/$fileName", ">")
		or die "Output file opening: $!";	
	print $outFile $devSet->toString(1);
	$outFile = IO::File->new("$dirPrefix/test/$fileName", ">")
		or die "Output file opening: $!";	
	print $outFile $testSet->toString(1);
	
	#Print stats.
	my $sentCount = @{$xpc->findnodes('/pml:lvmdata/pml:s', $devSet)};
	my $morphoCount = @{$xpc->findnodes('/pml:lvmdata/pml:s/pml:m', $devSet)};
	print "Development set contains $sentCount sentences and $morphoCount tokens.\n";
	$sentCount = @{$xpc->findnodes('/pml:lvmdata/pml:s', $testSet)};
	$morphoCount = @{$xpc->findnodes('/pml:lvmdata/pml:s/pml:m', $testSet)};
	print "Test set contains $sentCount sentences and $morphoCount tokens.\n";
	print "Processing $fileName finished!\n";
}

# Create new DOM document and copy header information from the given DOM
# document.
# _copyHeader (XPath context with set namespaces, source document)
# Returns new DOM document.
sub _copyHeader
{
	my $xpc = shift @_;
	my $oldDoc = shift @_;
	
	my $newDoc = XML::LibXML::Document->new(
		$oldDoc->version(), $oldDoc->encoding());
	my $newRoot = $oldDoc->documentElement->cloneNode(0);
	$newDoc->setDocumentElement($newRoot);
	
	my @copyNodes = $xpc->findnodes(
		'/pml:lvmdata/*[local-name()!=\'s\']', $oldDoc);
	foreach my $n (@copyNodes)
	{
		$newRoot->appendChild($n->cloneNode(1));
	}

	return $newDoc;
}
1;