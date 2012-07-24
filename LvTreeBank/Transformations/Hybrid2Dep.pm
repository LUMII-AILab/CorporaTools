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
# roles, ords per single node are not checked. To obtain best results, input
# files should have all nodes numbered (TODO: fix this).
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
	my $parser = XML::LibXML->new('no_blanks' => 1);
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
	print $outFile $doc->toString(1);
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
		} else
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
		die ($tree->find('@id'))." has ".(scalar @phrases)." non-node children for the root."
			if (scalar @phrases ne 1);
			
		# Process rootnode's constituents.
#		foreach my $ch ($xpc->findnodes('pml:children/pml:node', $phrases[0]))
#		{
#			&_transformSubtree($xpc, $ch);
#		}
		# Process rootnode's dependants.
#		foreach my $ch ($xpc->findnodes('pml:children/pml:node', $tree))
#		{
#			&_transformSubtree($xpc, $ch);
#		}
			
		# Process PMC (probably) node.
		my $chRole = &_getRole($xpc, $phrases[0]);
		my $newNode = &{\&{$chRole}}($xpc, $phrases[0], $role);
		&_moveAllChildren($xpc, $tree, $newNode);
		# Add reformed subtree to the main tree.
		$phrases[0]->replaceNode($newNode);

		# Process children.
		foreach my $ch ($xpc->findnodes('pml:children/pml:node', $newNode))
		{
			&_transformSubtree($xpc, $ch);
		}
		&_transformSubtree($xpc, $newNode);

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
	my $role = &_getRole($xpc, $node);

	# Process dependency children first.
#	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $node))
#	{
#		&_transformSubtree($xpc, $ch);
#	}
	
	# Find phrase nodes.
	my @phrases = $xpc->findnodes(
		#'pml:children/pml:xinfo|pml:children/pml:coordinfo|pml:children/pml:pmcinfo',
		'pml:children/*[local-name()!=\'node\']',
		$node);
	# A bit structure checking: only one phrase per regular node is allowed.
	die (($node->find('@id'))." has ".(scalar @phrases)." non-node children.")
		if (scalar @phrases gt 1);
	
	# If there is no phrase children, process dependency children and finish.
	if (scalar @phrases lt 1)
	{
		foreach my $ch ($xpc->findnodes('pml:children/pml:node', $node))
		{
			&_transformSubtree($xpc, $ch);
		}
		return;
	}
	
	# Process phrase constituents.
#	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $phrases[0]))
#	{
#		&_transformSubtree($xpc, $ch);
#	}
	
	# A bit structure checking: phrase can't have another phrase as direct child.
	my @phrasePhrases = $xpc->findnodes(
		'pml:children/*[local-name()!=\'node\']', $phrases[0]);
	die (($node->find('@id'))." has illegal phrase cascade as child.")
		if (scalar @phrasePhrases gt 0);
	
	# Process phrase node.
	my $phRole = &_getRole($xpc, $phrases[0]);
	my $newNode = &{\&{$phRole}}($xpc, $phrases[0], $role);
	&_moveAllChildren($xpc, $node, $newNode);
	$node->replaceNode($newNode);

	# Process childen.
	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $newNode))
	{
		&_transformSubtree($xpc, $ch);
	}
	&_transformSubtree($xpc, $newNode);
}


###############################################################################
# Phrase specific functions (does not process phrase constituent children, this
# is responsibility of &_transformSubtree.
# phrase_name (XPath context with set namespaces, DOM "xinfo", "coordinfo" or
#			   "pmcinfo" node for subtree root, parent role)
###############################################################################

### X-words ###################################################################
# TODO: xPred, xNum, xApp, phrasElem
sub xPrep
{
	return &_allNodesBelowOne('prep', @_);
}
sub xSimile
{
	return &_allNodesBelowOne('conj', @_);
}
sub xParticle
{
	return &_allNodesBelowOne('particle', @_);
}
sub wGrAnal
{
	return &defaultPhrase(@_);
}

sub namedEnt
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $parentRole = shift @_;

	# Find the new root ('subroot') for the current subtree.
	my @ch = $xpc->findnodes('pml:children/pml:node', $node);
	die 'namedEnt below '. $node->find('../../@id').' has no children.'
		if (@ch lt 1);

	if (@ch gt 1)
	{
		return &defaultPhrase($xpc, $node, $parentRole);
	} else
	{
		# Change role for the subroot.
		my $oldRole = &_getRole($xpc, $ch[0]);
		&_setNodeRole($xpc, $ch[0], "$parentRole-namedEnt-$oldRole");
		$ch[0]->unbindNode();
		return $ch[0];
	}
}

### Coordination ##############################################################

sub crdParts
{
	return &_defaultCoord(@_);
}
sub crdClauses
{
	return &_defaultCoord(@_);
}
sub crdGeneral
{
	return &_defaultCoord(@_);
}

### PMC #######################################################################
# TODO: tied, abbr
# TODO: everything with insertions

sub sent
{
	return &_defaultPmc(@_);
}
sub mainCl
{
	return &_defaultPmc(@_);
}
sub subrCl
{
	return &_defaultPmc(@_);
}
sub interj
{
	return &_defaultPmc(@_);
}
sub spcPmc
{
	return &_defaultPmc(@_);
}
sub numPmc
{
	return &_defaultPmc(@_);
}
sub insPmc
{
	return &_defaultPmc(@_);
}
sub particle
{
	return &_defaultPmc(@_);
}
sub dirSp
{
	return &_defaultPmc(@_);
}
sub report
{
	return &_defaultPmc(@_);
}
sub address
{
	return &_defaultPmc(@_);
}
sub quot
{
	return &_defaultPmc(@_);
}

sub utter
{

	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $parentRole = shift @_;
	
	# Find the new root ('subroot') for the current subtree.
	my @res = $xpc->findnodes(
		"pml:children/pml:node[pml:role!=\'no\' and pml:role!=\'punct\' and pml:role!=\'conj\']",
		$node);
	@res = $xpc->findnodes(
		"pml:children/pml:node[pml:role!=\'punct\']", $node) unless (@res);
	die "utter below ". $node->find('../../@id').' has no children.'
		if (not @res);
	warn "utter below ". $node->find('../../@id').' has '.(scalar @res)
		.' potential rootnodes.'
		if (scalar @res ne 1);
		
	my $newRoot = $res[0];
	# Change role for node with speciffied rootRole.
	my $oldRole = &_getRole($xpc, $newRoot);
	&_setNodeRole($xpc, $newRoot, "$parentRole-utter-$oldRole");
	
	# Rebuild subtree.
	$newRoot->unbindNode();
	&_moveAllChildren($xpc, $node, $newRoot);
		
	return $newRoot;
}

### What to do when don't know what to do #####################################

sub defaultPhrase
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $parentRole = shift @_;
	my $phraseRole = &_getRole($xpc, $node);
	
	# Find the new root ('subroot') for the current subtree.
	my @basElems = $xpc->findnodes(
		'pml:children/pml:node[pml:role=\'basElem\']', $node);
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
	&_moveAllChildren($xpc, $node, $lastBasElem);
	
	return $lastBasElem;
}

###############################################################################
# Aditional functions for phrase handling
###############################################################################
# Finds child element with specified role and makes ir parent of children
# nodes.
# _moveAllNodesBelowOne (role determining node to become root, XPath context
#						 with set namespaces, DOM node, role of the parent
#						 node)
sub _allNodesBelowOne
{
	my $rootRole = shift @_;
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $parentRole = shift @_;
	my $phraseRole = &_getRole($xpc, $node);
	
	# Find node with speciffied rootRole.
	my @res = $xpc->findnodes("pml:children/pml:node[pml:role=\'$rootRole\']", $node);
	die "$phraseRole below ". $node->find('../../@id').' has '.(scalar @res)." \"$rootRole\"."
		if (scalar @res ne 1);
	my $newRoot = $res[0];

	# Change role for node with speciffied rootRole.
	&_setNodeRole($xpc, $newRoot, "$parentRole-$phraseRole-$rootRole");
	
	# Rebuild subtree.
	$newRoot->unbindNode();
	&_moveAllChildren($xpc, $node, $newRoot);

	return $newRoot;
}

# _defaultPmc (XPath context with set namespaces, DOM node, role of the parent
#			   node)
sub _defaultPmc
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $parentRole = shift @_;
	my $phraseRole = &_getRole($xpc, $node);
	
	# Find the new root ('subroot') for the current subtree.
	#my @res = $xpc->findnodes(
	#	"pml:children/pml:node[pml:role!=\'no\' and pml:role!=\'punct\' and pml:role!=\'conj\']",
	#	$node);
	my @ch = $xpc->findnodes("pml:children/pml:node[pml:role=\'conj\']", $node);
	@ch = $xpc->findnodes("pml:children/pml:node[pml:role=\'punct\']", $node)
		if (not @ch);
	if (not @ch)
	{
		@ch = $xpc->findnodes("pml:children/pml:node[pml:role!=\'no\']", $node);
		warn "$phraseRole below ". $node->find('../../@id').' has '.(scalar @ch)
			.' potential non-punct/conj/no rootnodes.'
			if (scalar @ch ne 1);
	}
	die "$phraseRole below ". $node->find('../../@id').' has no children.'
		if (not @ch);
	my @res = sort {
			${$xpc->findnodes('pml:ord', $a)}[0]->textContent
			<=>
			${$xpc->findnodes('pml:ord', $b)}[0]->textContent
		} @ch;
	#die "$phraseRole below ". $node->find('../../@id').' has no children.'
	#	if (not @res);
	#warn "$phraseRole below ". $node->find('../../@id').' has '.(scalar @res)
	#	.' potential rootnodes.'
	#	if (scalar @res ne 1);
	
	my $newRoot = $res[0];
	# Change role for node with speciffied rootRole.
	my $oldRole = &_getRole($xpc, $newRoot);
	&_setNodeRole($xpc, $newRoot, "$parentRole-$phraseRole-$oldRole");
	
	# Rebuild subtree.
	$newRoot->unbindNode();
	&_moveAllChildren($xpc, $node, $newRoot);
		
	return $newRoot;
}

# _defaultCoord (XPath context with set namespaces, DOM node, role of the parent
#				 node)
sub _defaultCoord
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $parentRole = shift @_;
	my $phraseRole = &_getRole($xpc, $node);

	my @ch = $xpc->findnodes('pml:children/pml:node', $node);
	my @sorted = sort {
			${$xpc->findnodes('pml:ord', $a)}[0]->textContent
			<=>
			${$xpc->findnodes('pml:ord', $b)}[0]->textContent
		} @ch;
	
	die "$phraseRole below ". $node->find('../../@id').' has no children.'
		if (not @sorted);
	warn "$phraseRole below ". $node->find('../../@id').' has only '
		.(scalar @sorted).' children'
		if (scalar @sorted lt 3);
	
	my ($newRoot, $prevRoot, $tmpRoot);
	my @postponed = ();
	while (@sorted) # Loop through all potential 'subroots'.
	{
		# Find next subroot.
		while (not defined $tmpRoot)
		{
			my $tmp = shift @sorted;
			# All coordination constituents have been traverset, no new
			# subroots can be found: process last postponed nodes and exit.
			if (not defined $tmp)
			{
				# Move last nodes.
				my $chNode = &_getChildrenNode($xpc, $prevRoot);
				foreach my $ch (@postponed)
				{
					$ch->unbindNode();
					$chNode->appendChild($ch);
				}
				return $newRoot;
			}
			
			my $role = &_getRole($xpc, $tmp);
			if ($role eq 'punct' or $role eq 'conj')
			{
				$tmpRoot = $tmp;
				last;
			} else
			{
				push @postponed, $tmp;
			}
		}
		my $rootRole = &_getRole($xpc, $tmpRoot);
		die "$phraseRole below ". $node->find('../../@id')." ends with $rootRole."
			if (not @sorted);
		my $nextRole = &_getRole($xpc, $sorted[0]);
		
		# Deal with comma near  conjuction.
		if ($rootRole eq 'punct' and $nextRole eq 'conj')
		{
			push @postponed, $tmpRoot;
			$tmpRoot = shift @sorted;
		}
		if ($rootRole eq 'conj' and $nextRole eq 'punct')
		{
			push @postponed, shift @ch;
		}
		
		# Rebuild tree fragment.
		$tmpRoot->unbindNode();
		my $chNode = &_getChildrenNode($xpc, $tmpRoot);
		foreach my $ch (@postponed)
		{
			$ch->unbindNode();
			$chNode->appendChild($ch);
		}

		 # Set the pointer to the prhrase root, if this was first conj/punct.
		if (not defined $prevRoot)
		{
			$newRoot = $tmpRoot;
			$rootRole = &_getRole($xpc, $tmpRoot);
			&_setNodeRole($xpc, $newRoot, "$parentRole-$phraseRole-$rootRole");
		}
	} continue
	{
		$prevRoot = $tmpRoot;
		$tmpRoot = undef;
		@postponed = ();
	}
	
	# This is imposible exit.
	return $newRoot;
}

###############################################################################
# Techsupport functions
###############################################################################

# Move children from one node to an other.
# _getChildrenNode (XPath context with set namespaces, old parent of children
#					to be moved, new parent)
# return new parent
sub _moveAllChildren
{
	my $xpc = shift @_; # XPath context
	my $oldRoot = shift @_;
	my $newRoot = shift @_;
	
	my $chNode = &_getChildrenNode($xpc, $newRoot);
	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $oldRoot))
	{
		$ch->unbindNode();
		$chNode->appendChild($ch);
	}
	return $newRoot;
}

# _getChildrenNode (XPath context with set namespaces, DOM node)
# return "children" element below given node (creates one, if there was none)
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

# Set new role to the given node.
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

# _getRole (XPath context with set namespaces, DOM "node" node, new role value)
# return node's role or phrase role
sub _getRole
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