#!/usr/bin/perl

=pod

=head1 hydt-morph2pml.perl

=head2 Description

Converts Hyderabad Treebank *.morph data into pml. Errors and some
suspicious values of attributes are reported.

=head2 Usage

  hydt-morph2pml.perl [--max-sentences|-s num] language file1.morph [file2.morph...]

It is advisable to split large input files into smaller ones to
improve their later processing by TrEd. Output files will be named
C<file1.morph.pml> etc.

=head2 Parameters

B<language> is one of NO (performs no conversion), hindi (hi), bangla
(bn), and telugu (te).

B<--max-sentences|-s num> specifies the maximal number of sentences
per one generated file. Output files are named C<file1.morph.001.pml>
etc.

=head2 Author

  Jan Stepanek (givenname.familyname[at]matfyz.cz)

=cut

use strict;
use warnings;

my @FEATS = qw/lemma wxlemma pos g n p c v t X/;
my %langs = (NO=>'NO',
             hi=>'hi',hindi => 'hi',
             bn=>'bn',bangla => 'bn',
             te=>'te',telugu=>'te');

my $max_sent = 0;
if($ARGV[0] =~ /^-(?:-max-sentences|s)(.*)/){
  shift;
  if($1 and $1 > 0){
    $max_sent = $1;
  }else{
    $max_sent = shift;
  }
}

my $lang = shift;
die "Unknown language $lang\n" unless exists $langs{$lang};
$lang = $langs{$lang};

use FindBin;
use lib "$FindBin::RealBin/../lib";
use open qw(IO :utf8 :std);

my ($tr,%trans);
if($lang ne 'NO'){
  require translit;
  require translit::wc2utf;
  $tr = translit::wc2utf::inicializovat(\%trans,$lang);
}
sub transliterate {
  my $string = shift;
  return unless defined $string;
  if($lang ne 'NO' and $string !~ /NULL/){
    $string = translit::prevest(\%trans,$string,$tr);
  }
  return $string;
} # transliterate

sub print_subtree {
  my ($OUT,$tree,$root,$indent) = @_;
  my $space = ' ' x ++$indent;
  my $node = $tree->{$root};
  print $OUT qq($space <$node->{type});
  print $OUT qq( id="$node->{id}") if exists $node->{id};
  print $OUT ">\n";
  foreach my $attr (sort keys %$node){
    if ($attr eq 'feats' and scalar @{$node->{feats}}){
      print $OUT qq($space  <feats>\n);
      for(my $i=0;$i<=$#FEATS;$i++){
        print $OUT qq($space    <$FEATS[$i]>$node->{feats}[$i]</$FEATS[$i]>\n) if defined $node->{feats}[$i] and length $node->{feats}[$i];
      }
      print $OUT qq($space  </feats>\n);
    }elsif($attr eq 'error' and @{ $node->{$attr} }){
      print $OUT qq($space  <$attr>);
      foreach (@{ $node->{$attr} }){
        print $OUT "<LM>$_</LM>";
      }
      print $OUT qq(</$attr>\n);
    }elsif($attr !~ /^(?:feats|error|id)$/){
      print $OUT qq($space  <$attr>$node->{$attr}</$attr>\n)
        if defined $node->{$attr} and length $node->{$attr} and $attr !~ /^(?:parent|type|name|_.*)$/;
    }
  }

  my $ch = $node->{_children};
  if($ch and @$ch){
    print $OUT "$space  <children>\n";
    foreach my $child (sort {$tree->{$a}{ord} <=> $tree->{$b}{ord}} @$ch){
      print_subtree($OUT,$tree,$child,$indent+2);
    }
    print $OUT "$space  </children>\n";
  }

  print $OUT qq($space </$node->{type}>\n);
} # print_subtree

sub touch {
  my ($tree,$name) = @_;
  my $node = $tree->{$name};
  $node->{_touched} = 1;
  touch($tree,$_) foreach @{ $node->{_children} };
} # touch

sub header {
  my ($OUT,$in_file) = @_;
  print $OUT <<"EOF";
<?xml version="1.0" encoding="utf-8"?>

<hydtmorph xmlns="http://ufal.mff.cuni.cz/pdt/pml/">
 <head>
  <schema href="hydt-morph_schema.xml" />
 </head>
 <meta>
  <annotation_info>
   <desc>Hyderabad Treebank $in_file</desc>
  </annotation_info>
 </meta>
EOF
} # header


while (my $in_file = shift) {
  my $out_file;
  if($max_sent > 0){ 
    $out_file = "$in_file.000.pml";
  }else{
    $out_file = "$in_file.pml";
  }
  open my $IN,"<$in_file";
  open my $OUT,">$out_file";
  print STDERR "Processing: $in_file\n";
  my ($doc_id,           # document id
      $sentence_id,      # sentence id
      $wordid,$chunkid,  # counters for uniq ids
      $sentence_counter, # used if no sentence id's defined in source
      $filecount,        # counts files if chopping to smaller ones
      $sentcount,        # counts sentences for chopping
      $root,             # root of current tree
      $ord,              # ordering of nodes
      $chunk,            # depth (number of nested chunks)
      $name,             # name of the last chunk parsed
      $name_counter,     # used to generate uniq names for words
      @error,            # list of errors for current node
      $tree              # reference to hash with the current tree
     );
  ($wordid,$chunkid,$filecount,$sentcount) = (0,0,0,0);

  header($OUT,$in_file);

  while (my $line = <$IN>){
    chomp $line;

    if($line =~ /<document (?:doc)?id="(.*)">/){
      die "Document inside document.\n" if $doc_id;
      $doc_id = $1 || "doc1";
      $doc_id = "doc$doc_id" if $doc_id !~ /^[_a-zA-Z]/;
      if($max_sent > 0){
        print $OUT qq(<document id="$doc_id)
          .sprintf('_%03d',$filecount).qq(">\n);
      }else{
        print $OUT qq(<document id="$doc_id">\n);
      }

    }elsif($line =~ m:</document>:){
      if($doc_id){
        print $OUT "</document>\n";
        $doc_id = '';
      }else{
        die "End tag for document outside document.\n";
      }

    }elsif($line =~ /<Sentence id="(.*)"/){
      die "Sentence inside sentence\n" if $sentence_id;
      $sentence_id = $1 || $sentence_counter++;
      $sentence_id = "s$sentence_id" if $sentence_id !~ /^[_a-zA-Z]/;

      $sentcount++;
      if ($max_sent and $sentcount > $max_sent){
        print $OUT "</document>\n</hydtmorph>\n";
        close $OUT;
        $sentcount = 1;
        $out_file = "$in_file.".sprintf('%03d',++$filecount).'.pml';
        open $OUT,">$out_file";
        header($OUT,$in_file);
        print $OUT qq(<document id="$doc_id)
          .sprintf('_%03d',$filecount).qq(">\n);
      }

      $ord = 1;
      $tree = {};
      print $OUT qq(<sentence id="$sentence_id">\n);


    }elsif($line =~ m:</Sentence>:){
      if($sentence_id){

        $tree->{0} = {};
        foreach my $name (keys %$tree){
          next if $name eq '0';
          my $parent = $tree->{$name}{parent};
          if(not exists $tree->{$parent}){
            warn "WARNING: Missing parent '$parent' of '$name' at $..\n";
            push @{ $tree->{$name}{error} },'missing-parent';
            $tree->{$name}{parent} = '0';
            $parent = '0';
          }
          push @{ $tree->{$parent}{_children} },$name;
        }

        die "No root at $.\n" unless ref $tree->{0}{_children};

        touch($tree,'0');

        my @left = grep ! $tree->{$_}{_touched},keys %$tree;
        if(@left){
          # find any node in a cycle and start from it
          warn "WARNING: Not connected (@left) at $.\n";
          my %visited;
          my $n = $left[0];
          my $previous;
          while (not exists $visited{$n}){
            $visited{$n} = 1;
            $previous = $n;
            $n = $tree->{$n}{parent};
          }
          push @{ $tree->{$previous}{error} },'not-connected';
          $tree->{$n}{_children} = [grep $_ ne $previous,@{ $tree->{$n}{_children} }];
          $tree->{$previous}{parent} = '0';
          push @{ $tree->{0}{_children} },$previous;
        }


#        foreach my $root (@{ $children->{0}}){
        foreach my $root (sort { $tree->{$a}{ord} <=> $tree->{$b}{ord} }
                          @{ $tree->{0}{_children} }){
          print_subtree($OUT,$tree,$root,0);
        }

        print $OUT "</sentence>\n";
        @error = ();
        $tree = {};
        $sentence_id = '';
      }else{
        die "End tag for sentence outside sentence at $..\n";
      }

    }elsif($line =~ m{^([0-9.]+)\s+\(\(\s+(\S+)(?:\s+<(.+)>)?}){
      my ($num,$phrase,$detail) = ($1,$2,$3);
      my ($drel,$parent,$feats);
      if ($chunk) {
        $parent = $name;
        push @error,'inside-chunk';
        warn "WARNING: Chunk inside chunk at $..\n";
      }
      $chunk++;
      if($detail =~ m{name=([^/]+)}){
        $name = $1;
        if ($name =~ /:/) {
          warn "WARNING: Name '$name' resembles drel at $..\n";
          push @error,'drel-like-name';
        }
      }else{
        $name ||= '0';
        if($name =~ /(.*?-)/){ # first terminal child
          $name = $1.++$name_counter;
        }else{ # next terminal children
          $name = "$name-".++$name_counter;
        }

      }
      if($detail =~ m{drel=([^:/]+)}){
        $drel = $1;
        if($detail =~ m{drel=$drel:([^/]+)}){
          $parent = $1;
        }else{ # parent is root
          $parent = 0;
        }
      }
      if($detail =~ m{af=(.*?)[/]}){
        $feats = [split /,/,"$1,#"];
      }

      if($detail =~ /name=/ && !$name
         or $detail =~ /af=/ && !scalar(@$feats)
         or $detail =~ /drel=/ && !$drel){
        die "Wrong detail format: $detail at $.\n";
      }

      pop @$feats;
      $feats = [] unless @$feats;
      if(exists $tree->{$name}){
        warn "WARNING: Duplicate name '$name' at $.\n";
        $name = "$name|".++$name_counter;
        push @error,'duplicate-name';
      }

      my $lemma = transliterate($feats->[0]);
      unshift @$feats,$lemma;

      $tree->{$name} = {type=>'chunk',
                   id => $lang."c".$chunkid++,
                   phrase=>$phrase,
                   parent=>$parent || '0',
                   drel=>$drel,
                   name=>$name,
                   error=>[@error],
                   feats=>[@$feats],
                   ord=>$ord++};
      @error = ();

    }elsif($line =~ m{^([0-9.]+)\s+(\S+)(?:\s+(\S+)\s+<af=(.+)>)?}){
      my ($num,$form,$phrase,$morph) = ($1,$2,$3,$4);
      die "Word outside chunk.\n" unless $chunk;
      $morph ||= '';
      my $feats = [split /,/,"$morph,#"];
      pop @$feats;
      $feats = [] unless @$feats;
      my $lemma = transliterate($feats->[0]);
      unshift @$feats,$lemma;
      $tree->{"n$ord"} = {type=>'word',
                   id=>$lang."w".$wordid++,
                   parent=>$name,
                   wxform=>$form,
                   form=>transliterate($form),
                   phrase=>$phrase,
                   feats=>[@$feats],
                   error=>[@error],
                   name=>"n$ord",
                   ord=>$ord++};
      @error = ();

    }elsif($line =~ /^\s*\)\)/){
      die "Not inside chunk.\n" unless $chunk;
      $chunk--;

    }else{
      warn "WARNING: Ignored: $line\n" unless $line =~ m,^(?:|</?(?:head|text)>)\s*$,;
    }
  }
  print $OUT "</hydtmorph>\n";
  close $OUT;
}
