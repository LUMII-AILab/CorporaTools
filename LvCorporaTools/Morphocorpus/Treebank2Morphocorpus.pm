package LvCorporaTools::Morphocorpus::Treebank2Morphocorpus;

use strict;
use warnings;

use File::Copy;
use File::Path;
use IO::Dir;
use IO::File;

###############################################################################
# Collects the treebank morphology files and updates the morphocorpus repository.
# See documentation in &_printMan().
#
# Works with PML tree files, A level schema v.2.14.
# Input files - utf8.
#
# Peteris Paikens, LUMII, AILab, peteris@ailab.lv
# Licenced under GPL.
###############################################################################
sub _printMan {
	print <<END;
Collects the treebank morphology files and updates the morphocorpus repository.
Input files should be provided as UTF-8.

Usage
	...

Params:
  General
    --source	input directory, location of the treebank Corpora subfolder
    --target	target directory, location of morphocorpus repository Corpora subfolder. 
    
  NB! Current contents of target folder will be removed.

LUMII, 2013, provided under GPL
END
}

sub doStuff {
	autoflush STDOUT 1;
	if (not @_ or @_ < 3)
	{
		&_printMan;
		# exit 1;
	}
	my @flags = @_;

	# Parse parameters.
	my %params = ();
	my $lastFlag;
	for my $f (@flags)
	{
		if ($f =~ /^--/)
		{
			$lastFlag = $f;
			$params{$f} = [];
		} elsif (defined $lastFlag)
		{
			$params{$lastFlag} = [@{$params{$lastFlag}}, $f];
		} else
		{
			&_printMan;
			exit 1;
		}
	}

	# Get directories.
	my $source = '/Users/pet/Documents/Treebank/Corpora';
	$source = $params{'--source'}[0] if ($params{'--source'});
	my $target = '/Users/pet/Documents/Morfotageris/Morphocorpus/Corpora';
	$target = $params{'--target'}[0] if ($params{'--target'});
	$target = "$target/Treebank";

	if (-d $target or -e $target) {
    	print "\n==== Removing $target\n";
    	rmtree([ $target ]);
	}

	print "\n==== Moving files from $source to $target\n";

	&_collect($source, $target);

	print "\n==== Successful finish =======================================\n";
}

# Collect data recursively.
# return adress to step results.
# $source -> which folder to collect
# $dirPrefix -> where to put results
sub _collect
{
	my ($source, $dirPrefix) = @_;
	print "\n==== Recursive data collecting ===========================\n";
		
	my $fileCounter = 0;
	my @todoDirs = ();
	my $current = $source;
	mkpath ("$dirPrefix");
		
	# Traverse subdirectories.
	while ($current)
	{
		my $dir = IO::Dir->new($current) or die "Can't open folder $!";
		while (defined(my $item = $dir->read))
		{
			# Treebank file
			if ((-f "$current/$item") and ($item =~ /.[mw]$/))
			{
				copy("$current/$item", "$dirPrefix/");
				$fileCounter++;
			}
			elsif (-d "$current/$item" and $item !~ /^\.\.?$/)
			{
				# If copy source and dest is the same, result under Unix is empty file.
				push @todoDirs, "$current/$item";
			}
		}
	}
	continue
	{
		$current = shift @todoDirs;
	}
		
	print "Found $fileCounter files.\n";
}

# This ensures that when module is called from shell (and only then!)
# processDir is envoked.
&doStuff(@ARGV) unless caller;