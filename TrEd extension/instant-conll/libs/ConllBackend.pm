package ConllBackend;

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
	return 1 if ($filename =~ /.*\.conll(2007|07)?$/);
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
	$fsfile->changeBackend('ConllBackend');
	$fsfile->changeFileFormat('CoNLL-2007');
	my $schema = Treex::PML::Factory->createPMLSchema({
		'filename' => Treex::PML::FindInResourcePaths ('conll2007schema.xml')});
	$fsfile->changeMetaData('schema', $schema);
	#my $fsthingy = Treex::PML::Factory->createFSFormat([
	#	'@N ord', '@V form', '@K lemma', '@K cpostag', '@K postag',
	#	'@K deprel', '@K phead', '@K pdeprel']);
	#$fsfile->changeFS($fsthingy);
	$fsfile->changeFS(&_get_simple_FSformat($schema));
	
	print Treex::PML::FindInResourcePaths ('conll2007schema.xml');

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
		for my $token (@$sent)
		{
			my @fields = split /\t/, $token;
			$fields[1] =~ tr/_/ / if ($fields[1] !~ /^_$/);
			$fields[2] =~ tr/_/ / if ($fields[2] !~ /^_$/);
			my $node = Treex::PML::Factory->createTypedNode('node.type', $fsfile->schema);
			#my $node = Treex::PML::Factory->createTypedNode($fsfile->FS);
			$node->set_attr('ord', $fields[0]+0);
			$node->set_attr('form', $fields[1]);
			$node->set_attr('lemma', $fields[2]) unless ($fields[2] eq '_');
			$node->set_attr('cpostag', $fields[3]) unless ($fields[3] eq '_');
			$node->set_attr('postag', $fields[4]) unless ($fields[4] eq '_');
			my @feats = ();
			@feats = split /\s*\|\s*/, $fields[5] unless ($fields[5] eq '_');
			$node->set_attr ('feats', Treex::PML::Factory->createList(\@feats));
			$node->set_attr('deprel', $fields[7]) unless ($fields[7] eq '_');
			$node->set_attr('phead', $fields[8]) unless ($fields[8] eq '_');
			$node->set_attr('pdeprel', $fields[9]) unless ($fields[9] eq '_');
			
			my $otherChildren = $par2node{$fields[6]} or [];
			push @$otherChildren, $node;
			%par2node = (%par2node, $fields[6] => $otherChildren);
			$id2node{$fields[0]} = $node;
		}

		# Link the nodes in the tree.
		#my $root = $fsfile->new_tree($treeId);
		my $root = Treex::PML::Factory->createTypedNode('root.type', $fsfile->schema);
		$root->set_attr('ord', 0);
		
		$id2node{0} = $root;
		my @toDo = ('0');
		while (@toDo)
		{
			my $current = shift @toDo;
			for my $n (@{$par2node{$current}})
			{
				$n->cut;
				$n->paste_on($id2node{$current});#, $n->attr('ord'));
				push @toDo, $n->attr('ord');
			}
		}
		$fsfile->append_tree($root);
		$treeId++;
	}
	$fsfile->rebuildIDHash;
	return $fsfile;
}

# TODO later.
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
			print $filehandle ($n->attr('ord') or '_')."\t";
			my $form = $n->attr('form');
			$form =~ tr/ /_/;
			print $filehandle ($form or '_')."\t";
			my $lemma = $n->attr('lemma');
			$lemma =~ tr/ /_/;			
			print $filehandle ($lemma or '_')."\t";
			print $filehandle ($n->attr('cpostag') or '_')."\t";
			print $filehandle ($n->attr('postag') or '_')."\t";
			my $feats = join '|', $n->attr('feats')->values;
			$feats =~ tr/ /+/;
			print $filehandle ($feats or '_')."\t";
			print $filehandle ($n->parent->get_order or '0')."\t";
			print $filehandle ($n->attr('deprel') or '_')."\t";
			print $filehandle ($n->attr('phead') or '_')."\t";
			print $filehandle ($n->attr('pdeprel') or '_')."\t\n";
		}
	
		print $filehandle "\n";
	}
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
