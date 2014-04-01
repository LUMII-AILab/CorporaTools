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

#our $SCHEMA_DIR = '';

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
	
	#my $fsthingy = Treex::PML::Factory->createFSFormat([
	#	'@N ord', '@V form', '@K lemma', '@K cpostag', '@K postag',
	#	'@K deprel', '@K phead', '@K pdeprel']);
	#$fsfile->changeFS($fsthingy);
	$fsfile->changeBackend('ConllBackend');
	$fsfile->changeFileFormat('CoNLL-2007');
	my $schema = Treex::PML::Factory->createPMLSchema({
		'string' => &_get_schema});
		#'filename' => 'libs/conll2007schema.xml'});
	$fsfile->changeMetaData('schema', $schema);
#	$fsfile->changeSchemaURL('conll2007schema.xml');

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
	
	# Seperate fields for each token.
	my $treeId = 0;
	for my $sent (@text)
	{
		my %par2node = ();
		my %id2node = ();
		# Parse node atributes, make each node.
		for my $token (@$sent)
		{
			my @fields = split /\t/, $token;
			$fields[1] =~ tr/_/ / if ($fields[1] !~ /^_$/);
			$fields[2] =~ tr/_/ / if ($fields[2] !~ /^_$/);
			my $node = Treex::PML::Factory->createTypedNode('node.type', $fsfile->schema);
			#my $node = Treex::PML::Factory->createTypedNode($fsfile->FS);
			$node->set_attr('ord', $fields[0]);
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
			
			#$par2node{$fields[6]} = $node;
			my $otherChildren = $par2node{$fields[6]} or [];
			push @$otherChildren, $node;
			%par2node = (%par2node, $fields[6] => $otherChildren);
			$id2node{$fields[0]} = $node;
			#push @nodes, $node; 
			#push (@parentIds, ($fields[6] ? $fields[6] - 1 : -1));
		}

		# Link the nodes in the tree.
		#my $root = $fsfile->new_tree($treeId);
		my $root = Treex::PML::Factory->createTypedNode('root.type', $fsfile->schema);
		$root->set_attr('ord', 0);
		$root->set_attr('deprel', 'pseudoroot');
		
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
	return $fsfile;
}

# TODO later.
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

sub _get_schema
{
	my $schema = <<END;
<?xml version="1.0" encoding="utf-8"?>
<pml_schema
  xmlns="http://ufal.mff.cuni.cz/pdt/pml/schema/"
  version="1.1">
  <revision>0.1</revision>
  <description>Schema for CoNLL-2007 syntax data (10 column format).</description>

  <root name="conlldata" type="treelist.type"/>

  <type name="treelist.type">
    <structure>
      <member name="trees" role="#TREES" required="1">
        <list type="root.type" ordered="1"/>
      </member>
    </structure>
  </type>
  
  <type name="root.type"> <!-- Root.-->
	<structure role="#NODE" name="node">
      <member name="ord" role="#ORDER" as_attribute="1" required="1"><constant>0</constant></member>
	  <member name="deprel"><cdata format="any"/></member>
      <member name="children" role="#CHILDNODES">
        <list type="node.type" ordered="0"/>
      </member>	  
	</structure>
  </type>
  
  <type name="node.type"> <!-- Arbitrary node. -->
	<structure role="#NODE" name="node">
	  <member name="form" required="1"><cdata format="string"/></member>
      <member name="ord" role="#ORDER" as_attribute="1" required="1"><cdata format="positiveInteger"/></member>
	  <member name="lemma"><cdata format="string"/></member>
	  <member name="cpostag"><cdata format="any"/></member>
	  <member name="postag"><cdata format="any"/></member>
	  <member name="feats"><list odered='0'><cdata format="any"/></list></member>
	  <member name="deprel"><cdata format="any"/></member>
	  <member name="phead"><cdata format="nonNegativeInteger"/></member>
	  <member name="pdeprel"><cdata format="any"/></member>
      <member name="children" role="#CHILDNODES">
        <list type="node.type" ordered="0"/>
      </member>	  
	</structure>
  </type>
    
</pml_schema>
END
	return $schema;
}

1;
