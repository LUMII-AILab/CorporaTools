#!/usr/bin/perl
# Funkce pro přípravu transliterace z khmerského písma do latinky.
# (c) 2008 Dan Zeman <zeman@ufal.mff.cuni.cz>
# Licence: GNU GPL

package translit::khmer;
use utf8;



#------------------------------------------------------------------------------
# Uloží do globálního hashe přepisy souhlásek a slabik.
#------------------------------------------------------------------------------
sub inicializovat
{
    # Odkaz na hash, do kterého se má ukládat převodní tabulka.
    my $prevod = shift;
    # Má se do latinky přidávat nečeská diakritika, aby se neztrácela informace?
    my $bezztrat = 1;
    # Kód začátku segmentu s khmerským písmem.
    my $pocatek = 6016;
    my $souhlasky = 6016;
    my $samohlasky = 6050;
    my $diasamohlasky = 6070;
    my $anusvara = 6086; # khmersky nikahit
    my $visarga = 6087; # khmersky reahmuk
    my $coeng = 6098; # podobný účinek jako virám: následující písmeno jako dolní index, tj. inherentní samohláska tohoto se ruší.
    my $cislice = 6112;
    my $virama = $pocatek+77;
    # Některé souhlásky implicitně obsahují samohlásku "a", jiné "o".
    my @souhlasky =
    (
        "ka", "kha", "ko", "kho", "ngo",
        "ča", "čha", "čo", "čho", "ňo",
        "da", "tha", "do", "tho", "no",
        "ta", "tha", "to", "tho", "no",
        "ba", "pha", "po", "pho", "mo",
        "jo", "ro",  "lo", "wo",
        "śa", "ša",  "sa", "ha",  "la",
        ""
    );
    # Samohlásky řady a.
    my @samohlasky_a = ("á",  "e",  "ăi", "ă", "ăy", "o", "ou", "uă", "ă", "yă", "iă", "ej", "ae", "aj", "ao", "au");
    # Samohlásky řady o.
    my @samohlasky_o = ("iă", "ie", "í",  "y", "ý",  "u", "ú",  "uă", "ă", "yă", "iă", "é",  "é",  "yj", "ó",  "ău");
    # Samohlásková diakritická znaménka by se měla vždy vyskytovat pohromadě se souhláskami,
    # ale pro případ, že se někde stane chyba, vytvořit i převod jich samotných.
    # Samostatné samohlásky.
    my @samohlasky = ("a", "a", "á", "i", "í", "o", "ok", "ó", "ău", "r", "ŕ", "l", "ĺ", "é", "aj", "au", "au", "au");
    for(my $i = 0; $i<=$#samohlasky; $i++)
    {
        my $src = chr($samohlasky+$i);
        $prevod->{$src} = $samohlasky[$i];
    }
    # Slabiky.
    for(my $i = 0; $i<=$#souhlasky; $i++)
    {
        my $src = chr($souhlasky+$i);
        $prevod->{$src} = $souhlasky[$i];#." "; # mezera je pomocná, protože Khmerové nedělají mezery mezi slovy
        for(my $j = 0; $j<=$#samohlasky_a; $j++)
        {
            my $src2 = chr($diasamohlasky+$j);
            my $prevod = $souhlasky[$i];
            unless($prevod =~ s/a$/$samohlasky_a[$j]/)
            {
                $prevod =~ s/o$/$samohlasky_o[$j]/;
            }
            $prevod->{$src.$src2} = $prevod;
        }
        # Coeng maže inherentní samohlásku.
        my $prevod = $souhlasky[$i];
        $prevod =~ s/[ao]$//;
        $prevod->{$src.chr($coeng)} = $prevod;
    }
    # Znaménko nikahit (anusvár) způsobuje, že předcházející samohláska je nosová. Většinou se přepisuje jako koncové "m".
    $prevod->{chr($anusvara)} = "m";
    $prevod->{chr(6099)} = "m"; # Tohle je nějaký znak z lunárního kalendáře, ale dá se splést s nikahitem.
    # Visarga přidává neznělý dech za samohláskou.
    $prevod->{chr($visarga)} = "h";
    # Číslice.
    for(my $i = 0; $i<=9; $i++)
    {
        my $src = chr($cislice+$i);
        $prevod->{$src} = $i;
    }
}



1;
