package ConllBackend;

use strict;

#use Treex::PML::IO;
use Treex::PML::Factory;
use Treex::PML::Document;
use Treex::PML::Node;

#use Carp::Always;

#use Moose;
#extends qw (Treex::PML::IO);

#our @ISA = qw(Treex::PML::IO);
use parent qw(Treex::PML::IO);
#our @EXPORT_OK = qw(test open_backend read write close_backend);


#sub new
#{
#	my $class = shift;
#	#my $self = Treex::PML::IO->new;
#	my $self = $class->SUPER::new;
#	bless $self, $class;
#	return $self, $class;
#}

sub test
{
	#my ($self, $filename, $encoding) = @_;	#TrEd uses package oriented call, not object?
	my ($filename, $encoding) = @_;
	# Not very good.
	return 1 if ($filename =~ /.*\.conll(2007|07)?$/);
	return 0;
	
}

# This must be inhereted from Treex::PML::IO
# WHY U NO INHERIT YOURSELF?!
sub open_backend {
	return Treex::PML::IO::open_backend(@_);
}

sub read
{
	#my ($self, $filehandle, $fsfile) = @_;
	my ($filehandle, $fsfile) = @_;
	#$fsfile is Treex::PML::Document object.
	#my $schema = Treex::PML::Factory->createPMLSchema({
	#	'filename' => 'conll2007schema.xml'});
	my $fsthingy = Treex::PML::Factory->createFSFormat([
		'@N ord', '@V form', '@K lemma', '@K cpostag', '@K postag',
		'@K deprel', '@K phead', '@K pdeprel']);
	#$fsfile->initialize('conll-data', 'conll2007', $fsthingy);
	#$fsfile->initialize('conll-data', 'conll2007', $schema);
#	$fsfile->initialize;
	$fsfile->changeBackend('ConllBackend');
	$fsfile->changeFileFormat('CoNLL-2007');
	$fsfile->changeFS($fsthingy);
#	$document->changeSchemaURL('')
#	$fsfile->changeMetaData ('scheme', $fsthingy);
	
	#$fsfile->changeSchemaURL('conll2007schema.xml');
		use Data::Dumper;
		print Dumper($fsfile);
		print Dumper($fsfile->listMetaData);
	
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
			push @$tmpsent, $r;
		}
	}
	
	# Seperate fields for each token.
	#@text = map { map { [split("\t", $_)] } @$_ } @text;
	my $treeId = 0;
	for my $sent (@text)
	{
		my %par2node = ();
		my %id2node = ();
		# Parse node atributes, make each node.
		for my $token (@$sent)
		{
			my @fields = split "\t", $token;
			$fields[1] =~ tr/_/ / if ($fields[1] !~ /^_$/);
			$fields[2] =~ tr/_/ / if ($fields[2] !~ /^_$/);
			#my $node = Treex::PML::Factory->createTypedNode('node', $fsfile->schema);
			my $node = Treex::PML::Factory->createTypedNode($fsfile->FS);
			#print "|".$fsfile->schema."|-|";
			$node->set_attr('ord', $fields[0]);
			$node->set_attr('form', $fields[1]);
			$node->set_attr('lemma', $fields[2]) unless ($fields[2] eq '_');
			$node->set_attr('cpostag', $fields[3]) unless ($fields[3] eq '_');
			$node->set_attr('postag', $fields[4]) unless ($fields[4] eq '_');
			my @feats = split /\s*\|\s*/, $fields[5];
			$node->set_attr ('feats', Treex::PML::Factory->createList(\@feats));
			$node->set_attr('deprel', $fields[7]) unless ($fields[7] eq '_');
			$node->set_attr('phead', $fields[8]) unless ($fields[8] eq '_');
			$node->set_attr('pdeprel', $fields[9]) unless ($fields[9] eq '_');
			
			#$par2node{$fields[6]} = $node;
			my $otherChildren = $par2node{$fields[6]} or [];
			push @$otherChildren, $node;
			%par2node = (%par2node, $fields[6] => $otherChildren);
			$id2node{$fields[0]} = $node;
			#push @nodes, $node; 
			#push (@parentIds, ($fields[6] ? $fields[6] - 1 : -1));
		}

		# Link the nodes in the tree.
		# = Treex::PML::Factory->createNode();
		my $root = $fsfile->new_tree($treeId);
		$root->set_attr('ord', 0);
		$root->set_attr('name', 'pseudoroot');
		
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
				#print Dumper($id2node{current});
			}
		}
		
#		for my $i ([0..$#nodes-1])
#		{
#			if ($parentIds[$i] < 0)
#			{
#				#$root->paste_on($nodes[$i]); #,$i
#				$nodes[$i]->paste_on($root, $i);
#			} else
#			{
#				$nodes[$i]->paste_on($nodes[$parentIds[$i]], $i);
#			}
#		}
		$treeId++;
	}
	return $fsfile;
}

sub write
{
	#my ($self, $filehandle, $fsfile) = @_;
	my ($filehandle, $fsfile) = @_;
	#$fsfile is Treex::PML::Document object.
	
	#for my $tree ($fsfile->trees)
	#{
	#}
}

# This must be inhereted from Treex::PML::IO
# WHY U NO INHERIT YOURSELF?!
sub close_backend {
	return Treex::PML::IO::close_backend(@_);
}


1;
