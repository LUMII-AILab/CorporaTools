package LvCorporaTools::ParserEval;

use strict;
use warnings;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(evaluateFile);

our $SIMPLE_STATS = 1;	# Calculate only UAS, LAS, LA (require less memory).
#our $SIMPLE_STATS = 0;	# Calculate only UAS, LAS, LA, confusion matrix, etc.


sub evaluateFile
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 3)
	{
		print <<END;
Script for evaluating syntax parser output file in conll 2007 format regarding
to golden file. Only ROLE and HEAD columns are compared.
NB: If golden file has empty ('_') ROLE and HEAD fields for a token, this token
    is excluded from scoring.
	
Global variables:
   SIMPLE_STATS - If true, calculate only UAS, LAS, LA (require less memory).
                  Calculate all stats otherwise.

Params:
   data directory
   golden file
   system result file to evaluate
   output file [optional]

Latvian Treebank project, LUMII, 2013-now, provided under GPL
END
		exit 1;
	}
	my $dirName = shift @_;
	my $goldenConll = shift @_;
	my $systemConll = shift @_;
	my $outputFile = (shift @_ or "$systemConll.score");
	
	my $goldenIn = IO::File->new("$dirName/$goldenConll", "< :encoding(UTF-8)")
		or die "Could not open file $dirName/$goldenConll: $!";
	my $systemIn = IO::File->new("$dirName/$systemConll", "< :encoding(UTF-8)")
		or die "Could not open file $dirName/$systemConll: $!";
	my $out = IO::File->new("$dirName/$outputFile", "> :encoding(UTF-8)")
		or die "Could not open output file $dirName/$outputFile: $!";
	
	print $out "Golden: $goldenConll\n";
	print $out "System: $systemConll\n\n";
		
	my $total = 0;
	my $correctRole = 0;
	my $correctHead = 0;
	my $correctRoleAndHead = 0;
	my $ignored = 0;
	
	my $roleCOnfusionMatrix = {};
	my $goldRoles = {};
	my $sysRoles = {};
	
	my $rowNumber = 0; # For debuging purposes.
	for my $sysLine (<$systemIn>)
	{
		my $goldLine = <$goldenIn>;
		die "Gold file is shorter than sytem result file (line $rowNumber): $!"
			unless (defined $goldLine or not $sysLine); # It is okay to have few unnecessary enters in the end of file.
		# Nothing to count, if both lines are empty (end of sentence).
		next if ((not $sysLine or $sysLine =~ /^\s*$/) and (not $goldLine or $goldLine =~ /^\s*$/));
		die "Line $rowNumber in gold file have no corresponding line in system file: $!"
			if ($goldLine and not $sysLine);
		die "Line $rowNumber in system result file have no corresponding line in gold file: $!"
			if ($sysLine and not $goldLine);
		
		#$sysLine = s/^\s*(.*?)\s*$/$1/;
		#$goldLine = s/^\s*(.*?)\s*$/$1/;
		my @goldCol = split /[\t\n\r]/, $goldLine;
		my @sysCol = split /[\t\n\r]/, $sysLine;
		
		die "Missing columns in line $rowNumber: $!" if ( @goldCol < 8 or @sysCol < 8);
		
		die "Token ID mismatch in line $rowNumber: $!"
			unless ($goldCol[0] == $sysCol[0]);
		die "Token mismatch - $goldCol[1] and $sysCol[1] - in line $rowNumber: $!"
			unless ($goldCol[1] eq $sysCol[1]);
		
		if ($goldCol[7] ne '_' and $goldCol[6] ne '_')
		{
			$total++;
			$correctRole++ if ($goldCol[7] eq $sysCol[7]);
			$correctHead++ if ($goldCol[6] eq $sysCol[6]);
			$correctRoleAndHead++
				if ($goldCol[6] eq $sysCol[6] and $goldCol[7] eq $sysCol[7]);
				
			if (not $SIMPLE_STATS)
			{
				# We need list and counts for all roles for output.
				$goldRoles->{$goldCol[7]}++;
				$sysRoles->{$sysCol[7]}++;
				
				if ($goldCol[7] ne $sysCol[7])
				{
					# Create confusion matrix.
					$roleCOnfusionMatrix->{$goldCol[7]}->{$sysCol[7]}++;
				}
			}
		} elsif ($goldCol[7] eq '_' or $goldCol[6] eq '_')
		{
			$ignored++;
		}
	}
	continue
	{
		$rowNumber++;
	}
	
	while (<$goldenIn>)
	{
		die "System result file is shorter than gold file: $!"
			if ($goldenIn) # i.e. nonempty string (it is okay to have few unnecessary enters in the end of file)
	}
	$goldenIn->close();
	$systemIn->close();
	
	my $uas = 100 * $correctHead / $total;
	my $las = 100 * $correctRoleAndHead / $total;
	my $la = 100 * $correctRole / $total;
	print $out "UAS = $correctHead / $total * 100 (%)\t$uas\n";
	print $out "LAS =  $correctRoleAndHead / $total * 100 (%)\t$las\n";
	print $out "LA = $correctRole / $total * 100 (%)\t$la\n";
	print $out "Ignored arcs: $ignored\n";
	
	if (not $SIMPLE_STATS)
	{
		### Confusion matrices.
		my @sortedGold = sort {$a cmp $b} (keys %$goldRoles);
		my @sortedSys = sort {$a cmp $b} (keys %$sysRoles);
		print $out "\nRole confusion matrix (absolute numbers)\n";
		print $out "------------------------------------------\n";
		print $out "Golden\\System\t";
		print $out join("\t", @sortedSys);
		print $out "\n";
		for my $sysRole (@sortedSys)
		{
			print $out "$sysRole";
			for my $goldRole (@sortedGold)
			{
				print $out "\t".($roleCOnfusionMatrix->{$goldRole}->{$sysRole} or 0);
			}
			print $out "\n";
		}
		
		print $out "\nRole confusion matrix (%)\n";
		print $out "---------------------------\n";
		print $out "Golden\\System\t";
		print $out join("\t", @sortedSys);
		print $out "\n";
		for my $sysRole (@sortedSys)
		{
			print $out "$sysRole";
			for my $goldRole (@sortedGold)
			{
				print $out "\t".(($roleCOnfusionMatrix->{$goldRole}->{$sysRole} or 0)*100/$goldRoles->{$goldRole});
			}
			print $out "\n";
		}

		
	}
		
	$out->close();
}

# This ensures that when module is called from shell (and only then!)
# processDir is envoked.
&evaluateFile(@ARGV) unless caller;

1;