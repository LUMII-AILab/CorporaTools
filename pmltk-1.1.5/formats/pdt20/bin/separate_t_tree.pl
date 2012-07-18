#!/usr/bin/perl

use XML::LibXML;
use XML::LibXML::Reader;
use XML::LibXML::XPathContext;
use File::Basename qw(dirname);
use File::Spec;
use Encode;

usage() unless @ARGV;

use constant PML_NS => 'http://ufal.mff.cuni.cz/pdt/pml/';

my ($t_file, $n) = @ARGV;

sub usage {
  print STDERR "$0 t-file tree-no\n";
  exit 1;
}

# parse a file, preserving only specified nodes
sub parse {
  my (%opts) = @_;
  # file, ns, paths, depth, name, code
  my $reader = XML::LibXML::Reader->new(location => $opts{file}) ||
    die "Can't create XML reader for file $opts{file}\n";
  for my $path (@{ $opts{paths} }) {
    $reader->preservePattern( $path, $opts{ns} );
  }
  while ($reader->read) {
    if ($reader->depth == $opts{depth}) {
      if ($reader->nodeType == XML_ELEMENT_NODE  and
	  $reader->localName eq $opts{name} and
	  ($opts{code} ? &{$opts{code}}($reader) : 1)) {
	$reader->preserveNode;
	if ($opts{finish} ? &{$opts{finish}}($reader) : 1) {
	  $reader->finish;
	  last;
	} else {
	  $reader->nextSibling;
	  redo;
	}
      } else {
	$reader->nextSibling;
	redo;
      }
    }
  }
  $reader->finish;
  return $reader->document;
}

sub save_path {
  my ($filename)=@_;
  $filename =~ s/\.gz$//;
  $filename .= ".TREE$n.gz";
  return $filename;
}
sub save {
  my ($doc)=@_;
  my $fn = save_path($doc->URI);
  print STDERR "Saving $fn\n";
  $doc->toFile($fn, 1);
}

# returns @href of a reffile and possibly updates it to save_path
sub reffile {
  my ($doc, $name, $update) = @_;
  my $xpc = XML::LibXML::XPathContext->new($doc);
  $xpc->registerNs( pml => PML_NS );
  my ($href) =  $xpc->findnodes(qq{ /*/pml:head/pml:references/pml:reffile[\@name="$name"][1]/\@href });
  if ($href) {
    my $url = $href->value;
    $href->setValue(save_path($url));
    return File::Spec->rel2abs($url, dirname($doc->URI));
  } else {
    die "no such reffile $name\n";
  }
}

print STDERR "T_FILE: $t_file\n";
print STDERR "TREE NO: $n\n";

# read t_file
my ($a_file, $a_id);
{
  my $i=0;
  my $doc = parse( file=> $t_file,
		ns => { pml => PML_NS },
		paths => [ '/pml:tdata/pml:head', '/pml:tdata/pml:meta' ],
		depth => 2,
		name => 'LM',
		code => sub { ++$i == $n }
	      );
  my $xpc = XML::LibXML::XPathContext->new($doc);
  $xpc->registerNs( pml => PML_NS );
  $a_id = $xpc->findvalue(q{ string(//pml:atree.rf[1]) });
  $a_id =~ s/^.*?#//g;
  $a_file = reffile($doc,'adata',1);
  save($doc);
}

print STDERR "A_FILE: $a_file\n";
print STDERR "ID: $a_id\n";

# read a_file
my ($m_file, $m_id);
{
  my $doc = parse( file=> $a_file,
		ns => { pml => PML_NS },
		paths => [ '/pml:adata/pml:head', '/pml:adata/pml:meta' ],
		depth => 2,
		name => 'LM',
		code => sub { $_[0]->getAttribute('id') eq $a_id }
	      );
  my $xpc = XML::LibXML::XPathContext->new($doc);
  $xpc->registerNs( pml => PML_NS );
  $m_id = $xpc->findvalue(q{ string(//pml:s.rf[1]) });
  $m_id =~ s/^.*?#//g;
  $m_file = reffile($doc,'mdata',1);
  reffile($doc,'wdata',1); # update only
  save($doc);
}

print STDERR "M_FILE: $m_file\n";
print STDERR "ID: $m_id\n";

# read m_file
my ($w_file, @w_ids);
{
  my $doc = parse( file=> $m_file,
		ns => { pml => PML_NS },
		paths => [ '/pml:mdata/pml:head', '/pml:mdata/pml:meta' ],
		depth => 1,
		name => 's',
		code => sub { $_[0]->getAttribute('id') eq $m_id }
	      );
  my $xpc = XML::LibXML::XPathContext->new($doc);
  $xpc->registerNs( pml => PML_NS );
  @w_ids = map { my $id = $_->textContent;
		 $id =~ s/^.*?#//g;
		 $id
	       } $xpc->findnodes(q{ //pml:w.rf[not(*)]|//pml:w.rf/pml:LM });
  $w_file = reffile($doc,'wdata',1);
  save($doc);
}

print STDERR "W_FILE: $w_file\n";
print STDERR "IDs: @w_ids\n";

# read w_file
{
  my %w_ids; @w_ids{@w_ids}=();
  my $doc = parse( file=> $w_file,
		ns => { pml => PML_NS },
		paths => [ '/pml:wdata/pml:head', '/pml:wdata/pml:meta', '/pml:wdata/pml:doc/pml:docmeta' ],
		depth => 3,
		name => 'w',
		code => sub { 
		  my $id = $_[0]->getAttribute('id');
		  my $ret= exists $w_ids{ $id };
		  delete $w_ids{ $id };
		  return $ret;
		},
		finish => sub { %w_ids==0 }
	      );
  save($doc);
}

