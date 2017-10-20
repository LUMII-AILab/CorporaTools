#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::GenericUtils::SimpleSentBreaker;

use warnings;
use utf8;
use strict;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(simpleSentBreaker);


###############################################################################
# This program performs simple sentence breaking. This is not a valid Latvian
# tokenizer/sentence braker!
#
# Inpur parameters: data dir, otput dir, [encoding].
#
# Developed on ActivePerl 5.10.1.1007, tested on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2011-2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
sub simpleSentBreaker
{
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for performing simple sentence breaking. This is not a valid Latvian
tokenizer/sentence braker! Input and output directory must be different,
otherwise input file can be overwriten.

Params:
   input directory
   output directory (UTF-8 always)
   input data encoding [opt, UTF-8 used by default]

Latvian Treebank project, LUMII, 2011-2017, provided under GPL
END
		exit 1;
	}
	
	# Input paramaters.
	my $corpus = shift; #'dati';
	my $out_dir = shift; #'teik';
	my $encoding = (shift or 'UTF-8');
	opendir(DIR, $corpus) or die "Input directory error: $!";
	mkdir $out_dir;
	while (defined(my $file = readdir(DIR))) {
		# do something with "$dirname/$file"
		if (! -d "$corpus\\$file")
		{
			open INPUT, "<:encoding($encoding)", "$corpus\\$file"
				or warn "Input file error $file: $!";
			#open INPUT, "<:encoding(windows-1257)", "$corpus\\$file" or warn "i-fails $file: $!";
			#open INPUT, "<:utf8", "$corpus\\$file" or warn "i-fails $file: $!";
			open OUTPUT, ">:encoding(UTF-8)", "$out_dir\\$file" or warn "Output file error: $!";
			while (<INPUT>)	{
				s#(\D[.!?]["'\x{201D}\x{00AB}]?)(?!\s+["'\x{201E}\x{00BB}]?[a-zāčēģīķļņōŗšūž])\s#$1\n#gx; 
				#s#([;,:])\s*\n$#$1 #g;
				#s#([A-ZĀČĒĢĪĶĻŅōŖŠŪŽ][a-zāčēģīķļņōŗšūž]?\.)\s*\n#$1 #g;
				s#([;,:]|[A-ZĀČĒĢĪĶĻŅōŖŠŪŽ][a-zāčēģīķļņōŗšūž]?\.)\s*\n#$1 #g;
				s#[ \t]{2,}# #g;
				s#\n+#\n#g;
				if ($_ and ! m#^(\s)*$#)
				{
					print OUTPUT $_;
				}
			}
			close INPUT;
			close OUTPUT;
		}
	}
	closedir(DIR);
}

1;