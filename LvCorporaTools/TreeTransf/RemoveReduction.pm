package LvCorporaTools::TreeTransf::RemoveReduction;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw($LABEL_EMPTY processDir transformFile transformTree);

#use Carp::Always;	# Print stack trace on die.

use File::Path;
use IO::Dir;
use IO::File;
use XML::LibXML;  # XML handling library
use LvCorporaTools::PMLUtils::AUtils
	qw(getRole setNodeRole moveChildren hasChildrenNode getChildrenNode);

###############################################################################
# This program removes reduction nodes from Latvian Treebank analytical layer
# dependency-only simplification. If reduction node has no token, it is
# removed, parent's role is augumented with "red:child", children roles is
# augumented with "red:parent". If reduction node has token, its role is
# augumented with "red:self".
# Input files are supposed to be valid against coresponding PML schemas. To
# obtain results, input files files must have all nodes numbered (TODO: fix
# this).
#
# Works with A level schema v.2.14.
# Input files - utf8.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2013
# Lauma Pretkalniņa, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

# Global variables set how to transform specific elements.
# Unknown values cause fatal error.

our $LABEL_EMPTY = 1; 	# Roles red:child and red:parent are appended when
						# empty node is removed.
#our $LABEL_EMPTY = 0; 	# Roles red:child and red:parent are not appended. 


# If single argument provided, treat it as directory and process all .a files
# in it. Otherwise pass all arguments to transformFile. This can be used as
# entry point, if this module is used standalone.
sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 1)
	{
		print <<END;
Script for batch removing reduction nodes from Latvian Treebank .a files.
Input files should be provided as UTF-8.

Params:
   data directory 

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}
	my $dir_name = $_[0];
	my $dir = IO::Dir->new($dir_name) or die "dir $!";

	while (defined(my $in_file = $dir->read))
	{
		if ((! -d "$dir_name/$in_file") and ($in_file =~ /^(.+)\.a$/))
		{
			transformFile ($dir_name, $in_file, "$1-nored.a");
		}
	}
}


# Process single XML file. This can be used as entry point, if this module
# is used standalone.
sub transformFile
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 3)
	{
		print <<END;
Script for removing reduction nodes from Latvian Treebank .a files.
Input files should be provided as UTF-8.

Params:
   directory prefix
   file name
   new file name [opt, current file name used otherwise]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $oldName = shift @_;
	my $newName = (shift @_ or $oldName);

	mkpath("$dirPrefix/res/");
	#mkpath("$dirPrefix/warnings/");
	#my $warnFile = IO::File->new("$dirPrefix/warnings/$newName-warnings.txt", ">")
	#	or die "$newName-warnings.txt: $!";
	#print "Processing $oldName started...\n";

	# Load the XML.
	my $parser = XML::LibXML->new('no_blanks' => 1);
	my $doc = $parser->parse_file("$dirPrefix/$oldName");
	
	my $xpc = XML::LibXML::XPathContext->new();
	$xpc->registerNs('pml', 'http://ufal.mff.cuni.cz/pdt/pml/');
	#$xpc->registerNs('fn', 'http://www.w3.org/2005/xpath-functions/');
	
	# Process XML: process each tree...
	foreach my $tree ($xpc->findnodes('/pml:lvadepdata/pml:trees/pml:LM', $doc))
	{
		&transformTree($xpc, $tree);#, $warnFile);
		#&recalculateOrds($xpc, $tree); #TODO
	}
		
	# Print the XML.
	#File::Path::mkpath("$dirPrefix/res/");
	my $outFile = IO::File->new("$dirPrefix/res/$newName", ">")
		or die "Output file opening: $!";	
	print $outFile $doc->toString(1);
	$outFile->close();
	
	print "Processing $oldName finished!\n";
	#print $warnFile "Processing $oldName finished!\n";
	#$warnFile->close();
}


# Remove reductions from single tee (LM element in most tree files).
# transformTree (XPath context with set namespaces, DOM node for tree root)
sub transformTree
{
	my $xpc = shift @_; # XPath context
	my $tree = shift @_;
	#my $warnFile = shift @_;
	
	my @reds = $xpc->findnodes('//pml:node[./pml:reduction]', $tree);
	
	for my $red (@reds)
	{
		my @mRefs = $xpc->findnodes('pml:m.rf', $red);
		if (@mRefs > 0)	# Node has token.
		{
			my $role = getRole($xpc, $red);
			setNodeRole($xpc, $red, "$role-red:self");
			my @redTags = $xpc->findnodes('pml:reduction', $red);
			for my $redTag (@redTags)
			{
				$redTag->unbindNode();
			}
			
		}
		else		# Node doesn't have token.
		{
			
			#die "Unknown value \'$LABEL_EMPTY\' for global constant \$LABEL_EMPTY "
			#	if ($LABEL_EMPTY ne 'YES' and $LABEL_EMPTY ne 'NO');
			my $pmlParent = $red->parentNode->parentNode; #pml parent <-> children <-> this
			if ($pmlParent->nodeName() ne 'LM') # Parent is not the root of the tree.
			{
				my $role = getRole($xpc, $pmlParent);
				setNodeRole($xpc, $pmlParent, "$role-red:child")
					unless ($role =~ /-red:child$/ or not $LABEL_EMPTY);
			}
			
			# Node have children.
			if (hasChildrenNode($xpc, $red))
			{
				my $children = getChildrenNode($xpc, $red);
				for my $ch ($children->childNodes)
				{
					my $chRole = getRole($xpc, $ch);
					setNodeRole($xpc, $ch, "$chRole-red:parent")
						if ($LABEL_EMPTY);
				}
				moveChildren ($xpc, $red, $pmlParent);
			}
			$red->unbindNode();
		}
	}
}

1;
