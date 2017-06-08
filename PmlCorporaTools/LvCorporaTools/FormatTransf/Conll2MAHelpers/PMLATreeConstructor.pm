#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::Conll2MAHelpers::PMLATreeConstructor;

use strict;
use warnings;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(buildATreeFromConllArray transformRole);

# Everything related to taransforming array-represented CoNLL data to
# hash-represented hybrid tree

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

# &transformRole(role to transform, conll file name for error reporting)
# Get LVTB role from UD role by applying $udRole2LvtbRole and $udChildToPhrase
# mapping.
# Warn if provided role was not found in any mapping.
sub transformRole
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

# &buildATreeFromConllArray (conll node array, PML a level sentence ID, conll
#                            file name for error reporting
# Build a PML tree. Return it as hash from PML node IDs to nodes themselves.
sub buildATreeFromConllArray
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
	my $rootPmc = 0;
	for my $nodeId (keys %{$nodeMap{$aSentId}->{'children'}})
	{
		if ($nodeMap{$nodeId}->{'nodeType'} eq 'pmc')
		{
			$rootPmc = $nodeId;
			last;
		};
	}
	
	# If no PMC node found, add one.
	unless ($rootPmc)
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
		$rootPmc = $newId;
	}

	# If there are multiple phrases below root, leave just one pmc and move all
	# other below it.
	for my $nodeId (keys %{$nodeMap{$aSentId}->{'children'}})
	{
		if ($nodeId ne $rootPmc and $nodeMap{$nodeId}->{'nodeType'} ne 'node')
		{
			$nodeMap{$nodeId}->{'parent'} = $rootPmc;
			$nodeMap{$rootPmc}->{'children'}->{$nodeId} = $nodeMap{$aSentId}->{'children'}->{$nodeId};
			delete $nodeMap{$aSentId}->{'children'}->{$nodeId};
		}
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
	$currentNode->{'role'} = &transformRole($currentNode->{'UD-DEPREL'}, $conllName)
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
		'role' => &transformRole($node->{'UD-DEPREL'}, $conllName),
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

1;