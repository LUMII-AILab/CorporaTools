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
    substr($tag,3,2)." = pozice øídícího slova";
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
  my $zacatek = "pokraèování";
  if ($num =~ /^9\d$/) {
    $zacatek = "zaèátek";
    $num-=90;
  }
  @val =
    map { TrEd::Convert::encode($_) }
    (substr($tag,0,2)." = ".int($num).". vìta v souvìtí ($zacatek)",
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
3   |'-' |NA  |øídící slovo vlevo
3   |'_' |NA  |øídící slovo vlevo
4-5 |o   |NA  |kolik slov vlevo/vpravo je slovo øídící (u druhého a dal¹ích se udává èíslo èlenu nejbli¾¹ího; stejnì se zachycují vztahy mezi èleny sdru¾eného pojmenování i v pøípadì, ¾e nejsou samost. syntaktickými èleny);vzdálenosti men¹í ne¾ deset se zapisují 01, 02, ..., 09
6   |1   |NA  |koordinace (uvádí se pouze u druhého a dal¹ích èlenù koordinaèní øady)
1   |2   |NA  |predikát
2   |21  |NA  |slovesný
3   |'+' |NA  |øídící slovo vpravo
6   |2   |NA  |sdru¾ené pojmenování determinaèní povahy
2   |2   |NA  |spona
6   |3   |NA  |koordinace uvnitø sdru¾eného pojmenování
2   |23  |NA  |nom. èást spon. pred.
6   |4   |NA  |sdru¾ené pojmenování jiné
2   |24  |NA  |nomin.
6   |5   |NA  |sdru¾ené pojmenování v koordinaci s jiným sdru¾eným pojmenováním
2   |5   |NA  |spona u jednoèl. v.
6   |6   |NA  |dvojice spojkové a pøísloveèné
1   |3   |NA  |atribut
2   |31  |NA  |atribut
6   |9   |NA  |øídící výraz elidován
2   |32  |NA  |apozice
6   |0   |NA  |øídící výraz vyøazen
1   |4   |NA  |objekt
2   |41  |NA  |objekt
6   |7   |NA  |non-identical reduplication of a word
2   |42  |NA  |doplnìk
6   |8   |NA  |identical reduplication of a word
1   |5   |NA  |adverbiále
2   |51  |NA  |místa
2   |52  |NA  |èasu
2   |53  |NA  |zpùsobu
2   |54  |NA  |pøíèiny
2   |55  |NA  |pùvodu
2   |56  |NA  |pùvodce
2   |57  |NA  |výsledku
1   |6   |NA  |základ vìty jednoèlenné
2   |61  |NA  |substantivní
2   |62  |NA  |adjektivní
2   |63  |NA  |citosloveèné
2   |64  |NA  |èásticové
2   |65  |NA  |vokativní
2   |66  |NA  |pøísloveèené
2   |67  |NA  |infinitivní
2   |68  |NA  |slovesné
2   |69  |NA  |slovesnì jmenné
2   |60  |NA  |zájmenné
1   |7   |NA  |pøech. typ (s v¹eobecným subjektem)
1   |8   |NA  |samostatný vìtný èlen
1   |9   |NA  |parantéze
EOF


@ORIGT_INFO =
  map { chomp; [split /\S*\|\S*/] }
  grep /\S/,
  split /\n/, <<'EOF';
1   |1   |slovní druh SUBSTANTIVUM
2   |1   |NA  nesporné
3   |0   |valence bez pøedlo¾ky
4   |1   |rod m. ¾iv.
5   |1   |èíslo   singulár
6   |1   |pád nominativ
7   |8   |NA  neskl.
8   |4   |spisovnost  zastaralé
2   |2   |NA  adj.
3   |7   |valence s pøedlo¾kou
4   |2   |rod m. ne¾.
5   |2   |èíslo   plurál
6   |2   |pád genitiv
7   |3   |NA  zvrat.
8   |5   |spisovnost  nespis.
2   |3   |NA  zájm.
4   |3   |rod fem.
5   |3   |èíslo   duál
6   |3   |pád dativ
8   |6   |spisovnost  výplòk. nespis.
2   |4   |NA  èísl.
4   |4   |rod neutrum
5   |4   |èíslo   pomn.
6   |4   |pád akuzativ
8   |7   |spisovnost  výplòk.
2   |5   |NA  slov.
4   |9   |rod nelze urèit
5   |9   |èíslo   nelze urèit
6   |5   |pád vokativ
2   |6   |NA  slov. zvr.
6   |6   |pád lokál
2   |7   |NA  zkratka
6   |7   |pád instrumentál
2   |9   |NA  zkr. slovo
6   |9   |pád nelze urèit
2   |0   |NA  vl. jméno

1   |2   |slovní druh ADJEKTIVUM
2   |22  |NA  nesporné
3   |221 |NA  jm. tvar
3   |222 |NA  jm. zvrat.
2   |23  |NA  zájmenné
3   |231 |NA  jm. tvar
3   |232 |NA  neurèité
3   |234 |NA  ukazovací
3   |235 |NA  tázací
3   |236 |NA  vzta¾né
3   |237 |NA  záporné
3   |238 |NA  pøivl.
2   |24  |NA  èíslovka
3   |242 |NA  øadová
3   |243 |NA  druhová
3   |244 |NA  násobná
3   |245 |NA  neurèitá
2   |25  |NA  slovesné
3   |250 |NA  zvratné
3   |251 |NA  jmenné
3   |252 |NA  jm. zvrat.
2   |27  |NA  jmenné stø. rodu
2   |29  |NA  pøivlastòovací
4   |1   |rod m. ¾iv.
5   |1   |èíslo   singulár
6   |1   |pád nominativ
8   |4   |spisovnost  zastaralé
4   |2   |rod m. ne¾.
5   |2   |èíslo   plurál
6   |2   |pád genitiv
8   |5   |spisovnost  nespis.
4   |3   |rod fem.
5   |3   |èíslo   duál
6   |3   |pád dativ
7   |2   |stupeò  II.st.
8   |6   |spisovnost  výplòk. nespis.
4   |4   |rod neutrum
5   |4   |èíslo   pomn.
6   |4   |pád akuzativ
7   |3   |stupeò  III.st
8   |7   |spisovnost  výplòk.
4   |9   |rod nelze urèit
5   |9   |èíslo   nelze urèit
6   |5   |pád vokativ
7   |8   |stupeò  neskl.
6   |6   |pád lokál
6   |7   |pád instrumentál
6   |9   |pád nelze urèit

1   |3   |slovní druh ZÁJMENO
2   |1   |NA  osobní
3   |0   |valence bez pøedlo¾ky
4   |1   |rod m. ¾iv.
5   |1   |èíslo   singulár
6   |1   |pád nominativ
7   |1   |tvar    krat¹í
8   |4   |spisovnost  zastaralé
2   |2   |NA  neurèité
3   |7   |valence s pøedlo¾kou
4   |2   |rod m. ne¾.
5   |2   |èíslo   plurál
6   |2   |pád genitiv
7   |2   |tvar    del¹í
8   |5   |spisovnost  nespis.
2   |3   |NA  zvratné
4   |3   |rod fem.
5   |3   |èíslo   duál
6   |3   |pád dativ
8   |6   |spisovnost  výplòk. nespis.
2   |4   |NA  ukazovací
4   |4   |rod neutrum
5   |9   |èíslo   nelze urèit
6   |4   |pád akuzativ
8   |7   |spisovnost  výplòk.
2   |5   |NA  tázací
4   |7   |rod bezrodé
6   |5   |pád vokativ
2   |6   |NA  vzta¾né
4   |9   |rod nelze urèit
6   |6   |pád lokál
2   |7   |NA  záporné
6   |7   |pád instrumentál
6   |9   |pád nelze urèit

1   |4   |slovní druh ÈÍSLOVKA
2   |1   |NA  základní
3   |0   |valence bez pøedlo¾ky
4   |1   |rod m. ¾iv.
5   |1   |èíslo   singulár
6   |1   |pád nominativ
7   |1   |pád nom.
8   |4   |spisovnost  zastaralé
2   |3   |NA  druhová
3   |7   |valence s pøedlo¾kou
4   |2   |rod m. ne¾.
5   |2   |èíslo   plurál
6   |2   |pád genitiv
7   |2   |pád gen.
8   |5   |spisovnost  nespis.
2   |4   |NA  násobná
4   |3   |rod fem.
5   |3   |èíslo   duál
6   |3   |pád dativ
7   |3   |pád dat.
8   |6   |spisovnost  výplòk. nespis.
2   |5   |NA  neurèitá
4   |4   |rod neutrum
5   |4   |èíslo   pomn.
6   |4   |pád akuzativ
7   |4   |pád akz.
8   |7   |spisovnost  výplòk.
2   |6   |NA  podílná
4   |7   |rod bezrodé
5   |9   |èíslo   nelze urèit
6   |5   |pád vokativ
7   |5   |pád vok.
4   |8   |rod neskl.
6   |6   |pád lokál
7   |6   |pád lok.
4   |9   |rod nelze urèit
6   |7   |pád instrumentál
7   |7   |pád ins.
6   |9   |pád nelze urèit
7   |9   |pád nelze urèit

1   |5   |slovní druh SLOVESO
2   |1   |NA  dokonavé
3   |1   |osoba èíslo 1.sg.
4   |1   |èas zpùsob  ind.préz.akt.
5   |1   |imperativ   imp.akt.
6   |1   |NA  jednosl.
7   |1   |jmenný rod  m.¾iv.
8   |4   |spisovnost  zastaralé
2   |2   |NA  nedokonavé
3   |2   |osoba èíslo 2.sg.
4   |2   |èas zpùsob  ind.préz.pas.
5   |2   |imperativ   imp.pas.
6   |2   |NA  vícesl. nezvr.
7   |2   |jmenný rod  m.ne¾.
8   |5   |spisovnost  nespis.
2   |3   |NA  obojvidé
3   |3   |osoba èíslo 3.sg.
4   |3   |èas zpùsob  kond.préz.ak.
5   |3   |imperativ   kond.pf.akt.
6   |7   |NA  zvrat. neslo¾.
7   |3   |jmenný rod  fem.
8   |6   |spisovnost  výplòk. nespis.
3   |4   |osoba èíslo 1.pl.
4   |4   |èas zpùsob  kond.préz.ps.
5   |4   |imperativ   kond.pf.pas.
6   |8   |NA  zvrat. slo¾.
7   |4   |jmenný rod  neutr.
8   |7   |spisovnost  výplòk.
3   |5   |osoba èíslo 2.pl.
4   |5   |èas zpùsob  ind.prét.akt.
5   |5   |imperativ   p.sl. ``bývati''
6   |9   |NA  pf.stav.pøít.
7   |5   |jmenný rod  pl.m.¾.
3   |6   |osoba èíslo 3.pl.
4   |6   |èas zpùsob  ind.prét.pas.
5   |6   |imperativ   part.pas.
6   |0   |NA  pf.stav.min.
7   |6   |jmenný rod  pl.m.n.
3   |7   |osoba èíslo inf.ak.
4   |7   |èas zpùsob  kond.min.akt.
5   |7   |imperativ   pøech.pø.akt.
7   |7   |jmenný rod  pl.fem.
3   |8   |osoba èíslo inf.ps.
4   |8   |èas zpùsob  kond.min.pas.
5   |8   |imperativ   pøech.pø.ps.
7   |8   |jmenný rod  pl.neu.
3   |9   |osoba èíslo neos.
4   |9   |èas zpùsob  fut.ind.akt.
5   |9   |imperativ   pøech.min.ak.
7   |9   |jmenný rod  nelze urèit
4   |0   |èas zpùsob  fut.ind.pas.
5   |0   |imperativ   pøech.min.ps.

1   |6   |slovní druh ADVERBIUM
2   |6   |NA  nesporné
2   |2   |NA  predik.
4   |2   |stupeò  II. st.
8   |4   |spisovnost  zastaralé
2   |3   |NA  zájmenné
4   |3   |stupeò  III. st.
8   |5   |spisovnost  nespis.
2   |4   |NA  èíselné
3   |4   |NA  násobné
8   |6   |spisovnost  výplòk. nespis.
3   |6   |NA  podílné
8   |7   |spisovnost  výplòk.
3   |5   |NA  neurèité
2   |8   |NA  spojovací výraz

1   |7   |slovní druh PØEDLO®KA
2   |7   |NA  vlastní
3   |2   |pád s genitivem
8   |4   |spisovnost  zastaralé
2   |8   |NA  nevlastní
3   |3   |pád s dativem
8   |5   |spisovnost  nespis.
3   |4   |pád s akuzativem
3   |6   |pád s lokálem
3   |7   |pád s instrumentálem

1   |8   |slovní druh SPOJKA
2   |1   |NA  souøadící
8   |4   |spisovnost  zastaralé
2   |2   |NA  podøadící
8   |5   |spisovnost  nespis.
2   |9   |NA  jiný výraz

1   |9   |slovní druh CITOSLOVCE
2   |9   |NA  nesporné
8   |4   |spisovnost  zastaralé
2   |1   |NA  subst.
8   |5   |spisovnost  nespis.
2   |2   |NA  adj.
8   |6   |spisovnost  výplòk. nespis.
2   |3   |NA  zájm.
8   |7   |spisovnost  výplòk.
2   |5   |NA  slov.
2   |6   |NA  adv.

1   |000 |slovní druh CITÁTOVÉ VÝRAZY
1   |0   |slovní druh ÈÁSTICE
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
1-2 èíslo   èíslo vìty  vìty uvnitø vìtného celku (poøadí vìty v souvìtí);(èísla vìty se zapisují do dvou sloupcù, 02, 10, 11) (tým¾ èíslem se znaèí i pokraèování vìty za vìtou vlo¾enou); zaèátek vìtného celku má v prvním sloupci 9 místo 0 (91)
3   |1   |druh    |jednoduchá
3   |2   |druh    |hlavní
3   |3   |druh    |vedlej¹í
3   |4   |druh    |???
4   |1   |vìty    |vìta subjektová
4   |2   |vìty    |vìta predikátová
4   |3   |vìty    |vìta atributivní
4   |4   |vìty    |vìta objektová
4   |5   |vìty    |vìta místní
4   |6   |vìty    |vìta èasová
4   |7   |vìty    |vìta zpùsobová
4   |8   |vìty    |vìta pøíèinná
4   |9   |vìty    |vìta doplòková
5   |!   |èíslo øí|dícího jména vìty atributivní   nepravá vìta vzta¾ná
5   |    |èíslo øí|dícího jména vìty atributivní   9
5   |0   |èíslo øí|dícího jména vìty atributivní   více ne¾ 9
5   |1   |èíslo øí|dícího jména vìty atributivní   závislost na bezprostøednì pøedcházejícím jménu
5   |2   |èíslo øí|dícího jména vìty atributivní   závislost na 2.,
5   |3   |èíslo øí|dícího jména vìty atributivní   závislost na jménu pøed vzta¾nou vìtou
8   |!   |vztahy m|ezi vìtami  chyba ve stavbì souvìtí
8   |1   |vztahy m|ezi vìtami  koordinace
8   |2   |vztahy m|ezi vìtami  parentéze
8   |3   |vztahy m|ezi vìtami  pøímá øeè
8   |5   |vztahy m|ezi vìtami  parentéze v pøímé øeèi
8   |6   |vztahy m|ezi vìtami  uvozovací vìta
8   |7   |vztahy m|ezi vìtami  ???
8   |8   |vztahy m|ezi vìtami  parentéze v uvoz. vìtì
9   1   ??? ???
EOF
