#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::TreeTransformations::DepPml2Conll;

use strict;
use warnings;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(transformFile space_replacement);

use Data::Dumper;
use File::Path;
use IO::File;
use XML::LibXML;  # XML handling library

use LvCorporaTools::TagTransformations::TagPurifier qw(purifyKamolsTag);
use LvCorporaTools::TagTransformations::Tag2FeatureList qw(parseTagSet decodeTag);

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
# What postag to use?
#our $postag = 'purify'; #'full' or 'purify', default is full.
# What cpostag to use?
#our $cpostag = 'first'; #'purify' or 'first' (single letter) or 'none', default is none;

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
   CPOSTAG mode [opt, 'purify' or 'first' (single letter) or 'none'(default)]
   POSTAG mode [opt, 'purify' or 'full' (default)]
   new file name [opt, current file name used otherwise]
   use CoNLL-2009 format [opt, false by default]

Latvian Treebank project, LUMII, 2012, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $printLabels = shift @_;
	my $dirPrefix = shift @_;
	my $oldName = shift @_;
	my $cpostag = (shift @_ or 'none');
	my $postag = (shift @_ or 'full');
	my $newName = (shift @_ or $oldName);
	my $conll2009 = (shift @_ or 0);
	
	# Open output file.
	File::Path::mkpath("$dirPrefix/res/");
	my $out = IO::File->new("$dirPrefix/res/$newName", "> :encoding(UTF-8)")
		or die "Could create file $newName: $!";
		
	# Load the tagset XML.
	my $tagDecoder = parseTagSet();
	
	# Load the data XML.
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
			print $out "$lemma\t"; #LEMMA

			if ($conll2009)
			{
				print $out "$lemma\t"; #PLEMMA
			}
			
			my $tag = ${$xpc->findnodes('pml:m.rf/pml:tag', $n)}[0]->textContent;
			$tag =~ tr/][//d;
			$tag =~ s#^N/A$#_#;
			
			if (! $conll2009)
			{
				if ($cpostag eq 'purify')
				{
					print $out purifyKamolsTag($tag); #CPOSTAG
				} elsif ($cpostag eq 'first')
				{
					$tag =~/^(.)/;
					print $out "$1"; #CPOSTAG
				} else
				{
					print $out "_"; #CPOSTAG
				}
				print $out "\t"; #CPOSTAG
			}

			if ($postag eq 'purify')
			{
				print $out purifyKamolsTag($tag); #POSTAG
			} else
			{
				print $out "$tag"; #POSTAG
			}
			print $out "\t"; #POSTAG

			if ($conll2009)
			{
				if ($postag eq 'purify')
				{
					print $out purifyKamolsTag($tag); #PPOS
				} else
				{
					print $out "$tag"; #PPOS
				}
				print $out "\t"; #PPOS
			}
			
			my $decoded = decodeTag($tag, $tagDecoder);
			my $feats = join '|', map{ join '=', @$_} @$decoded;
			$feats =~ s/ /\+/g;
			if (not $feats)
			{
				$feats = '_';
				warn "Tag $tag was not decoded!";
			}
			print $out "$feats\t"; #FEATS
			if ($conll2009)
			{
				print $out "$feats\t"; #PFEATS
			}

			exists($n2id{$head->findvalue('@id')}) ?
				print $out "$n2id{$head->findvalue('@id')}" : print $out "0";	#HEAD
			print $out "\t";
			if ($conll2009)
			{
				exists($n2id{$head->findvalue('@id')}) ?
					print $out "$n2id{$head->findvalue('@id')}" : print $out "0";	#PHEAD
				print $out "\t";
			}

			if ($printLabels)
			{
				print $out ${$xpc->findnodes('pml:role', $n)}[0]->textContent; #DEPREL
			} else
			{
				print $out "_"; #DEPREL
			}
			print $out "\t";
			
			if ($conll2009)
			{
				if ($printLabels)
				{
					print $out ${$xpc->findnodes('pml:role', $n)}[0]->textContent; #PDEPREL
				} else
				{
					print $out "_"; #PDEPREL
				}
			}

			if ($conll2009)
			{
				print $out "\t_\t_\n"; #FILLPRED, PRED, APRED1-6
			} else 
			{
				print $out "\t_\t_\n"; #PHEAD, PDEPREL
			}
		}
		print $out "\n";
	}
	
	print "Processing $oldName finished!\n";
}

1;
