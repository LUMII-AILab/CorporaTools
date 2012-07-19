#!C:\strawberry\perl\bin\perl -w
package LvTreeBank::Transformations::Hybrid2Dep;

use strict;
use warnings;
#use utf8;

use File::Path;
use IO::File;
use XML::LibXML;  # XML handling library

###############################################################################
# This program transforms Latvian Treebank analytical layer files from native
# hybrid format to dependency-only simplification. Input files are supposed to
# be valid against coresponding PML schemas. Invalid features like multiple
# roles, ords per single node are not checked.
#
# Input files - utf8.
# Output file can have diferent XML element order. To obtain standard order
# resave file with TrEd.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalniņa, LUMII, AILab, lauma@ailab.lv
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
Script for transfoming Latvian Treebank .a files from native hybrid format to
dependency-only format.
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

	# Load the XML.
	my $parser = XML::LibXML->new();
	my $doc = $parser->parse_file("$dirPrefix/$oldName");
	
	my $xpc = XML::LibXML::XPathContext->new();
	$xpc->registerNs('pml', 'http://ufal.mff.cuni.cz/pdt/pml/');
	$xpc->registerNs('fn', 'http://www.w3.org/2005/xpath-functions/');
	
	# Process XML:
	# process each tree...
	foreach my $tree ($xpc->findnodes('/pml:lvadata/pml:trees/pml:LM', $doc))
	{
		&transformTree($xpc, $tree);
		&recalculateOrds($xpc, $tree);
	}
	
	# ... and update the schema information and root name.
	my @schemas = $xpc->findnodes(
		'pml:lvadata/pml:head/pml:schema[@href=\'lvaschema.xml\']', $doc);
	$schemas[0]->setAttribute('href', 'lvaschema-deponly.xml');
	$doc->documentElement->setNodeName('lvadepdata');
	
	# Print the XML.
	File::Path::mkpath("$dirPrefix/res/");
	my $outFile = IO::File->new("$dirPrefix/res/$newName", ">")
		or die "Output file opening: $!";	
	print $outFile $doc->toString;	
	print "Processing $oldName finished!\n";
}

# Recalculate values for "ord" fields - make them start with 1 and be
# sequential. Remove ord for root node.
# recalculateOrds (XPath context with set namespaces, DOM node for tree root
#				 (usualy "LM"))
sub recalculateOrds
{
	my $xpc = shift @_; # XPath context
	my $tree = shift @_;

	# Find ord nodes and sort them.
	my @ords = $xpc->findnodes('.//pml:ord', $tree);
	my @sorted = sort {$a->textContent <=> $b->textContent} @ords;
	
	# Renumber ord nodes.
	my $nextId = 1;
	foreach my $o (@sorted)
	{
		my $parent = $o->parentNode;
		my $parName = $parent->nodeName;
		if ($parName ne 'node')
		{
			$parent->removeChild($o);
		}
		else
		{
			warn "recalculateOrds warns: Multiple textnodes below ord node.\n"
				if (@{$o->childNodes()} gt 1); # This should not happen.
			$o->removeChild($o->firstChild);
			$o->appendText($nextId);
			$nextId++;
		}
	}
}

# Transform single tee (LM element in most tree files).
# transformTree (XPath context with set namespaces, DOM node for tree root
#				 (usualy "LM"))
sub transformTree
{
	my $xpc = shift @_; # XPath context
	my $tree = shift @_;
	my $role = 'ROOT';
	
	# Well, actually, for valid trees there should be only one children node.
	foreach my $childrenWrap ($xpc->findnodes('pml:children', $tree))
	{
		my @phrases = $xpc->findnodes(
			'pml:children/*[local-name()!=\'node\']',
			$tree);
		die ($tree->find('../@id'))." has ".(scalar @phrases)." non-node children for the root."
			if (scalar @phrases ne 1);
			
		# Process rootnode's constituents.
		foreach my $ch ($xpc->findnodes('pml:children/pml:node', $phrases[0]))
		{
			&_transformSubtree($xpc, $ch);
		}
		# Process rootnode's dependants.
		foreach my $ch ($xpc->findnodes('pml:children/pml:node', $tree))
		{
			&_transformSubtree($xpc, $ch);
		}
			
		# Process PMC (probably) node.
		my $roleTag = $phrases[0]->nodeName();
		$roleTag =~ s/info$/type/;
		my $chRole = ${$xpc->findnodes("pml:$roleTag", $phrases[0])}[0]->textContent;
		my $newNode = &{\&{$chRole}}($xpc, $phrases[0], $role);
		
		# Create or find "children" element.
		my $newNodeChWrap = &_getChildrenNode($xpc, $newNode);	
		# Add children.
		foreach my $ch ($xpc->findnodes('pml:children/pml:node', $tree))
		{
			$ch->unbindNode();
			$newNodeChWrap->appendChild($ch);
		}
			
		# Add reformed subtree to the main tree.
		$phrases[0]->replaceNode($newNode);
	}
	
	&_finishRoles($xpc, $tree);
}

# Transform roles in form "someRole" to "someRole-0-0". Leave roles in form
# "firstRole-secondRole-thirdRole" intact.
# _finishRoles (XPath context with set namespaces, DOM node for tree root
#				 (usualy "LM"))
sub _finishRoles
{
	my $xpc = shift @_; # XPath context
	my $tree = shift @_;
	my @roles = $xpc->findnodes('.//pml:role', $tree);
	
	foreach my $r (@roles)
	{
		my $oldRole = $r->textContent;
		next if $oldRole =~/-.*-/;
		warn "_finishRoles warns: Multiple textnodes below role node.\n"
			if (@{$r->childNodes()} gt 1); # This should not happen.
		$r->removeChild($r->firstChild);
		$r->appendText("$oldRole-0-0");
	}
}

# Traversal function for procesing any subtree except "the big Tree" starting
# _transformSubtree (XPath context with set namespaces, DOM "node" node for
#					 subtree root)
sub _transformSubtree
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $role = ${$xpc->findnodes('pml:role', $node)}[0]->textContent;
	
	# Process dependency children first.
	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $node))
	{
		&_transformSubtree($xpc, $ch);
	}
	
	# Find phrase nodes.
	my @phrases = $xpc->findnodes(
		#'pml:children/pml:xinfo|pml:children/pml:coordinfo|pml:children/pml:pmcinfo',
		'pml:children/*[local-name()!=\'node\']',
		$node);
	# A bit structure checking: only one phrase per regular node is allowed.
	die (($node->find('@id'))." has ".(scalar @phrases)." non-node children.")
		if (scalar @phrases gt 1);
	
	# If there is no phrase children, finish.
	return if (scalar @phrases lt 1);
	
	# Process phrase constituents.
	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $phrases[0]))
	{
		&_transformSubtree($xpc, $ch);
	}
	
	# A bit structure checking: phrase can't have another phrase as direct child.
	my @phrasePhrases = $xpc->findnodes('pml:children/*[local-name()!=\'node\']', $phrases[0]);
	die (($node->find('@id'))." has illegal phrase cascade as child.")
		if (scalar @phrasePhrases gt 0);
	
	# Process phrase root.
	my $roleTag = $phrases[0]->nodeName();
	$roleTag =~ s/info$/type/;	
	my $phRole = ${$xpc->findnodes("pml:$roleTag", $phrases[0])}[0]->textContent;
	my $newNode = &{\&{$phRole}}($xpc, $phrases[0], $role);

	# Add dependency children.
	my $newNodeChWrap = &_getChildrenNode($xpc, $newNode);	
	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $node))
	{
		$ch->unbindNode();
		$newNodeChWrap->appendChild($ch);
	}

	$node->replaceNode($newNode);
}


###############################################################################
# Phrase specific functions (does not process phrase constituent children, this
# is responsibility of &_transformSubtree.
# phrase_name (XPath context with set namespaces, DOM "xinfo", "coordinfo" or
#			   "pmcinfo" node for subtree root)
###############################################################################

sub xPrep
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $parentRole = shift @_;
	
	# Find prep.
	my @preps = $xpc->findnodes('pml:children/pml:node[pml:role=\'prep\']', $node);
	die "xPrep below ". $node->find('../@id')." has ".(scalar @preps)." \"brep\"."
		if (scalar @preps ne 1);
	my $newRoot = $preps[0];

	# Change role for prep.
	&_setNodeRole($xpc, $newRoot, "$parentRole-xPrep-prep");
	
	# Rebuild subtree.
	$newRoot->unbindNode();
	my $basElemChNode = &_getChildrenNode($xpc, $newRoot);
	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $node))
	{
		# Change structure.
		$ch->unbindNode();
		$basElemChNode->appendChild($ch);
	}

	return $newRoot;
}

sub defaultPhrase
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $parentRole = shift @_;
	my $roleTag = $node->nodeName();
	$roleTag =~ s/info$/type/;
	my $phraseRole = ${$xpc->findnodes("pml:$roleTag", $node)}[0]->textContent;
	
	# Find the new root ('subroot') for the current subtree.
	my @basElems = $xpc->findnodes('pml:children/pml:node[pml:role=\'basElem\']', $node);
	my $lastBasElem = undef;
	my $curentPosition = -1;
	foreach my $ch (@basElems)	# Find a basElem with the greatest 'ord'.
	{
		my $tmpOrd = ${$xpc->findnodes('pml:ord', $ch)}[0]->textContent;
		if ($tmpOrd ge $curentPosition)
		{
			$lastBasElem = $ch;
			$curentPosition = $tmpOrd;
		}
	}
	if (not defined $lastBasElem) # If this phrase contains no basElem, use last constituent.
	{
		my @children = $xpc->findnodes('pml:children/pml:node', $node);
		$lastBasElem = $children[-1];
	}

	# Change role for the subroot.
	my $oldRole = ${$xpc->findnodes('pml:role', $lastBasElem)}[0]->textContent;
	&_setNodeRole($xpc, $lastBasElem, "$parentRole-$phraseRole-$oldRole");
	
	# Rebuild subtree.
	$lastBasElem->unbindNode();
	my $lastBasElemChNode = &_getChildrenNode($xpc, $lastBasElem);
	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $node))
	{
		# Change structure.
		$ch->unbindNode();
		$lastBasElemChNode->appendChild($ch);
	}
	
	return $lastBasElem;
}

###############################################################################
# Techsupport functions
###############################################################################

# _getChildrenNode (XPath context with set namespaces, DOM node)
# returns "children" element below given node (creates one, if there was none)
sub _getChildrenNode
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my @bestTry = $xpc->findnodes('pml:children', $node);

	return $bestTry[0] if (@bestTry);
	
	my $childrenNode = XML::LibXML::Element->new('children');
	$childrenNode->setNamespace('http://ufal.mff.cuni.cz/pdt/pml/');
	$node->appendChild($childrenNode);
	return $childrenNode;
}

# Sets new role to the given node.
# _setNodeRole (XPath context with set namespaces, DOM "node" node, new role value)
sub _setNodeRole
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $newRole = shift @_;
	die 'Can\'t set \"role\" for '.$node->nodeName."\n"
		unless ($node->nodeName eq 'node');
		
	my @roles = $xpc->findnodes('pml:role', $node);
	my $newRoleNode = XML::LibXML::Element->new('role');
	$newRoleNode->setNamespace('http://ufal.mff.cuni.cz/pdt/pml/');
	$newRoleNode->appendTextNode($newRole);
	$roles[0]->replaceNode($newRoleNode);
}

# AUTOLOAD is called when someone tries to access nonexixting method through
# this package. See perl documentation.
# This is used for handling unknown PMC/X-word/coordination types.
sub AUTOLOAD
{
	our $AUTOLOAD;
	my $name = $AUTOLOAD;
	$name =~ s/.*::(.*?)$/$1/;
	print "Don't know how to process \"$name\", will use default rule.\n";
	return &defaultPhrase;
}
1;