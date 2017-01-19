#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::GenericUtils::SimpleXmlIo;
###############################################################################
# This module contains most basic helper functions for reading and writing
# lv-PML files with XML::Simple.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2012
# Lauma Pretkalniņa, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(loadXml printXml);

#use Data::Dumper;
use XML::Simple;  # XML handling library
use IO::File;

# loadXml (file name with extension and everything, reference to array
#          containing 'ForceArray' options for XML::Simple, [reference to
#		   array containing 'KeyAttr' options for XML::Simple, empty used
#		   as default])
# returns hash refernece:
#		'xml' => loaded XML data structure, 'header' => XML header,
#       'handler' => XML::Simple object
sub loadXml
{
	my $filename = shift;
	my $forceArrayOpts = shift;
	my $keyAttrOpts = (shift or []);
	
	my $in = IO::File->new($filename, "< :encoding(UTF-8)")
		or die "Could not open file $filename: $!";
	my $xmlString = join '', <$in>;
	$xmlString =~ /^\s*(\Q<?\E.*?\Q?>\E)/;
	my $header = $1;
	my $sxml = XML::Simple->new();
	my $data = $sxml->XMLin(
		$xmlString,
		'KeyAttr' => $keyAttrOpts,
		'ForceContent' => 1,
		'ForceArray' => $forceArrayOpts,
#		'GroupTags' => {},
		);
	$in->close();
	
	return {'xml' => $data, 'header' => $header, 'handler' => $sxml};	
}

# printXml (file name with extension and everything, XML::Simple object to
#          handle data convertation, data object, name of the root element,
#          header)
sub printXml
{
	my $filename = shift;
	my $xmlHandler = shift;
	my $data = shift;
	my $root = shift;
	my $header = shift;
	
	my $out = IO::File->new($filename, "> :encoding(UTF-8)")
		or die "Could not create file $filename: $!";
	$xmlHandler->XMLout($data,
		'KeyAttr' => [],
		'AttrIndent' => 1,
		'RootName' => $root,
		'OutputFile' => $out,
		'SuppressEmpty' => 1,
		'NoSort' => 1,
		'XMLDecl' => $header,
#		'NoEscape' => 1,
#		'GroupTags' => {},
		);
	$out->close();
}
1;