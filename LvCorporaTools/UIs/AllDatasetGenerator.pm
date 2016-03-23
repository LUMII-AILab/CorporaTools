package LvCorporaTools::UIs::AllDatasetGenerator;

use strict;
use warnings;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processDir $PARAMS);

#use Carp::Always;	# Print stack trace on die.

use File::Copy;
use Data::Dumper;

use LvCorporaTools::UIs::TreeTransformatorUI qw(
	collect unnest dep red knit conll fold);
our $PARAMS = {
	'--collect' => {},
	'--unnest' => {
		'ord' => [], #[0, 1],
		},
	'--dep' => {
		'xpred' => ['BASELEM_NO_RED', 'DEFAULT_NO_RED'],
		'coord' => ['ROW', 'ROW_NO_CONJ', '3_LEVEL', 'DEFAULT'],
		'pmc' => ['BASELEM', 'DEFAULT'],
		'root' => [0], #[0, 1],
		'phdep' => [1], #[0, 1],
		'na' => [0], #[0, 1],
		'subrt' => [0], #[0, 1],
		'ord' => [], #[0, 1],
		'mark' => [''],
		#'mark' => ['crdParts,crdClauses', 'xPred',
		#		   'sent,utter,mainCl,subrCl,interj,spcPmc,insPmc,particle,dirSpPmc,address,quot'],
		'markphdep' => [''],
		#'markphdep' => ['crdParts,crdClauses', 'xPred',
		#				'sent,utter,mainCl,subrCl,interj,spcPmc,insPmc,particle,dirSpPmc,address,quot'],
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
		'marked' => [0], #[0, 1],
		#'marked' => [1], #[0, 1],
		'conll09' => [0], #[0, 1],
		},
	'--fold' => {
		'p' => [1],
		#'seed' => [],
		#'name' => [],
		'name' => ['LETA_test'],
		#'name' => ['treebank'],
		},
	};

# Omit single value parameters in folder names? Boolean.
our $OMIT_PARAMS = 0;
# Omit step name in folder names? Boolean.
our $OMIT_STEP_NAME = 0;

sub _printMan
{
	print <<END;
Script for automatically running TreeTransformatorUI several times with
different parameter values. For parameter names and accepted values, see
TreeTransformatorUI manual. Currently params can be changed by setting
global $PARAMS variable. This behaviour might be subject to change.

Params:
   data directory
   log file [opt]
   omit single value parameters in folder names [1/0 (default), opt]
   omit step name in folder names [1/0 (default), opt]

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
	my $destPrefix = shift @_;
	my $sources = [$destPrefix];
	my %params = %$PARAMS;
	my $logFile = shift @_;
	$OMIT_PARAMS = (shift @_ or 0);
	$OMIT_STEP_NAME = (shift @_ or 0);
	
	if ($logFile)
	{
		my $logStream = IO::File->new($logFile, "> :encoding(UTF-8)")
			or die "Could not open file $logFile: $!";
		print $logStream Dumper ($PARAMS);
		$logStream->close;
	}
		
	# Do things step by step.
	if ($params{'--collect'})	
	{
		# collect has no additional parameters.
		my $dest = "0";
		$dest .= "_collected" unless ($OMIT_STEP_NAME);
		$sources = [&collect($sources->[0], "$destPrefix/$dest")];
	}
	
	if ($params{'--unnest'})	
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&unnest, 'unnest', '1', $params{'--unnest'});
	}
	
	if ($params{'--dep'})
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&dep, 'dep', '2', $params{'--dep'});
	}
	
	if ($params{'--red'})	
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&red, 'red', '3', $params{'--red'});
	}
	
	if ($params{'--knit'})	
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&knit, 'knit', '4', $params{'--knit'});
	}
	
	if ($params{'--conll'})	
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&conll, 'conll', '5', $params{'--conll'});
	}
	
	if ($params{'--fold'})	
	{
		$sources = &_performStep(
			$sources, $destPrefix, \&fold, 'fold', '6', $params{'--fold'});
	}
	
	print "\n==== Successful finish =======================================\n";
	
}

# Perform one step for all given input folders and for all parameter
# combinations.
# _performStep (pointer to list of folders to process, global working
#				directory, pointer to step function for actual data processing,
#				step name (used as postfix for result folder names), numerical
#				prefix for result folder name (to make sroting easier),
#				parameters for step function)
# return pointer to list of result folder names.
sub _performStep
{
	my ($sourceDirs, $destPrefix, $stepFunct, $stepName, $stepSortNum, $params)
		= @_;
	
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
				if (@{$params->{$param}} > 0 && !$OMIT_PARAMS || @{$params->{$param}} > 1)
				{
					if ($paramVal =~ m/^[^ ]{0,11}$/)
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
		$sourceNameEnding =~ s#^.*[\\/](\d+_?)?([^\\/]*?)[\\/]?$#$2#;
		for my $dirNameStub (keys %paramSets)
		{
			my $newResName = "$destPrefix/${stepSortNum}";
			$newResName .= "_$sourceNameEnding" if ($sourceNameEnding);
			$newResName .= "_$stepName" unless ($OMIT_STEP_NAME);
			$newResName .= $dirNameStub if ($setCount > 1);
			
			my $tempResDir = &$stepFunct(
				$source, $newResName, $paramSets{$dirNameStub});
			push @resultDirs, $newResName;
		}
	}
	
	return \@resultDirs;
}


# This ensures that when module is called from shell (and only then!)
# processDir is envoked.
&processDir(@ARGV) unless caller;

1;
