package LvCorporaTools::GenericUtils::UIWrapper;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processDir transformFile);

use File::Path;
use IO::Dir;
use IO::File;
use XML::LibXML;  # XML handling library

# processDir(pointer to function for processing each file, regular expression
#			 filter for selecting files to process, extension/ending for names
#			 of result files (no aditional dot is added), pass output file name
#			 to file processing function (0/1), data directory, arbitrary list
#			 of other parameters to pass to file processing function)
# Function for file processing will be used according to signature (data
# directory prefix, input file (without dir name), output file (if 4th
# parameter true), other parameters)
sub processDir
{
	my $processFileFunct = shift @_;
	my $filter = shift @_;
	my $ext = shift @_;
	my $output = shift @_;
	my $dirName = shift @_;
	my @otherPrams = @_;
	my $dir = IO::Dir->new($dirName) or die "dir $!";

	while (defined(my $inFile = $dir->read))
	{
		if ((! -d "$dirName/$inFile") and ($inFile =~ /$filter/))
		{
			if ($output)
			{
				my $outFile = $inFile =~ /^(.*)\.[^.]*$/ ? "$1$ext" : "$inFile$ext";
				&$processFileFunct($dirName, $inFile, $outFile, @otherPrams);
			}
			else
			{
				&$processFileFunct($dirName, $inFile, @otherPrams);
			}
		}
	}
}
