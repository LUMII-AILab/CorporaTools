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
our @EXPORT_OK = qw(loadXml printXml @DEFAULT_FORCEARRAY_W @DEFAULT_FORCEARRAY_M @DEFAULT_FORCEARRAY_A @DEFAULT_ID);

use Data::Dumper;
use XML::Simple;  # XML handling library
use IO::File;

our @DEFAULT_FORCEARRAY_W = qw(para w schema title source author authorgender published genre keywords msc);
our @DEFAULT_FORCEARRAY_M = qw(s m reffile schema LM);
our @DEFAULT_FORCEARRAY_A = qw(node LM reffile schema);
our @DEFAULT_IDS = qw(id);

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
	my $xmlString = $xmlHandler->XMLout($data,
		'KeyAttr' => [],
		'AttrIndent' => 1,
		'RootName' => $root,
#		'OutputFile' => $out,
		'SuppressEmpty' => 1,
		'NoSort' => 1,
		'XMLDecl' => $header,
#		'NoEscape' => 1,
#		'GroupTags' => {},
		);
	# Normalization for TrEd:
	# for A files
	$xmlString =~ s#(<trees[ >].*</trees>)(\s*)(<meta>.*</meta>)#$3$2$1#s;
	# for M files
	$xmlString =~ s#(<s[ >].*</s>)(\s*)(<meta>.*</meta>)#$3$2$1#s;
	# for W files
	$xmlString =~ s#(<doc[ >].*</doc>)(\s*)(<meta>.*</meta>)#$3$2$1#s;
	print $out $xmlString;
	$out->close();
}
1;