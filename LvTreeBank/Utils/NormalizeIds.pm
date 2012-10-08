#!C:\strawberry\perl\bin\perl -w
package LvTreeBank::Utils::NormalizeIds;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA= qw( Exporter );
our @EXPORT_OK = qw(normalizeIds load process doOutput);

use Data::Dumper;
use XML::Simple;  # XML handling library
use Tie::IxHash; # This class provides analogue to java's LinkedHashMap
use IO::File;
use File::Path;
use LvTreeBank::Utils::SimpleXmlIo qw(loadXml printXml);

###############################################################################
# This program recalculate IDs in the given PML dataset. Number of first
# paragraph, first sentence and first token may be given as input parameter,
# otherwise 1 assumed.
#
# Input files - utf8.
# Output file can have diferent XML element order. To obtain standard order
# resave file with TrEd.
#
# Developed on ActivePerl 5.10.1.1007, tested on Strawberry Perl 5.12.3.0
# Latvian Treebank project, 2011-2012
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################
sub normalizeIds
{
	autoflush STDOUT 1;
	if (not @_ or @_ le 2)
	{
		print <<END;
Script for recalculating IDs in given PML dataset (.w + .m + .a). This should
be used before concatenating multiple files, but after the checkW is used.
Input files should be provided as UTF-8.

Params:
   directory prefix
   file name without extension
   new file name [opt, current file name used otherwise]
   ID of the first paragraph [opt, int, 1 used otherwise]
   ID of the first sentence [opt, int, 1 used otherwise]
   ID of the first token [opt, int, 1 used otherwise]

Latvian Treebank project, LUMII, 2011, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $oldName = shift @_;
	my $newName = (shift @_ or $oldName);
	my $firstPara = (shift @_ or 1);
	my $firstSent = (shift @_ or 1);
	my $firstWord = (shift @_ or 1);

	print "Starting...\n";
	
	my $xmls = &load($dirPrefix, $oldName, $newName);
	#my $res = 
	&process(
		$newName, $xmls->{'w'}->{'xml'}, $xmls->{'m'}->{'xml'},
		$xmls->{'a'}->{'xml'}, $firstPara, $firstSent, $firstWord);

	&doOutput($dirPrefix, $newName, $xmls);
	
	print "NormalizeIds.pl has finished procesing \"$oldName\".\n";
}

# load (source directory, file name without extension, [new file name])
# returns hash refernece:
#		'w' => XML data from &loadXML for w,
#		'm' => XML data from &loadXML for m,
#		'a' => XML data from &loadXML for a
# see &loadXML
sub load
{
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $oldName = shift @_;
	my $newName = (shift @_ or $oldName);

	# Load w-level.
	my $w = loadXml ("$dirPrefix\\$oldName.w", ['para', 'w', 'schema']);
	print "W file loaded.\n";

	# Load m-level.
	my $m = loadXml ("$dirPrefix\\$oldName.m", ['s', 'm','reffile','schema']);
	print "M file loaded.\n";
		
	# Load the a-level.
	my $a = loadXml ("$dirPrefix\\$oldName.a", ['node', 'LM','reffile','schema']);
	print "A file loaded.\n";

	return {'w' => $w, 'm' => $m, 'a' => $a};
}
# process (new file name, w data, m data, a data, [ID of the first paragraph],
#		[ID of the first sentence], [ID of the first token])
# returns hash refernece:
#		'w' => result for w, 'm' => result for m, 'a' => result for a,
#		'nextPara' => next free paragraph ID, 'nextSent' => next free sentence
#		ID, 'nextTree' => next free tree ID
sub process
{
	# Input paramaters.
	my $newName = shift @_;
	my $w = shift @_;
	my $m = shift @_;
	my $a = shift @_;
	my $firstPara = (shift @_ or 1);
	my $firstSent = (shift @_ or 1);
	my $firstWord = (shift @_ or 1);

	# Process w-level.
	my $wRes = &_normalizeW($w, $newName, $firstPara, $firstWord);
	print "W file processed.\n";

	# Process m-level.
	my $mRes = &_normalizeM(
		$m, $newName, $wRes->{'idMap'}, $firstPara, $firstSent);
	print "M file processed.\n";
	
	# Process the a-level XML.
	my $aRes = &_normalizeA(
		$a, $newName, $mRes->{'idMap'}, $firstPara, $firstSent);
	print "A file processed.\n";

	return {'w' => $wRes->{'xml'}, 'm' => $mRes->{'xml'}, 'a' => $aRes->{'xml'},
			'nextPara' => $wRes->{'nextPara'}, 'nextSent' => $mRes->{'nextSent'},
			'nextTree' => $aRes->{'nextTree'},};
}

sub doOutput
{
	my $dirPrefix = shift @_;
	my $newName = shift @_;	
	my $xmls = shift @_;
	#my $res = (shift @_ or $xmls->{'xml'});

	mkpath("$dirPrefix/res/");

	printXml ("$dirPrefix/res/$newName.w", $xmls->{'w'}->{'handler'},
			$xmls->{'w'}->{'xml'}, 'lvwdata', $xmls->{'w'}->{'header'});
	print "W file printed.\n";
	printXml ("$dirPrefix/res/$newName.m", $xmls->{'m'}->{'handler'},
			$xmls->{'m'}->{'xml'}, 'lvmdata', $xmls->{'m'}->{'header'});
	print "M file printed.\n";
	printXml ("$dirPrefix/res/$newName.a", $xmls->{'a'}->{'handler'},
			$xmls->{'a'}->{'xml'}, 'lvadata', $xmls->{'a'}->{'header'});
	print "A file printed.\n";

}

###############################################################################
# Helper functions
###############################################################################

# normalizeW (xml data structure, new filename, 
#			  ID of first paragraph [opt], ID of first word [opt])
# returns hash refernece:
#		'idMap' => old IDs mapping to new IDs, 'xml' => updated XML tree,
#		'nextPara' => next free paragraph ID
sub _normalizeW
{
	my $lvwdata = shift;
	my $newName = shift;
	my $firstPara = (shift or 1);
	my $firstW = (shift or 1);
	
	my %oldId2newId = ();
	tie %oldId2newId, 'Tie::IxHash';
	
	# Modify the fields in the header.
	$lvwdata->{'doc'}->{'id'} = $newName;
	#my $source_ext = $lvwdata->{'doc'}->{'source_id'};
	#$source_ext =~ s/^.+(\..+)$/$1/;
	$lvwdata->{'doc'}->{'id'} = $newName;#.$source_ext;
	
	# Normalize IDs in the main data.
	my $paraShift = $firstPara;
	my $wShift = $firstW;
	for my $para (@{$lvwdata->{'doc'}->{'para'}})
	{
		#print "$para->{'w'}\n";
		#print "@{[ %{$para->{'w'}} ]}\n";
		for my $w (@{$para->{'w'}})
		{
			# Goes through all w-s in all para-s.
			my $newId = "w-$newName-p${paraShift}w$wShift";
			warn "Duplicate key: $w->{'id'}" if (exists $oldId2newId{$w->{'id'}});
			$oldId2newId{$w->{'id'}} = $newId;
			$w->{'id'} = $newId;
		}
		continue
		{
			$wShift++;
		}
	} continue
	{
		$wShift = 1;
		$paraShift++;
	}

	# Return the ID mapping and modified XML hash.
	return {'idMap' => \%oldId2newId, 'xml' => \%$lvwdata, 'nextPara' => $paraShift};
}

# normalizeM (xml data structure, new filename, mapping for w IDs,
#             ID of first paragraph [opt], ID of first sentence [opt],
#             ID of first word [opt])
# returns hash refernece:
#		'idMap' => old IDs mapping to new IDs, 'xml' => updated XML tree,
#		'nextSent' => next free sentence ID
sub _normalizeM
{
	my $lvmdata = shift;
	my $newName = shift;
	my $wMap = shift;
	my $firstPara = (shift or 1);
	my $firstSent = (shift or 1);
	my $firstM = (shift or 1);

	my %oldId2newId = ();
	tie(%oldId2newId, 'Tie::IxHash');
		
	# Update references.
	&_updateReffiles ($lvmdata->{'head'}->{'references'}, $newName);
	#$lvmdata->{'head'}->{'references'}->{'reffile'}->{'href'} = "$newName.w";
	
	my $sentId = $firstSent;
	my $mId = $firstM;
	my $paraId = $firstPara;
	#We don't want ID of 1st paragraph be different depending on index of 1s word.
	my $docBegin = 1;
	my $prevPara = -1;
	# Normalize IDs in the main data.
	for my $s (@{$lvmdata->{'s'}})
	{
		# Remove m nodes marked for deletion.
		my @onlyUndeleted = grep
			{not $_->{'deleted'} or $_->{'deleted'} eq 0}
			@{$s->{'m'}};
		$s->{'m'} = \@onlyUndeleted;
		
		my $newSId;
		
		for my $m (@{$s->{'m'}})
		{	# Goes through all m-s in all s-s.
			
			# Update references to w layer.
			if ($m->{'w.rf'})
			{
				my $oldWId;
				#if (ref $m->{'w.rf'})
				if ($m->{'w.rf'}->{'content'})
				{	# Morphological unit cosists of single token.
					$oldWId = $m->{'w.rf'}->{'content'};
					$oldWId =~ s/w#(.*)$/$1/;
					warn "w ID $oldWId was not found!"
						if (not exists $wMap->{$oldWId});
					$m->{'w.rf'}->{'content'} = 'w#'.$wMap->{$oldWId};
				} else
				{	# Morphological unit cosists of multiple tokens.
					for (my $lmNo = 0; $lmNo < @{$m->{'w.rf'}->{'LM'}}; $lmNo++)
					{
						$oldWId = $m->{'w.rf'}->{'LM'}[$lmNo]->{'content'};
						$oldWId =~ s/w#(.*)$/$1/;
						warn "w ID $oldWId was not found!"
							if (not exists $wMap->{$oldWId});
						$m->{'w.rf'}->{'LM'}[$lmNo]->{'content'} = 'w#'.$wMap->{$oldWId};
					}
				}
				$wMap->{$oldWId} =~ m/p(.+)w.+$/;
				my $thisPara = $1;
				
				if (($wMap->{$oldWId} =~ /\Qw1\E$/ or $thisPara gt $prevPara)
					and not $docBegin)
				{
					$sentId = 1;
					$paraId++;
				}
				$prevPara = $thisPara;
			}
			
			# Change sentence ID.
			if (not defined $newSId)
			{
				$newSId = "m-$newName-p${paraId}s$sentId";
				$oldId2newId{$s->{'id'}} = $newSId;
				$s->{'id'} = $newSId;
			}
			# Change morpological unit's ID.
			my $newMId = "${newSId}w$mId";
			$oldId2newId{$m->{'id'}} = $newMId;
			$m->{'id'} = $newMId;
			$docBegin = 0;	# This means that 1st word of this document has passed.
		} continue
		{
			$mId++;
		};
	} continue
	{
		$mId = 1;
		$sentId++;
	}
	
	# Return the ID mapping and modified XML hash.
	return {'idMap' => \%oldId2newId, 'xml' => \%$lvmdata, 'nextSent' => $sentId};
}

# normalizeA (xml data structure, new filename, mapping for m IDs,
#             ID of first paragraph [opt], ID of first sentence [opt])
# returns hash refernece:
#		'idMap' => old IDs mapping to new IDs, 'xml' => updated XML tree,
#		'nextTree' => next free tree ID
sub _normalizeA
{
	my $lvadata = shift;
	my $newName = shift;
	my $mMap = shift;
	my $firstPara = (shift or 1);
	my $firstSent = (shift or 1);
	#my $firstWord = (shift or 1);
	
	my %oldId2newId = ();
	tie(%oldId2newId, 'Tie::IxHash');
	
	# Update references.
	&_updateReffiles ($lvadata->{'head'}->{'references'}, $newName);

	my $sentId = $firstSent;
	#my $mId = $firstw;
	my $paraId = $firstPara;
	#We don't want ID of 1st paragraph be different depending on index of 1s sentence.
	my $docBegin = 1;
	for my $tree (@{$lvadata->{'trees'}->{'LM'}})
	{
		my $xId = 1;
		
		# Reference to the sentence in the m file.
		my $oldSref = $tree->{'s.rf'}->{'content'};
		$oldSref =~ s/^m#(.*)$/$1/;
		
		# Update paragraph ID.
		print "ID $oldSref! from A file was not found in M file. Please, check!\n"
			if (not $mMap->{$oldSref});
		if (not $docBegin and $mMap->{$oldSref} =~ /^.*s1$/)
		{
			$sentId = 1;
			$paraId++;
		}
		$docBegin = 0; # This means that begining of 1st sentence of this document has passed.
		# Change sentence ID.
		my $newSId = "a-$newName-p${paraId}s$sentId";
		$oldId2newId{$tree->{'id'}} = $newSId;
		$tree->{'id'} = $newSId;
		# Update reference to m sentence.
		$tree->{'s.rf'}->{'content'} = 'm#'.$mMap->{$oldSref};
		
		# BFS for all nodes.
		my @checkThese = ();
		
		#TODO izlabot, lai njem pmc/x/node
		#push(@checkThese, @{$tree->{'children'}});
		my @tmp = &_findAChildren($tree->{'children'});
		push @checkThese, @tmp;
		#push(@checkThese, &findAChildren($tree->{'children'}));
		
		while (@checkThese > 0)
		{
			my $current = shift @checkThese;
			# Add children to the "todo" list.
			#push(@checkThese, @{$current->{'children'}}) if ($current->{'children'});
			push (@checkThese, &_findAChildren($current->{'children'}))
				if ($current->{'children'});
			
			if ($current->{'m.rf'})
			{	# Regular nodes.
				my $oldSref = $current->{'m.rf'}->{'content'};
				$oldSref =~ s/^m#(.*)$/$1/;
				warn "m ID $oldSref was not found!"
					if (not exists $mMap->{$oldSref});
				$current->{'m.rf'}->{'content'} = 'm#'.$mMap->{$oldSref};
				$mMap->{$oldSref} =~ /^.*w(.*?)$/;
				my $newId = "${newSId}w$1";
				$oldId2newId{$current->{'id'}} = $newId;
				$current->{'id'} = $newId;
			}
			else
			{	# Empty nodes.
				next if (not $current->{'id'});
				my $newId = "${newSId}x$xId";
				$oldId2newId{$current->{'id'}} = $newId;
				$current->{'id'} = $newId;
				$xId++;
			}
		}
	} continue
	{
		$sentId++;
	}
	return {'idMap' => \%oldId2newId, 'xml' => \%$lvadata, 'nextTree' => $sentId};	
}

# findAChildren (pointer to hashmap corresponding to key 'children')
# returns array with childnodes
# Helper function for normalizeA.
sub _findAChildren
{
	my $children = shift;
	my @res = ();
	print @res;
	for my $k (keys %$children)
	{
		if ($k eq 'node')
		{
			push @res, @{$children->{'node'}}
		} else
		{
			push @res, $children->{$k};
		}
	}
	return @res;
}

# updateReffiles (pointer to hashmap corresponding to key 'references',
#                 new file name)
# returns pointer to the same hashmap
# Helper function for normalizeM and normalizeA.
sub _updateReffiles
{
	my ($node, $newName) = @_;
	for my $ref (@{$node->{'reffile'}})
	{
		$ref->{'href'} =~ s/^.*(\.[wma])$/$newName$1/;
	}
	return $node;
}
1;