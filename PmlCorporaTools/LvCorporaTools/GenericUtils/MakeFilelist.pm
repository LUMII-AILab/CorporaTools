package LvCorporaTools::GenericUtils::MakeFilelist;

use strict;
use warnings;
use IO::Dir;
use IO::File;

###############################################################################
# This program traverses given folder and lists all .a files into TrEd and
# PML-TQ applicable file list.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2013
# Lauma Pretkalniņa, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

sub makeFilelist
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 1)
	{
		print <<END;
Script for creating .fl filelist for given folder.

Params:
   data directory 
   filelist name without extension [opt, 'LatvianTreebank' by default]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}

	my $dirName = shift @_;
	my $listName = (shift @_ or 'LatvianTreebank');
	my $out = IO::File->new("$dirName/$listName.fl", "> :encoding(UTF-8)")
		or die "Could not create $listName.fl $!";
	
	my @todoDirs = ();
	my $current = $dirName;
	print $out "$listName\n";
	my @filelist = ();
		
	# Traverse subdirectories.
	while ($current)
	{
		my $dir = IO::Dir->new($current) or die "Can't open folder $!";
		while (defined(my $item = $dir->read))
		{
			# Treebank file
			if ((-f "$current/$item") and ($item =~ /.a$/))
			{
				my $link = "$current/$item";
				$link =~ s#^\Q$dirName\E[\\/]*##;
				push @filelist, $link;
				#print $out "$link\n";
			}
			elsif (-d "$current/$item" and $item !~ /^\.\.?$/)
			{
				push @todoDirs, "$current/$item";
			}
		}
	}
	continue
	{
		$current = shift @todoDirs;
	}
	for my $filepath (sort @filelist)
	{
		print $out "$filepath\n";
	}

	$out->close();
}

# This ensures that when module is called from shell (and only then!)
# makeFilelist is envoked.
&makeFilelist(@ARGV) unless caller;

1;