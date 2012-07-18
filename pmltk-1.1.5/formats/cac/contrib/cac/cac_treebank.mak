# -*- cperl -*-
#encoding iso-8859-2

package AcademicTreebank;

BEGIN { import TredMacro; }
use vars qw(%ORIGA_INFO %ORIGT_INFO %ORIGS_INFO @ORIGT_INFO);

sub cac_file_detected {
  my $fsfile = CurrentFile();
  if ($fsfile and
      $fsfile->FS->exists('x_origt') and
      $fsfile->FS->exists('x_origa')) {
    return 1;
  }
  return;
}

push @TredMacro::AUTO_CONTEXT_GUESSING, sub {
  return cac_file_detected() ? 'AcademicTreebank' : ();
};

sub allow_switch_context_hook {
  return cac_file_detected() ? 1 : 'stop';
}

#ifinclude <contrib/pdt10_a/pdt_tags.mak>
#bind show_tag to Alt+t menu Describe PDT tag
sub show_tag {
  describe_tag($this->{tag});
  ChangingFile(0);
}

#bind show_x_origa to Alt+a menu Describe CAC syntactic-analytic tag
sub show_x_origa {
  describe_x_origa($this->{origa}) if $this->{origa} ne "";
  ChangingFile(0);
}

#bind swap_afun_from_dep_type to Alt+z menu Swap afun value with the one in dep_type
sub swap_afun_from_dep_type {
    my ($first, $new_afun, $last) = $this->{dep_type} =~ /^(.*)-a\(([^\(\)]*)\)(.*)$/;
    if($new_afun ne "") {
        $this->{dep_type} = $first."-A(".$this->{afun}.")".$last;
        $this->{afun} = $new_afun;
    }
    ChangingFile(0);
}

#bind accept_afun_as_it_is to Alt+x menu Accept afun as it is
sub accept_afun_as_it_is {
  my ($first, $last);
  $first = $last = "";
    if($this->{dep_type} =~ /-a/) {
        ($first, $last) = $this->{dep_type} =~ /^(.*)-a(.*)$/;
        $this->{dep_type} = $first."-A".$last;
    }
    ChangingFile(0);
}

#bind accept_dep_as_it_is to Alt+c menu Accept dep as it is
sub accept_dep_as_it_is {
  my ($first,$last);
  $first = $last = "";
    if($this->{dep_type} =~ /-d/) {
        ($first, $last) = $this->{dep_type} =~ /^(.*)-d(.*)$/;
        $this->{dep_type} = $first."-D".$last;
    }
    ChangingFile(0);
}

#bind accept_tree_as_it_is to Alt+1 menu Accept tree as it is
sub accept_tree_as_it_is {
    my $current = $this;
    $this = $this->root;
    while ($this) {
        if($this->{dep_type} =~ /-a/) {
            my ($first, $last) = $this->{dep_type} =~ /^(.*)-a(.*)$/;
            $this->{dep_type} = $first."-A".$last;
        }
        if($this->{dep_type} =~ /-d/) {
            my ($first, $last) = $this->{dep_type} =~ /^(.*)-d(.*)$/;
            $this->{dep_type} = $first."-D".$last;
        }
        $this=$this->following;
    }
    $this = $current;
    ChangingFile(0);
}




#bind show_x_origs to Alt+s menu Describe CAC clause tag
sub show_x_origs {
  describe_x_origs($this->{origs}) if $this->{origs} ne "";
  ChangingFile(0);
}


#bind show_x_origt to Alt+m menu Describe CAC morphological tag
sub show_x_origt {
  describe_x_origt($this->{origt}) if $this->{origt} ne "";
  ChangingFile(0);
}

# style modification
sub default_ar_attrs {

    return unless $grp->{FSFile};
    my $pattern = 'style:<? if($${dep_type}) { my $p = $this->parent; if ($p and ($p->{ord} eq $${dep_type})) {"#{Line-fill:blue}";}} ?>';
    my ($hint, $cntxt, $style) = GetStylesheetPatterns();
    my @filter = grep { $_ ne $pattern } @{$style};
    SetStylesheetPatterns([ $hint, $cntxt, [ @filter, $pattern ] ]);
    ChangingFile(0);
    return 1;
}




#bind toggle_cac_tree to Alt+r menu Toggle showing CAC tree structure
our %show_rebuilt_tree;
sub toggle_cac_tree {
  # first toggle
  $show_rebuilt_tree{$grp}=!$show_rebuilt_tree{$grp};

  # now take the opportunity to cleanup the window hash
  my %windows; @windows{ TrEdWindows() } = ();
  for (keys %show_rebuilt_tree) {
    delete $show_rebuilt_tree{$_} unless exists $windows{$_};
  }
}

sub get_nodelist_hook {
  my ($fsfile,$treeNo,$recentActiveNode,$show_hidden)=@_;
  return unless $fsfile;
  unless ($show_rebuilt_tree{$grp}) {
    return [ $fsfile->nodes($treeNo,$recentActiveNode,$show_hidden) ];
  }

  my $tree = $fsfile->tree($treeNo) || return;
  my $active_pos=-1;
  while ($recentActiveNode) {
    $active_pos++;
    $recentActiveNode=$recentActiveNode->previous;
  }
  my $clone = $fsfile->FS->clone_subtree($tree);
  {
    local $root = $clone;
    rebuild_cac_tree();
  }
  my @nodes = ($clone, $clone->descendants);
  my $active = $active_pos>=0 ? $nodes[$active_pos] : $clone;
  return [
    [ sort { $a->{ord} <=> $b->{ord} } @nodes ],
    $active
  ];
}

sub rebuild_cac_tree {
  # prepare style
  default_ar_attrs();

  my @nodes = sort {$a->{origr} <=> $b->{origr}}
    grep { $_->{origr} ne "" }
      $root->descendants;

  for (my $i=0; $i<@nodes;$i++) {
    my $n = $nodes[$i];

    if ($n->{origa} ne "") {
      my $plusminus = substr($n->{origa},2,1);
      my $delta = substr($n->{origa},3,2);
      my $p;

      # KR: distinguish type - begin
      #   if ($plusminus eq '_' or $plusminus eq '-' or $plusminus eq '+') {$n->{dep_type} = "00";}
      # KR: distinguish type - end

      if ($plusminus eq '+') {
        $p = $nodes[$i+$delta];
      } elsif ($plusminus eq '_' or $plusminus eq '-') {
        $p = $nodes[$i-$delta];
      }
      
      if ($p) {
	$n->{dep_type} = $p->{ord};
      }				# KR
      
      CutPaste($n,$p) if ($p && node_is_in_subtree_of($p,$n) == 0) ;
    } elsif ($n->{origt}=~/^81/) {
      my $j = $i+1;
      while ($j<@nodes) {
	my $p = $nodes[$j];
	if ($p->{origt} ne "" and substr($p->{origa},5,1) =~ /[12]/) {
	  CutPaste($n,$p);
	  last;
	}
	$j++
      }
    } else {
      my $j = $i+1;
      while ($j<@nodes) {
        my $p = $nodes[$j];
        if ($p->{origt} ne "" and substr($p->{origt},2,1) eq '7') {
	  CutPaste($n,$p);
	  last;
	}
        $j++
      }
    }
  }

### 16/2/09 BH: zobrazit puvodni zavislosti; ted nepotrebuji videt, kam by se mely povesit predlozky
###  # put prespositions
###  my ($n, $ntag, $ntag1, $nn, $nntag, $nntag1, $nn_parent);
###  for (my $i=0; $i<@nodes;$i++) {

###    $n = $nodes[$i];
###    $ntag = $n->{'m'}{tag};
###    ($ntag1) = $ntag =~ /(.)............../;

###    # get next node and tag if current one is a prepostion and find the noun the preposion is supposed to be attached to
###    if($ntag1 eq 'R' and $i<@nodes) {
###        $nn = $nodes[$i+1];
###        $nntag = $nn->{'m'}{tag};
###        ($nntag1) = $nntag =~ /(.)............../;

###        # if the next node is not a noun, go up the tree until you find a noun
###        while($nntag1 ne 'N' and $nn->parent) {
###            $nn = $nn->parent;
###            $nntag = $nn->{'m'}{tag};
###            ($nntag1) = $nntag =~ /(.)............../;
###        }

###        # if the noun was found
###        #   attach the preposition to the parent of the noun
###        #   and the subtree of the noun to the preposition
###        if($nntag1 eq 'N') {
###            #get parent of the noun (that is of $nn)
###            if($nn->parent) {
###                $nn_parent = $nn->parent;

###                # exchange subtrees
###                CutPaste($n, $nn_parent);
###                CutPaste($nn, $n);
###            }
###        }
###  #  print "$ntag1-$nntag1  ";
###    }


###  } # end for

}

sub node_is_in_subtree_of {
  my ($node, $subtree) = @_;
  while ($node) {
     return 1 if $node == $subtree;
     $node = $node->parent;
  }
  return 0;
}


sub do_edit_attr_hook {
  my ($atr,$node)=@_;
  print "$atr\n";
  if ($atr eq 'origa') {
    show_x_origa();
    return 'stop';
  } elsif ($atr eq 'origt') {
    show_x_origt();
    return 'stop';
  } elsif ($atr eq 'origs') {
    show_x_origs();
    return 'stop';
  } elsif ($atr eq 'tag') {
    show_tag();
    return 'stop';
  }
  return 1;
}

sub describe_x_origa {
  my ($tag) = @_;
  my @sel;
  my @val =
    map { TrEd::Convert::encode($_) }
    map {
    if ($_ == 4) {
      if (substr($tag,3,2)=~/\S/) {
    substr($tag,3,2)." = pozice ��d�c�ho slova";
      } else { () }
    } elsif ( $_ <= length($tag) ) {
      my $w = substr($tag,0,$_);
      my $v = substr($tag,$_-1,1);
      if (exists $ORIGA_INFO{"$_;$w"}) {
    "$v  = ".$ORIGA_INFO{"$_;$w"}
      } elsif (exists $ORIGA_INFO{"$_;$v"}) {
    "$v  = ".$ORIGA_INFO{"$_;$v"}
      } elsif ($v eq " ") {
    ()
      } else {
    "$v  = UNKNOWN"
      }
    } else { () }
  } 1,2,3,4,6;
  ListQuery("$tag - detailed info",
        'browse',
        \@val,
        \@sel);
  ChangingFile(0);
  return;
}

sub get_x_origt_description {
  my ($tag) = @_;
  if (exists $ORIGT_INFO{"$tag;desc"}) {
    return "$tag = ".$ORIGT_INFO{"$tag;desc"};
  } else {
    my $POS=substr($tag,0,1);
    if (exists $ORIGT_INFO{"$POS;desc"}) {
      my @v = ("$POS = ".$ORIGT_INFO{"$POS;desc"});
      for my $pos (2..length($tag)) {
    my $v=substr($tag,$pos-1,1);
    my $w=substr($tag,0,$pos);
    if (exists $ORIGT_INFO{"$POS;$pos;$w;desc"}) {
      push @v,"$v = ".$ORIGT_INFO{"$POS;$pos;$w;desc"};
    } elsif (exists $ORIGT_INFO{"$POS;$pos;$v;type"}) {
      push @v,"$v = ".$ORIGT_INFO{"$POS;$pos;$v;desc"};
    } elsif ($v ne ' ') {
      push @v,"$v = UNKNOWN";
    }
      }
      return @v;
    } else {
      return "$POS = UNKNOWN";
    }
  }
}

sub describe_x_origt {
  my ($tag) = @_;
  my @sel;
  my @val = map { TrEd::Convert::encode($_) } get_x_origt_description($tag);
  ListQuery("$tag - detailed info",
        'browse',
        \@val,
        \@sel);
  ChangingFile(0);
  return;
}

sub describe_x_origs {
  my ($tag) = @_;
  my (@val,@sel);
  return if $tag eq "";
  my $num = substr($tag,0,2);
  my $zacatek = "pokra�ov�n�";
  if ($num =~ /^9\d$/) {
    $zacatek = "za��tek";
    $num-=90;
  }
  @val =
    map { TrEd::Convert::encode($_) }
    (substr($tag,0,2)." = ".int($num).". v�ta v souv�t� ($zacatek)",
         (map {
           if ( $_ <= length($tag) ) {
         my $v = substr($tag,$_-1,1);
         if (exists $ORIGS_INFO{"$_;$v"}) {
           "$v  = ".$ORIGS_INFO{"$_;$v"}
         } elsif ($v eq " ") {
           ()
         } else {
           "$v  = UNKNOWN"
         }
           } else { () }
         } 3..9)
        );
  ListQuery("$tag - detailed info",
        'browse',
        \@val,
        \@sel);
  ChangingFile(0);
  return;
}


%ORIGA_INFO = map { my ($pos, $val, $type, $desc) = split /\S*\|\S*/,$_,4;
            $val=$1 if ($val =~ /'(.)'/);
             ("$pos;$val" => $desc) } split /\n/, <<'EOF';
1   |1   |NA  |subjekt
3   |'-' |NA  |��d�c� slovo vlevo
3   |'_' |NA  |��d�c� slovo vlevo
4-5 |o   |NA  |kolik slov vlevo/vpravo je slovo ��d�c� (u druh�ho a dal��ch se ud�v� ��slo �lenu nejbli���ho; stejn� se zachycuj� vztahy mezi �leny sdru�en�ho pojmenov�n� i v p��pad�, �e nejsou samost. syntaktick�mi �leny);vzd�lenosti men�� ne� deset se zapisuj� 01, 02, ..., 09
6   |1   |NA  |koordinace (uv�d� se pouze u druh�ho a dal��ch �len� koordina�n� �ady)
1   |2   |NA  |predik�t
2   |21  |NA  |slovesn�
3   |'+' |NA  |��d�c� slovo vpravo
6   |2   |NA  |sdru�en� pojmenov�n� determina�n� povahy
2   |2   |NA  |spona
6   |3   |NA  |koordinace uvnit� sdru�en�ho pojmenov�n�
2   |23  |NA  |nom. ��st spon. pred.
6   |4   |NA  |sdru�en� pojmenov�n� jin�
2   |24  |NA  |nomin.
6   |5   |NA  |sdru�en� pojmenov�n� v koordinaci s jin�m sdru�en�m pojmenov�n�m
2   |5   |NA  |spona u jedno�l. v.
6   |6   |NA  |dvojice spojkov� a p��slove�n�
1   |3   |NA  |atribut
2   |31  |NA  |atribut
6   |9   |NA  |��d�c� v�raz elidov�n
2   |32  |NA  |apozice
6   |0   |NA  |��d�c� v�raz vy�azen
1   |4   |NA  |objekt
2   |41  |NA  |objekt
6   |7   |NA  |non-identical reduplication of a word
2   |42  |NA  |dopln�k
6   |8   |NA  |identical reduplication of a word
1   |5   |NA  |adverbi�le
2   |51  |NA  |m�sta
2   |52  |NA  |�asu
2   |53  |NA  |zp�sobu
2   |54  |NA  |p���iny
2   |55  |NA  |p�vodu
2   |56  |NA  |p�vodce
2   |57  |NA  |v�sledku
1   |6   |NA  |z�klad v�ty jedno�lenn�
2   |61  |NA  |substantivn�
2   |62  |NA  |adjektivn�
2   |63  |NA  |citoslove�n�
2   |64  |NA  |��sticov�
2   |65  |NA  |vokativn�
2   |66  |NA  |p��slove�en�
2   |67  |NA  |infinitivn�
2   |68  |NA  |slovesn�
2   |69  |NA  |slovesn� jmenn�
2   |60  |NA  |z�jmenn�
1   |7   |NA  |p�ech. typ (s v�eobecn�m subjektem)
1   |8   |NA  |samostatn� v�tn� �len
1   |9   |NA  |parant�ze
EOF


@ORIGT_INFO =
  map { chomp; [split /\S*\|\S*/] }
  grep /\S/,
  split /\n/, <<'EOF';
1   |1   |slovn� druh SUBSTANTIVUM
2   |1   |NA  nesporn�
3   |0   |valence bez p�edlo�ky
4   |1   |rod m. �iv.
5   |1   |��slo   singul�r
6   |1   |p�d nominativ
7   |8   |NA  neskl.
8   |4   |spisovnost  zastaral�
2   |2   |NA  adj.
3   |7   |valence s p�edlo�kou
4   |2   |rod m. ne�.
5   |2   |��slo   plur�l
6   |2   |p�d genitiv
7   |3   |NA  zvrat.
8   |5   |spisovnost  nespis.
2   |3   |NA  z�jm.
4   |3   |rod fem.
5   |3   |��slo   du�l
6   |3   |p�d dativ
8   |6   |spisovnost  v�pl�k. nespis.
2   |4   |NA  ��sl.
4   |4   |rod neutrum
5   |4   |��slo   pomn.
6   |4   |p�d akuzativ
8   |7   |spisovnost  v�pl�k.
2   |5   |NA  slov.
4   |9   |rod nelze ur�it
5   |9   |��slo   nelze ur�it
6   |5   |p�d vokativ
2   |6   |NA  slov. zvr.
6   |6   |p�d lok�l
2   |7   |NA  zkratka
6   |7   |p�d instrument�l
2   |9   |NA  zkr. slovo
6   |9   |p�d nelze ur�it
2   |0   |NA  vl. jm�no

1   |2   |slovn� druh ADJEKTIVUM
2   |22  |NA  nesporn�
3   |221 |NA  jm. tvar
3   |222 |NA  jm. zvrat.
2   |23  |NA  z�jmenn�
3   |231 |NA  jm. tvar
3   |232 |NA  neur�it�
3   |234 |NA  ukazovac�
3   |235 |NA  t�zac�
3   |236 |NA  vzta�n�
3   |237 |NA  z�porn�
3   |238 |NA  p�ivl.
2   |24  |NA  ��slovka
3   |242 |NA  �adov�
3   |243 |NA  druhov�
3   |244 |NA  n�sobn�
3   |245 |NA  neur�it�
2   |25  |NA  slovesn�
3   |250 |NA  zvratn�
3   |251 |NA  jmenn�
3   |252 |NA  jm. zvrat.
2   |27  |NA  jmenn� st�. rodu
2   |29  |NA  p�ivlast�ovac�
4   |1   |rod m. �iv.
5   |1   |��slo   singul�r
6   |1   |p�d nominativ
8   |4   |spisovnost  zastaral�
4   |2   |rod m. ne�.
5   |2   |��slo   plur�l
6   |2   |p�d genitiv
8   |5   |spisovnost  nespis.
4   |3   |rod fem.
5   |3   |��slo   du�l
6   |3   |p�d dativ
7   |2   |stupe�  II.st.
8   |6   |spisovnost  v�pl�k. nespis.
4   |4   |rod neutrum
5   |4   |��slo   pomn.
6   |4   |p�d akuzativ
7   |3   |stupe�  III.st
8   |7   |spisovnost  v�pl�k.
4   |9   |rod nelze ur�it
5   |9   |��slo   nelze ur�it
6   |5   |p�d vokativ
7   |8   |stupe�  neskl.
6   |6   |p�d lok�l
6   |7   |p�d instrument�l
6   |9   |p�d nelze ur�it

1   |3   |slovn� druh Z�JMENO
2   |1   |NA  osobn�
3   |0   |valence bez p�edlo�ky
4   |1   |rod m. �iv.
5   |1   |��slo   singul�r
6   |1   |p�d nominativ
7   |1   |tvar    krat��
8   |4   |spisovnost  zastaral�
2   |2   |NA  neur�it�
3   |7   |valence s p�edlo�kou
4   |2   |rod m. ne�.
5   |2   |��slo   plur�l
6   |2   |p�d genitiv
7   |2   |tvar    del��
8   |5   |spisovnost  nespis.
2   |3   |NA  zvratn�
4   |3   |rod fem.
5   |3   |��slo   du�l
6   |3   |p�d dativ
8   |6   |spisovnost  v�pl�k. nespis.
2   |4   |NA  ukazovac�
4   |4   |rod neutrum
5   |9   |��slo   nelze ur�it
6   |4   |p�d akuzativ
8   |7   |spisovnost  v�pl�k.
2   |5   |NA  t�zac�
4   |7   |rod bezrod�
6   |5   |p�d vokativ
2   |6   |NA  vzta�n�
4   |9   |rod nelze ur�it
6   |6   |p�d lok�l
2   |7   |NA  z�porn�
6   |7   |p�d instrument�l
6   |9   |p�d nelze ur�it

1   |4   |slovn� druh ��SLOVKA
2   |1   |NA  z�kladn�
3   |0   |valence bez p�edlo�ky
4   |1   |rod m. �iv.
5   |1   |��slo   singul�r
6   |1   |p�d nominativ
7   |1   |p�d nom.
8   |4   |spisovnost  zastaral�
2   |3   |NA  druhov�
3   |7   |valence s p�edlo�kou
4   |2   |rod m. ne�.
5   |2   |��slo   plur�l
6   |2   |p�d genitiv
7   |2   |p�d gen.
8   |5   |spisovnost  nespis.
2   |4   |NA  n�sobn�
4   |3   |rod fem.
5   |3   |��slo   du�l
6   |3   |p�d dativ
7   |3   |p�d dat.
8   |6   |spisovnost  v�pl�k. nespis.
2   |5   |NA  neur�it�
4   |4   |rod neutrum
5   |4   |��slo   pomn.
6   |4   |p�d akuzativ
7   |4   |p�d akz.
8   |7   |spisovnost  v�pl�k.
2   |6   |NA  pod�ln�
4   |7   |rod bezrod�
5   |9   |��slo   nelze ur�it
6   |5   |p�d vokativ
7   |5   |p�d vok.
4   |8   |rod neskl.
6   |6   |p�d lok�l
7   |6   |p�d lok.
4   |9   |rod nelze ur�it
6   |7   |p�d instrument�l
7   |7   |p�d ins.
6   |9   |p�d nelze ur�it
7   |9   |p�d nelze ur�it

1   |5   |slovn� druh SLOVESO
2   |1   |NA  dokonav�
3   |1   |osoba ��slo 1.sg.
4   |1   |�as zp�sob  ind.pr�z.akt.
5   |1   |imperativ   imp.akt.
6   |1   |NA  jednosl.
7   |1   |jmenn� rod  m.�iv.
8   |4   |spisovnost  zastaral�
2   |2   |NA  nedokonav�
3   |2   |osoba ��slo 2.sg.
4   |2   |�as zp�sob  ind.pr�z.pas.
5   |2   |imperativ   imp.pas.
6   |2   |NA  v�cesl. nezvr.
7   |2   |jmenn� rod  m.ne�.
8   |5   |spisovnost  nespis.
2   |3   |NA  obojvid�
3   |3   |osoba ��slo 3.sg.
4   |3   |�as zp�sob  kond.pr�z.ak.
5   |3   |imperativ   kond.pf.akt.
6   |7   |NA  zvrat. neslo�.
7   |3   |jmenn� rod  fem.
8   |6   |spisovnost  v�pl�k. nespis.
3   |4   |osoba ��slo 1.pl.
4   |4   |�as zp�sob  kond.pr�z.ps.
5   |4   |imperativ   kond.pf.pas.
6   |8   |NA  zvrat. slo�.
7   |4   |jmenn� rod  neutr.
8   |7   |spisovnost  v�pl�k.
3   |5   |osoba ��slo 2.pl.
4   |5   |�as zp�sob  ind.pr�t.akt.
5   |5   |imperativ   p.sl. ``b�vati''
6   |9   |NA  pf.stav.p��t.
7   |5   |jmenn� rod  pl.m.�.
3   |6   |osoba ��slo 3.pl.
4   |6   |�as zp�sob  ind.pr�t.pas.
5   |6   |imperativ   part.pas.
6   |0   |NA  pf.stav.min.
7   |6   |jmenn� rod  pl.m.n.
3   |7   |osoba ��slo inf.ak.
4   |7   |�as zp�sob  kond.min.akt.
5   |7   |imperativ   p�ech.p�.akt.
7   |7   |jmenn� rod  pl.fem.
3   |8   |osoba ��slo inf.ps.
4   |8   |�as zp�sob  kond.min.pas.
5   |8   |imperativ   p�ech.p�.ps.
7   |8   |jmenn� rod  pl.neu.
3   |9   |osoba ��slo neos.
4   |9   |�as zp�sob  fut.ind.akt.
5   |9   |imperativ   p�ech.min.ak.
7   |9   |jmenn� rod  nelze ur�it
4   |0   |�as zp�sob  fut.ind.pas.
5   |0   |imperativ   p�ech.min.ps.

1   |6   |slovn� druh ADVERBIUM
2   |6   |NA  nesporn�
2   |2   |NA  predik.
4   |2   |stupe�  II. st.
8   |4   |spisovnost  zastaral�
2   |3   |NA  z�jmenn�
4   |3   |stupe�  III. st.
8   |5   |spisovnost  nespis.
2   |4   |NA  ��seln�
3   |4   |NA  n�sobn�
8   |6   |spisovnost  v�pl�k. nespis.
3   |6   |NA  pod�ln�
8   |7   |spisovnost  v�pl�k.
3   |5   |NA  neur�it�
2   |8   |NA  spojovac� v�raz

1   |7   |slovn� druh P�EDLO�KA
2   |7   |NA  vlastn�
3   |2   |p�d s genitivem
8   |4   |spisovnost  zastaral�
2   |8   |NA  nevlastn�
3   |3   |p�d s dativem
8   |5   |spisovnost  nespis.
3   |4   |p�d s akuzativem
3   |6   |p�d s lok�lem
3   |7   |p�d s instrument�lem

1   |8   |slovn� druh SPOJKA
2   |1   |NA  sou�ad�c�
8   |4   |spisovnost  zastaral�
2   |2   |NA  pod�ad�c�
8   |5   |spisovnost  nespis.
2   |9   |NA  jin� v�raz

1   |9   |slovn� druh CITOSLOVCE
2   |9   |NA  nesporn�
8   |4   |spisovnost  zastaral�
2   |1   |NA  subst.
8   |5   |spisovnost  nespis.
2   |2   |NA  adj.
8   |6   |spisovnost  v�pl�k. nespis.
2   |3   |NA  z�jm.
8   |7   |spisovnost  v�pl�k.
2   |5   |NA  slov.
2   |6   |NA  adv.

1   |000 |slovn� druh CIT�TOV� V�RAZY
1   |0   |slovn� druh ��STICE
EOF

%ORIGT_INFO=();
do {{
  no warnings(qw(uninitialized));
  my $last1=undef;
  foreach (@ORIGT_INFO) {
    my ($pos, $val, $type, $desc)=@$_;
    next unless defined $pos;
    if ($pos==1) {
      $last1=$val;
      $ORIGT_INFO{"$val;type"}=$type;
      $ORIGT_INFO{"$val;desc"}=$desc;
    } else {
      $ORIGT_INFO{"$last1;$pos;$val;type"} = $type;
      $ORIGT_INFO{"$last1;$pos;$val;desc"} = $desc;
    }
  }
}};

%ORIGS_INFO = map { my ($pos, $val, $type, $desc) = split /\S*\|\S*/,$_,4;
            $val= (defined($val) and ($val =~ /'(.)'/)) ? $1 : "";
             ("$pos;$val" => $desc) } split /\n/, <<'EOF';
1-2 ��slo   ��slo v�ty  v�ty uvnit� v�tn�ho celku (po�ad� v�ty v souv�t�);(��sla v�ty se zapisuj� do dvou sloupc�, 02, 10, 11) (t�m� ��slem se zna�� i pokra�ov�n� v�ty za v�tou vlo�enou); za��tek v�tn�ho celku m� v prvn�m sloupci 9 m�sto 0 (91)
3   |1   |druh    |jednoduch�
3   |2   |druh    |hlavn�
3   |3   |druh    |vedlej��
3   |4   |druh    |???
4   |1   |v�ty    |v�ta subjektov�
4   |2   |v�ty    |v�ta predik�tov�
4   |3   |v�ty    |v�ta atributivn�
4   |4   |v�ty    |v�ta objektov�
4   |5   |v�ty    |v�ta m�stn�
4   |6   |v�ty    |v�ta �asov�
4   |7   |v�ty    |v�ta zp�sobov�
4   |8   |v�ty    |v�ta p���inn�
4   |9   |v�ty    |v�ta dopl�kov�
5   |!   |��slo ��|d�c�ho jm�na v�ty atributivn�   neprav� v�ta vzta�n�
5   |    |��slo ��|d�c�ho jm�na v�ty atributivn�   9
5   |0   |��slo ��|d�c�ho jm�na v�ty atributivn�   v�ce ne� 9
5   |1   |��slo ��|d�c�ho jm�na v�ty atributivn�   z�vislost na bezprost�edn� p�edch�zej�c�m jm�nu
5   |2   |��slo ��|d�c�ho jm�na v�ty atributivn�   z�vislost na 2.,
5   |3   |��slo ��|d�c�ho jm�na v�ty atributivn�   z�vislost na jm�nu p�ed vzta�nou v�tou
8   |!   |vztahy m|ezi v�tami  chyba ve stavb� souv�t�
8   |1   |vztahy m|ezi v�tami  koordinace
8   |2   |vztahy m|ezi v�tami  parent�ze
8   |3   |vztahy m|ezi v�tami  p��m� �e�
8   |5   |vztahy m|ezi v�tami  parent�ze v p��m� �e�i
8   |6   |vztahy m|ezi v�tami  uvozovac� v�ta
8   |7   |vztahy m|ezi v�tami  ???
8   |8   |vztahy m|ezi v�tami  parent�ze v uvoz. v�t�
9   1   ??? ???
EOF
