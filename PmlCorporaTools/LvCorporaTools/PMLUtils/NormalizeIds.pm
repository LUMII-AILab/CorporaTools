#!C:\strawberry\perl\bin\perl -w
package LvCorporaTools::PMLUtils::NormalizeIds;

use strict;
use warnings;
#use utf8;

use Exporter();
our @ISA = qw(Exporter);
our @EXPORT_OK = qw(normalizeIds processDir load process doOutput);

use Data::Dumper;
use IO::Dir;
use IO::File;
use File::Path;
use Tie::IxHash; # This class provides analogue to java's LinkedHashMap
use XML::Simple;  # XML handling library

use LvCorporaTools::GenericUtils::SimpleXmlIo
	qw(loadXml printXml @FORCE_ARRAY_W @FORCE_ARRAY_M @FORCE_ARRAY_A);

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
# Latvian Treebank project, 2011-2017
# Lauma Pretkalnina, LUMII, AILab, lauma@ailab.lv
# Licenced under GPL.
###############################################################################

# Recalculate IDs in multiple datasets. This can be used as entry point, if
# this module is used standalone.
sub processDir
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 1)
	{
		print <<END;
Script for recalculating IDs in given PML datasets (all .w + .m + .a in the
given wolder). This should be used before concatenating multiple files, but
after the checkW is used.
Input files should be provided as UTF-8.

Params:
   data directory
Returns:
   count of failed files

Latvian Treebank project, LUMII, 2011-2017, provided under GPL
END
		exit 1;
	}

	my $dir_name = shift @_;
	my $dir = IO::Dir->new($dir_name) or die "dir $!";
	my $problems = 0;

	while (defined(my $in_file = $dir->read))
	{
		if ((! -d "$dir_name/$in_file") and ($in_file =~ /^(.+)\.w$/))
		{
			eval
			{
				local $SIG{__WARN__} = sub { die $_[0] }; # This magic makes eval act as if all warnings were fatal.
				normalizeIds ($dir_name, $1, $1, $1);
			};
			if ($@)
			{
				$problems++;
				print $@;
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

# Recalculate IDs in single dataset. This can be used as entry point, if this
# module is used standalone.
sub normalizeIds
{
	autoflush STDOUT 1;
	if (not @_ or @_ < 2)
	{
		print <<END;
Script for recalculating IDs in given PML dataset (.w + .m + .a). This should
be used before concatenating multiple files, but after the checkW is used.
Input files should be provided as UTF-8.

Params:
   directory prefix
   file name without extension
   new file name - used for naming files and w doc ID
       [opt, current file name used otherwise]
   new source id - used for w source_id and in paragraph/token/sentence IDs
       [opt, current file name used otherwise]
   ID of the first paragraph [opt, int, 1 used otherwise]
   ID of the first sentence [opt, int, 1 used otherwise]
   ID of the first token [opt, int, 1 used otherwise]

Latvian Treebank project, LUMII, 2011-2017, provided under GPL
END
		exit 1;
	}
	# Input paramaters.
	my $dirPrefix = shift @_;
	my $oldName = shift @_;
	my $newName = (shift @_ or $oldName);
	my $newSourceId = shift @_;
	my $firstPara = (shift @_ or 1);
	my $firstSent = (shift @_ or 1);
	my $firstWord = (shift @_ or 1);

	my $xmls = &load($dirPrefix, $oldName);
	
	&process(
		$newName, $newSourceId, $xmls->{'w'}->{'xml'}, $xmls->{'m'}->{'xml'},
		$xmls->{'a'}->{'xml'}, $firstPara, $firstSent, $firstWord);	

	&doOutput($dirPrefix, $newName, $xmls);
	
	print "\nNormalizeIds has finished procesing \"$oldName\".\n";
}

# load (source directory, file name without extension)
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

	# Load w-level.
	my $wXml = loadXml ("$dirPrefix\\$oldName.w", \@FORCE_ARRAY_W);
	print 'Loaded W';

	# Load m-level.
	my $mXml = loadXml ("$dirPrefix\\$oldName.m", \@FORCE_ARRAY_M);
	print ', M';
		
	
	if (-f "$dirPrefix\\$oldName.a")
	{
		# Load the a-level.
		my $aXml = loadXml ("$dirPrefix\\$oldName.a", \@FORCE_ARRAY_A);
		print ', A. ';
		return {'w' => $wXml, 'm' => $mXml, 'a' => $aXml};
	}
	else
	{
		print '. ';
		return {'w' => $wXml, 'm' => $mXml};
	}
}
# process (new file name, new source_id (used also for paragraph/token ID gen),
#          w data, m data, a data, [ID of the first paragraph], [ID of the first
#          sentence], [ID of the first token])
# returns hash refernece:
#		'w' => result for w, 'm' => result for m, 'a' => result for a,
#		'nextPara' => next free paragraph ID, 'nextSent' => next free sentence
#		ID,
sub process
{
	# Input paramaters.
	my $newName = shift @_;
	my $newSourceId = shift @_;
	my $w = shift @_;
	my $m = shift @_;
	my $a = shift @_;
	my $firstPara = (shift @_ or 1);
	my $firstSent = (shift @_ or 1);
	my $firstWord = (shift @_ or 1);

	# Process w-level.
	my $wRes = &_normalizeW($w, $newName, $newSourceId, $firstPara, $firstWord);
	print 'Processed W';

	# Process m-level.
	my $mRes = &_normalizeM($m, $newName, $wRes->{'idMap'}, $wRes->{'tokId2paraId'}, $firstSent);
	print ', M';
	
	if ($a)
	{
		# Process the a-level XML.
		my $aRes = &_normalizeA($a, $newName, $mRes->{'idMap'});
		print ', A. ';

		return {'w' => $wRes->{'xml'}, 'm' => $mRes->{'xml'}, 'a' => $aRes->{'xml'},
				'nextPara' => $wRes->{'nextPara'},
			    'nextSent' => $mRes->{'nextSent'}};
	}
	else
	{
		print '. ';
		return {'w' => $wRes->{'xml'}, 'm' => $mRes->{'xml'},
			    'nextPara' => $wRes->{'nextPara'},
			    'nextSent' => $mRes->{'nextSent'}};
	}	
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
	print 'Printed W';
	printXml ("$dirPrefix/res/$newName.m", $xmls->{'m'}->{'handler'},
			$xmls->{'m'}->{'xml'}, 'lvmdata', $xmls->{'m'}->{'header'});
	print ', M';
	if ($xmls->{'a'} and $xmls->{'a'}->{'xml'})
	{
		printXml ("$dirPrefix/res/$newName.a", $xmls->{'a'}->{'handler'},
				$xmls->{'a'}->{'xml'}, 'lvadata', $xmls->{'a'}->{'header'});
		print ', A';
	}
	print '. ';
}

###############################################################################
# Helper functions
###############################################################################

# normalizeW (xml data structure, new filename (used for doc id), new source_id
#             (used also paragraph/token ID gen), ID of first paragraph [opt],
#             ID of first word [opt])
# returns hash refernece:
#		'idMap' => old IDs mapping to new IDs,
#		'tokId2paraId' => new token IDs mapping to corresponding paragraphs' IDs,
#       'xml' => updated XML tree,
#		'nextPara' => next free paragraph ID
sub _normalizeW
{
	my $lvwdata = shift;
	my $newName = shift;
	my $newSouceId = shift;
	my $firstPara = (shift or 1);
	my $firstW = (shift or 1);
	
	my %oldId2newId = ();
	tie %oldId2newId, 'Tie::IxHash';

	my %newTokId2newParId = ();
	
	# Modify the fields in the header.
	$lvwdata->{'doc'}->{'id'} = $newName;
	$lvwdata->{'doc'}->{'source_id'} = $newSouceId;
	
	# Normalize IDs in the main data.
	my $paraShift = $firstPara;
	my $wShift = $firstW;
	for my $para (@{$lvwdata->{'doc'}->{'para'}})
	{
		my $newParaId = "w-$newSouceId-p${paraShift}";
		warn "Duplicate key: $para->{'id'}" if (exists $oldId2newId{$para->{'id'}});
		$oldId2newId{$para->{'id'}} = $newParaId;
		$para->{'id'} = $newParaId;
		for my $w (@{$para->{'w'}})
		{
			# Goes through all w-s in all para-s.
			my $newId = "w-$newSouceId-p${paraShift}w$wShift";
			warn "Duplicate key: $w->{'id'}" if (exists $oldId2newId{$w->{'id'}});
			$oldId2newId{$w->{'id'}} = $newId;
			$w->{'id'} = $newId;
			$newTokId2newParId{$newId} = $newParaId;
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
	return {'idMap' => \%oldId2newId, 'tokId2paraId' => \%newTokId2newParId,
		'xml' => \%$lvwdata, 'nextPara' => $paraShift};
}

# normalizeM (xml data structure, new filename (used for file refs), mapping for
#             W IDs, mapping from new W token IDs to paragraph IDs they belong
#             to (paragraph IDs used to create sentence and token IDs for M
#             layer), ID of first sentence [opt], ID of first word [opt])
# returns hash refernece:
#		'idMap' => old IDs mapping to new IDs, 'xml' => updated XML tree,
#		'nextSent' => next free sentence ID
sub _normalizeM
{
	my $lvmdata = shift;
	my $newName = shift;
	my $wMap = shift;
	my $wTok2Para = shift;
	my $firstSent = (shift or 1);
	my $firstM = (shift or 1);

	my %oldId2newId = ();
	tie(%oldId2newId, 'Tie::IxHash');
		
	# Update references.
	&_updateReffiles ($lvmdata->{'head'}->{'references'}, $newName);
	#$lvmdata->{'head'}->{'references'}->{'reffile'}->{'href'} = "$newName.w";
	
	my $sentId = $firstSent;
	my $mId = $firstM;
	my $docBegin = 1;
	my $paraId = -1;
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
			my $idStub;
			# Update references to w layer.
			if ($m->{'w.rf'} and %{$m->{'w.rf'}}) # Nonempty hash.
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
					# Get stub from already updated paragraph ID.
					$idStub = $wTok2Para->{$wMap->{$oldWId}} unless ($idStub);

				} else
				{	# Morphological unit cosists of multiple tokens.
					for (my $lmNo = 0; $lmNo < @{$m->{'w.rf'}->{'LM'}}; $lmNo++)
					{
						$oldWId = $m->{'w.rf'}->{'LM'}[$lmNo]->{'content'};
						$oldWId =~ s/w#(.*)$/$1/;
						warn "w ID $oldWId was not found!"
							if (not exists $wMap->{$oldWId});
						$m->{'w.rf'}->{'LM'}[$lmNo]->{'content'} = 'w#'.$wMap->{$oldWId};
						# Get stub from already updated paragraph ID.
						$idStub = $wTok2Para->{$wMap->{$oldWId}} unless ($idStub);
					}
				}
				$idStub =~ m/w-(.*?-p(\d+))$/;
				$paraId = $2;
				$idStub = $1;

				# If this is a new paragraph, restart sentence numbering.
				# However, allow the number of the first sentence in the whole
				# document to be whatever was passed to function.
				$sentId = 1 if (($paraId gt $prevPara) and not $docBegin);
				$prevPara = $paraId;
			}
			
			# Change sentence ID.
			if (not defined $newSId)
			{
				$newSId = "m-${idStub}s$sentId";
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

# normalizeA (xml data structure, new filename (used for file refs), mapping for
#             m IDs (used also to create IDs for A layer),)
# returns hash refernece:
#		'idMap' => old IDs mapping to new IDs, 'xml' => updated XML tree,
sub _normalizeA
{
	my $lvadata = shift;
	my $newName = shift;
	my $mMap = shift;

	my %oldId2newId = ();
	tie(%oldId2newId, 'Tie::IxHash');
	
	# Update references.
	&_updateReffiles ($lvadata->{'head'}->{'references'}, $newName);

	#We don't want ID of 1st paragraph be different depending on index of 1s sentence.
	my $docBegin = 1;
	for my $tree (@{$lvadata->{'trees'}->{'LM'}})
	{
		my $xId = 1;
		
		# Reference to the sentence in the m file.
		my $oldSref = $tree->{'s.rf'}->{'content'};
		$oldSref =~ s/^m#(.*)$/$1/;
		print "ID $oldSref! from A file was not found in M file. Please, check!\n"
			if (not $mMap->{$oldSref});
		# Get stub for new IDs.
		my $idStub =  $mMap->{$oldSref};
		$idStub =~ s/^m-(.*)$/$1/;

		$docBegin = 0; # This means that begining of 1st sentence of this document has passed.
		# Change sentence ID.
		my $newSId = "a-$idStub";
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
				my $oldMref = $current->{'m.rf'}->{'content'};
				$oldMref =~ s/^m#(.*)$/$1/;
				warn "m ID $oldMref was not found!"
					if (not exists $mMap->{$oldMref});
				$current->{'m.rf'}->{'content'} = 'm#'.$mMap->{$oldMref};
				$mMap->{$oldMref} =~ /^.*w(\d+)$/;
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
	}
	return {'idMap' => \%oldId2newId, 'xml' => \%$lvadata};
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