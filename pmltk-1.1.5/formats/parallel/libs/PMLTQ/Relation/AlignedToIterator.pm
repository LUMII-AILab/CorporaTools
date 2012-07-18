{
  package PMLTQ::Relation::AlignedToIterator;
  use strict;

  use PMLTQ::Relation {
      name => 'aligned_to',
      reversed_relation => 'aligned_to',
      start_node_type => 'node',
      target_node_type => 'node',
      iterator_class => __PACKAGE__,
      iterator_weight => 2,
      test_code => 'grep($_->[0]==$end, @{'.__PACKAGE__.'::_get_aligned_nodes($start,$start_fsfile)})',
  };

  use base qw(PMLTQ::Relation::SimpleListIterator);
  sub get_node_list  {
    my ($self,$node)=@_;
    my $fsfile = $self->start_file;
    return _get_aligned_nodes($node,$fsfile);
  }
  sub _get_aligned_nodes {
    my ($node,$fsfile)=@_;
    my $map;
    my $id = $node->{'xml:id'};
    my @ret;
    for my $p_fs (grep { PML::SchemaName($_) eq 'tree_alignment' } @{$fsfile->appData('fs-part-of')}) {
      my $map = $p_fs->appData('alignment_map');
      unless (ref($map)) {
	# generate alignment map (this map should be invalidated/updated on any change)
	print STDERR "generating alignment map\n";
	$map={};
	for my $i (0..$p_fs->lastTreeNo) {
	  for my $alignment ($p_fs->tree($i)->children) {
	    my ($A,$B)=($alignment->{'a.rf'},$alignment->{'b.rf'});
	    push @{$map->{$A}},$B;
	    push @{$map->{$B}},$A;
	  }
	}
	$p_fs->changeAppData('alignment_map',$map);
      }
      my ($a_file_id, $b_file_id)=map $p_fs->metaData('refnames')->{$_}, qw(document_a document_b);
      my @refs;
      if ($fsfile==$p_fs->appData('ref')->{$a_file_id}) {
	@refs = @{$map->{"$a_file_id#$id"}||[]};
      } elsif ($fsfile==$p_fs->appData('ref')->{$b_file_id}) {
	@refs = @{$map->{"$b_file_id#$id"}||[]};
      }
      for my $ref (@refs) {
	my ($doc_id, $node_id)=split /#/,$ref,2;
	my $ref_file = $p_fs->appData('ref')->{$doc_id};
	my $ref_node = $ref_file && PML::GetNodeByID($node_id,$ref_file);
	push @ret, [$ref_node, $ref_file] if $ref_node;
      }
    }
    return \@ret;
  }
}

1;
