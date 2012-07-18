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
sub transform
{

	autoflush STDOUT 1;
	autoflush STDERR 1;
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

Latvian Treebank project, LUMII, 2011, provided under GPL
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
	
	my $xpc = XML::LibXML::XPathContext->new;
	$xpc->registerNs('pml', 'http://ufal.mff.cuni.cz/pdt/pml/');
	$xpc->registerNs('fn', 'http://www.w3.org/2005/xpath-functions/');
	
	#Process XML.
	foreach my $tree ($xpc->findnodes('/pml:lvadata/pml:trees/pml:LM', $doc))
	{
		#Now we should process each tree.
		
		my $role = 'ROOT';
		#my @childrenWraps = $tree->findnodes('children');
		foreach my $childrenWrap ($xpc->findnodes('pml:children', $tree))
		{
			my @phrases = $xpc->findnodes('pml:children/*[local-name()!=\'node\']', $tree);
			#my @children = $childrenWrap->nonBlankChildNodes;
			die ($tree->find('..@id'))." has ".(scalar @phrases)." non-node children for the root."
				if (scalar @phrases ne 1);
			my $roleTag = $phrases[0]->nodeName();
			$roleTag =~ s/info$/type/;
			my $chRole = ${$xpc->findnodes("pml:$roleTag", $phrases[0])}[0]->textContent;
			my $newNode = &{\&{$chRole}}($xpc, $phrases[0], $role);
#			no strict 'refs';
#			my $newNode = &$chRole($phrases[0], $role);
#			use strict 'refs';
			$newNode->unbindNode(); # for safety reasons;
			
			# Create or find "children" element.
			my $newNodeChWrap = &getChildrenNode($xpc, $newNode);	
			# Add children.
			foreach my $ch ($xpc->findnodes('pml:children/pml:node', $tree))
			{
				dfsProcessing($xpc, $ch);
				$ch->unbindNode();
				#print "child ".$ch->textContent."\n";
				#print "parent ".$newNodeChWrap->textContent."\n";
				$newNodeChWrap->appendChild($ch);
			}
			
			# Add reformed subtree to the main tree.
			$phrases[0]->replaceNode($newNode);
		}
	}
	
	# Print the XML.
	File::Path::mkpath("$dirPrefix/res/");
	my $outFile = IO::File->new("$dirPrefix/res/$newName", ">")
		or die "Output file opening: $!";	
	print $outFile $doc->toString;	
}

sub dfsProcessing
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $role = ${$xpc->findnodes('pml:role', $node)}[0]->textContent;
	
	# Process dependency children first.
	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $node))
	{
		dfsProcessing($xpc, $ch);
	}
	
	# Find phrase nodes.
	my @phrases = $xpc->findnodes('pml:children/*[local-name()!=\'node\']', $node);
	# A bit structure checking: only one phrase per regular node is allowed.
	die (($node->find('@id'))." has ".(scalar @phrases)." non-node children.")
		if (scalar @phrases gt 1);
	
	# If there is no phrase children, finish.
	return if (scalar @phrases le 1);
	print "moo\n";
	
	# Process phrase constituents.
	foreach my $ch ($xpc->findnodes('pml:children/pml:node', $phrases[0]))
	{
		dfsProcessing($xpc, $ch);
	}
	
	# A bit structure checking: phrase can't have another phrase as direct child.
	my @phrasePhrases = $xpc->findnodes('pml:children/*[local-name()!=\'node\']', $phrases[0]);
	die (($node->find('@id'))." has illegal phrase cascade as child.")
		if (scalar @phrasePhrases gt 0);
	
	# Process phrase root.
	my $phRole = ${$xpc->findnodes('pml:role', $phrases[0])}[0]->textContent;
	print "phRole $phRole\n";
	my $newNode = &{\&{$phRole}}($xpc, $phrases[0], $role);
	$node->replaceNode($newNode);
}



###############################################################################
# Phrase specific functions
###############################################################################

sub xPrep
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $parentRole = shift @_;
	print "xPrep\n";
	
	# Preprocessing.
	my @childrenWraps = $xpc->findnodes('pml:children', $node);
	die "xPrep below ".$node->find('..@id')." has ".(scalar @childrenWraps)." \"children\"."
		if (scalar @childrenWraps ne 1);
	my @children = $childrenWraps[0]->nonBlankChildNodes;
	print "xPrep has ".scalar(@children)." children.\n";
	
	# Find basElem.
	my @basElems = $xpc->findnodes('pml:children/pml:node[pml:role=\'basElem\']', $node);
	die "xPrep below ". $node->find('..@id')." has ".(scalar @basElems)." \"basElem\"."
		if (scalar @basElems ne 1);
	my $basElem = $basElems[0];

	# Change role for basElem.
	my $newRole = XML::LibXML::Element->new('role');
	$newRole->appendTextNode("$parentRole-xPrep-basElem");
	$basElem->replaceChild($newRole, $xpc->find('pml:role', $basElem));
	$basElem->unbindNode();
	
	# Rebuild subtree.
	@childrenWraps = $xpc->find('pml:children', $node);
	die "xPrep below ".$node->find('..@id')." has ".(scalar @childrenWraps)." \"children\"."
		if (scalar @childrenWraps ne 1);
	my $basElemChNode = &getChildrenNode($xpc, $basElem);
	foreach my $ch ($childrenWraps[0]->nonBlankChildNodes)
	{
		# Change role.
		my $oldChRole = ${$xpc->findnodes('pml:role', $ch)}[0]->textContent;
		my $newChRole = XML::LibXML::Element->new('role');
		$newChRole->appendTextNode("$parentRole-xPrep-$oldChRole");
		$ch->replaceChild($newChRole, ${$xpc->findnodes('pml:role', $basElem)}[0]);
		
		# Change structure.
		$ch->unbindNode();
		$basElemChNode->appendChild($ch);
	}

	#$basElem->unbindNode();	
	return $basElem;
}

sub defaultPhrase
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my $parentRole = shift @_;
	
	# Preprocessing.
	my $roleTag = $node->nodeName();
	$roleTag =~ s/info$/type/;
	my $phraseRole = ${$xpc->findnodes("pml:$roleTag", $node)}[0]->textContent;
	my @childrenWraps = $xpc->findnodes('pml:children', $node);
	die "$phraseRole below ".$node->find('..@id')." has ".(scalar @childrenWraps)." \"children\"."
		if (scalar @childrenWraps ne 1);
	my @children = $childrenWraps[0]->nonBlankChildNodes;
	
	# Find the new root ('subroot') for the current subtree.
	my @basElems = $xpc->findnodes('pml:children/pml:node[pml:role=\'basElem\']', $node);
	my $lastBasElem = undef;
	my $curentPosition = -1;
	foreach my $ch (@basElems)
	{
		my $tmpOrd = ${$xpc->findnodes('pml:ord', $ch)}[0]->textContent;
		if ($tmpOrd ge $curentPosition)
		{
			$lastBasElem = $ch;
			$curentPosition = $tmpOrd;
		}
	}
	$lastBasElem = $children[-1] unless (defined $lastBasElem);

	# Change role for the subroot.
	my $oldRole = ${$xpc->find('pml:role', $lastBasElem)}[0]->textContent;
	my $newRole = XML::LibXML::Element->new('role');
	$newRole->appendTextNode("$parentRole-$phraseRole-$oldRole");
	$lastBasElem->replaceChild($newRole, ${$xpc->findnodes('pml:role', $lastBasElem)}[0]);
	
	# Rebuild subtree.
	my $lastBasElemChNode = &getChildrenNode($xpc, $lastBasElem);
	$lastBasElem->unbindNode();
	@childrenWraps = $xpc->findnodes('pml:children', $node);
	die "$phraseRole below ".$node->find('..@id')." has ".(scalar @childrenWraps)." \"children\"."
		if (scalar @childrenWraps ne 1);
	foreach my $ch ($childrenWraps[0]->nonBlankChildNodes)
	{
		# Change role.
		my $oldChRole = ${$xpc->findnodes('pml:role', $ch)}[0]->textContent;
		my $newChRole = XML::LibXML::Element->new('role');
		$newChRole->appendTextNode("$parentRole-$phraseRole-$oldChRole");
		my @roles = $xpc->findnodes('pml:role', $ch);
		$ch->replaceChild($newChRole, $roles[0]);
		
		# Change structure.
		$ch->unbindNode();
		$lastBasElemChNode->appendChild($ch);
	}
	
	#$lastBasElem->unbindNode();
	return $lastBasElem;
}

###############################################################################
# Techsupport functions
###############################################################################

sub getChildrenNode
{
	my $xpc = shift @_; # XPath context
	my $node = shift @_;
	my @bestTry = $xpc->findnodes('pml:children', $node);
	if (scalar @bestTry ge 1)
	{
		return $bestTry[0];
	}
	else
	{
		my $childrenNode = XML::LibXML::Element->new('children');
		$node->appendChild($childrenNode);
		return $childrenNode;
	}
}


sub AUTOLOAD
{
	return &defaultPhrase;
}
1;