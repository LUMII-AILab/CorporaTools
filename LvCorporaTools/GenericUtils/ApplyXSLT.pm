#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::GenericUtils::ApplyXSLT;

#use utf8;
use strict;

sub applyXSLT1_0
{
	use XML::LibXSLT;
	use XML::LibXML;

	print "Usage: xml_data_file xslt_transform_file output_file\n"
		if (@ARGV != 3);

	my ($input, $transf, $output) = @ARGV;  
	my $parser = XML::LibXML->new();
	my $xslt = XML::LibXSLT->new();
	  
	my $source = $parser->parse_file($input);
	my $style_doc = $parser->parse_file($transf);
	  
	my $stylesheet = $xslt->parse_stylesheet($style_doc);
	  
	my $results = $stylesheet->transform($source);

	open OUT, ">:utf8", "$output"
				or die "Error with output file $output: $!";

	print OUT $stylesheet->output_string($results);
}

1;
