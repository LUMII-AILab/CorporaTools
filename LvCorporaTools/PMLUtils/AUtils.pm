#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::PMLUtils::AUtils;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(
	renumberTokens renumberNodes getOrd setOrd getRole setNodeRole
	getChildrenNode hasChildrenNode moveChildren sortNodesByOrd);

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

# Renumber nodes with links to tokens based on node ordering already in tree.
# TODO: renumbering based on m IDs?
# renumberTokens (XPath context with set namespaces, DOM node for tree root
#				  (usualy "LM"))
sub renumberTokens
{
	my $xpc = shift @_; # XPath context
	my $tree = shift @_;

	# Remove 'ord' from root.
	my @rootOrds = $xpc->findnodes('./pml:ord', $tree);
	for (@rootOrds)
	{
		$tree->removeChild($_);
	}
	
	# Find nodes with ords and remove unneeded ords.
	my @noMorphoNodes = $xpc->findnodes(
		'.//*[pml:ord and not(pml:m.rf) and not(pml:m)]', $tree);
	for my $n (@noMorphoNodes)
	{
		#print &getRole ($xpc, $n);
		my @ords = $xpc->findnodes('pml:ord', $n);
		for (@ords)
		{
			#$n->removeChild($_);
			$_->unbindNode();
		}
	}
	
	# Find reminding nodes with ords and recalculate ords.
	my @morphoNodes = $xpc->findnodes('.//*[pml:ord]', $tree);
	my @sorted = @{&sortNodesByOrd($xpc, 0, @morphoNodes)};
	my $nextOrd = 1;
	for my $n (@sorted)
	{
		&setOrd($xpc, $n, $nextOrd);
		#my @ords = $xpc->findnodes('pml:ord', $n);
		#my $newOrdNode = XML::LibXML::Element->new('ord');
		#$newOrdNode->setNamespace('http://ufal.mff.cuni.cz/pdt/pml/');
		#$newOrdNode->appendTextNode($nextOrd);
		#$ords[0]->replaceNode($newOrdNode);
		$nextOrd++;
	}
	return $tree;
}

# renumberNodes (XPath context with set namespaces, DOM node for tree root
#				 (usualy "LM"))
# NB! Essentially this ir the same as in LV_A_Edit.mak
sub renumberNodes
{
	my $xpc = shift @_; # XPath context
	my $tree = shift @_;
	
	&renumberTokens($xpc, $tree);
	
	&_renumberNodesSubtree($xpc, $tree, $tree);
	
	return $tree;
}

sub _renumberNodesSubtree
{
	my $xpc = shift @_; # XPath context
	my $root = shift @_;
	my $tree = shift @_;
	my $smallerSibOrd = (shift or 0);

	# Currently best found ID for root node.
	my $newId = 0;
	
	# Process children.
	if (&hasChildrenNode($xpc, $root))
	{
		# At first we process those children who have nonzero ord somewhere
		# below them. After that - all other children.
	
		# Seperate children with nonzero ords.
		# TODO: Rewrite this with sort?
		my (@processFirst, @postponed) = ((), ());
		my $chNode = &getChildrenNode($xpc, $root);
		for my $ch ($chNode->childNodes())
		{
			#my @ordChildren = $xpc->findnodes('.//pml:ord', $ch);
			my @mChildren = $xpc->findnodes('.//pml:m.rf|.//pml:m', $ch);
			if (@mChildren and @mChildren > 0)
			{
				push @processFirst, $ch;
			}
			else
			{
				push @postponed, $ch;
			}
		}
		
		# Process children recursively.
		push @processFirst, @postponed;
		for my $ch (@processFirst)
		{
			# Find smallest sibling ord.
			my @sibOrds = sort {$a <=> $b } (
				map {$_->textContent} ($xpc->findnodes('pml:children/*/pml:ord', $root)));
			&_renumberNodesSubtree($xpc, $ch, $tree, $sibOrds[0]);
			my $tmpOrd = &getOrd($xpc, $ch);
			$newId = $tmpOrd
				if ($tmpOrd and ($tmpOrd < $newId or $newId <= 0));
			
		}
	}
	#return if (${$xpc->findnodes('pml:ord', $root)}[0]->textContent > 0);
	return if (&getOrd($xpc, $root));
	
	# Obtain new id if given node has no children.
	$newId = $smallerSibOrd if ($newId <= 0);
	
	# Obtain new id if given node has no children and no siblings.	
	if ($newId <= 0)
	{
		warn "No ord could be calculated $!";
	}
	$newId++;

	#if ($newId <= 0)
	#{
	#	my $follower = $tree->following;
	#	while ($follower and $follower->attr('ord') <= 0)
	#	{
	#		$follower = $follower->following;
	#	}
	#	$newId = $follower->attr('ord') if ($follower);
	#} else
	#{
	#	$newId++;
	#}
	
	# Shift by one all ords greater or equal $newId so that $newId can be used
	# for root of the subtree.
	for my $node ($xpc->findnodes('.//*[pml:ord]', $tree))
	{
		my $ord = &getOrd($xpc, $node);
		&setOrd ($xpc, $node, $ord + 1)
			if ($ord and $ord >= $newId);
	}
	
	# Give ord to root of the subtree.	
	&setOrd($xpc, $root, $newId);
}

# getOrd (XPath context with set namespaces, DOM node)
# return ord value, if there is one, undef otherwise.
sub getOrd
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my @ords = $xpc->findnodes('pml:ord', $node);
	return $ords[0]->textContent
		if (@ords and @ords > 0);
	return undef;
}

# Set new ord to the given node.
# setOrd (XPath context with set namespaces, DOM node, new ord value)
sub setOrd
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $newOrd = shift @_;
	
	my @ords = $xpc->findnodes('pml:ord', $node);
	my $newOrdNode = XML::LibXML::Element->new('ord');
	$newOrdNode->setNamespace('http://ufal.mff.cuni.cz/pdt/pml/');
	$newOrdNode->appendTextNode($newOrd);
	if (@ords and @ords > 0)
	{
		$ords[0]->replaceNode($newOrdNode);
	}
	else
	{
		$node->addChild($newOrdNode);
	}
}

# getRole (XPath context with set namespaces, DOM node/xinfo/coordinfo/pmcinfo
#		   node)
# return node's role or phrase role
sub getRole
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $tag = $node->nodeName();
	
	# Process root.
	return 'ROOT' if ($tag eq 'LM');
	
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
	die 'Can\'t set \"role\" for '.$node->nodeName.": $!"
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
	
	my @res = sort
	{
		#${$xpc->findnodes('pml:ord', $a)}[0]->textContent * (1 - 2*$desc) <=>
		#${$xpc->findnodes('pml:ord', $b)}[0]->textContent * (1 - 2*$desc)
		&getOrd($xpc, $a) <=> &getOrd($xpc, $b)
	} @nodes;
	return \@res;
}

1;