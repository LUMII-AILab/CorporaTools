package ConlluBackend;

use strict;

use Treex::PML::Factory;
use Treex::PML::Document;
use Treex::PML::Node;
use Treex::PML;

#use Carp::Always;

#use Moose;
#extends qw (Treex::PML::IO);
our @ISA = qw(Treex::PML::IO);
#use parent qw(Treex::PML::IO);

sub new
{
	my $class = shift;
	#my $self = Treex::PML::IO->new;
	my $self = $class->SUPER::new;
	bless $self, $class;
	return $self, $class;
}

sub test
{
	#my ($self, $filename, $encoding) = @_;
	my ($filename, $encoding) = @_;		#TrEd uses package oriented call, not object?
	# Not very good.
	return 1 if ($filename =~ /.*\.conllu$/);
	return 0;
	
}

# This must be inhereted from Treex::PML::IO, not sure why it does not happen
# magically.
sub open_backend {
	return Treex::PML::IO::open_backend(@_);
}

sub read
{
	#my ($self, $filehandle, $fsfile) = @_;
	my ($filehandle, $fsfile) = @_;	#It seems that TrEd uses call through package, not object.
	#$fsfile is Treex::PML::Document object.
	
	# Prepare Document object with scheme information etc.
	$fsfile->changeBackend('ConlluBackend');
	$fsfile->changeFileFormat('CoNLL-U');
	my $schema = Treex::PML::Factory->createPMLSchema({
		'filename' => Treex::PML::FindInResourcePaths ('conlluschema.xml')});
	$fsfile->changeMetaData('schema', $schema);
	$fsfile->changeFS(&_get_simple_FSformat($schema));
	
	print Treex::PML::FindInResourcePaths ('conlluschema.xml');

	# Fetch imput data.
	my @rows = <$filehandle>;
	my @text = ();	# Array of sentences, where sentence = array of tokens.
	my $tmpsent = [];
	for my $r (@rows)
	{
		if ($r =~ /^\s*$/)
		{
			push @text, $tmpsent if (@$tmpsent);
			$tmpsent = [];
		}
		else
		{
			$r =~ /^\s*(.*?)\s*$/;
			push @$tmpsent, $1;
		}
	}
	
	# Parse node atributes, make object for each node.
	my $treeId = 0;
	for my $sent (@text)
	{
		my %par2node = ();
		my %id2node = ();
		my %id2parId = ();
		my @surfaceTokRows = ();
		my $nodeCount = 0;
		for my $token (@$sent)
		{
			my @fields = split /\t/, $token;
			#$fields[1] =~ tr/_/ / if ($fields[1] !~ /^_$/);
			#$fields[2] =~ tr/_/ / if ($fields[2] !~ /^_$/);
			if ($fields[0] =~ /^\d+-\d+/)
			{
				push @surfaceTokRows, $token;
			}
			else
			{
				$nodeCount++;
				my $node = Treex::PML::Factory->createTypedNode('node.type', $fsfile->schema);
				#$node->set_attr('ord', $fields[0]+0);
				$node->set_attr('ord', $nodeCount);
				$node->set_attr('id', $fields[0]);
				$node->set_attr('form', $fields[1]);
				$node->set_attr('lemma', $fields[2]) unless ($fields[2] eq '_');
				$node->set_attr('upostag', $fields[3]) unless ($fields[3] eq '_');
				$node->set_attr('xpostag', $fields[4]) unless ($fields[4] eq '_');
				my @feats = ();
				@feats = split /\s*\|\s*/, $fields[5] unless ($fields[5] eq '_');
				my @featStructs = ();
				for my $featPart (@feats)
				{
					my $featStruct = Treex::PML::Factory->createTypedNode('feat.type', $fsfile->schema);
					if ($featPart =~ /([^=]*)=(.*)/g)
					{
						$featStruct->set_attr('feat', $1);
						$featStruct->set_attr('value', $2);
					}
					else
					{
						$featStruct->set_attr('feat', $featPart);
					}
					push @featStructs, $featStruct;
				}
				$node->set_attr ('feats', Treex::PML::Factory->createList(\@featStructs));
				$node->set_attr('deprel', $fields[7]) unless ($fields[7] eq '_');
				my @deps = ();
				@deps = split /\s*\|\s*/, $fields[8] unless ($fields[8] eq '_');
				my @depStructs = ();
				for my $depPart (@deps)
				{
					my $depStruct = Treex::PML::Factory->createTypedNode('dep.type', $fsfile->schema);
					if ($depPart =~ /([^:]*):(.*)/g)
					{
						$depStruct->set_attr('head', $1);
						$depStruct->set_attr('label', $2);
					}
					else
					{
						$depStruct->set_attr('label', $depPart);
					}
					push @depStructs, $depStruct;
				}
				$node->set_attr ('deps', Treex::PML::Factory->createList(\@depStructs));
				my @misc = ();
				@misc = split /\s*\|\s*/, $fields[9] unless ($fields[9] eq '_');
				$node->set_attr ('misc', Treex::PML::Factory->createList(\@misc));
				
				my $otherChildren = $par2node{$fields[6]} or [];
				push @$otherChildren, $node;
				%par2node = (%par2node, $fields[6] => $otherChildren);
				$id2node{$fields[0]} = $node;
				$id2parId{$fields[0]} = $fields[6];
			}
		}
		for my $surfaceToken (@surfaceTokRows)
		{
			my @fields = split /\t/, $surfaceToken;
			my @borders = split /-/, $fields[0];
			my $struct = Treex::PML::Factory->createTypedNode('surfacetok.type', $fsfile->schema);
			$struct->set_attr('form', $fields[1]);
			$struct->set_attr('endord', $borders[1]);
			my @misc = ();
			@misc = split /\s*\|\s*/, $fields[9] unless ($fields[9] eq '_');
			$struct->set_attr ('misc', Treex::PML::Factory->createList(\@misc));
			
			my $node = $id2node{$borders[0]};
			$node->attr('surfaceToken')->append($struct);
		}

		# Link the nodes in the tree.
		#my $root = $fsfile->new_tree($treeId);
		my $root = Treex::PML::Factory->createTypedNode('root.type', $fsfile->schema);
		$root->set_attr('ord', 0);
		
		$id2node{0} = $root;
		my @toDo = ('0');
		for my $n (@{$par2node{'_'}})
		{
			$n->cut;
			my $surogateParent = $root;
			if ($n->attr('id') =~ /(\d+)\.(\d+)/g)
			{
				$surogateParent = $id2node{$id2parId{$1}};
			}
			$n->paste_on($surogateParent);#, $n->attr('ord'));
			push @toDo, $n->attr('id');
		}
		while (@toDo)
		{
			my $current = shift @toDo;
			for my $n (@{$par2node{$current}})
			{
				$n->cut;
				$n->paste_on($id2node{$current});#, $n->attr('ord'));
				push @toDo, $n->attr('id');
			}
		}
		
		$fsfile->append_tree($root);
		$treeId++;
	}
	$fsfile->rebuildIDHash;
	return $fsfile;
}

# TODO interval tokens and comments.
# TODO test
sub write
{
	#my ($self, $filehandle, $fsfile) = @_;
	my ($filehandle, $fsfile) = @_; #It seems that TrEd uses call through package, not object.
	#$fsfile is Treex::PML::Document object.
	
	# For each tree in the current document...
	for my $tree ($fsfile->trees)
	{
		# Get all token nodes sorted by order.
		my @nodes = $tree->descendants;
		@nodes = sort {$a->get_order <=> $b->get_order} @nodes;
		shift @nodes while ($nodes[0]->get_order < 1);
		# Far each node in the tree print attributes (escaped, if needed).
		for my $n (@nodes)
		{
			if ($n->attr('surfaceToken'))
			{
				print $filehandle ($n->attr('ord') or '_').'-';
				print $filehandle ($n->attr('surfaceToken/endord') or '_')."\t";
				
				print $filehandle ($n->attr('surfaceToken/form') or '_')."\t_\t_\t_\t_\t_\t_\t_\t";
				my $misc = join '|', $n->attr('surfaceToken/misc')->values;
				$misc =~ tr/ /+/;
				print $filehandle ($misc or '_')."\n";
			}
			print $filehandle ($n->attr('ord') or '_')."\t";
			print $filehandle ($n->attr('form') or '_')."\t";
			my $lemma = $n->attr('lemma');
			$lemma =~ tr/ /_/;			
			print $filehandle ($lemma or '_')."\t";
			print $filehandle ($n->attr('upostag') or '_')."\t";
			print $filehandle ($n->attr('xpostag') or '_')."\t";
			my $feats = join '|', sort { "\L$a" cmp "\L$b" } 
					(map {$_->attr('feat')."=".$_->attr('value')} $n->attr('feats')->values);
			$feats =~ tr/ /+/;
			print $filehandle ($feats or '_')."\t";
			
			print $filehandle ($n->attr('id') =~ /\./ ? '_' : ($n->parent->attr('id') or '0'))."\t";
			print $filehandle ($n->attr('deprel') or '_')."\t";
			my $deps = join '|', map {$_->attr('head').":".$_->attr('label')}
					(sort { $a->attr('head') <=> $b->attr('head') } $n->attr('deps')->values);
			$deps =~ tr/ /+/;
			print $filehandle ($deps or '_')."\t"; 
			my $misc = join '|', $n->attr('misc')->values;
			$misc =~ tr/ /+/;
			print $filehandle ($misc or '_')."\n";
		}
	
		print $filehandle "\n";
	}
	
	return 1;
}

# This must be inhereted from Treex::PML::IO, not sure why it does not happen
# magically.
sub close_backend {
	return Treex::PML::IO::close_backend(@_);
}

sub _get_simple_FSformat
{
	my $schema = shift;
	return unless $schema;
	my $fs = Treex::PML::Factory->createFSFormat;
	
	OUTER: for my $nt ($schema->node_types)
	{
		for my $attrName ($nt->get_normal_fields)
		{
			my $attrType = $schema->find_type_by_path($attrName, 1, $nt);
			if ($attrType->get_role eq '#ORDER')
			{
				$fs->addNewAttribute('N K', '', $attrName);
				#last OUTER;
			}
		}
		#$fs->addNewAttribute('N K', '', 'aita');
		# Looks like passing multiple type letters (N and K) might allow to
		# hack FSFormat to have multiple Numering atributes.
	}
	
	return $fs;
}

1;
