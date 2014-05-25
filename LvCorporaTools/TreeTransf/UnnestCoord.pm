package LvCorporaTools::TreeTransf::UnnestCoord;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processDir transformFile transformTree);

#use Carp::Always;	# Print stack trace on die.

use File::Path;
use IO::Dir;
use IO::File;
use XML::LibXML;  # XML handling library
use LvCorporaTools::PMLUtils::AUtils qw(
	getRole sortNodesByOrd getChildrenNode renumberNodes);
use LvCorporaTools::GenericUtils::UIWrapper;

###############################################################################
# This program transforms multi-level coordination constructions into single
# level. This ir done only when levels are of the same type. Constructions with
# generalizing word are not transformed.
# Input files are supposed to be valid against coresponding PML schemas. 
#
# Works with A level schema v.2.14.
# Input files - utf8.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2013
# Lauma Pretkalniņa, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

#TODO iznest redundantaas funkcijas atsevishkjaa wraperii.

# Process all .a files in given folder. This can be used as entry point, if
# this module is used standalone.
sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 1)
	{
		print <<END;
Script for batch flattening multi-level coordinations in Latvian Treebank .a
files.
Input files should be provided as UTF-8.

Params:
   data directory 
   input data have all nodes ordered [opt, 0/1, 0 (no) assumed by default]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}
	
	LvCorporaTools::GenericUtils::UIWrapper::processDir(
		\&transformFile, "^.+\\.a\$", '-flatCoord.a', 1, 0, @_);
}

# Process single XML file. This can be used as entry point, if this module
# is used standalone.
sub transformFile
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 3)
	{
		print <<END;
Script for flattening multi-level coordinations in Latvian Treebank .a files.
Input files should be provided as UTF-8.

Params:
   directory prefix
   file name
   new file name [opt, current file name used otherwise]
   input data have all nodes ordered [opt, 0/1, 0 (no) assumed by default]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $oldName = shift @_;
	my $newName = (shift @_ or $oldName);
	my $numberedNodes = (shift @_ or 0);

	# This is how each tree sould be processed.
	my $treeProc = sub {
		renumberNodes(@_) unless ($numberedNodes);
		&transformTree(@_);
	};
	
	# File procesing wrapper customised with tree processing instructions.
	LvCorporaTools::GenericUtils::UIWrapper::transformAFile(
		$treeProc, 1, 0, '', '', $dirPrefix, $oldName, $newName);
}

# Remove reductions from single tee (LM element in most tree files).
# transformTree (XPath context with set namespaces, DOM node for tree root)
sub transformTree
{
	my $xpc = shift @_; # XPath context
	my $tree = shift @_;
	my $warnFile = shift @_;
	
	my $xPath = './/pml:coordinfo[./pml:coordtype=\'crdClauses\' and '.
		'./pml:children/pml:node[./pml:role=\'crdPart\''.
		' and ./pml:children/pml:coordinfo/pml:coordtype=\'crdClauses\']'.
		' or ./pml:coordtype=\'crdParts\' and '.
		'./pml:children/pml:node[./pml:role=\'crdPart\''.
		' and ./pml:children/pml:coordinfo/pml:coordtype=\'crdParts\']]';
	my @coords = $xpc->findnodes($xPath, $tree);
	while (@coords)
	{
		my $parent = $coords[0];
		my $parRole = getRole($xpc, $parent);
		my @lowerCrdNode = $xpc->findnodes(
			"pml:children/pml:node[./pml:children/pml:coordinfo/pml:coordtype=\'$parRole\']",
			$parent);
		
		my $parentCh = getChildrenNode($xpc, $parent);
		# Process each node that should be "unnested". More than one is rare.
		for my $node (@lowerCrdNode)
		{
			my @dependants = $xpc->findnodes('pml:children/pml:node', $node);
			my @constituents = $xpc->findnodes(
				'pml:children/pml:coordinfo/pml:children/pml:node[./pml:role=\'crdPart\']',
				$node);
				
			# Warning about suspective structure.
			if (not @constituents)
			{
				print "$parRole has no crdPart children.\n";
				print $warnFile "$parRole below ". $node->find('@id')
					." has no children.\n";
				
				@constituents = $xpc->findnodes(
					'pml:children/pml:coordinfo/pml:children/pml:node',
					$node);
				die "$parRole below ". $node->find('@id').' has no children!'
					unless (@constituents);
			}
				
			@constituents = @{sortNodesByOrd($xpc, 0, @constituents)};
			
			# Dependants of the lower coordination is transfered to first
			# constituent of that coordination.
			my $const0Ch = getChildrenNode($xpc, $constituents[0]);
			for my $d (@dependants)
			{
				$d->unbindNode();
				$const0Ch->appendChild($d);
			}
			
			# Dependants of the lower coordination is transfered to the upper
			# coordination.
			@constituents = $xpc->findnodes(
				'pml:children/pml:coordinfo/pml:children/pml:node', $node);
			for my $c (@constituents)
			{
				$c->unbindNode();
				$parentCh->appendChild($c);
			}
			$node->unbindNode();
		}
	}
	continue
	{
		# This ensures that 3+ level cascades of coordinations are handled
		# correctly. It can turn out to be slow.
		@coords = $xpc->findnodes($xPath, $tree);
	}
}

1;