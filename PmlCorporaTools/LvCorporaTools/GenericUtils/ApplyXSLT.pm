#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::GenericUtils::ApplyXSLT;

#use utf8;
use strict;
use warnings;

use File::Path;
use IO::Dir;
use XML::LibXSLT;
use XML::LibXML;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(applyXSLT1_0 transformDir);

sub transformDir
{
	print "Usage: data_dir xslt_transform_file output_dir [.input_extension] [.output_extension]\n"
		if (@_ < 3 or @) > 5);
	my $inputDir = shift @_;
	my $transf = shift @_;
	my $output = shift @_;
	my $inputExt = (shift @_ or '');
	my $outputExt = (shift @_ or '');
	my $dir = IO::Dir->new($inputDir) or die "dir $!";
	mkpath($output);

	my $baddies = 0;
	while (defined(my $inFile = $dir->read))
	{
		if ((-f "$inputDir/$inFile") and ($inFile =~ /^(.*?)$inputExt$/))
		{
			local $SIG{__WARN__} = sub { $baddies++; warn $_[0] }; # This magic makes eval count warnings.
			applyXSLT1_0("$inputDir/$inFile", $transf, "$output/$1$outputExt");
		}
	}
	print "$baddies failed.\n" if ($baddies);
}

sub applyXSLT1_0
{

	print "Usage: xml_data_file xslt_transform_file output_file\n"
		if (@_ != 3);

	my ($input, $transf, $output) = @_;
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
