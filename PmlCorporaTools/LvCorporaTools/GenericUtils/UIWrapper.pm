package LvCorporaTools::GenericUtils::UIWrapper;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processDir transformAFile);

use File::Path;
use IO::Dir;
use IO::File;
use XML::LibXML;  # XML handling library

# processDir(pointer to function for processing each file, regular expression
#			 filter for selecting files to process, extension/ending for names
#			 of result files (no aditional dot is added), pass output file name
#			 to file processing function (0/1), strip extension from input file
#            name (0/1), data directory, arbitrary list of other parameters to
#			 pass to file processing function)
# Function for file processing will be used according to signature (data
# directory prefix, input file (without dir name), output file (if 4th
# parameter true), other parameters)
# Returns number of files ending with die or warn.
sub processDir
{
	my $processFileFunct = shift @_;
	my $filter = shift @_;
	my $ext = shift @_;
	my $output = shift @_;
	my $noInExt = shift @_;
	my $dirName = shift @_;
	my @otherPrams = @_;
	my $dir = IO::Dir->new($dirName) or die "dir $!";

	my $baddies = 0;

	while (defined(my $inFile = $dir->read))
	{
		if ((-f "$dirName/$inFile") and ($inFile =~ /$filter/))
		{
			my $coreName = $inFile =~ /^(.*)\.[^.]*$/ ? $1 : $inFile;
			$inFile = $coreName if ($noInExt);
			if ($output)
			{
				my $outFile = "$coreName$ext";
				eval
				{
					#local $SIG{__WARN__} = sub { die $_[0] }; # This magic makes eval act as if all warnings were fatal.
					local $SIG{__WARN__} = sub { $baddies++; warn $_[0] }; # This magic makes eval count warnings.
					&$processFileFunct($dirName, $inFile, $outFile, @otherPrams);
				};
			}
			else
			{
				eval
				{
					#local $SIG{__WARN__} = sub { die $_[0] }; # This magic makes eval act as if all warnings were fatal.
					local $SIG{__WARN__} = sub { $baddies++; warn $_[0] }; # This magic makes eval count warnings.
					&$processFileFunct($dirName, $inFile, @otherPrams);
				};
			}
			if ($@)
			{
				$baddies++;
				print $@;
			}

		}
	}
	return $baddies;
}

# transformAFile(pointer to function for transforming each tree, create file
#				 for warnings and pass it to tree transforming function (0/1),
#				 print notification about file begining (0/1), new schema name
#				 (if empty, schema name left unchanged), new root element name
#				 (if empty, root element name left unchanged), directory
#				 prefix - this is where everything goes on, name of  file to
#				 process, name of output file (optional, input file name used
#				 name used otherwise))
# Function for tree processing will be used according to signature (XPath
# context with set namespaces, DOM node for tree root - /*/tree/LM/, warning
# file (if 2nd parameter true))
sub transformAFile
{
	# Input paramaters.
	my $processTreeFunct = shift @_;
	my $warns = shift @_;
	my $notifyBegin = shift @_;
	my $newSchema = shift @_;
	my $newRoot = shift @_;
	my $dirPrefix = shift @_;
	my $oldName = shift @_;
	my $newName = (shift @_ or $oldName);

	mkpath("$dirPrefix/res/");
	#File::Path::mkpath("$dirPrefix/res/");
	my $warnFile;
	if ($warns)
	{
		mkpath("$dirPrefix/warnings/");
		$warnFile = IO::File->new("$dirPrefix/warnings/$newName-warnings.txt", ">")
			or die "$newName-warnings.txt: $!";
	}
	print "Processing $oldName started...\n" if ($notifyBegin);

	# Load the XML.
	my $parser = XML::LibXML->new('no_blanks' => 1);
	my $doc = $parser->parse_file("$dirPrefix/$oldName");
	
	my $xpc = XML::LibXML::XPathContext->new();
	$xpc->registerNs('pml', 'http://ufal.mff.cuni.cz/pdt/pml/');
	$xpc->registerNs('fn', 'http://www.w3.org/2005/xpath-functions/');
	
	# Process XML: process each tree...
	foreach my $tree ($xpc->findnodes('/*/pml:trees/pml:LM', $doc))
	{
		$warns ?
			&$processTreeFunct($xpc, $tree, $warnFile):
			&$processTreeFunct($xpc, $tree);
	}
	
	# ... and update the schema information and root name.
	if ($newSchema)
	{
		my @schemas = $xpc->findnodes(
			'pml:lvadata/pml:head/pml:schema', $doc);
		$_->setAttribute('href', $newSchema) for @schemas; #There should be only one.
	}
	
	if ($newRoot)
	{
		$doc->documentElement->setNodeName($newRoot);
	}
	
	# Print the XML.
	my $outFile = IO::File->new("$dirPrefix/res/$newName", ">")
		or die "Output file opening: $!";	
	print $outFile $doc->toString(1);
	$outFile->close();
	
	print "Processing $oldName finished!\n";
	if ($warns)
	{
		print $warnFile "Processing $oldName finished!\n";
		$warnFile->close();
	}
}


