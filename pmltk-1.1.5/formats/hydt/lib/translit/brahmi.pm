#!/usr/bin/perl
# Funkce pro přípravu transliterace z indického písma do latinky.
# Copyright © 2007, 2008, 2009 Dan Zeman <zeman@ufal.mff.cuni.cz>
# Licence: GNU GPL
# 2.8.2009: přidána vědecká transliterace (potřebná pro články, ale nepraktická v terminálu)

package translit::brahmi;
use utf8;



# První sloupec = Danova transliterace (podle české výslovnosti, snaha neztrácet informaci, vyhýbá se odděleným znakům pro diakritiku).
# Druhý sloupec = Vědecká transliterace (mezinárodnější a vhodnější do článků, využívá oddělené znaky pro diakritiku).
@altlat =
(
    ['m'.chr(771), 'm'.chr(771)], # čandrabindu = m s vlnovkou
    [chr(241), 'm'.chr(775)], # anusvár = n s vlnovkou, resp. m s tečkou nahoře
    ["'", 'h'.chr(803)], # visarg
    ['a', 'a'],
    ['á', chr(257)],
    ['i', 'i'],
    ['í', chr(299)],
    ['u', 'u'],
    ['ú', chr(363)],
    ['ŕ', 'r'.chr(805)],
    ['ĺ', 'l'.chr(805)],
    [chr(234), chr(234)], # čandra e
    ['e', chr(232)], # krátké e
    ['é', 'e'], # normální e je polodlouhé nebo dlouhé
    ['ai', 'ai'], # vyslovuje se jako ae, otevřené dlouhé e
    ['ô', 'ô'], # čandra o
    ['o', chr(242)], # krátké o
    ['ó', 'o'], # normální o je polodlouhé nebo dlouhé
    ['au', 'au'], # vyslovuje se jako ao, otevřené dlouhé o, jako v anglickém "automatic"
    ['k', 'k'],
    ['kh', 'kh'],
    ['g', 'g'],
    ['gh', 'gh'],
    [chr(331), 'n'.chr(775)], # ng
    ['č', 'c'],
    ['čh', 'ch'],
    ['dž', 'j'],
    ['džh', 'jh'],
    ['ň', chr(241)],
    ['ţ', 't'.chr(803)], # retroflexní t
    ['ţh', 't'.chr(803).'h'],
    [chr(273), 'd'.chr(803)],
    [chr(273).'h', 'd'.chr(803).'h'],
    [chr(326), 'n'.chr(803)],
    ['t', 't'], # zubové t
    ['th', 'th'],
    ['d', 'd'],
    ['dh', 'dh'],
    ['n', 'n'],
    [chr(329), 'n'], # "NNNA", specifické pro tamilštinu
    ['p', 'p'],
    ['ph', 'ph'],
    ['b', 'b'],
    ['bh', 'bh'],
    ['m', 'm'],
    ['j', 'y'],
    ['r', 'r'],
    [chr(343), 'r'], # tvrdé R z jižních jazyků
    ['l', 'l'],
    [chr(316), 'l'.chr(803)], # tvrdé (retroflexní?) L (maráthština)
    ['ř', 'l'], # něco mezi L, americkým R a Ž nebo Ř (tamilština, malajálamština)
    ['v', 'v'],
    ['ś', 'ś'], # normální š
    ['š', 's'.chr(803)], # retroflexní š ze sanskrtu, v hindštině se vyslovuje stejně jako normální š
    ['s', 's'],
    ['h', 'h'],
    ['q', 'q'],
    ['ch', 'x'],
    [chr(287), chr(287)], # hrdelní gh z arabštiny
    ['z', 'z'],
    [chr(343), 'r'.chr(803)], # DDDHA = DDA + NUKTA
    [chr(343).'h', 'r'.chr(803).'h'], # RHA = DDHA + NUKTA
    ['f', 'f'],
    [chr(309), chr(375)], # YYA = YA + NUKTA (zřejmě odpovídá JYA z ISCII (bengálština, ásámština a urijština), výslovnost snad něco mezi j a ď, můj přepis je "j", resp. vědecky "y" se stříškou)
    ['ŕ', 'r'.chr(772).chr(805)], # VOCALIC RR
    ['ĺ', 'l'.chr(772).chr(805)], # VOCALIC LL
    ['óm', 'om'], # posvátná slabika z modliteb
);



#------------------------------------------------------------------------------
# Uloží do hashe přepisy souhlásek a slabik. Odkaz na cílový hash převezme jako
# parametr. Vrátí délku nejdelšího řetězce, jehož přepis je v hashi definován.
#------------------------------------------------------------------------------
sub inicializovat
{
    # Odkaz na hash, do kterého se má ukládat převodní tabulka.
    my $prevod = shift;
    # Kód začátku segmentu s daným písmem (o 1 nižší než kód znaku čandrabindu).
    my $pocatek = shift;
    # Volba alternativní sady výstupních znaků.
    my $alt = shift;
    local $maxl = 1;
    my $candrabindu = $pocatek+1;
    my $anusvara = $pocatek+2;
    my $visarga = $pocatek+3;
    my $samohlasky = $pocatek+5;
    my $souhlasky = $pocatek+21;
    my $diasamohlasky = $pocatek+62;
    my $virama = $pocatek+77;
    my $om = $pocatek+80;
    my $souhlasky2 = $pocatek+88;
    my $cislice = $pocatek+102;
    # Má se do latinky přidávat nečeská diakritika, aby se neztrácela informace?
    my $bezztrat = 1;
    # Zvolit výstupní sadu latinských písmen.
    my @lat = map {$_->[$alt]} @altlat;
    my $latcandrabindu = $lat[0];
    my $latanusvara = $lat[1];
    my $latvisarga = $lat[2];
    my @samohlasky = @lat[3..18];
    my @diasamohlasky = @lat[4..18];
    my @souhlasky = @lat[19..55];
    my @souhlasky2 = @lat[56..64];
    # Samostatné samohlásky.
    for(my $i = 0; $i<=$#samohlasky; $i++)
    {
        my $src = chr($samohlasky+$i);
        $prevod->{$src} = $samohlasky[$i];
        $maxl = length($src) if(length($src)>$maxl);
    }
    # Souhlásky implicitně obsahují samohlásku "a".
    # Pokud má slabika obsahovat jinou samohlásku, musí za znakem pro souhlásku následovat diakritické znaménko samohlásky.
    for(my $i = 0; $i<=$#souhlasky; $i++)
    {
        my $src = chr($souhlasky+$i);
        pridat_slabiky($prevod, $src, $souhlasky[$i], $diasamohlasky, \@diasamohlasky, $virama);
    }
    # V některých indických písmech (dévanágarí, bangla) se vyskytují další souhlásky, používané
    # ve slovech přejatých z arabštiny, perštiny, angličtiny aj.
    # Jejich znaky vypadají jako znaky jiných souhlásek s tečkou (diakritické znaménko nukta).
    # Lze potkat buď znak, jehož glyf už tečku zahrnuje, nebo dvojici znaků, které se píší přes sebe:
    # podkladová souhláska a nukta.
    my $nukta = $pocatek+60;
    my $pssn = $pocatek+88; # počátek souhlásek s nuktou
    my %nukta =
    (
        chr($pssn+0) => chr($pocatek+2325-2304).chr($nukta), # QA = KA + NUKTA
        chr($pssn+1) => chr($pocatek+2326-2304).chr($nukta), # KHHA = KHA + NUKTA
        chr($pssn+2) => chr($pocatek+2327-2304).chr($nukta), # GHHA = GA + NUKTA
        chr($pssn+3) => chr($pocatek+2332-2304).chr($nukta), # ZA = JA + NUKTA
        chr($pssn+4) => chr($pocatek+2337-2304).chr($nukta), # DDDHA = DDA + NUKTA
        chr($pssn+5) => chr($pocatek+2338-2304).chr($nukta), # RHA = DDHA + NUKTA
        chr($pssn+6) => chr($pocatek+2347-2304).chr($nukta), # FA = PHA + NUKTA
        chr($pssn+7) => chr($pocatek+2351-2304).chr($nukta)  # YYA = YA + NUKTA
    );
    for(my $i = 0; $i<=$#souhlasky2; $i++)
    {
        my $src = chr($souhlasky2+$i);
        pridat_slabiky($prevod, $src, $souhlasky2[$i], $diasamohlasky, \@diasamohlasky, $virama);
        # Jestliže existuje i varianta se samostatnou nuktou (netýká se pouze slabikotvorného r a l), vytvořit ještě převod i pro ni.
        # Poznámka: v datech se může nukta vyskytnout i za libovolnou jinou souhláskou.
        # Při přepisu bychom ji pak mohli úplně vymazat, ale zatím to neděláme, aby byly podivnosti v datech lépe vidět.
        if(exists($nukta{$src}))
        {
            pridat_slabiky($prevod, $nukta{$src}, $souhlasky2[$i], $diasamohlasky, \@diasamohlasky, $virama);
        }
    }
    # Anusvara způsobuje, že předcházející samohláska je nosová.
    # Anusvára se na konci vyslovuje m, jinde n, ň nebo m podle následující souhlásky.
    # Znaménko candrabindu rovněž nazalizuje předcházející samohlásku.
    $prevod->{chr($candrabindu)} = $latcandrabindu;
    $prevod->{chr($anusvara)} = $latanusvara;
    # Visarga přidává neznělý dech za samohláskou.
    $prevod->{chr($visarga)} = $latvisarga;
    # Číslice.
    for(my $i = 0; $i<=9; $i++)
    {
        my $src = chr($cislice+$i);
        $prevod->{$src} = $i;
        $maxl = length($src) if(length($src)>$maxl);
    }
    # Další znaky.
    $prevod->{chr($om)} = $lat[65];
    return $maxl;
}



#------------------------------------------------------------------------------
# Přidá do převodní tabulky kombinace dané souhlásky se všemi samohláskami.
#------------------------------------------------------------------------------
sub pridat_slabiky
{
    my $prevod = shift; # odkaz na převodní tabulku (hash)
    my $src = shift; # řetězec obsahující počáteční souhlásku slabiky v daném indickém písmu
    my $tgt = shift; # řetězec obsahující přepis této souhlásky do latinky
    my $srcsam = shift; # kód první nesamostatné samohlásky v daném indickém písmu
    my $tgtsam = shift; # odkaz na pole přepisů nesamostatných samohlásek do latinky
    my $virama = shift; # kód znaku virám v daném indickém písmu
    $prevod->{$src} = $tgt.'a';
    $maxl = length($src) if(length($src)>$maxl);
    # Znaménko virám likviduje implicitní samohlásku "a".
    my $src2 = chr($virama);
    $prevod->{$src.$src2} = $tgt;
    $maxl = length($src.$src2) if(length($src.$src2)>$maxl);
    for(my $j = 0; $j<=$#{$tgtsam}; $j++)
    {
        my $src2 = chr($srcsam+$j);
        $prevod->{$src.$src2} = $tgt.$tgtsam->[$j];
        $maxl = length($src.$src2) if(length($src.$src2)>$maxl);
    }
}



1;
