package LvCorporaTools::UIs::AllDatasetGenerator;

use strict;
use warnings;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processDir $PARAMS);

use Carp::Always;	# Print stack trace on die.
use Data::Dumper;
#use Carp qw(croak);

use File::Copy;
#use File::Path;
#use IO::Dir;
#use IO::File;

use LvCorporaTools::UIs::TreeTransformatorUI qw(
	collect unnest dep red knit conll fold);
our $PARAMS = {
	'--collect' => {},
	'--unnest' => {
		'ord' => [], #[0, 1],
		},
	'--dep' => {
		'xpred' => ['BASELEM', 'DEFAULT'],
		'coord' => ['ROW', 'ROW_NO_CONJ', '3_LEVEL', 'DEFAULT'],
		'pmc' => ['BASELEM', 'DEFAULT'],
		'root' => [0], #[0, 1],
		'phdep' => [1], #[0, 1],
		'na' => [0], #[0, 1],
		'subrt' => [0], #[0, 1],
		'ord' => [], #[0, 1],
		},
	'--red' => {
		'label' => [0], #[0, 1],
		'ord' => [], #[0, 1],
		},
	'--knit' => {
		'path' => [], #e.g., ['TrEd extension/lv-treebank/resources']
		},
	'--conll' => {
		'label' => [1], #[0, 1],
		'cpostag' => ['FIRST'], #['NONE', 'FIRST', 'PURIFY'],
		'postag' => ['FULL'], #['FULL', 'PURIFY'],
		'conll09' => [0], #[0, 1],
		},
#	'--fold' => {'p' => [],
#		'seed' => [],
#		},
	};

sub _printMan
{
	print <<END;
Script for automatically running TreeTransformatorUI several times with
different parameter values. For parameter names and accepted values, see
TreeTransformatorUI manual. Currently params can be changed by setting
global $PARAMS variable. This behaviour might be subject to change.

Params:
   data directory

Latvian Treebank project, LUMII, 2013, provided under GPL
END
}

sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 1)
	{
		&_printMan;
		exit 1;
	}

	# Parse parameters.
	# TODO.
	#my $destPrefix = (keys %{$params{'--dir'}})[0];
	my $destPrefix = shift @_;
	my $sources = [$destPrefix];
	my %params = %$PARAMS;
		
	# Do things step by step.
	if ($params{'--collect'})	
	{
		# collect has no additional parameters.
		$sources = [&collect($sources->[0], "$destPrefix/collected")];
	}
	
	if ($params{'--unnest'})	
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&unnest, 'unnest', $params{'--unnest'});
	}
	
	if ($params{'--dep'})
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&dep, 'dep', $params{'--dep'});
	}
	
	if ($params{'--red'})	
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&red, 'red', $params{'--red'});
	}
	
	if ($params{'--knit'})	
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&knit, 'knit', $params{'--knit'});
	}
	
	if ($params{'--fold'})	
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&fold, 'fold', $params{'--fold'});
	}
	
	print "\n==== Successful finish =======================================\n";
	
}

# Perform one step for all given input folders and for all parameter
# combinations.
# _performStep (pointer to list of folders to process, global working
#				directory, pointer to step function for actual data processing,
#				step name (used as prefix for result folder names), parameters
#				for step function)
# return pointer to list of result folder names.
sub _performStep
{
	my ($sourceDirs, $destPrefix, $stepFunct, $stepName, $params) = @_;
	
	# Generate parameter sets.
	my %paramSets = ('' => {});
	for my $param (sort keys %$params)
	{
		my %newParamSets = ();
		for my $dirNameStub (keys %paramSets)
		{
			my $paramValId = 0;
			for my $paramVal (sort @{$params->{$param}})
			{
				$paramValId++;
				my $newStub = $dirNameStub;
				if (@{$params->{$param}} > 0)
				{
					if ($paramVal =~ m/^[^ ]{0,10}$/)
					{
						$newStub .= "-$param$paramVal";
					}
					else
					{
						$paramVal =~ m/^([^ ]{0,10})/;
						$newStub .= "-$param$1$paramValId";
					}
				}
						
				$newParamSets{$newStub} = {
					%{$paramSets{$dirNameStub}}, $param => $paramVal};
			}
		}
		%paramSets = %newParamSets if %newParamSets;
	}
	
	%paramSets = ('' => {}) unless %paramSets;
	
	# Run next step for each source folder and for each parameter set.
	my @resultDirs = ();
	my $setCount = keys %paramSets;
	for my $source (@$sourceDirs)
	{
		my $sourceNameEnding = $source;
		$sourceNameEnding =~ s#^.*[\\/]([^\\/]*?)[\\/]?$#$1#;
		for my $dirNameStub (keys %paramSets)
		{
			my $newResName = "$destPrefix/${sourceNameEnding}_$stepName";
			$newResName .= $dirNameStub if ($setCount > 1);
			
			my $tempResDir = &$stepFunct(
				$source, $newResName, $paramSets{$dirNameStub});
			push @resultDirs, $newResName;
		}
	}
	
	return \@resultDirs;
}

# Get parameters with multiple values. 
#sub _getMultivalueAttr
#{
#	my $paramAV = shift @_;
#	my @res = ();
#	for my $attr (keys %$paramAV)
#	{
#		push(@res, $attr) if ($paramAV->{$attr} and @{$paramAV->{$attr}} > 1);
#	}
#	
#	return \@res;
#}


# This ensures that when module is called from shell (and only then!)
# processDir is envoked.
&processDir(@ARGV) unless caller;

1;
