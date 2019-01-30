#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::Conll2MA;

use warnings;
use utf8;
use strict;

use IO::File;
use IO::Dir;
use LvCorporaTools::GenericUtils::SimpleXmlIo qw (loadXml @FORCE_ARRAY_W);
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


our $vers = 0.4;
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
   morphosyntax directory (.conll files)
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
   output directory

Latvian Treebank project, LUMII, 2017-2018, provided under GPL
END
		exit 1;
	}
	my $wDirName = shift;
	my $morphoDirName = shift;
	my $outDirName = shift;

	my $wDir = IO::Dir->new($wDirName) or die "$!";
	mkdir($outDirName);

	my $baddies = 0;

	while (defined(my $inWFile = $wDir->read))
	{
		my $isBad = 0;
		if (! -d "$wDirName/$inWFile")
		{
			my $coreName = $inWFile =~ /^(.*)\.w*$/ ? $1 : $inWFile;
			#&processFileSet("$wDirName/$inWFile", $outDirName, "$morphoDirName/$coreName.conll")
			my $res = eval
            {
            	#local $SIG{__WARN__} = sub { die $_[0] }; # This magic makes eval act as if all warnings were fatal.
            	local $SIG{__WARN__} = sub { $isBad = 1; warn $_[0] }; # This magic makes eval count warnings.
				&processFileSet("$wDirName/$inWFile", $outDirName, "$morphoDirName/$coreName.conll")
            };
            if ($@ or ! defined $res)
            {
            	$isBad = 1;
            	print $@;
            }
		}
		$baddies = $baddies + $isBad;
	}
	if ($baddies)
    {
    	print "$baddies files failed.\n";
    }
    else
    {
    	print "All finished.\n";
    }
    return $baddies;

}

sub processFileSet
{
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for creating PML M and A files, if  w file and (optional) CoNLL file are
provided. Currently morphology is mandatory, syntax is optional. All input files
must be UTF-8. Fileset must have the same text in both W and CoNLL file. Tokens
in W must be either the same or substrings of tokens in CoNLL.

Params:
   w file name
   output folder
   .conll file name [opt, w-file path + name-stub + .conll used otherwise]
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

	my $wName = shift @_;
	my $nameStub = ($wName =~ /^(.*[\\\/](.*?))(\.w)?$/ ? $2 : $wName);
	my $outDirName = shift @_;
	my $conllName = (shift @_ or "$1.conll");

	my $w = loadXml($wName, \@FORCE_ARRAY_W);
	my $conllIn = IO::File->new($conllName, '< :encoding(UTF-8)')
		or die "Could not open file $conllName: $!";

	my $mOut = IO::File->new("$outDirName/$nameStub.m", '> :encoding(UTF-8)')
		or die "Could not initiate $outDirName/$nameStub.m for writing: $!";
	my $timeNow = localtime time;
	printMFileBegin($mOut, $nameStub, "$progname,  $timeNow");
	my $aOut = IO::File->new("$outDirName/$nameStub.a", '> :encoding(UTF-8)');
	printAFileBegin($aOut, $nameStub, "$progname,  $timeNow");

	my %status = (
		#'paraId' => 1,
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
		my $paraIdString = $wPara->{'id'};
		$paraIdString =~ /^w-(.*-p(\d+))$/;
		#$status{'paraId'} = $2;
		$status{'paraIdStub'} = $1;

		for my $wTok (@{$wPara->{w}})
		{
			# Get the next conll line to use - either the unused one from
			# previous loop or read a new one.
			my ($line, $mustEndSentence) = &_getNextConllContentLine(\%status, $conllIn);
			&_endSentence(\%status, $mOut, $aOut, $conllName)
				if ($mustEndSentence);
			&_doOneTokenOrLine(\%status, $line, $nameStub, $mOut, $wTok);
		}
	}

	# Process unused CoNLL lines in the end of the file and warn
	my ($line, $mustEndSentence) = &_getNextConllContentLine(\%status, $conllIn);
	&_endSentence(\%status, $mOut, $aOut, $conllName)
		if ($mustEndSentence);
	#$status{'paraId'}++;

	#if ($line and $status{'unusedTokens'})
	if ($line or $status{'unusedTokens'})
	{
		warn "Can't match ".$status{'unusedTokens'}." from dataset $nameStub with CoNLL! Maybe tokenization is weird?"
	}
#	else
#	{
#		while ($line)
#		{
#			&_doOneTokenOrLine(\%status, $line, $nameStub, $mOut);
#			($line, $mustEndSentence) = &_getNextConllContentLine(\%status, $conllIn);
#			&_endSentence(\%status, $mOut, $aOut, $conllName)
#				if ($mustEndSentence);
#		}
#
#		# Warn about spare w nodes
#		warn "W tokens ".$status{'unusedTokens'}." from dataset $nameStub found unused after the end of paragraph!"
#			if ($status{'unusedTokens'});
#	}

	&_endSentence(\%status, $mOut, $aOut, $conllName)
		if ($status{'isInsideOfSentence'});

	printMFileEnd($mOut);
	$mOut->close();
	printAFileEnd($aOut);
	$aOut->close();
}

# _getNextConllContentLine(hash with file processing status variables, CoNLL
#                          input flow)
# Finds the next CoNLL line to be processed - either $status->{'unusedConll'}
# or next nonempty line, if there is one.
# Returns the found line and flag, if &_endSentence should be called.
sub _getNextConllContentLine
{
	my ($status, $conllIn) = @_;
	# Read a new CoNLL line, if previous one has been used.
	my $line = ($status->{'unusedConll'} or <$conllIn>);
	my $mustEndSentence = 0;
	# Process empty lines, if there any.
	while ($line and $line !~ /^(\d+)\t([\S ]+)\t([\S ]+)\t(\S+)\t(\S+)\s/)
	{
		$mustEndSentence = 1 if ($status->{'isInsideOfSentence'});
		$status->{'unusedConll'} = '';
		$line = <$conllIn>;
	}
	return ($line, $mustEndSentence);
}

# _startSentence(hash with file processing status variables, PML-M output flow)
# Writes sentence begining in the PML-M flow and resets status variables
# appropriately.
sub _startSentence
{
	my ($status, $mOut) = @_;
	$status->{'isInsideOfSentence'} = 1;
	$status->{'sentenceCounter'}++;
	$status->{'sentIdStub'} = $status->{'paraIdStub'};
	my $mSentId = &_getMSentIdFromStub($status->{'sentIdStub'}, $status->{'sentenceCounter'});
	printMSentBegin($mOut, $mSentId);
	$status->{'unprocessedATokens'} = [];
}

# _endSentence(hash with file processing status variables, PML-M output flow,
#              PML-A output flow, CoNLL file name for error reporting)
# Transforms previously collected CoNLL data to PML-A tree, writes it into PML-A
# flow. Then writes sentence ending in both PML-M and PML-A flow and resets
# status variables appropriately.
sub _endSentence
{
	my ($status, $mOut, $aOut, $conllName) = @_;
	if (@{$status->{'unprocessedATokens'}})
	{
		my $aSentId = &_getASentIdFromStub($status->{'sentIdStub'}, $status->{'sentenceCounter'});
		my $mSentId = &_getMSentIdFromStub($status->{'sentIdStub'}, $status->{'sentenceCounter'});
		my $nodeMap = buildATreeFromConllArray($status->{'unprocessedATokens'}, $aSentId, $mSentId, $conllName);
		#use Data::Dumper;
		#print Dumper($nodeMap);
		&_printATreeFromHash($aOut, $nodeMap, $aSentId, $status->{'isFirstTree'});
		$status->{'isFirstTree'} = 0;
	}

	printMSentEnd($mOut);
	$status->{'isInsideOfSentence'} = 0;
	$status->{'wordCounter'} = 0;
	delete $status->{'sentIdStub'};
}

# _doOneTokenOrLine(hash with file processing status variables, optional CoNLL
#                   line to process, PML dateset name, PML-M output flow,
#                   optional PML-W node)
# Process the given CoNLL line and PML-W node. If everything fits nicely, a
# PML-M node is printed out and data for PML-A tree is added to the status hash.
# Otherwise the available data is put in the status hash for later use.
sub _doOneTokenOrLine
{
	my ($status, $conllLine, $nameStub, $mOut, $wNode) = @_;

	# Preprocess given w node - add its contents as yet to be processed.
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
	return unless ($conllLine and $conllLine =~ /^(\d+)\t([\S ]+)\t([\S ]+)\t(\S+)\t(\S+)(?:\t(\S+)\t(\S+)\t(\S+))?\s/);

	my ($conllId, $conllToken, $lemma, $tag, $headId, $role) = ($1, $2, $3, $5, $7, $8);
	#$conllToken =~ s/_/ /g;
	#$lemma =~ s/_/ /g;
	# This usually happens if at the end of the file something is missing,
	# or some kind of mismatch has happened.
	warn "CoNLL token $conllToken from dataset $nameStub found unused after the end of paragraph!"
		if ($conllToken and not $wNode);

	# Start sentence if needed.
	&_startSentence ($status, $mOut)
		unless ($status->{'isInsideOfSentence'});

	$status->{'unusedTokens'} =~ /^\s*(.*?)\s*$/;
	if ($1 eq $conllToken or (not $wNode and not $status->{'unusedTokens'}))
	{
		# If conll token and unused w token matches, print next PML node.
		$status->{'wordCounter'}++;
		my $mId = &_getMNodeIdFromStub($status->{'sentIdStub'}, $status->{'sentenceCounter'}, $status->{'wordCounter'});
		my $aId = &_getANodeIdFromStub($status->{'sentIdStub'}, $status->{'sentenceCounter'}, $status->{'wordCounter'});
		printMDataNode($mOut, $mId, $status->{'unusedWIds'}, $conllToken, $lemma, $tag);
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

# Form an ID for a-node by using given ID stub, sentence number and token number.
sub _getANodeIdFromStub
{
	my ($stub, $sentId, $tokId) = @_;
	return "a-${stub}s${sentId}w$tokId";
}

# Form an ID for m-node by using given ID stub, sentence number and token number.
sub _getMNodeIdFromStub
{
	my ($stub, $sentId, $tokId) = @_;
	return "m-${stub}s${sentId}w$tokId";
}

# Form an ID for a-root by using given ID stub, sentence number and token number.
# To get stub for x node, just add 'x' to the end.
sub _getASentIdFromStub
{
	my ($stub, $sentId) = @_;
	return "a-${stub}s${sentId}";
}

# Form an ID for m-root by using given ID stub, sentence number and token number.
sub _getMSentIdFromStub
{
	my ($stub, $sentId) = @_;
	return "m-${stub}s${sentId}";
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
# Print out the subtree assuming it is PMC in the node with type 'root'.
sub _printARootFromHash
{
	my ($aOut, $nodeMap, $rootId, $isFirst) = @_;
	my $rootNode = $nodeMap->{$rootId};
	#my $parentType = 0;
	#$parentType = $nodeMap->{$rootNode->{'parent'}}->{'nodeType'}
	#	if (exists $rootNode->{'parent'} and exists $nodeMap->{$rootNode->{'parent'}});

	# 1. Start sentence enclosing part.
	printASentBegin($aOut, $rootNode->{'aId'}, $rootNode->{'mId'}, $isFirst ? $firstSentComment : 0);
	if (exists $rootNode->{'children'} and (keys %{$rootNode->{'children'}}) > 0)
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
	if (exists $rootNode->{'children'} and (keys %{$rootNode->{'children'}}) > 0)
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
	if (exists $rootNode->{'children'} and (keys %{$rootNode->{'children'}}) > 0)
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
	unless (exists $rootNode->{'children'} and (keys %{$rootNode->{'children'}}) > 0)
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
