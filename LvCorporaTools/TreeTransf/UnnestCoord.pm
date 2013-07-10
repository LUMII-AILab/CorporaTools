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
use LvCorporaTools::PMLUtils::AUtils qw(getRole sortNodesByOrd getChildrenNode);

###############################################################################
# This program transforms multi-level coordination constructions into single
# level. This ir done only when levels are of the same type. Constructions with
# generalizing word are not transformed.
# Input files are supposed to be valid against coresponding PML schemas. To
# obtain results, input files files must have all nodes numbered.
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
			transformFile ($dir_name, $in_file, "$1-flatCoord.a");
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
Script for flattening multi-level coordinations in Latvian Treebank .a files.
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
	$xpc->registerNs('fn', 'http://www.w3.org/2005/xpath-functions/');
	
	# Process XML: process each tree...
	foreach my $tree ($xpc->findnodes('/pml:lvadata/pml:trees/pml:LM', $doc))
	{
		&transformTree($xpc, $tree);#, $warnFile);
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
	
	my $xPath = './/pml:coordinfo[./pml:coordtype=\'crdClauses\' and '.
		'./pml:children/pml:node/pml:children/pml:coordinfo/pml:coordtype=\'crdClauses\''.
		' or ./pml:coordtype=\'crdParts\' and '.
		'./pml:children/pml:node/pml:children/pml:coordinfo/pml:coordtype=\'crdParts\']';
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
				'pml:children/pml:coordinfo/pml:children/pml:node', $node);
			die "$parRole below ". $node->find('@id').' has no children!'
				unless (@constituents);
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