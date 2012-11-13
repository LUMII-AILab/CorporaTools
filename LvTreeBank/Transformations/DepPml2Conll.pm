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

# If form or token contains space, it will be replaced with this string.
our $space_replacement = "_";

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
   0/1 - print labeled output (if true, trees with reductions will be ommited)
   directory prefix
   file name
   new file name [opt, current file name used otherwise]

Latvian Treebank project, LUMII, 2012, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $printLabels = shift @_;
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
		if ($printLabels and @{$xpc->findnodes('.//pml:reduction', $tree)} gt 0)
		{
			print "Tree ".$tree->find('@id')." has reductions and, thus, will be omitted.\n";
			next;
		}
		my @nodes = sort
			{
				${$xpc->findnodes('pml:ord', $a)}[0]->textContent
				<=> ${$xpc->findnodes('pml:ord', $b)}[0]->textContent
			}
			($xpc->findnodes('.//pml:node[pml:m.rf]', $tree));
		
		# Calculate ID's for CoNLL format.
		my $id = 1;
		my %n2id = ();
		foreach my $n (@nodes)
		{
			$n2id{$n->findvalue('@id')} = $id;
			$id++;
		}

		# Do output in CoNLL format.
		foreach my $n (@nodes)
		{
			my $head = $n->parentNode;
			while (not exists($n2id{$head->findvalue('@id')}) and
				$head->findvalue('@id') ne $tree->findvalue('@id'))
			{
				$head = $head->parentNode;
			}
			print $out "$n2id{$n->findvalue('@id')}\t"; #ID
			my $form = ${$xpc->findnodes('pml:m.rf/pml:form', $n)}[0]->textContent;
			$form =~ s/ /\Q$space_replacement\E/g;
			print $out "$form\t"; #FORM
			my $lemma = ${$xpc->findnodes('pml:m.rf/pml:lemma', $n)}[0]->textContent;
			$lemma =~ s/ /\Q$space_replacement\E/g;
			print $out "$lemma\t_\t"; #LEMMA, CPOSTAG
			print $out ${$xpc->findnodes('pml:m.rf/pml:tag', $n)}[0]->textContent; #POSTAG
			print $out "\t_\t"; #FEATS
			exists($n2id{$head->findvalue('@id')}) ?
				print $out "$n2id{$head->findvalue('@id')}" : print $out "0";	#HEAD
			print $out "\t";
			if ($printLabels)
			{
				print $out ${$xpc->findnodes('pml:role', $n)}[0]->textContent; #DEPREL
			} else
			{
				print $out "_"; #DEPREL
			}
			print $out "\t_\t_\n"; #PHEAD, PDEPREL
		}
		print $out "\n";
	}
	
	print "Processing $oldName finished!\n";
}

1;
