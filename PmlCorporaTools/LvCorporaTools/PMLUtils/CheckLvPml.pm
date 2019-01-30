#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::PMLUtils::CheckLvPml;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(checkLvPml processDir);

use Data::Dumper;
use IO::File;
use IO::Dir;
use File::Path;
use List::Util qw(first);
use XML::Simple;  # XML handling library

use LvCorporaTools::GenericUtils::SimpleXmlIo
	qw(loadXml @FORCE_ARRAY_W @FORCE_ARRAY_M @FORCE_ARRAY_A @LOAD_AS_ID);

###############################################################################
# This program checks the given PML dataset for following things:
#	* IDs from w file that are not refferenced in m file;
#	* IDs from m file that are not refferenced in a file (morphemes with
#	  "deleted" element are listed separately from others);
#	* IDs from m file linking to non-existing IDs in w file;
#	* IDs from a file linking to non-existing IDs or elements marked for
#	  deletion in m file;
#	* trees in a file not corresponding to single sentence in m file;
#	* (WIP) m-level token order must be the same as a-level;
#	* inserted punctuation must have "form_change" "punct";
#	* multi-token m has "form_change" "union";
#	* m with no reference to w has "form_change" "insert";
#	* m with form different from what described in w has at least one more
#	  "form_change";
#   * multiple m refering to single w is error.
# Refferences to multiple files not supported. ID duplication are not checked
# (TODO).
#
# Input files - utf8.
# Output file - list with problematic IDs.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2011-2013
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

# Perform error-chacking in multiple datasets. This can be used as entry point,
# if this module is used standalone.
sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 1)
	{
		print <<END;
Script for checking references in the given PML datasets (all .w + .m + .a in
the given folder) for following erorrs:
* IDs from w file that are not refferenced in m file;
* IDs from m file that are not refferenced in a file (morphemes with "deleted"
  element are listed separately from others);
* IDs from m file linking to non-existing IDs in w file;
* IDs from a file linking to non-existing IDs in m file;
* trees in a file not corresponding to single sentence in m file;
* (WIP) m-level token order must be the same as a-level;
* inserted punctuation must have "form_change" "punct";
* multi-token m having no "form_change" "union";
* m with no reference to w having no "form_change" "insert";
* m with form different from what described in w must have at least one more
  "form_change";
* IDs should not be duplicated (TODO: represent it properly in the logfile);
* multiple m refering to single w are considered to be error.


Params:
   data directory
   file type [M (.m + .w files will be checked) / A (.a + .m + .w files will
   be checked)]
Returns:
   count of failed files

Latvian Treebank project, LUMII, 2017, provided under GPL
END
		exit 1;
	}

	my $dirName = shift @_;
	my $mode = shift @_;
	my $dir = IO::Dir->new($dirName) or die "dir $!";

	my $problems = 0;

	while (defined(my $inFile = $dir->read))
	{
		if ((! -d "$dirName/$inFile") and ($inFile =~ /^(.+)\.w$/))
		{
			my $res = eval
			{
				local $SIG{__WARN__} = sub { die $_[0] }; # This magic makes eval act as if all warnings were fatal.
				checkLvPml ($dirName, $1, $mode, "$1-errors.txt");
			};
			if ($@)
			{
				$problems++;
				print $@;
			} elsif ($res)
			{
				$problems++;
			}
		}
	}

	if ($problems)
	{
		print "$problems files failed.\n";
	}
	else
	{
		print "All finished.\n";
	}
	return $problems;
}

# Perform error-chacking in single dataset. This can be used as entry point, if
# this module is used standalone.
sub checkLvPml
{

	autoflush STDOUT 1;
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for checking references in the given PML dataset (.w + .m + .a) for
following erorrs:
* IDs from w file that are not refferenced in m file;
* IDs from m file that are not refferenced in a file (morphemes with "deleted"
  element are listed separately from others);
* IDs from m file linking to non-existing IDs in w file;
* IDs from a file linking to non-existing IDs in m file;
* trees in a file not corresponding to single sentence in m file;
* (WIP) m-level token order must be the same as a-level;
* inserted punctuation must have "form_change" "punct";
* multi-token m having no "form_change" "union";
* m with no reference to w having no "form_change" "insert";
* m with form different from what described in w must have at least one more
  "form_change";
* duplicate IDs (onscreen warning only);
* multiple m refering to single w are considered to be error.

Params:
   directory prefix
   file name without extension
   file type [M (.m + .w files will be checked) / A (.a + .m + .w files will
   be checked)]
   output file name [opt, "file_name-errors.txt" used otherwise]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $inputName = shift @_;
	my $mode = shift @_;
	my $resName = (shift @_ or "$inputName-errors.txt");
	
	die "Invalid file processing type $mode, use A/a/M/m instead.\n"
		unless ($mode =~ /^[aAmM]$/);

	# Load PML data.
	my $wData = &_loadW($dirPrefix, $inputName);
	print 'Parsed W';
	my $mData = &_loadM($dirPrefix, $inputName);
	print ', M';
	my $aData;
	if ($mode =~ /^[aA]$/)
	{
		$aData = &_loadA($dirPrefix, $inputName);
		print ', A';
	}
	print ".\n";
	my $out = IO::File->new("$dirPrefix\\$resName", "> :encoding(UTF-8)")
		or die "Error while processing $inputName - can't create $resName: $!";

	# Test conformity of w and m file.
	my $problems = &_testMW({%$wData, %$mData}, $out);
	
	if ($mode =~ /^[aA]$/)
	{
		# Test conformity of m and a file. 
		$problems = $problems + &_testAM({%$mData, %$aData}, $out);
	}
	
	$out->close;
	if ($problems)
	{
		print "CheckIds has finished procesing \"$inputName\" with $problems errors.\n";
	}
	else
	{
		print "CheckIds has finished procesing \"$inputName\" successfully.\n";
	}
	return $problems;
}

# _testMW (data hashmap with keys 'w2token', 'm2w', 'w2m'; something where to
#		   print output, e.g., opened file)
# Checks errors related to links between w and m file.
sub _testMW
{
	my $data = shift @_;
	my $out = shift @_;

	my $problems = 0;
	my $badIds = &_findUnusedIds($data->{'w2token'}, $data->{'w2m'});
	#print 'Found '.scalar @$badIds." w ID(s) never referenced in m file.\n";
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "W IDs never referenced in m file:\n";
		print $out join("\n", @$badIds);
	}
	
	$badIds = &_findUnusedIds($data->{'w2m'}, $data->{'w2token'});
	#print 'Found '.scalar @$badIds." non-existing w reference(s) in m file.\n";
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nNon-existing w references in m file:\n";
		print $out join("\n", @$badIds);
	}
	
	$badIds = &_checkFormChange($data->{'m2w'}, $data->{'w2m'}, $data->{'w2token'});
	#print 'Found '.scalar @$badIds." m node(s) whose \'form_change\' must be checked.\n";
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nM nodes with incomplete \'form_change\':\n";
		print $out join("\n", @$badIds);
	}

	$badIds = &_checkSeq($data->{'wSeq'}, $data->{'mSeq'}, $data->{'w2m'}, 1);
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nM nodes occouring out of order:\n";
		print $out join("\n", @$badIds);
	}

	return $problems;
}

# _testAM (data hashmap with keys 'tree2node', 'node2tree', 'tree2sent',
#		   'sent2tree', 'node2m', 'm2node'; something where to print output,
#		   e.g., opened file)
# Checks errors related to links between m and a file.
sub _testAM
{
	my $data = shift @_;
	my $out = shift @_;
	
	my $badIds = &_findUnusedIds($data->{'m2w'}, $data->{'m2node'});
	my @notDel = grep {not $data->{'m2w'}->{$_}->{'deleted'}} @$badIds;
	#print 'Found '.scalar @$badIds.' m ID(s) never referenced in a file, '.
	#	(+@$badIds - @notDel)." of them are marked for deletion.\n";
	my $problems = scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nM IDs never referenced in a file:\n";
		print $out join("\n", @notDel);
		my @del = grep {$data->{'m2w'}->{$_}->{'deleted'}} @$badIds;
		if (scalar @del)
		{
			print $out "\nmarked for deletion:\n";
			print $out join("\n", @del);
		}
	}

	$badIds = &_findUnusedIds($data->{'m2node'}, $data->{'m2w'});
	#print 'Found '.scalar @$badIds." non-existing m reference(s) in a file.\n";
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nNon-existing m references in a file:\n";
		print $out join("\n", @$badIds);
	}

	@$badIds = grep {$data->{'m2w'}->{$_}->{'deleted'}} (values %{$data->{'node2m'}});
	#print 'Found '.scalar @$badIds." m element(s) marked for deletion, but used in a file.\n";
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nM elements marked for deletion, but used in a file:\n";
		print $out join("\n", @$badIds);
	}

	$badIds = &_findUnusedIds($data->{'sent2m'}, $data->{'sent2tree'});
	#print 'Found '.scalar @$badIds." s ID(s) never referenced in a file.\n";
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nS IDs never referenced in a file:\n";
		print $out join("\n", @$badIds);
	}

	$badIds = &_findUnusedIds($data->{'sent2tree'}, $data->{'sent2m'});
	#print 'Found '.scalar @$badIds." non-existing s reference(s) in a file.\n";
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nNon-existing s references in a file:\n";
		print $out join("\n", @$badIds);
	}

	$badIds = &_validateSentBound(
		$data->{'sent2tree'}, $data->{'m2node'}, $data->{'sent2m'}, $data->{'node2tree'});
	#print 'Found '.scalar @$badIds." m node(s) not reffered to in coressponding tree.\n";
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nM nodes not reffered from coressponding tree:\n";
		print $out join("\n", @$badIds);
	}

	$badIds = &_validateSentBound(
		$data->{'tree2sent'}, $data->{'node2m'}, $data->{'tree2node'}, $data->{'m2sent'});
	#print 'Found '.scalar @$badIds." a node(s) not reffered to in coressponding sentence.\n";
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nA nodes not reffered from coressponding sentence:\n";
		print $out join("\n", @$badIds);
	}

	$badIds = &_checkSeq($data->{'sentSeq'}, $data->{'treeSeq'}, $data->{'sent2tree'}, );
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nA trees occouring out of order:\n";
		print $out join("\n", @$badIds);
	}

	$badIds = &_checkSeq($data->{'mSeq'}, $data->{'nodeSeq'}, $data->{'m2node'}, );
	$problems = $problems + scalar @$badIds;
	if (scalar @$badIds)
	{
		print $out "\n\nA nodes occouring out of order:\n";
		print $out join("\n", @$badIds);
	}

	return $problems;
}

# _loadW (source directory, file name without extension)
# returns hash refernece:
#		'wSeq' => array with w IDs in the order as they appear in source XML
#				 (source: w layer),
#		'w2token' => hash from w IDs to tokens and spaces (source: w layer).
# see &loadXML
sub _loadW
{
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $inputName = shift @_;

	# Load w-file.
	my $w = loadXml ("$dirPrefix\\$inputName.w", \@FORCE_ARRAY_W,);
	#print Dumper($w->{'xml'}->{'doc'});

	# Token IDs by order;
	my @wIdOrder = ();
	# Maping from token IDs to tokens.
	my %wIdsTokens = ();
	for my $para ( @{$w->{'xml'}->{'doc'}->{'para'}})
	{
		%wIdsTokens = (%wIdsTokens, map {
			my $tok = $_->{'token'}->{'content'};
			$tok .= ' ' unless ($_->{'no_space_after'}->{'content'});
			$_->{'id'} => $tok} (@{$para->{'w'}}));
		@wIdOrder = (@wIdOrder, map {$_->{'id'}} (@{$para->{'w'}}));
	}
	return {
		'wSeq' => \@wIdOrder,
		'w2token' => \%wIdsTokens,
	};
}

# _loadM (source directory, file name without extension)
# returns hash refernece:
#		'sentSeq' => array with sentence IDs in the order as they appear in
#					 source XML (source: m layer),
#		'mSeq' => array with m IDs in the order as they appear in source XML
#				 (source: m layer),
#		'm2w' => hash from m IDs to lists of w IDs, deletion marks, and lists
#				 of form changes (source: m layer),
#		'w2m' => hash from w IDs to lists of m IDs (source: m layer),
#		'sent2m' => hash from sentence IDs to lists of m IDs ordered in the
#					order they appear in source XML (source: m layer),
#		'm2sent' => hash from m IDs to sentence IDs (source: m layer).
# see &loadXML
sub _loadM
{
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $inputName = shift @_;

	# Load m-file.
	my $m = loadXml ("$dirPrefix\\$inputName.m", \@FORCE_ARRAY_M);

	# Map sentence IDs to lists of morpheme IDs.
	my %mSent2morpho = ();
	# Array showing sentence order.
	my @mSentSeq = ();
	for my $sent (@{$m->{'xml'}->{'s'}})
	{
		my @valArr = map {$_->{'id'}} (@{$sent->{'m'}});
		%mSent2morpho = (%mSent2morpho, $sent->{'id'} => \@valArr);
		@mSentSeq = (@mSentSeq, $sent->{'id'});
	}
	
	# Map morpheme IDs to sentence IDs.
	my %morpho2mSent = ();
	for my $sent (keys %mSent2morpho)
	{
		my $morphos = $mSent2morpho{$sent};
		%morpho2mSent = (%morpho2mSent, map {$_ => $sent} @$morphos)
			if (defined $morphos);
	}
	
	# Map morpheme IDs to fom changes and lists of token IDs.
	my %m2w = ();
	for my $sent (@{$m->{'xml'}->{'s'}})
	{
		for my $thisM (@{$sent->{'m'}})
		{
			my @ref = ();
			@ref = ($thisM->{'w.rf'}->{'content'})
				if (defined $thisM->{'w.rf'}->{'content'});
			@ref = map {$_->{'content'}} (@{$thisM->{'w.rf'}->{'LM'}})
				if (defined $thisM->{'w.rf'}->{'LM'});
			@ref = map {$_ =~ /^w#(.*)$/; $1} @ref;
			my @change = ();
			@change = ($thisM->{'form_change'}->{'content'})
				if (defined $thisM->{'form_change'}->{'content'});
			@change = map {$_->{'content'}} (@{$thisM->{'form_change'}->{'LM'}})
				if (defined $thisM->{'form_change'}->{'LM'});
			my $del = undef;
			$del = $thisM->{'deleted'}->{'content'}
				if (defined $thisM->{'deleted'}->{'content'});
			%m2w = (%m2w,
				$thisM->{'id'} => {
					'rf' => @ref ? \@ref : undef,
					'deleted' => $del,
					'form_change' => @change ? \@change : undef,
					'form' => $thisM->{'form'}->{'content'},
					'tag' => $thisM->{'tag'}->{'content'},
				});
		}
	}

	# Array showing morpheme order.
	my $mSeq = &_simpleSeqMaker(\@mSentSeq, \%mSent2morpho);

	# Map token IDs to morpheme IDs.	
	my %w2m = ();
	for my $morpho (@$mSeq) # Correct order is important here!
	{
		my $refs = $m2w{$morpho}->{'rf'};
		%w2m = (%w2m, map {$_ => [$w2m{$_} ? @{$w2m{$_}} : (), $morpho]} @$refs)
			if (defined $refs);
	}

	return {
		'sentSeq' => \@mSentSeq,
		'mSeq' => $mSeq,
		'm2w' => \%m2w,
		'w2m' => \%w2m,
		'sent2m' => \%mSent2morpho,
		'm2sent' => \%morpho2mSent,
	};
}


# _loadA (source directory, file name without extension)
# returns hash refernece:
#		'treeSeq' => array with tree IDs in the order as they appear in source
#					 XML (source: a layer),
#		'nodeSeq' => array with the node IDs ordered by sentence and then by
#					 their 'ord' value(source: a layer),
#		'tree2node' => hash from tree IDs to lists of node IDs (source: a layer),
#		'node2tree' => hash from node IDs to tree IDs (source: a layer),
#		'tree2sent' => hash from tree IDs to sentence IDs (source: a layer),
#		'sent2tree' => hash from sentence IDs to tree IDs (source: a layer),
#		'node2m' => hash from node IDs to m IDs (source: a layer),
#		'm2node' => hash from m IDs to node IDs (source: a layer).
# see &loadXML
sub _loadA
{
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $inputName = shift @_;

	# Load the a-file.
	my $a = loadXml ("$dirPrefix\\$inputName.a", \@FORCE_ARRAY_A,);

	my @treeSeq = ();
	my %tree2node = ();
	my %tree2mSent = ();
	my %node2morpho = ();
	my %node2ord = ();
	# Process each tree.
	for my $tree (@{$a->{'xml'}->{'trees'}->{'LM'}})
	{
		# Shortcut: current tree's ID.
		my $treeId = $tree->{'id'};
		@treeSeq = (@treeSeq, $treeId);

		# Map tree ID to sentence ID.
		$tree->{'s.rf'}->{'content'} =~ /^m#(.*)$/;
		$tree2mSent{$treeId} = $1;
		
		# Traverse tree and collect all nodes with links to morphology.
		my @todoNodes = ();
		@todoNodes = @{$tree->{'children'}->{'node'}} if ($tree->{'children'}->{'node'});
		@todoNodes = (@todoNodes, @{$tree->{'children'}->{'pmcinfo'}->{'children'}->{'node'}})
			if ($tree->{'children'}->{'pmcinfo'});
		@todoNodes = (@todoNodes, @{$tree->{'children'}->{'coordinfo'}->{'children'}->{'node'}})
			if ($tree->{'children'}->{'coordinfo'});
		@todoNodes = (@todoNodes, %{$tree->{'children'}->{'xinfo'}->{'children'}->{'node'}})
			if ($tree->{'children'}->{'xinfo'});
		
		while (@todoNodes)
		{
			my $aNode = shift @todoNodes;
			my $aNodeId = $aNode->{'id'};

			# Update result data structutures.
			if ($aNode->{'m.rf'}->{'content'})
			{
				# Map node ID to morpheme IDs.
				$aNode->{'m.rf'}->{'content'} =~ /^m#(.*)$/;
				$node2morpho{$aNodeId} = $1;
				# Add node ID to list to which tree ID maps to.	
				$tree2node{$treeId} = [] unless ($tree2node{$treeId});
				push @{$tree2node{$treeId}}, $aNodeId;
			}
			$node2ord{$aNodeId} = $aNode->{'ord'}->{'content'};

			# Add children nodes to hashmap containing nodes yet to be
			# processed.
			if ($aNode->{'children'})
			{
				@todoNodes = (@todoNodes, @{$aNode->{'children'}->{'node'}})
					if ($aNode->{'children'}->{'node'});
				@todoNodes = (@todoNodes,
					@{$aNode->{'children'}->{'pmcinfo'}->{'children'}->{'node'}})
					if ($aNode->{'children'}->{'pmcinfo'});
				@todoNodes = (@todoNodes,
					@{$aNode->{'children'}->{'coordinfo'}->{'children'}->{'node'}})
					if ($aNode->{'children'}->{'coordinfo'});
				@todoNodes = (@todoNodes,
					@{$aNode->{'children'}->{'xinfo'}->{'children'}->{'node'}})
					if ($aNode->{'children'}->{'xinfo'});
			}
		}
	}
	
	# Map node IDs to tree IDs.	
	my %node2tree = ();
	for my $tree (keys %tree2node)
	{
		my $nodes = $tree2node{$tree};
		%node2tree = (%node2tree, map {$_ => $tree} @$nodes)
			if (defined $nodes);
	}
	
	# Map sentence IDs to tree IDs.	
	my %mSent2tree = map {$tree2mSent{$_} => $_} (keys %tree2mSent);
	
	# Map morpheme IDs to node IDs.	
	my %morpho2node = map {$node2morpho{$_} => $_} (keys %node2morpho);

	return {
		'treeSeq'   => \@treeSeq,
		'nodeSeq'   => &_seqMakerExternalOrds(\@treeSeq, \%tree2node, \%node2ord),
		'tree2node' => \%tree2node,
		'node2tree' => \%node2tree,
		'tree2sent' => \%tree2mSent,
		'sent2tree' => \%mSent2tree,
		'node2m'    => \%node2morpho,
		'm2node'    => \%morpho2node,
		#'node2ord'  => \%node2ord,	# hash from node IDs to node ord value (source: a layer).
	};
}

# _validateSentBound(upper level mapping, lower level mapping, source hash
#					 (upper to multiple lower), target hash (lower to single
#					 upper)
# returns: array with source elements (values) counterparts of which do not
#		   mapped to corresponding uper level element in the target map.
sub _validateSentBound
{
	my $sentMap = shift @_;
	my $elemMap = shift @_;
	my $source = shift @_;
	my $target = shift @_;
	
	my @res = ();

	for my $sourceId (keys %{$sentMap})
	{
		for my $elemId (@{$source->{$sourceId}})
		{
			push (@res, $elemId)
				unless ($elemMap->{$elemId} and $target->{$elemMap->{$elemId}}
					and $sentMap->{$sourceId} and
					($target->{$elemMap->{$elemId}} eq $sentMap->{$sourceId}));
		}
	}
	return \@res;
}

# _checkMSeq (lower level key sequence, upper level key sequence,  mapping from
#			  lower level to upper level(can be array or single value), can
#			  multiple upper elements use the same lower element position
#			 (optional, false by default))
# returns: array with upper level keys that distrupt the ascending order.
sub _checkSeq
{
	my $lowerSeq = shift @_;
	my $upperSeq = shift @_;
	my $lower2upper = shift @_;
	my $isEqAllowed = (shift @_ or 0);
	my @res = ();
	my $previos = -1;
	for my $lowerId (@$lowerSeq)
	{
		my $upperId = $lower2upper->{$lowerId};
		next unless ($upperId); # This kind of errors is checked elsewhere.
		my @upperIds = ref $upperId ? @$upperId : ($upperId);
		for my $upperId (@upperIds)
		{
			my $upperPos = first {$upperSeq->[$_] eq $upperId } 0..$#$upperSeq;
			push (@res, $upperId)
				if ($upperPos <= $previos and not $isEqAllowed or $upperPos < $previos);
			$previos = $upperPos;
		}
	}
	return \@res;
}

# _findUnusedIds(source hash, target hash)
# returns: array with keys from source not found int target hash.
sub _findUnusedIds
{
	my $source = shift @_;
	my $target = shift @_;
	
	my @res = ();
	
	for my $id (keys %{$source})
	{
		push (@res, $id) unless (defined $target->{$id});
	}
	
	return \@res;
}

# _checkFormChange(source hash, target hash)
# returns: array with m IDs whose 'form_change' field should be checked.
sub _checkFormChange
{
	my $m2w = shift @_;
	my $w2m = shift @_;
	my $w2token = shift @_;
	
	my @res = ();

	for my $m (keys %$m2w)
	{
		my $v = $m2w->{$m};
		if (not defined $v->{'rf'} or @{$v->{'rf'}} == 0 ) # Verify m with no 'rf'.
		{
			my $containsIns = 0;
			my $containsPunct = 0;
			for my $change (@{$v->{'form_change'}})
			{
				$containsIns = 1 if ($change eq 'insert');
				$containsPunct = 1 if ($change eq 'punct');
			}
			my $problem = 0;
			#eval
			#{
			#	local $SIG{__WARN__} = sub { $problem = 1; warn $_[0] }; # This magic makes eval count warnings.
			push @res,
				$m unless ($containsIns && ($containsPunct || $v->{'tag'} !~ /^z/ ));
			#};
			#print Dumper(($m => $v)) if ($problem);
			#print  if ($problem);
		}
		elsif (@{$v->{'rf'}} == 1) 	# Verify m with single 'rf'.
		{
			my $wId = $v->{'rf'}[0];
			my $tok = defined($w2token->{$wId}) ? $w2token->{$wId} : '';
			$tok =~ /^\s*(.*?)\s*$/;
			push @res, $m
				unless ($1 eq $v->{'form'} or 
					($v->{'form_change'} and @{$v->{'form_change'}} > 0));
		} else					# Verify m with multiple 'rf'.
		{
			my $containsUni = 0;
			my $containsPunct = 0;
			for my $change (@{$v->{'form_change'}})
			{
				$containsUni = 1 if ($change eq 'union');
				$containsPunct = 1 if ($change eq 'punct');
			}
			my $tok = join '', (map {defined($w2token->{$_}) ? $w2token->{$_} : ''} @{$v->{'rf'}});
			$tok =~ s/^\s*(.*?)\s*$/$1/;
			my $form = $v->{'form'};
			push @res, $m
				if (!$containsUni or
					($tok ne $form and @{$v->{'form_change'}} < 2) or
					(!$containsPunct and $tok =~ /([.,!?()]+\Q$form\E[.,!?()]*|[.,!?()]*\Q$form\E[.,!?()]+)/) or
					($containsPunct and $tok !~ /\p{P}/ and $form !~ /\p{P}/)); # "punct" label should be used only in relation to punctuation marks, not average words.

		}
	}
	
	# Verify cases when mutiple m reference single w.
	for my $w (keys %$w2m)
	{
		# Previously
		if (@{$w2m->{$w}} > 1)
		{
			# Previously we requested just to have 'spacing' form_change.
			# Changed as an always error in 2018-02-07.
			for my $m (@{$w2m->{$w}})
			{
#				my $v = $m2w->{$m};
#				my $contains = 0;
#				for my $change (@{$v->{'form_change'}})
#				{
#					$contains = 1 if ($change eq 'spacing');
#				}
#				push @res, $m unless ($contains);
				push @res, $m;
			}
		}
	}
	
	return \@res;
}

# _simpleSeqMaker (array with upper-level ID ordering, hash from upper-level
#				   IDs to ordered lower-level IDs)
# returns: array with accordingly ordered lower-level IDs.
sub _simpleSeqMaker
{
	my $upperOrdering = shift @_;
	my $lowerOrdering = shift @_;
	my @res = ();

	for my $upId (@$upperOrdering)
	{
		@res = (@res, @{$lowerOrdering->{$upId}});
	}
	return \@res;
}

# _seqMakerExternalOrds (array with upper-level ID ordering, hash from
#						 upper-level IDs to lower-level IDs, hash from
#						 lower-level IDs to their ordering)
# returns: array with accordingly ordered lower-level IDs.
sub _seqMakerExternalOrds
{
	my $upperOrdering = shift @_;
	my $upper2Lower = shift @_;
	my $lowerOrdering = shift @_;
	my @res = ();

	for my $upId (@$upperOrdering)
	{
		my @subResult = sort {
				$lowerOrdering->{$a} <=> $lowerOrdering->{$b}
			} (@{$upper2Lower->{$upId}});
		@res = (@res, @subResult);
	}
	return \@res;
}
1;
