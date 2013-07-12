package LvCorporaTools::PMLUtils::Knit;

use strict;
use warnings;

#use Carp::Always;	# Print stack trace on die.

use File::Path;
use IO::Dir;
use Treex::PML;
use Treex::PML::Instance;
use Treex::PML::Instance::Writer;
use LvCorporaTools::GenericUtils::UIWrapper;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(knit processDir);

###############################################################################
# Script for knitting in arbitrary PML files - the same as in pmltk 1.1.5, but
# without calling depracated fslib.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2013
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
	
# Knit-in all the referenced data into provided PML file, print out with
# different file name. This can be used as entry point, if this module
# is used standalone.
sub knit
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for knitting arbitrary PML file.

Params:
   directory prefix
   input file name
   output file name
   directory with PML schema/-s are [opt]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}

	my $dirPrefix = shift @_;
	my $inFile = shift @_;
	my $outFile = shift @_;
	my $schemaDir = shift @_;
	
	mkpath("$dirPrefix/res/");
	Treex::PML::AddResourcePath( $schemaDir ) if ($schemaDir);
	my $pml = Treex::PML::Instance->load({ 'filename' => "$dirPrefix/$inFile" });
	$Treex::PML::Instance::Writer::KEEP_KNIT = 1;
	$pml->save({ 'filename' => "$dirPrefix/res/$outFile", 'refs_save' => {}});
	
	#$inFile =~ m#^.*[\\/](.*?)$#;
	print "Processing $inFile finished!\n";

}

# Knit-in all files with given extension in given directory. This can be used
# as entry point, if this module is used standalone.
sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for batch knitting arbitrary PML files.

Params:
   data directory 
   file extension (only these files will be processed) 
   directory with PML schema/-s are [opt]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}
	
	my $dirName = shift @_;
	my $ext = shift @_;
	my $schemaDir = (shift @_ or 0);
	LvCorporaTools::GenericUtils::UIWrapper::processDir(
		\&knit, "^.+\\.\Q$ext\E\$", '.pml', 1, 0, $dirName, $schemaDir);
}

