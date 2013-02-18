#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::PMLUtils::CheckIds;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(checkIds);

use Data::Dumper;
use XML::Simple;  # XML handling library
use IO::File;
use File::Path;
use LvCorporaTools::GenericUtils::SimpleXmlIo qw(loadXml);

###############################################################################
# This program checks references in the given PML dataset for following things:
#	* IDs from w file that are not refferenced in m file;
#	* IDs from m file that are not refferenced in a file (morphemes with
#	  "deleted" element are listed separately from others);
#	* IDs from m file linking to non-existing IDs in w file;
#	* IDs from a file linking to non-existing IDs or elements marked for
#	  deletion in m file;
#	* trees in a file not corresponding to single sentence in m file.
# TODO: check form_change.
# Refferences to multiple files not supported. ID duplication are not checked.
# (TrEd can be used for that)
#
# Input files - utf8.
# Output file - list with problematic IDs.
#
# Developed on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2011-2013
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

sub checkIds
{

	autoflush STDOUT 1;
	if (not @_ or @_ le 1)
	{
		print <<END;
Script for checking references in the given PML dataset for following eroors:
* IDs from w file that are not refferenced in m file;
* IDs from m file that are not refferenced in a file (morphemes with "deleted"
  element are listed separately from others);
* IDs from m file linking to non-existing IDs in w file;
* IDs from a file linking to non-existing IDs in m file;
* trees in a file not corresponding to single sentence in m file.

Params:
   directory prefix
   file name without extension
   output file name [opt, "res.txt" used otherwise]

Latvian Treebank project, LUMII, 2013, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $inputName = shift @_;
	my $resName = (shift @_ or 'res.txt');

	print "Starting...\n";
	my $ids = &_loadIds($dirPrefix, $inputName);

	my $out = IO::File->new("$dirPrefix\\$resName", "> :encoding(UTF-8)")
		or die "Could not create file $resName: $!";

	my $badIds = &_findUnusedIds($ids->{'w2token'}, $ids->{'w2m'});
	print 'Found '.scalar @$badIds." w ID(s) never referenced in m file.\n";
	print $out "W IDs never referenced in m file:\n";
	print $out join("\n", @$badIds);
	
	$badIds = &_findUnusedIds($ids->{'w2m'}, $ids->{'w2token'});
	print 'Found '.scalar @$badIds." non-existing w reference(s) in m file.\n";
	print $out "\n\nNon-existing w references in m file:\n";
	print $out join("\n", @$badIds);
	
	$badIds = &_findUnusedIds($ids->{'m2w'}, $ids->{'m2node'});
	my @notDel = grep {not $ids->{'m2w'}->{$_}->{'deleted'}} @$badIds;
	print 'Found '.scalar @$badIds.' m ID(s) never referenced in a file, '.
		(+@$badIds - @notDel)." of them are marked for deletion.\n";
	print $out "\n\nM IDs never referenced in a file:\n";
	print $out join("\n", @notDel);
	print $out "\nmarked for deletion:\n";
	my @del = grep {$ids->{'m2w'}->{$_}->{'deleted'}} @$badIds;
	print $out join("\n", @del);
	
	$badIds = &_findUnusedIds($ids->{'m2node'}, $ids->{'m2w'});
	print 'Found '.scalar @$badIds." non-existing m reference(s) in a file.\n";
	print $out "\n\nNon-existing m references in a file:\n";
	print $out join("\n", @$badIds);

	@$badIds = grep {$ids->{'m2w'}->{$_}->{'deleted'}} (values %{$ids->{'node2m'}});
	print 'Found '.scalar @$badIds." m elements marked for deletion, but used in a file.\n";
	print $out "\n\nM elements marked for deletion, but used in a file:\n";
	print $out join("\n", @$badIds);
	
	$badIds = &_findUnusedIds($ids->{'sent2m'}, $ids->{'sent2tree'});
	print 'Found '.scalar @$badIds." s ID(s) never referenced in a file.\n";
	print $out "\n\nS IDs never referenced in a file:\n";
	print $out join("\n", @$badIds);
	
	$badIds = &_findUnusedIds($ids->{'sent2tree'}, $ids->{'sent2m'});
	print 'Found '.scalar @$badIds." non-existing s reference(s) in a file.\n";
	print $out "\n\nNon-existing s references in a file:\n";
	print $out join("\n", @$badIds);

	$badIds = &_validateSentBound(
		$ids->{'sent2tree'}, $ids->{'m2node'}, $ids->{'sent2m'}, $ids->{'node2tree'});
	print 'Found '.scalar @$badIds." m nodes not reffered to in coressponding tree.\n";
	print $out "\n\nM nodes not reffered from coressponding tree:\n";
	print $out join("\n", @$badIds);

	$badIds = &_validateSentBound(
		$ids->{'tree2sent'}, $ids->{'node2m'}, $ids->{'tree2node'}, $ids->{'m2sent'});
	print 'Found '.scalar @$badIds." a nodes not reffered to in coressponding sentence.\n";
	print $out "\n\nA nodes not reffered from coressponding sentence:\n";
	print $out join("\n", @$badIds);

	$badIds = &_checkFormChange($ids->{'m2w'}, $ids->{'w2token'});
	print 'Found '.scalar @$badIds." m nodes whose \'form_change\' must be checked.\n";
	print $out "\n\nM nodes with incomplete \'form_change\':\n";
	print $out join("\n", @$badIds);
	
	$out->close;
	print "CheckIds has finished procesing \"$inputName\".\n";
}

# load (source directory, file name without extension)
# returns hash refernece:
#		'w2token' => hash from w IDs to tokens and spaces (source: w layer),
#		'm2w' => hash from m IDs to lists of w IDs, deletion marks, and lists
#				 of form changes (source: m layer),
#		'w2m' => hash from w IDs to m IDs (source: m layer),
#		'sent2m' => hash from sentence IDs to lists of m IDs (source: m layer),
#		'tree2sent' => hash from tree IDs to sentence IDs (source: a layer),
#		'sent2tree' => hash from sentence IDs to tree IDs (source: a layer),
#		'tree2node' => hash from tree IDs to lists of node IDs (source: a layer),
#		'node2m' => hash from node IDs to m IDs (source: a layer),
#		'm2node' => has from m IDs to node IDs (source: a layer).
# see &loadXML
sub _loadIds
{
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $inputName = shift @_;

	# Load w-level.
	my $w = loadXml ("$dirPrefix\\$inputName.w", ['para', 'w', 'schema'], ['id']);
	
	# Map token IDs to tokens.
	my %wIds = ();
	for my $para (@{$w->{'xml'}->{'doc'}->{'para'}})
	{
		%wIds = (%wIds, map {
			my $tmpw = $para->{'w'}->{$_};
			my $tok = $tmpw->{'token'}->{'content'};
			$tok .= ' ' unless ($tmpw->{'no_space_after'}->{'content'});
			$_ => $tok} (keys %{$para->{'w'}}));
	}
	print "W file parsed.\n";
	
	# Load m-level.
	my $m = loadXml ("$dirPrefix\\$inputName.m", ['s', 'm','reffile','schema'], ['id']);
	
	# Map sentence IDs to lists of morpheme IDs.
	my %mSent2morpho = map
	{
		my @valArr = keys %{$m->{'xml'}->{'s'}->{$_}->{'m'}};
		$_ => \@valArr;
	} (keys %{$m->{'xml'}->{'s'}});
	
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
	for my $sent (values %{$m->{'xml'}->{'s'}})
	{
		%m2w = (%m2w, map
		{
			my $thisM = $sent->{'m'}->{$_};
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
			$_ => {
				'rf' => @ref ? \@ref : undef,
				'del' => $del,
				'changes' => @change ? \@change : undef,
				'form' => $thisM->{'form'}->{'content'},};
		} (keys %{$sent->{'m'}}));
	}

	# Map token IDs to morpheme IDs.	
	my %w2m = ();
	for my $morpho (keys %m2w)
	{
		my $refs = $m2w{$morpho}->{'rf'};
		%w2m = (%w2m, map {$_ => $morpho} @$refs) if (defined $refs);
	}
	print "M file parsed.\n";
		
	# Load the a-level.
	my $a = loadXml ("$dirPrefix\\$inputName.a", ['node', 'LM','reffile','schema'], ['id']);
	my %tree2node = ();
	my %tree2mSent = ();
	my %node2morpho = ();
	# Process each tree.
	for my $treeId (keys %{$a->{'xml'}->{'trees'}->{'LM'}})
	{
		# Shortcut: current tree.
		my $tree = $a->{'xml'}->{'trees'}->{'LM'}->{$treeId};
		
		# Map tree ID to sentence ID.
		$tree->{'s.rf'}->{'content'} =~ /^m#(.*)$/;
		$tree2mSent{$treeId} = $1;
		
		# Traverse tree and collect all nodes with links to morphology.
		my %todoNodes = %{$tree->{'children'}};
		while (%todoNodes)
		{
			my $someKey = (keys %todoNodes)[0]; # Change to tied, if BFS is necessary.
			my $value = $todoNodes{$someKey};
			delete $todoNodes{$someKey};
			# Process nodes without ID (pmcinfo/coordinfo/xinfo).
			if ($someKey eq 'pmcinfo' or $someKey eq 'coordinfo' or $someKey eq 'xinfo')
			{
				%todoNodes = (%todoNodes, %{$value->{'children'}->{'node'}});
			}
			else # Process nodes with ID.
			{
				# Update result data structutures.
				if ($value->{'m.rf'}->{'content'})
				{
					# Map node ID to morpheme IDs.
					$value->{'m.rf'}->{'content'} =~ /^m#(.*)$/;
					$node2morpho{$someKey} = $1;
					# Add node ID to list to which tree ID maps to.	
					$tree2node{$treeId} = [] unless ($tree2node{$treeId});
					push @{$tree2node{$treeId}}, $someKey;
				}
				
				# Add children nodes to hashmap containing nodes yet to be
				# processed.
				if ($value->{'children'})
				{
					%todoNodes = (%todoNodes, %{$value->{'children'}});
					if ($todoNodes{'node'})
					{
						%todoNodes = (%todoNodes, %{$todoNodes{'node'}});
						delete $todoNodes{'node'};
					}
				}
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

	print "A file parsed.\n";
	
	#print Dumper (\%w2m);

	return {
		'w2token' => \%wIds,
		'm2w' => \%m2w,
		'w2m' => \%w2m,
		'sent2m' => \%mSent2morpho,
		'm2sent' => \%morpho2mSent,
		'tree2node' => \%tree2node,
		'node2tree' => \%node2tree,
		'tree2sent' => \%tree2mSent,
		'sent2tree' => \%mSent2tree,		
		'node2m' => \%node2morpho,
		'm2node' => \%morpho2node
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
			#print Dumper ($elemMap->{$elemId});
			#print Dumper ($sentMap->{$sourceId});
			#print Dumper ($target->{$elemMap->{$elemId}});
			push (@res, $elemId)
				unless ($elemMap->{$elemId} and $target->{$elemMap->{$elemId}}
					and $sentMap->{$sourceId} and
					($target->{$elemMap->{$elemId}} eq $sentMap->{$sourceId}));
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
		push (@res, $id) unless ($target->{$id});
	}
	
	return \@res;
}

# _checkFormChange(source hash, target hash)
# returns: array with m IDs whose 'form_change' field should be checked.
sub _checkFormChange
{
	my $m2w = shift @_;
	my $w2token = shift @_;
	
	my @res = ();
	
	for my $m (keys %$m2w)
	{
		my $v = $m2w->{$m};
		if (not defined $v->{'rf'} or @{$v->{'rf'}} == 0 ) # Verify m with no 'rf'.
		{
			my $contains = 0;
			for my $change (@{$v->{'form_change'}})
			{
				$contains = 1 if ($change eq 'insert');
			}
			push @res, $m unless ($contains);
		}
		elsif (@{$v->{'rf'}} == 1) 	# Verify m with single 'rf'.
		{
			my $wId = $v->{'rf'}[0];
			my $tok = $w2token->{$wId} ? $w2token->{$wId} : '';
			$tok =~ /^\s*(.*?)\s*$/;
			push @res, $m
				unless ($1 eq $v->{'form'} or 
					($v->{'form_change'} and @{$v->{'form_change'}} > 1));
		} else					# Verify m with multiple 'rf'.
		{
			my $contains = 0;
			for my $change (@{$v->{'form_change'}})
			{
				$contains = 1 if ($change eq 'union');
			}
			my $tok = join '', @{$v->{'rf'}};
			$tok =~ /^\s*(.*?)\s*$/;
			push @res, $m unless ($contains);
			push @res, $m
				if ($contains and $1 ne $v->{'form'} and @{$v->{'rf'}} == 1);
		}
	}
	
	return \@res;
}

1;