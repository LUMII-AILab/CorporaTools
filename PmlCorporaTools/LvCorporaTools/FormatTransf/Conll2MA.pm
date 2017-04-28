#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::FormatTransf::Conll2MA;

use warnings;
use utf8;
use strict;

use IO::File;
use IO::Dir;
use LvCorporaTools::GenericUtils::SimpleXmlIo;
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

# Corse mapping from UDv2 roles to roles used in Latvian Treebank.
our $udRole2LvtbRole = {
	#TODO for all clauses subrCl + pred?
	'acl' => 'attrCl',
	'advcl' => 'spc', # all kinds of adverbial clauses and several kinds of SPCs
	'advmod' => 'adv',
	'amod' => 'attr',
	'appos' => 'basElem', # TODO: xApp
	'aux' => 'auxVerb', #TODO xPred
	'aux:pass' => 'auxVerb', #TODO xPred
	'case' => 'prep', #TODO xPrep
	'cc' => 'conj', #TODO coordParts ??
	'ccomp' => 'objCl', # subject clause, predicate clause, some kinds of subjects and SPCs.
	'clf' => 'N/A',
	'compound' => 'basElem', #TODO subrAnal/coordAnal + xNum
	'conj' => 'crdPart', #TODO coordParts/coordClauses?
	'cop' => 'auxVerb', #TODO xPred
	'csubj' => 'subjCl',
	'csubj:pass' => 'subjCl',
	'dep' => 'N/A',
	'det' => 'attr',
	'discourse' => 'no', # also insertions and free-use conjunctions
	'dislocated' => 'N/A',
	'expl' => 'N/A',
	'fixed' => 'conj', # a specific case of xSimile only?
	'flat' => 'basElem', #TODO phrasElem/unstruct/interj?
	'flat:foreign' => 'basElem', #TODO unstruct
	'flat:name' => 'basElem', #TODO namedEnt
	'goeswith' => 'N/A',
	'iobj' => 'obj',
	'list' => 'N/A',
	'mark' => 'conj', #TODO subrCl/sent/xSimile?
	'nmod' => 'attr', # also some SPCs
	'nsubj' => 'subj',
	'nsubj:pass' => 'subj',
	'nummod' => 'attr', # also some SPCs
	'obj' => 'obj',
	'obl' => 'spc', # also adverbial modifiers, situants, determinants, SPC subjects. TODO det if dative
	'orphan' => 'subj', # subjects, objects, spcs, subject clauses, object clauses, predicate clauses
	'parataxis' => 'basElem', # TODO coordClauses/insertions/utter?S
	'punct' => 'punct', # TODO pmc?
	'reparandum' => 'N/A',
	'root' => 'pred', # TODO basElem, if not verb #TODO sent/utter
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
	&_printMBegin($mOut, $nameStub);
	my $aOut = IO::File->new("$outDirName/$nameStub.a", '> :encoding(UTF-8)');
	&_printABegin($aOut, $nameStub);

	my $insideOfSent = 0;
	my $paraId = 1;
	my $sentCounter = 0;
	my $wordCounter = 0;
	my @unusedWIds = ();
	my $unusedTokens = '';
	my $unusedConll = '';
	my @unprocessedATokens = ();

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
					&_printANodesFromArray($aOut, \@unprocessedATokens, $conllName)
						if (@unprocessedATokens);
					&_printMSentEnd($mOut);
					&_printASentEnd($aOut);
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
					&_printMSentBegin($mOut, $nameStub, $paraId, $sentCounter);
					&_printASentBegin($aOut, $nameStub, $paraId, $sentCounter);
					@unprocessedATokens = ();
				}
				push @unusedWIds, $wTok->{'id'};
				$unusedTokens = $unusedTokens . $wTok->{'token'}->{'content'};
				$unusedTokens = "$unusedTokens " unless ($wTok->{'no_space_after'});
				$unusedTokens =~ /^\s*(.*?)\s*$/;
				#print 'CoNLL ' . Dumper($conllToken);
				#print 'W-W-W ' . Dumper($unusedTokens);
				if ($1 eq $conllToken)
				{
					$wordCounter++;
					#my $pmlRole = &_transformRole($role, $conllName);
					my $mId = &_getMNodeId($nameStub, $paraId, $sentCounter, $wordCounter);
					my $aId = &_getANodeId($nameStub, $paraId, $sentCounter, $wordCounter);
					&_printMDataNode($mOut, $nameStub, $mId, \@unusedWIds,
						$conllToken, $lemma, $tag);
					#&_printADataSimple($aOut, $aId, $mId, $wordCounter, $conllToken, $pmlRole);
					$unprocessedATokens[$conllId] = {
							'aId' => $aId,
							'mId' => $mId,
							'conllId' => $conllId,
							'ord' => $wordCounter,
							'token' => $conllToken,
							'UD-DEPREL' =>$role,
							'UD-HEAD' => $headId,
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
				&_printMSentBegin($mOut, $nameStub, $paraId, $sentCounter);
				&_printASentBegin($aOut, $nameStub, $paraId, $sentCounter);
				@unprocessedATokens = ();
			}
			$unusedTokens =~ /^\s*(.*?)\s*$/;
			if ($1 eq $conllToken)
			{
				$wordCounter++;
				#my $pmlRole = &_transformRole($role, $conllName);
				my $mId = &_getMNodeId($nameStub, $paraId, $sentCounter, $wordCounter);
				my $aId = &_getANodeId($nameStub, $paraId, $sentCounter, $wordCounter);
				&_printMDataNode($mOut, $nameStub, $mId, \@unusedWIds,
					$conllToken, $lemma, $tag); # ${@unusedWIds}
				#&_printADataSimple($aOut, $aId, $mId, $wordCounter, $conllToken, $pmlRole);
				$unprocessedATokens[$conllId] = {
					'aId' => $aId,
					'mId' => $mId,
					'conllId' => $conllId,
					'ord' => $wordCounter,
					'token' => $conllToken,
					'UD-DEPREL' =>$role,
					'UD-HEAD' => $headId,
				};
				@unusedWIds = ();
				$unusedTokens = '';
				$unusedConll = '';
			}
		}

	}
	if ($insideOfSent)
	{
		&_printANodesFromArray($aOut, \@unprocessedATokens, $conllName)
			if (@unprocessedATokens);
		&_printMSentEnd($mOut);
		&_printASentEnd($aOut);
	}

	&_printMEnd($mOut);
	$mOut->close();
	&_printAEnd($aOut);
	$aOut->close();
}

# Get LVTB role from UD role by applying $udRole2LvtbRole mapping.
# Warn if provided role was not found in the mapping.
sub _transformRole
{
	my ($udRole, $conllName) = @_;
	my $pmlRole = 'N/A';
	if ($udRole and $udRole ne '_')
	{
		$pmlRole = $udRole2LvtbRole->{$udRole};
		unless ($pmlRole)
		{
			warn "File $conllName contains unknown role $udRole.\n";
			return 'N/A';
		}
	}
	return $pmlRole;
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

# &_printANodesFromArray(output stream, list of node data from conll, conll
#                        file name)
# Form a tree structure (and print it out as PML-A) from array containing conll
# data. Each array element is supposed to have following keys: aId, mId,
# conllId (token number from CoNLL file), ord, token (wordform), UD-DEPREL,
# UD-HEAD. conllId is expected to be equal with the index number for that node
# in this array.
sub _printANodesFromArray
{
	my ($aOut, $aNodes, $conllName) = @_;
	my @rootChildren = ();
	# Populate childlist for each node.
	for my $aNode (@$aNodes)
	{
		if ($aNode)
		{
			my $headId = $aNode->{'UD-HEAD'};
			if ($headId and $headId ne '0' and $headId ne '_' and $aNodes->[$headId])
			{
				my @tmpCh = ();
				@tmpCh = @{$aNodes->[$headId]->{'children'}}
					if ($aNodes->[$headId]->{'children'});
				push @tmpCh, $aNode->{'conllId'};
				$aNodes->[$headId]->{'children'} = \@tmpCh;
			}
			else
			{
				push @rootChildren, $aNode->{'conllId'};
			}
		}
	}
	# Print out everything reachable from root.
	for my $rootChild (@rootChildren)
	{
		&_printASubtree($aOut, $aNodes, $aNodes->[$rootChild]->{'conllId'}, $conllName);
	}

	for my $aNode (@$aNodes)
	{
		if ($aNode)
		{
			unless ($aNode->{'printed'})
			{
				&_printASubtree($aOut, $aNodes, $aNode->{'conllId'}, $conllName);
				warn 'Node with CoNLL_ID='.$aNode->{'conllId'}.
						' and PML_A_ID='.$aNode->{'aId'}.
						" in file $conllName was not reachable through DFS; is the CoNLL tree valid?\n";
				$aNode->{'printed'} = 1;
			}
		}

	}

	# Print out each node.
#	for my $aNode (@$aNodes)
#	{
#		if ($aNode)
#		{
#			#print Dumper($aNode->{'children'});
#			my $pmlRole = &_transformRole($aNode->{'UD-DEPREL'}, $conllName);
#			&_printANodeLeaf($aOut, $aNode->{'aId'}, $aNode->{'mId'}, $aNode->{'ord'},
#				$aNode->{'token'}, $pmlRole);
#		}
#	}
}

# &_printASubtree(output stream, list of node data from conll, conll ID for
#                 subtree root,conll file name)
# Form a tree structure (and print it out as PML-A) for a given subroot. All
# nodes' data is obtainend from array containing conll data. Each array element
# is supposed to have following keys: aId, mId, conllId (token number from CoNLL
# file), ord, token (wordform), UD-DEPREL, UD-HEAD, children (list of children
# conllIds. conllId is expected to be equal with the index number for that node
# in this array.
sub _printASubtree
{
	my ($aOut, $aNodesWithChildLists, $conllId, $conllName) = @_;
	my $aNode = $aNodesWithChildLists->[$conllId];
	my $pmlRole = &_transformRole($aNode->{'UD-DEPREL'}, $conllName);
	if ($aNode->{'children'})
	{
		&_printANodeWithChildrenStart($aOut, $aNode->{'aId'}, $aNode->{'mId'}, $aNode->{'ord'},
			$aNode->{'token'}, $pmlRole);
		for my $childConllId (@{$aNode->{'children'}})
		{
			&_printASubtree($aOut, $aNodesWithChildLists, $childConllId, $conllName);
		}
		&_printANodeWithChildrenEnd($aOut);
		$aNode->{'printed'} = 1;
	}
	else
	{
		&_printANodeLeaf($aOut, $aNode->{'aId'}, $aNode->{'mId'}, $aNode->{'ord'},
			$aNode->{'token'}, $pmlRole);
		$aNode->{'printed'} = 1;
	}
}

# Just print stuff in output stream.
sub _printANodeWithChildrenStart
{
	my ($output, $aId, $mId, $ord, $token, $role) = @_;
	$role = 'N/A' unless $role;
	print $output <<END;
						<node id="$aId">\t<!-- $token -->
							<m.rf>m#$mId</m.rf>
							<role>$role</role>
							<ord>$ord</ord>
							<children>
END
}

# Just print stuff in output stream.
sub _printANodeWithChildrenEnd
{
	my $output = shift;
	print $output <<END;
							</children>
						</node>
END
}

# Just print stuff in output stream.
sub _printANodeLeaf
{
	my ($output, $aId, $mId, $ord, $token, $role) = @_;
	$role = 'N/A' unless $role;
	print $output <<END;
						<node id="$aId">\t<!-- $token -->
							<m.rf>m#$mId</m.rf>
							<role>$role</role>
							<ord>$ord</ord>
						</node>
END
}

# Just print stuff in output stream.
sub _printASentBegin
{
	my ($output, $docId, $parId, $sentId) = @_;
	print $output <<END;

		<LM id="a-${docId}-p${parId}s${sentId}">
			<s.rf>m#m-${docId}-p${parId}s${sentId}</s.rf>
			<children>
				<pmcinfo>
					<pmctype>sent</pmctype>
					<children>
END
}

# Just print stuff in output stream.
sub _printASentEnd
{
	my $output = shift;
	print $output <<END;
					</children>
				</pmcinfo>
			</children>
		</LM>
END
}

# Just print stuff in output stream.
sub _printABegin
{
	my ($output, $docId) = @_;
	my $timeNow = localtime time;
	print $output <<END;
<?xml version="1.0" encoding="utf-8"?>

<lvadata xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
	<head>
		<schema href="lvaschema.xml" />
		<references>
			<reffile id="m" name="mdata" href="$docId.m" />
			<reffile id="w" name="wdata" href="$docId.w" />
		</references>
	</head>
	<meta>
		<annotation_info>
			<desc>$progname, $timeNow</desc>
		</annotation_info>
	</meta>

	<trees>
END
}

# Just print stuff in output stream.
sub _printAEnd
{
	my $output = shift @_;
	print $output <<END;
	</trees>
</lvadata>
END
}

# Just print stuff in output stream.
sub _printMDataNode
{
	my ($output, $docId, $mId, $wIds, $token, $lemma, $tag) = @_;
	$lemma = 'N/A' unless ($lemma and $lemma !~ /^\s*$/);
	$tag = 'N/A' unless ($tag and $tag !~ /^\s*$/);
	my $wIdString = '';
	if (@$wIds > 1)
	{
		$wIdString = '<LM>w#' . join('</LM><LM>w#', @$wIds) . '</LM>';
	}
	elsif (@$wIds == 1)
	{
		$wIdString = "w#@$wIds[0]";
	}
	print $output <<END;
		<m id="$mId">
			<src.rf>$docId</src.rf>
END
	if ($wIdString)
	{
		print $output <<END;
			<w.rf>$wIdString</w.rf>
END
	}
	if (@$wIds > 1)
	{
		print $output <<END;
			<form_change>union</form_change>
END
	}
	print $output <<END;
			<form>$token</form>
			<lemma>$lemma</lemma>
			<tag>$tag</tag>
		</m>
END
}

# Just print stuff in output stream.
sub _printMSentBegin
{
	my ($output, $docId, $parId, $sentId) = @_;
	print $output <<END;
	<s id="m-${docId}-p${parId}s${sentId}">
END
}

# Just print stuff in output stream.
sub _printMSentEnd
{
	my $output = shift @_;
	print $output <<END;
	</s>
END
}

# Just print stuff in output stream.
sub _printMBegin
{
	my ($output, $docId) = @_;
	my $timeNow = localtime time;
	print $output <<END;
<?xml version="1.0" encoding="utf-8"?>
<lvmdata xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
	<head>
		<schema href="lvmschema.xml" />
		<references>
			<reffile id="w" name="wdata" href="$docId.w" />
		</references>
	</head>
	<meta>
		<lang>lv</lang>
		<annotation_info id="semi-automatic">$progname,  $timeNow</annotation_info>
	</meta>

END
}

# Just print stuff in output stream.
sub _printMEnd
{
	my $output = shift @_;
	print $output <<END;
</lvmdata>
END
}

1;
