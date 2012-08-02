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
#use IO::Dir;
use XML::LibXML;

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

Latvian Treebank project, LUMII, 2012, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $fileName = shift @_;
	my $prob = shift @_;

	# Load the XML.
	my $parser = XML::LibXML->new('no_blanks' => 1);
	my $data = $parser->parse_file("$dirPrefix/$fileName");
	
	my $xpc = XML::LibXML::XPathContext->new();
	$xpc->registerNs('pml', 'http://ufal.mff.cuni.cz/pdt/pml/');
	#$xpc->registerNs('fn', 'http://www.w3.org/2005/xpath-functions/');
	
	# Create DOMs for result files.
	my $devSet = &_copyHeader($xpc, $data);
	my $testSet = &_copyHeader($xpc, $data);
	
	# Process XML:

	# Print out all the results.
	mkpath("$dirPrefix/dev/");
	mkpath("$dirPrefix/test/");
	my $outFile = IO::File->new("$dirPrefix/dev/$fileName", ">")
		or die "Output file opening: $!";	
	print $outFile $devSet->toString(1);
	$outFile = IO::File->new("$dirPrefix/test/$fileName", ">")
		or die "Output file opening: $!";	
	print $outFile $testSet->toString(1);
	print "Processing $fileName finished!\n";
}

sub _copyHeader
{
	my $xpc = shift @_;
	my $oldDoc = shift @_;	
	my $newDoc = (shift @_ or XML::LibXML::Document->new(
		$oldDoc->version(), $oldDoc->encoding()));
	my $newRoot = $oldDoc->documentElement->cloneNode(0);
	$newDoc->setDocumentElement($newRoot);
	
	my @copyNodes = $xpc->findnodes(
		'pml:lvmdata/*[local-name()!=\'s\']', $oldDoc);
	foreach my $n (@copyNodes)
	{
		$newRoot->appendChild($n->cloneNode(1));
	}

	return $newDoc;
}
1;