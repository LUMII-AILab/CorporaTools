#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::Conll2MA;

use warnings;
use utf8;
use strict;

use IO::File;
use IO::Dir;
use LvCorporaTools::GenericUtils::SimpleXmlIo;
use LvCorporaTools::FormatTransf::Conll2MAHelpers::MPrinter
	qw(printMBegin printMEnd printMSentBegin printMSentEnd printMDataNode);
use LvCorporaTools::FormatTransf::Conll2MAHelpers::APrinter
	qw(printABegin printAEnd printASentBegin printASentEnd printAPhraseBegin printAPhraseEnd printInnerANodeStart printInnerANodeEnd printLeafANode);
use Data::Dumper;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(processDir processFileSet);

###############################################################################
# This program creates PML M and A files, if CONLL file containing morphology
# (and optional syntax) and w files are provided. All input files must be UTF-8.
#
# Input parameters: conll dir, w dir, otput dir.
#
# Developed on Strawberry Perl
# Latvian Treebank project, 2017
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

our $vers = 0.1;
our $progname = "CoNLL automātiskais konvertors, $vers";
our $firstSentComment = "AUTO";

our $udChildToPhrase = {
	'appos' => [
		{'nodeType' => 'x', 'phraseSubType' => 'xApp', 'childRole' => 'basElem', 'parentRole' => 'basElem',}],
	'aux' => [
		{'nodeType' => 'x', 'phraseSubType' => 'xPred', 'childRole' => 'auxVerb', 'parentRole' => 'basElem',}],
	'aux:pass' => [
		{'nodeType' => 'x', 'phraseSubType' => 'xPred', 'childRole' => 'auxVerb', 'parentRole' => 'basElem',}],
	'case' => [
		{'nodeType' => 'x', 'phraseSubType' => 'xPrep', 'childRole' => 'prep', 'parentRole' => 'basElem',}],
	'cc' => [
		{'nodeType' => 'coord', 'phraseSubType' => 'N/A', 'childRole' => 'conj', 'parentRole' => 'crdPart',}], #TODO crdParts/crdClauses ??
	'compound' => [
		{'nodeType' => 'x', 'phraseSubType' => 'subrAnal', 'childRole' => 'basElem', 'parentRole' => 'basElem',}], #TODO subrAnal/coordAnal + xNum
	'conj' => [
		{'nodeType' => 'coord', 'phraseSubType' => 'N/A', 'childRole' => 'crdPart', 'parentRole' => 'crdPart',}], #TODO crdParts/crdClauses?
	'cop' => [
		{'nodeType' => 'x', 'phraseSubType' => 'xPred', 'childRole' => 'auxVerb', 'parentRole' => 'basElem',}],
	'flat' => [
		{'nodeType' => 'x', 'phraseSubType' => 'phrasElem', 'childRole' => 'basElem', 'parentRole' => 'basElem',}], #TODO phrasElem/unstruct/interj?
	'flat:foreign' => [
		{'nodeType' => 'x', 'phraseSubType' => 'unstruct', 'childRole' => 'basElem', 'parentRole' => 'basElem',}],
	'flat:name' => [
		{'nodeType' => 'x', 'phraseSubType' => 'namedEnt', 'childRole' => 'basElem', 'parentRole' => 'basElem',}],
	'mark' => [
		{'nodeType' => 'pmc', 'phraseSubType' => 'subrCl', 'childRole' => 'conj', 'parentRole' => 'basElem',}], #TODO subrCl/sent/xSimile?
	'punct' => [
		{'nodeType' => 'pmc', 'phraseSubType' => 'N/A', 'childRole' => 'punct', 'parentRole' => 'basElem',},
		{'nodeType' => 'coord', 'phraseSubType' => 'N/A', 'childRole' => 'punct', 'parentRole' => 'crdPart',}], #TODO subrCl/sent/xSimile?
	'root' => [
		{'nodeType' => 'pmc', 'phraseSubType' => 'sent', 'childRole' => 'pred',}], #TODO basElem & utter
};

# Corse mapping from UDv2 roles to roles used in Latvian Treebank.
our $udRole2LvtbRole = {
	#TODO for all clauses subrCl + pred?
	'acl' => 'attrCl',
	'advcl' => 'spc', # all kinds of adverbial clauses and several kinds of SPCs
	'advmod' => 'adv',
	'amod' => 'attr',
	#'appos' => 'basElem', # TODO: xApp
	#'aux' => 'auxVerb', #TODO xPred
	#'aux:pass' => 'auxVerb', #TODO xPred
	#'case' => 'prep', #TODO xPrep
	#'cc' => 'conj', #TODO coordParts ??
	'ccomp' => 'objCl', # subject clause, predicate clause, some kinds of subjects and SPCs.
	'clf' => 'N/A',
	#'compound' => 'basElem', #TODO subrAnal/coordAnal + xNum
	#'conj' => 'crdPart', #TODO coordParts/coordClauses?
	#'cop' => 'auxVerb', #TODO xPred
	'csubj' => 'subjCl',
	'csubj:pass' => 'subjCl',
	'dep' => 'N/A',
	'det' => 'attr',
	'discourse' => 'no', # also insertions and free-use conjunctions
	'dislocated' => 'N/A',
	'expl' => 'N/A',
	'fixed' => 'conj', # a specific case of xSimile only?
	#'flat' => 'basElem', #TODO phrasElem/unstruct/interj?
	#'flat:foreign' => 'basElem', #TODO unstruct
	#'flat:name' => 'basElem', #TODO namedEnt
	'goeswith' => 'N/A',
	'iobj' => 'obj',
	'list' => 'N/A',
	#'mark' => 'conj', #TODO subrCl/sent/xSimile?
	'nmod' => 'attr', # also some SPCs
	'nsubj' => 'subj',
	'nsubj:pass' => 'subj',
	'nummod' => 'attr', # also some SPCs
	'obj' => 'obj',
	'obl' => 'spc', # also adverbial modifiers, situants, determinants, SPC subjects. TODO det if dative
	'orphan' => 'subj', # subjects, objects, spcs, subject clauses, object clauses, predicate clauses
	'parataxis' => 'basElem', # TODO coordClauses/insertions/utter?S
	#'punct' => 'punct', # TODO pmc?
	'reparandum' => 'N/A',
	#'root' => 'pred', # TODO basElem, if not verb #TODO sent/utter
	'vocative' => 'no', #TODO address
	'xcomp' => 'spc' # also predicate parts
};

sub processDir
{
	if (not @_ or @_ < 3)
	{
		print <<END;
Script for batch creating PML M and A files, if CONLL files and w files are
provided. Currently morphology is mandatory, syntax is optional. All input files
must be UTF-8. Corresponding files must have corresponding filenames.

Params:
   w files directory (.w files)
   morphology directory (.conll files)
   	  expected columns in conll file:
         1 - ID (word index, integer starting at 1 for each new sentence)
         2 - FORM
         3 - LEMMA
         4 - UPOSTAG or other short tag (currently not used)
         5 - XPOSTAG (SemTi-Kamols style part-of-speech tag)
         (further columns are optional)
         6 - FEATS (currently not used)
         7 - HEAD (head of the current word, which is either a value of ID
                   or zero)
         8 - DEPREL (Universal dependency relation to the HEAD)
         9 - DEPS (currently not used)
        10 - MISC (currently not used)
        (any further columns are ignored)   output directory

Latvian Treebank project, LUMII, 2017, provided under GPL
END
		exit 1;
	}
	my $wDirName = shift;
	my $morphoDirName = shift;
	my $outDirName = shift;

	my $wDir = IO::Dir->new($wDirName) or die "$!";
	mkdir($outDirName);

	while (defined(my $inWFile = $wDir->read))
	{
		if (! -d "$wDirName/$inWFile")
		{
			my $coreName = $inWFile =~ /^(.*)\.w*$/ ? $1 : $inWFile;
			&processFileSet($coreName, $outDirName, "$wDirName/$inWFile", "$morphoDirName/$coreName.conll")
		}
	}

}

sub processFileSet
{
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for creating PML M and A files, if and w file and (optional) CoNLL file
are provided. Currently morphology is mandatory, syntax is optional. All input
files must be UTF-8.

Params:
   file name stub for output
   output folder
   .w file name [opt, stub + .w used otherwise]
   .conll file name [opt, stub + .conll used otherwise]
   	  expected columns in conll file:
         1 - ID (word index, integer starting at 1 for each new sentence)
         2 - FORM
         3 - LEMMA
         4 - UPOSTAG or other short tag (currently not used)
         5 - XPOSTAG (SemTi-Kamols style part-of-speech tag)
         (further columns are optional)
         6 - FEATS (currently not used)
         7 - HEAD (head of the current word, which is either a value of ID
                   or zero)
         8 - DEPREL (Universal dependency relation to the HEAD)
         9 - DEPS (currently not used)
        10 - MISC (currently not used)
        (any further columns are ignored)

Latvian Treebank project, LUMII, 2017, provided under GPL
END
		exit 1;
	}

	my $nameStub = shift;
	my $outDirName = shift;
	my $wName = (shift or "$nameStub.w");
	my $conllName = (shift or "$nameStub.conll");

	my $w = LvCorporaTools::GenericUtils::SimpleXmlIo::loadXml($wName, ['para', 'w', 'schema'], []);
	my $conllIn = IO::File->new($conllName, '< :encoding(UTF-8)')
		or die "Could not open file $conllName: $!";

	my $mOut = IO::File->new("$outDirName/$nameStub.m", '> :encoding(UTF-8)');
	my $timeNow = localtime time;
	printMBegin($mOut, $nameStub, "$progname,  $timeNow");
	my $aOut = IO::File->new("$outDirName/$nameStub.a", '> :encoding(UTF-8)');
	printABegin($aOut, $nameStub, "$progname,  $timeNow");

	my $insideOfSent = 0;
	my $paraId = 1;
	my $sentCounter = 0;
	my $wordCounter = 0;
	my @unusedWIds = ();
	my $unusedTokens = '';
	my $unusedConll = '';
	my @unprocessedATokens = ();
	my $isFirstTree = 1;

	# A and M files are made by going through W file.
	for my $wPara (@{$w->{'xml'}->{'doc'}->{'para'}})
	{
		for my $wTok (@{$wPara->{w}})
		{
			# Read a new CoNLL line, if previous one has been used.
			my $line = ($unusedConll or <$conllIn>);
			# Process empty lines, if there any.
			while ($line and $line !~ /^(\d+)\t(\S+)\t(\S+)\t(\S+)\t(\S+)\s/)
			{
				if ($insideOfSent)
				{
					if (@unprocessedATokens)
					{
						my $aSentId = &_getASentId($nameStub, $paraId, $sentCounter);
						my $mSentId = &_getMSentId($nameStub, $paraId, $sentCounter);
						my $nodeMap = &_buildATreeFromConllArray(\@unprocessedATokens, $aSentId, $mSentId, $conllName);
						&_printANodesFromHash($aOut, $nodeMap, $aSentId, $isFirstTree);
						$isFirstTree = 0;
					}

					printMSentEnd($mOut);
					$insideOfSent = 0;
					$wordCounter = 0;
				}
				$unusedConll = '';
				$line = <$conllIn>;
			}

			#print Dumper($wTok);
			$wTok->{'id'} =~ /-p(\d+)w\d+$/;
			$paraId = $1;
			if ($line and $line =~ /^(\d+)\t(\S+)\t(\S+)\t(\S+)\t(\S+)(?:\t(\S+)\t(\S+)\t(\S+))?\s/)
			{
				my ($conllId, $conllToken, $lemma, $tag, $headId, $role) = ($1, $2, $3, $5, $7, $8);
				$conllToken =~ s/_/ /g;
				$lemma =~ s/_/ /g;
				unless($insideOfSent)
				{
					$insideOfSent = 1;
					$sentCounter++;
					my $aSentId = &_getASentId($nameStub, $paraId, $sentCounter);
					my $mSentId = &_getMSentId($nameStub, $paraId, $sentCounter);
					printMSentBegin($mOut, $mSentId);
					$isFirstTree = 0;
					@unprocessedATokens = ();
				}
				push @unusedWIds, $wTok->{'id'};
				$unusedTokens = $unusedTokens . $wTok->{'token'}->{'content'};
				$unusedTokens = "$unusedTokens " unless ($wTok->{'no_space_after'});
				$unusedTokens =~ /^\s*(.*?)\s*$/;
				if ($1 eq $conllToken)
				{
					$wordCounter++;
					my $mId = &_getMNodeId($nameStub, $paraId, $sentCounter, $wordCounter);
					my $aId = &_getANodeId($nameStub, $paraId, $sentCounter, $wordCounter);
					printMDataNode($mOut, $nameStub, $mId, \@unusedWIds,
						$conllToken, $lemma, $tag);
					$unprocessedATokens[$conllId] = {
							'aId' => $aId,
							'mId' => $mId,
							'conllId' => $conllId,
							'ord' => $wordCounter,
							'token' => $conllToken,
							'UD-DEPREL' =>$role,
							'UD-HEAD' => $headId,
							'nodeType' => 'node',
					};
					@unusedWIds = ();
					$unusedTokens = '';
					$unusedConll = '';
				}
				else
				{
					$unusedConll = $line;
				}
			}
		}
		# Process unused CoNLL lines in the end of the paragraph and warn
		if ($unusedConll =~ /^(\d+)\t(\S+)\t(\S+)\t(\S+)\t(\S+)(?:\t(\S+)\t(\S+)\t(\S+))?\s/)
		{
			my ($conllId, $conllToken, $lemma, $tag, $headId, $role) = ($1, $2, $3, $5, $7, $8);
			warn "CoNLL token $conllToken and W tokens $unusedTokens found unused after the end of paragraph! $!";
			$conllToken =~ s/_/ /g;
			$lemma =~ s/_/ /g;
			unless($insideOfSent)
			{
				$insideOfSent = 1;
				$sentCounter++;
				my $mSentId = &_getMSentId($nameStub, $paraId, $sentCounter);
				printMSentBegin($mOut, $mSentId);
				$isFirstTree = 0;
				@unprocessedATokens = ();
			}
			$unusedTokens =~ /^\s*(.*?)\s*$/;
			if ($1 eq $conllToken)
			{
				$wordCounter++;
				my $mId = &_getMNodeId($nameStub, $paraId, $sentCounter, $wordCounter);
				my $aId = &_getANodeId($nameStub, $paraId, $sentCounter, $wordCounter);
				printMDataNode($mOut, $nameStub, $mId, \@unusedWIds,
					$conllToken, $lemma, $tag); # ${@unusedWIds}
				$unprocessedATokens[$conllId] = {
					'aId' => $aId,
					'mId' => $mId,
					'conllId' => $conllId,
					'ord' => $wordCounter,
					'token' => $conllToken,
					'UD-DEPREL' =>$role,
					'UD-HEAD' => $headId,
					'nodeType' => 'node',
				};
				@unusedWIds = ();
				$unusedTokens = '';
				$unusedConll = '';
			}
		}

	}
	if ($insideOfSent)
	{
		my $aSentId = &_getASentId($nameStub, $paraId, $sentCounter);
		my $mSentId = &_getMSentId($nameStub, $paraId, $sentCounter);
		my $nodeMap = &_buildATreeFromConllArray(\@unprocessedATokens, $aSentId, $mSentId, $conllName);
		&_printANodesFromHash($aOut, $nodeMap, $aSentId, $isFirstTree);
		printMSentEnd($mOut);
	}

	printMEnd($mOut);
	$mOut->close();
	printAEnd($aOut);
	$aOut->close();
}

# &_transformRole(role to transform, conll file name for error reporting)
# Get LVTB role from UD role by applying $udRole2LvtbRole and $udChildToPhrase
# mapping.
# Warn if provided role was not found in any mapping.
sub _transformRole
{
	my ($udRole, $conllName) = @_;
	return 'N/A' unless ($udRole and $udRole ne '_');
	my $pmlRole;
	$pmlRole = $udChildToPhrase->{$udRole}->[0]->{'childRole'}
		if (exists $udChildToPhrase->{$udRole} and
			exists $udChildToPhrase->{$udRole}->[0]->{'childRole'});
	$pmlRole = $udRole2LvtbRole->{$udRole}
		if (not $pmlRole and exists $udRole2LvtbRole->{$udRole});
	return $pmlRole if ($pmlRole);
	warn "File $conllName contains unknown role $udRole.\n";
	return 'N/A';
}

# Form an ID for a-node by using given numerical parameters.
sub _getANodeId
{
	my ($docId, $parId, $sentId, $tokId) = @_;
	return "a-${docId}-p${parId}s${sentId}w$tokId";
}

# Form an ID for m-node by using given numerical parameters.
sub _getMNodeId
{
	my ($docId, $parId, $sentId, $tokId) = @_;
	return "m-${docId}-p${parId}s${sentId}w$tokId";
}

# Form an ID for a-root by using given numerical parameters.
# To get stub for x node, just add 'x' to the end.
sub _getASentId
{
	my ($docId, $parId, $sentId) = @_;
	return "a-${docId}-p${parId}s${sentId}";
}

# Form an ID for m-root by using given numerical parameters.
sub _getMSentId
{
	my ($docId, $parId, $sentId) = @_;
	return "m-${docId}-p${parId}s${sentId}";
}

# &_buildATreeFromConllArray (conll node array, PML a level sentence ID, conll
#                             file name for error reporting
# Build a PML tree. Return it as hash from PML node IDs to nodes themselves.
sub _buildATreeFromConllArray
{
	my ($aNodes, $aSentId, $mSentId, $conllName) = @_;
	my %nodeMap = ();

	# Make fictional root node in the hashmap.
	$nodeMap{$aSentId} = {
		'aId' => $aSentId,
		'mId' => $mSentId,
		'nodeType' => 'root',
	};

	# Populate node hashmap.
	for my $aNode (@$aNodes)
	{
		$nodeMap{$aNode->{'aId'}} = $aNode if ($aNode);
	}

	# Populate childlist, parent for each node.
	for my $aNode (@$aNodes)
	{
		if ($aNode)
		{
			my $conllHeadId = $aNode->{'UD-HEAD'};
			my $pmlParentId = $aSentId;
			$pmlParentId = $aNodes->[$conllHeadId]->{'aId'}
				if ($conllHeadId and $conllHeadId ne '0' and $conllHeadId ne '_'
					and $aNodes->[$conllHeadId]);
			$nodeMap{$pmlParentId}->{'children'}->{$aNode->{'aId'}} = 'dep';
			$aNode->{'parent'} = $pmlParentId;
		}
	}

	# Build tree structure.
	&_buildASubtreeFromMap($aSentId, \%nodeMap, $aSentId.'x', $conllName);

	#Check if right below the root is a PMC node.
	my $hasPmc = 0;
	for my $nodeId (keys %{$nodeMap{$aSentId}->{'children'}})
	{
		$hasPmc ++ if ($nodeMap{$nodeId}->{'nodeType'} eq 'pmc');
	}
	# If no PMC node found, add one.
	unless ($hasPmc)
	{
		my $newId = 1;
		$newId++ while (exists $nodeMap{"${aSentId}x$newId"});
		$newId = "${aSentId}x$newId";
		$nodeMap{$newId} = {
			'aId' => $newId,
			'nodeType' => 'pmc',
			'phraseSubType' => 'sent',
			'parent' => $aSentId,
		};
		for my $nodeId (keys %{$nodeMap{$aSentId}->{'children'}})
		{
			$nodeMap{$newId}->{'children'}->{$nodeId} = 'phrase';
			delete $nodeMap{$aSentId}->{'children'}->{$nodeId};
		}
		$nodeMap{$aSentId}->{'children'}->{$newId} = 'dep';
	}
	return \%nodeMap;
}

# &_buildASubtreeFromMap (PML ID of the subtree root, maping from PML IDs to
#                         nodes, stub for forming PML IDs for the new (phrase)
#                         nodes, conll file name for error reporting)
# Transform a dependency subtree to a LVTB hybrid model subtree.
# DFS + actual processing is done before traversing each all the children of the
# node.
sub _buildASubtreeFromMap
{
	my ($currentNodeId, $nodeMap, $xIdStub, $conllName) = @_;
	my $currentNode = $nodeMap->{$currentNodeId};
	#print "Entered _buildASubtreeFromMap : ". Dumper($nodeMap);

	if (exists $currentNode->{'children'})
	{
		my @childrenToProcess = keys %{$currentNode->{'children'}};
		# First thing: let's make some phrases in this level.
		# First process children signaling only one possible type of phrase.
		for my $childId (@childrenToProcess)
		{
			my $child = $nodeMap->{$childId};
			&_makePhraseFromDepLink($childId, $nodeMap, $xIdStub, $conllName)
				if ($child->{'UD-DEPREL'} and
					exists $udChildToPhrase->{$child->{'UD-DEPREL'}} and
					@{$udChildToPhrase->{$child->{'UD-DEPREL'}}} == 1);
		}
		# Then process children that can be invoked in multiple types of phrases.
		for my $childId (@childrenToProcess)
		{
			my $child = $nodeMap->{$childId};
			&_makePhraseFromDepLink($childId, $nodeMap, $xIdStub, $conllName)
				if ($child->{'UD-DEPREL'} and
					exists $udChildToPhrase->{$child->{'UD-DEPREL'}} and
					@{$udChildToPhrase->{$child->{'UD-DEPREL'}}} > 1);
		}

		# After current level is processed, let's do recursion.
		# First process the unambiguous children.
		for my $childId (@childrenToProcess)
		{
			my $child = $nodeMap->{$childId};
			&_buildASubtreeFromMap($childId, $nodeMap, $xIdStub, $conllName)
				if (not $child->{'UD-DEPREL'} or
					not exists $udChildToPhrase->{$child->{'UD-DEPREL'}} or
					@{$udChildToPhrase->{$child->{'UD-DEPREL'}}} <= 1);
		}
		# Then process children that can be invoked in multiple types of phrases.
		for my $childId (@childrenToProcess)
		{
			my $child = $nodeMap->{$childId};
			&_buildASubtreeFromMap($childId, $nodeMap, $xIdStub, $conllName)
				if ($child->{'UD-DEPREL'} and
					exists $udChildToPhrase->{$child->{'UD-DEPREL'}} and
					@{$udChildToPhrase->{$child->{'UD-DEPREL'}}} > 1);
		}
	}

	# Current node gets a PML role. Later iterations may rename this when making
	# new phrases.
	$currentNode->{'role'} = &_transformRole($currentNode->{'UD-DEPREL'}, $conllName)
		if ($currentNode->{'UD-DEPREL'} and not exists $currentNode->{'role'});
}

# $_makePhraseFromDepLink(PML ID of the node to process, maping from PML IDs to
#                         nodes, stub for forming PML IDs for the new (phrase)
#                         nodes, conll file name for error reporting)
# Given a childId, make a phrase from dependency link connesting given node with
# its parent. For some children parent is not included in phrase.
sub _makePhraseFromDepLink
{
	my ($childId, $nodeMap, $xIdStub, $conllName) = @_;
	my $child = $nodeMap->{$childId};
	# Check if given child should be transformed as a part of phrase.
	return unless ($child->{'UD-DEPREL'});
	return unless (exists $udChildToPhrase->{$child->{'UD-DEPREL'}});
	my @phrasePatterns = @{$udChildToPhrase->{$child->{'UD-DEPREL'}}};
	return unless (@phrasePatterns);

	# Search if suitable phrase has already been made.
	my ($premadePhraseId, $phrasePattern) = @{&_findPhrase($childId, $nodeMap) or [0, 0]};
	$nodeMap->{$premadePhraseId}->{'phraseSubType'} = $phrasePattern->{'phraseSubType'}
		if ($premadePhraseId and $nodeMap->{$premadePhraseId}->{'phraseSubType'} eq 'N/A');

	my $parentId = $child->{'parent'};
	my $parent = $nodeMap->{$parentId};

	# If no phrase found, make one.
	unless ($premadePhraseId)
	{
		$phrasePattern = $phrasePatterns[0];
		my $isParentIncluded = (exists $phrasePattern->{'parentRole'} and $phrasePattern->{'parentRole'});
		my $newId = $isParentIncluded ?
			&_makePhrase($parentId, $nodeMap, $phrasePattern, $xIdStub, $conllName) :
			&_makePhrase($childId, $nodeMap, $phrasePattern, $xIdStub, $conllName);

		# If parent for dependency link is moved (this happens only for newly
		# made phrases), it must be given LVTB role
		$parent->{'role'} = $phrasePattern->{'parentRole'} if ($isParentIncluded);
		$premadePhraseId = $newId;
	}

	# Anyway, now we need to move the child to the found or newly-made phrase.
	my $phraseNode = $nodeMap->{$premadePhraseId};
	for my $nodeId (keys %$nodeMap)
	{
		delete $nodeMap->{$nodeId}->{'children'}->{$childId};
	}
	$child->{'parent'} = $premadePhraseId;
	$phraseNode->{'children'}->{$childId} = 'phrase';

	# And set the child role.
	$child->{'role'} = $phrasePattern->{'childRole'};
}

# $_findPhrase(PML ID of the child node to process (phrases will be searched in
#              the grandparent level and above), maping from PML IDs to nodes)
# For given child finds the parent phrase sattisfing conditions given in
# $udChildToPhrase.
# Taken out from &_makePhraseFromDepLink for readability.
sub _findPhrase
{
	my ($childId, $nodeMap) = @_;
	my $child = $nodeMap->{$childId};
	# Check if given child has any description what kinf of phrase should
	# be searched for.
	return unless ($child->{'UD-DEPREL'});
	return unless (exists $udChildToPhrase->{$child->{'UD-DEPREL'}});
	my @phrasePatterns = @{$udChildToPhrase->{$child->{'UD-DEPREL'}}};
	return unless (@phrasePatterns);

	my $parent = $nodeMap->{$child->{'parent'}}; # Parent is always 'node'
	# TODO check if everything works when more "parentless" phrases are used
	my $phraseIdTocheck = $parent->{'parent'};
	my $resultPhraseId = 0;
	my $resultPhrasePattern = 0;
	while ($phraseIdTocheck)
	{
		my $nodeToCheck = $nodeMap->{$phraseIdTocheck};
		my $nodeType = $nodeToCheck->{'nodeType'};
		last if ($nodeType eq 'root' or $nodeType eq 'node');
		for my $pattern (@phrasePatterns)
		{
			if ($nodeType eq $pattern->{'nodeType'}
				and ($nodeToCheck->{'phraseSubType'} eq $pattern->{'phraseSubType'}
				or $nodeToCheck->{'phraseSubType'} eq 'N/A' or $pattern->{'phraseSubType'} eq 'N/A'))
			{
				$resultPhraseId = $phraseIdTocheck;
				$resultPhrasePattern = $pattern;
				last;
			}
		}
		last unless (exists $nodeToCheck->{'parent'});
		# Check phrase parent, too.
		$phraseIdTocheck = $nodeToCheck->{'parent'};
	}
	return [$resultPhraseId, $resultPhrasePattern];
}

# $_makePhrase(PML ID of the node above which phrase will be inserted, maping
#              from PML IDs to nodes, phrase pattern from $udChildToPhrase,
#              sufix for new PML id (next free integer will be added
#              automatically), conll file name for error reporting)
# For given node and given pattern from $udChildToPhrase make a phrase node
# between given node and it's ancestor.
# Taken out from &_makePhraseFromDepLink for readability.
sub _makePhrase
{
	my ($nodeId, $nodeMap, $phrasePattern, $xIdStub, $conllName) = @_;
	my $node = $nodeMap->{$nodeId};
	#print "Entered _makePhrase for node $nodeId: ". Dumper($nodeMap);

	# Find free ID.
	my $newId = 1;
	$newId++ while (exists $nodeMap->{"$xIdStub$newId"});
	$newId = "$xIdStub$newId";

	# Make structural fields
	$nodeMap->{$newId} = {
		'aId' => $newId,
		'nodeType' => $phrasePattern->{'nodeType'},
		'phraseSubType' => $phrasePattern->{'phraseSubType'},
		'children' => {$nodeId => 'phrase'},
		'UD-DEPREL' => $node->{'UD-DEPREL'},
		'role' => &_transformRole($node->{'UD-DEPREL'}, $conllName),
		'parent' => $nodeMap->{$nodeId}->{'parent'},
	};

	# Put the new phrase in the tree between parent and grandparent
	for my $tmpId (keys %$nodeMap)
	{
		if ($tmpId ne $newId and
			exists $nodeMap->{$tmpId}->{'children'}->{$nodeId})
		{
			$nodeMap->{$tmpId}->{'children'}->{$newId} =
				$nodeMap->{$tmpId}->{'children'}->{$nodeId};
			delete $nodeMap->{$tmpId}->{'children'}->{$nodeId};
		}
	}
	$node->{'parent'} = $newId;
	#print "Exiting _makePhrase : ". Dumper($nodeMap);

	return $newId;
}

# $_printANodesFromHash(output stream, maping from PML IDs to nodes, ID of the
#                       tree root (only ancestors of this node are printed),
#                       should the first sentence comment be added?)
# Prints out as XML the tree which described in the mapping and rooted in the
# given ID.
sub _printANodesFromHash
{
	my ($aOut, $nodeMap, $rootId, $isFirst) = @_;
	my $rootNode = $nodeMap->{$rootId};
	my $rootType = $rootNode->{'nodeType'};
	my $parentType = 0;
	$parentType = $nodeMap->{$rootNode->{'parent'}}->{'nodeType'}
		if (exists $rootNode->{'parent'} and exists $nodeMap->{$rootNode->{'parent'}});

	if ($rootType eq 'root')
	{
		printASentBegin($aOut, $rootNode->{'aId'}, $rootNode->{'mId'}, $isFirst ? $firstSentComment : 0);
		if (exists $rootNode->{'children'} and keys $rootNode->{'children'} > 0)
		{
			for my $nodeId (keys %{$rootNode->{'children'}})
			{
				_printANodesFromHash($aOut, $nodeMap, $nodeId, 0)
					if ($nodeMap->{$nodeId}->{'nodeType'} ne 'node');
			}
			for my $nodeId (keys %{$rootNode->{'children'}})
			{
				_printANodesFromHash($aOut, $nodeMap, $nodeId, 0)
					if ($nodeMap->{$nodeId}->{'nodeType'} eq 'node');
			}
		}
		else
		{
			warn "Node $rootId with type \'$rootType\' has no children!"
		}
		printASentEnd($aOut);
	}
	elsif ($rootType eq 'pmc' or $rootType eq 'x' or $rootType eq 'coord')
	{
		printInnerANodeStart($aOut, $rootNode->{'aId'}, $rootNode->{'role'})
			if ($parentType ne 'root');
		printAPhraseBegin($aOut, $rootType, $rootNode->{'phraseSubType'});
		if (exists $rootNode->{'children'} and keys $rootNode->{'children'} > 0)
		{
			for my $nodeId (keys %{$rootNode->{'children'}})
			{
				_printANodesFromHash($aOut, $nodeMap, $nodeId, 0)
					if ($rootNode->{'children'}->{$nodeId} eq 'phrase');
			}
		}
		else
		{
			warn "Node $rootId with type \'$rootType\' has no children!"
		}
		printAPhraseEnd($aOut, $rootType);
		if (exists $rootNode->{'children'} and keys $rootNode->{'children'} > 0)
		{
			for my $nodeId (keys %{$rootNode->{'children'}})
			{
				my $relType = $rootNode->{'children'}->{$nodeId};
				_printANodesFromHash($aOut, $nodeMap, $nodeId, 0)
					if ($relType ne 'phrase');
			}
		}
		printInnerANodeEnd($aOut) if ($parentType ne 'root');
	}
	else
	{
		warn "Node $rootId has unrecognized type \'$rootType\'!" unless ($rootType eq 'node');
		if (exists $rootNode->{'children'} and keys $rootNode->{'children'} > 0)
		{
			printInnerANodeStart($aOut, $rootNode->{'aId'}, $rootNode->{'role'},
				$rootNode->{'mId'}, $rootNode->{'ord'}, $rootNode->{'token'});
			for my $nodeId (keys %{$rootNode->{'children'}})
			{
				_printANodesFromHash($aOut, $nodeMap, $nodeId, 0);
			}
			printInnerANodeEnd($aOut);
		}
		else
		{
			printLeafANode($aOut, $rootNode->{'aId'}, $rootNode->{'role'},
				$rootNode->{'mId'}, $rootNode->{'ord'}, $rootNode->{'token'})
		}
	}
}

1;
