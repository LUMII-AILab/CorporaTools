#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::Conll2MA;

use warnings;
use utf8;
use strict;

use IO::File;
use IO::Dir;
use LvCorporaTools::GenericUtils::SimpleXmlIo;
use LvCorporaTools::FormatTransf::Conll2MAHelpers::PMLMStubPrinter
	qw(printMFileBegin printMFileEnd
		printMSentBegin printMSentEnd printMDataNode);
use LvCorporaTools::FormatTransf::Conll2MAHelpers::PMLAStubPrinter
	qw(printAFileBegin printAFileEnd
		printASentBegin printASentEnd
		printAPhraseBegin printAPhraseEnd
		printANodeStart printANodeEnd printALeaf);
use LvCorporaTools::FormatTransf::Conll2MAHelpers::PMLATreeConstructor
	qw(buildATreeFromConllArray);
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


our $vers = 0.2;
our $progname = "CoNLL automātiskais konvertors, $vers";
our $firstSentComment = "AUTO";

sub processDir
{
	if (not @_ or @_ < 3)
	{
		print <<END;
Script for batch creating PML M and A files, if CONLL files and w files are
provided. Currently morphology is mandatory, syntax is optional. All input files
must be UTF-8. Corresponding files must have corresponding filenames and texts.

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
files must be UTF-8. Fileset must have the same text in both W and CoNLL file.

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
	printMFileBegin($mOut, $nameStub, "$progname,  $timeNow");
	my $aOut = IO::File->new("$outDirName/$nameStub.a", '> :encoding(UTF-8)');
	printAFileBegin($aOut, $nameStub, "$progname,  $timeNow");

	my %status = (
		'paraId' => 1,
		'sentenceCounter' => 0,
		'wordCounter' => 0,
		'isInsideOfSentence' => 0,
		'isFirstTree' => 1,
		'unprocessedATokens' => [],
		'unprocessedWIds' => [],
		'unusedConll' => '',
		'unusedTokens' => '',
	);

	# A and M files are made by going through W  and CoNLL files at the same time.
	for my $wPara (@{$w->{'xml'}->{'doc'}->{'para'}})
	{
		for my $wTok (@{$wPara->{w}})
		{
			# Get the next conll line to use - either the unused one from
			# previous loop or read a new one.
			my ($line, $mustEndSentence) = &_getNextConllContentLine(\%status, $conllIn);
			&_endSentence(\%status, $nameStub, $mOut, $aOut, $conllName)
				if ($mustEndSentence);
			#print Dumper($wTok);
			$wTok->{'id'} =~ /-p(\d+)w\d+$/;
			$status{'paraId'} = $1;
			&_doOneTokenOrLine(\%status, $line, $nameStub, $mOut, $wTok);
		}
	}

	# Process unused CoNLL lines in the end of the file and warn
	$status{'paraId'}++;
	my ($line, $mustEndSentence) = &_getNextConllContentLine(\%status, $conllIn);
	&_endSentence(\%status, $nameStub, $mOut, $aOut, $conllName)
		if ($mustEndSentence);
	while ($line)
	{
		&_doOneTokenOrLine(\%status, $line, $nameStub, $mOut);
		($line, $mustEndSentence) = &_getNextConllContentLine(\%status, $conllIn);
		&_endSentence(\%status, $nameStub, $mOut, $aOut, $conllName)
			if ($mustEndSentence);
	}

	# Warn about spare w nodes
	warn "W tokens ".$status{'unusedTokens'}." from dataset $nameStub found unused after the end of paragraph!"
		if ($status{'unusedTokens'});

	&_endSentence(\%status, $nameStub, $mOut, $aOut, $conllName)
		if ($status{'isInsideOfSentence'});

	printMFileEnd($mOut);
	$mOut->close();
	printAFileEnd($aOut);
	$aOut->close();
}

sub _getNextConllContentLine
{
	my ($status, $conllIn) = @_;
	# Read a new CoNLL line, if previous one has been used.
	my $line = ($status->{'unusedConll'} or <$conllIn>);
	my $mustEndSentence = 0;
	# Process empty lines, if there any.
	while ($line and $line !~ /^(\d+)\t(\S+)\t(\S+)\t(\S+)\t(\S+)\s/)
	{
		$mustEndSentence = 1 if ($status->{'isInsideOfSentence'});
		$status->{'unusedConll'} = '';
		$line = <$conllIn>;
	}
	return ($line, $mustEndSentence);
}
sub _startSentence
{
	my ($status, $nameStub, $mOut) = @_;
	$status->{'isInsideOfSentence'} = 1;
	$status->{'sentenceCounter'}++;
	my $mSentId = &_getMSentId($nameStub, $status->{'paraId'}, $status->{'sentenceCounter'});
	printMSentBegin($mOut, $mSentId);
	$status->{'isFirstTree'} = 0;
	$status->{'unprocessedATokens'} = [];
}

sub _endSentence
{
	my ($status, $nameStub, $mOut, $aOut, $conllName) = @_;
	if (@{$status->{'unprocessedATokens'}})
	{
		my $aSentId = &_getASentId($nameStub, $status->{'paraId'}, $status->{'sentenceCounter'});
		my $mSentId = &_getMSentId($nameStub, $status->{'paraId'}, $status->{'sentenceCounter'});
		my $nodeMap = buildATreeFromConllArray($status->{'unprocessedATokens'}, $aSentId, $mSentId, $conllName);
		&_printATreeFromHash($aOut, $nodeMap, $aSentId, $status->{'isFirstTree'});
		$status->{'isFirstTree'} = 0;
	}

	printMSentEnd($mOut);
	$status->{'isInsideOfSentence'} = 0;
	$status->{'wordCounter'} = 0;
}

sub _doOneTokenOrLine
{
	my ($status, $conllLine, $nameStub, $mOut, $wNode) = @_;

	# Preprocess given w node - add its contents as jet to be processed.
	if ($wNode)
	{
		# If corresponding token from w file is available, add it to "to-process".
		push @{$status->{'unusedWIds'}}, $wNode->{'id'};
		$status->{'unusedTokens'} = $status->{'unusedTokens'} . $wNode->{'token'}->{'content'};
		$status->{'unusedTokens'} = "$status->{'unusedTokens'} " unless ($wNode->{'no_space_after'});
	}
	else
	{
		warn "W tokens ".$status->{'unusedTokens'}." from dataset $nameStub found unused after the end of paragraph!"
			if ($status->{'unusedTokens'});
	}

	# If the provided CoNLL line do not match, there is nothing more to do.
	return unless ($conllLine and $conllLine =~ /^(\d+)\t(\S+)\t(\S+)\t(\S+)\t(\S+)(?:\t(\S+)\t(\S+)\t(\S+))?\s/);

	my ($conllId, $conllToken, $lemma, $tag, $headId, $role) = ($1, $2, $3, $5, $7, $8);
	$conllToken =~ s/_/ /g;
	$lemma =~ s/_/ /g;
	# This usually happens if at the end of the file something is missing,
	# or some kind of mismatch has happened.
	warn "CoNLL token $conllToken from dataset $nameStub found unused after the end of paragraph!"
		if ($conllToken and not $wNode);

	# Start sentence if needed.
	&_startSentence ($status, $nameStub, $mOut)
		unless ($status->{'isInsideOfSentence'});

	$status->{'unusedTokens'} =~ /^\s*(.*?)\s*$/;
	if ($1 eq $conllToken or (not $wNode and not $status->{'unusedTokens'}))
	{
		# If conll token and unused w token matches, print next PML node.
		$status->{'wordCounter'}++;
		my $mId = &_getMNodeId($nameStub, $status->{'paraId'}, $status->{'sentenceCounter'}, $status->{'wordCounter'});
		my $aId = &_getANodeId($nameStub, $status->{'paraId'}, $status->{'sentenceCounter'}, $status->{'wordCounter'});
		printMDataNode($mOut, $nameStub, $mId, $status->{'unusedWIds'},
			$conllToken, $lemma, $tag);
		$status->{'unprocessedATokens'}->[$conllId] = {
			'aId' => $aId,
			'mId' => $mId,
			'conllId' => $conllId,
			'ord' => $status->{'wordCounter'},
			'token' => $conllToken,
			'UD-DEPREL' =>$role,
			'UD-HEAD' => $headId,
			'nodeType' => 'node',
		};
		$status->{'unusedWIds'} = [];
		$status->{'unusedTokens'} = '';
		$status->{'unusedConll'} = '';
	}
	else
	{
		$status->{'unusedConll'} = $conllLine;
	}
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

# &_printATreeFromHash(output stream, maping from PML IDs to nodes, ID of the
#                      tree root (only descendents of this node are printed),
#                      should the first sentence comment be added?)
# Prints out as XML the tree which described in the mapping and rooted in the
# given ID.
sub _printATreeFromHash
{
	my ($aOut, $nodeMap, $rootId, $isFirst) = @_;
	my $rootNode = $nodeMap->{$rootId};
	my $rootType = $rootNode->{'nodeType'};
	my $parentType = 0;
	$parentType = $nodeMap->{$rootNode->{'parent'}}->{'nodeType'}
		if (exists $rootNode->{'parent'} and exists $nodeMap->{$rootNode->{'parent'}});

	# Depending on node type varies the processing order of children.
	if ($rootType eq 'root')
	{
		_printARootFromHash($aOut, $nodeMap, $rootId, $isFirst);
	}
	elsif ($rootType eq 'pmc' or $rootType eq 'x' or $rootType eq 'coord')
	{
		_printAPhraseFromHash($aOut, $nodeMap, $rootId);
	}
	else
	{
		warn "Node $rootId has unrecognized type \'$rootType\'!" unless ($rootType eq 'node');
		_printANodeFromHash($aOut, $nodeMap, $rootId);
	}
}

# &_printARootFromHash(output stream, maping from PML IDs to nodes, ID of the
#                      tree root (only descendents of this node are printed),
#                      should the first sentence comment be added?)
# Print out the subtree assuming it is rooten in the node with type 'root'.
sub _printARootFromHash
{
	my ($aOut, $nodeMap, $rootId, $isFirst) = @_;
	my $rootNode = $nodeMap->{$rootId};
	my $parentType = 0;
	$parentType = $nodeMap->{$rootNode->{'parent'}}->{'nodeType'}
		if (exists $rootNode->{'parent'} and exists $nodeMap->{$rootNode->{'parent'}});

	# 1. Start sentence enclosing part.
	printASentBegin($aOut, $rootNode->{'aId'}, $rootNode->{'mId'}, $isFirst ? $firstSentComment : 0);
	if (exists $rootNode->{'children'} and keys $rootNode->{'children'} > 0)
	{
		# 2. Print out (hopefully only one) phrase children.
		for my $nodeId (keys %{$rootNode->{'children'}})
		{
			_printATreeFromHash($aOut, $nodeMap, $nodeId, 0)
				if ($nodeMap->{$nodeId}->{'nodeType'} ne 'node');
		}
		# 3. Print out sentence dependents.
		for my $nodeId (keys %{$rootNode->{'children'}})
		{
			_printATreeFromHash($aOut, $nodeMap, $nodeId, 0)
				if ($nodeMap->{$nodeId}->{'nodeType'} eq 'node');
		}
	}
	else
	{
		warn "Root node $rootId with type has no children!"
	}
	# 4. End sentence enclosing part.
	printASentEnd($aOut);
}
# &_printAPhraseFromHash(output stream, maping from PML IDs to nodes, ID of the
#                       tree root (only descendents of this node are printed))
# Print out the subtree assuming it is rooten in the node with type 'pmc', 'x'
# or 'coord'.
sub _printAPhraseFromHash
{
	my ($aOut, $nodeMap, $rootId) = @_;
	my $rootNode = $nodeMap->{$rootId};
	my $rootType = $rootNode->{'nodeType'};
	my $parentType = 0;
	$parentType = $nodeMap->{$rootNode->{'parent'}}->{'nodeType'}
		if (exists $rootNode->{'parent'} and exists $nodeMap->{$rootNode->{'parent'}});

	# 1. Start phrase enclosing dependency node.
	printANodeStart($aOut, $rootNode->{'aId'}, $rootNode->{'role'})
		if ($parentType ne 'root');
	# 2. Start phrase node.
	printAPhraseBegin($aOut, $rootType, $rootNode->{'phraseSubType'});
	# 3. Print out phrase constituents.
	if (exists $rootNode->{'children'} and keys $rootNode->{'children'} > 0)
	{
		for my $nodeId (keys %{$rootNode->{'children'}})
		{
			_printATreeFromHash($aOut, $nodeMap, $nodeId, 0)
				if ($rootNode->{'children'}->{$nodeId} eq 'phrase');
		}
	}
	else
	{
		warn "Node $rootId with type \'$rootType\' has no children!"
	}
	# 4. End phrase node.
	printAPhraseEnd($aOut, $rootType);
	# 5. Print out phrase dependents.
	if (exists $rootNode->{'children'} and keys $rootNode->{'children'} > 0)
	{
		for my $nodeId (keys %{$rootNode->{'children'}})
		{
			my $relType = $rootNode->{'children'}->{$nodeId};
			_printATreeFromHash($aOut, $nodeMap, $nodeId, 0)
				if ($relType ne 'phrase');
		}
	}
	# 6. End phrase enclosing dependency node.
	printANodeEnd($aOut) if ($parentType ne 'root');
}

# &_printAPhraseFromHash(output stream, maping from PML IDs to nodes, ID of the
#                       tree root (only descendents of this node are printed))
# Print out the subtree assuming it is rooten in the node with type 'node'.
sub _printANodeFromHash
{
	my ($aOut, $nodeMap, $rootId) = @_;
	my $rootNode = $nodeMap->{$rootId};

	# Just print out the node, if there are no children for it.
	unless (exists $rootNode->{'children'} and keys $rootNode->{'children'} > 0)
	{
		printALeaf($aOut, $rootNode->{'aId'}, $rootNode->{'role'},
			$rootNode->{'mId'}, $rootNode->{'ord'}, $rootNode->{'token'});
		return;
	}

	# If there are some children...
	# 1. Start the node.
	printANodeStart($aOut, $rootNode->{'aId'}, $rootNode->{'role'},
		$rootNode->{'mId'}, $rootNode->{'ord'}, $rootNode->{'token'});
	# 2. Print out the children.
	for my $nodeId (keys %{$rootNode->{'children'}})
	{
		_printATreeFromHash($aOut, $nodeMap, $nodeId, 0);
	}
	# 3. End the node.
	printANodeEnd($aOut);
}

1;
