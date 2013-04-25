#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::PMLUtils::AUtils;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(getRole setNodeRole getChildrenNode hasChildrenNode moveChildren sortNodesByOrd);

use XML::LibXML;

###############################################################################
# Helper functions for processing Latvian Treebank analytical layer PML files.
# Input data are supposed to be valid against coresponding PML schemas.
#
# Works with A level schema v.2.14.
# Input files - utf8.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2013
# Lauma Pretkalniņa, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

# getRole (XPath context with set namespaces, DOM node/xinfo/coordinfo/pmcinfo
#		   node)
# return node's role or phrase role
sub getRole
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $tag = $node->nodeName();
	
	# Process dependency/constituent nodes.
	return ${$xpc->findnodes('pml:role', $node)}[0]->textContent
		if ($tag eq 'node');
	# Process phrase roles.
	$tag =~ s/info$/type/;
	my $role = ${$xpc->findnodes("pml:$tag", $node)}[0]->textContent;
	return $role;
}

# Set new role to the given node.
# setNodeRole (XPath context with set namespaces, DOM "node" node, new role value)
sub setNodeRole
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $newRole = shift @_;
	die 'Can\'t set \"role\" for '.$node->nodeName."!\n"
		unless ($node->nodeName eq 'node');
		
	my @roles = $xpc->findnodes('pml:role', $node);
	my $newRoleNode = XML::LibXML::Element->new('role');
	$newRoleNode->setNamespace('http://ufal.mff.cuni.cz/pdt/pml/');
	$newRoleNode->appendTextNode($newRole);
	$roles[0]->replaceNode($newRoleNode);
}

# getChildrenNode (XPath context with set namespaces, DOM 
#				   node/xinfo/coordinfo/pmcinfo node)
# return "children" element below given node (creates one, if there was none)
sub getChildrenNode
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

# hasChildrenNode (XPath context with set namespaces, DOM
#				   node/xinfo/coordinfo/pmcinfo node)
# return "children" element below given node if it has one, otherwise false.
sub hasChildrenNode
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my @bestTry = $xpc->findnodes('pml:children', $node);
	return $bestTry[0] if (@bestTry);
	return 0;
}

# Move all children of type "node" from one node to an other.
# moveChildren (XPath context with set namespaces, old parent of children
#					to be moved, new parent)
# return new parent
sub moveChildren
{
	my $xpc = shift @_; # XPath context
	my $oldRoot = shift @_;
	my $newRoot = shift @_;
	
	my $chNode = getChildrenNode($xpc, $newRoot);
	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $oldRoot))
	{
		$ch->unbindNode();
		$chNode->appendChild($ch);
	}
	return $newRoot;
}

# sortNodesByOrd (XPath context with set namespaces, should array be sorted in
#				   descending order?, [array with] DOM nodes)
# returns reference to sorted array (original array is not mutated)
sub sortNodesByOrd
{
	my $xpc = shift @_; # XPath context
	my $desc = shift @_;
	my @nodes = @_;
	
	my @res = sort {
			${$xpc->findnodes('pml:ord', $a)}[0]->textContent * (1 - 2*$desc)
			<=>
			${$xpc->findnodes('pml:ord', $b)}[0]->textContent * (1 - 2*$desc)
		} @nodes;
	return \@res;
}

1;