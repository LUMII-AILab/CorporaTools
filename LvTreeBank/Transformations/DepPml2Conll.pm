#!C:\strawberry\perl\bin\perl -w
package LvTreeBank::Transformations::DepPml2Conll;

use strict;
use warnings;

use Data::Dumper;
use File::Path;
use IO::File;
use XML::LibXML;  # XML handling library

###############################################################################
# This program transforms Latvian Treebank files in dependency-only form from
# knited-in PML to CoNLL format. Inputfiles must containtain no empty nodes
# except the root node for each tree. Invalid features like multiple ords per
# single node are not checked.
#
# Works with A level schema v.2.12.
# Input files - utf8.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

# Process single XML file. This should be used as entry point, if this module
# is used standalone.

sub transformFile
{
	autoflush STDOUT 1;
	if (not @_ or @_ le 2)
	{
		print <<END;
Script for transfoming dependency-only Latvian Treebank files from knited-in
PML format to CoNLL format. 
Input files should be provided as UTF-8.

Params:
   directory prefix
   file name
   new file name [opt, current file name used otherwise]

Latvian Treebank project, LUMII, 2012, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $oldName = shift @_;
	my $newName = (shift @_ or $oldName);
	
	# Open output file.
	File::Path::mkpath("$dirPrefix/res/");
	my $out = IO::File->new("$dirPrefix/res/$newName", "> :encoding(UTF-8)")
		or die "Could create file $newName: $!";
	
	# Load the XML.
	my $parser = XML::LibXML->new('no_blanks' => 1);
	my $doc = $parser->parse_file("$dirPrefix/$oldName");
	
	my $xpc = XML::LibXML::XPathContext->new();
	$xpc->registerNs('pml', 'http://ufal.mff.cuni.cz/pdt/pml/');
	$xpc->registerNs('fn', 'http://www.w3.org/2005/xpath-functions/');

	# Process each tree.
	foreach my $tree ($xpc->findnodes('/pml:lvadepdata/pml:trees/pml:LM', $doc))
	{
		my @nodes = sort
			{
				${$xpc->findnodes('pml:ord', $a)}[0]->textContent
				<=> ${$xpc->findnodes('pml:ord', $b)}[0]->textContent
			}
			($xpc->findnodes('.//pml:node', $tree));
		
		# Calculate ID's for CoNLL format.
		my $id = 1;
		my %n2id = ();
		foreach my $n (@nodes)
		{
			$n2id{$n} = $id;
			$id++;
			# Do stuff.
		}
		
		# Do output in CoNLL format.
		foreach my $n (@nodes)
		{
			my $head = $n->parentNode->parentNode;
			print Dumper($n);
			print $out "$n2id{$n}\t"; #ID
			print $out ${$xpc->findnodes('pml:m.rf/pml:form', $n)}[0]->textContent; #FORM
			print $out "\t";
			print $out ${$xpc->findnodes('pml:m.rf/pml:lemma', $n)}[0]->textContent; #LEMMA
			print $out "\t_\t"; #CPOSTAG
			print $out ${$xpc->findnodes('pml:m.rf/pml:tag', $n)}[0]->textContent; #POSTAG
			print $out "\t_\t"; #FEATS
			exists($n2id{$head}) ? print $out "$n2id{$head}" : print $out "0";	#HEAD
			print $out "\t";
			print $out ${$xpc->findnodes('pml:role', $n)}[0]->textContent; #DEPREL
			print $out "\t_\t_\n"; #PHEAD, PDEPREL
		}
		print $out "\n";
	}
	
	print "Processing $oldName finished!\n";
}

1;
