#!/usr/bin/perl
# Funkce pro přípravu transliterace do latinky.
# (c) 2008 Dan Zeman <zeman@ufal.mff.cuni.cz>
# Licence: GNU GPL

package translit;
use utf8;



#------------------------------------------------------------------------------
# Ladění: vypsat převodní tabulku.
#------------------------------------------------------------------------------
sub vypsat
{
    my $prevod = shift; # odkaz na hash s převodní tabulkou
    binmode(STDOUT, ":utf8");
    foreach my $klic (sort(keys(%{$prevod})))
    {
        print("$klic\t$prevod->{$klic}\n");
    }
}



#------------------------------------------------------------------------------
# Převede řetězec z jednoho písma nebo kódování do druhého. Potřebujeme
# nejdřív v příslušném modulu inicalizovat převodní tabulku (hash). Tato funkce
# neklade omezení na délku řetězce, jehož převod může být v hashi definován,
# ale ani hash neprochází, aby zjistila délku nejdelšího takového řetězce
# (nebylo by to efektivní, pokud by funkce byla volána např. pro každé slovo
# zvlášť, což třeba u anotovaného korpusu potřebujeme). Místo toho je možné jí
# předem zjištěnou maximální délku předat jako parametr. Pokud tento parametr
# nedostane, funkce testuje převody řetězců do délky 2 znaky.
#------------------------------------------------------------------------------
sub prevest
{
    my $prevod = shift; # odkaz na hash s převodní tabulkou
    my $retezec = shift;
    my $maxl = shift; # maximální možná délka zdrojové skupiny znaků
    $maxl = 2 unless($maxl);
    my $vysledek;
    while($retezec)
    {
        for(my $i = $maxl; $i>0; $i--)
        {
            if($retezec =~ m/^(.{$i})/s && exists($prevod->{$1}))
            {
                $vysledek .= $prevod->{$1};
                $retezec =~ s/^.{$i}//s;
                last;
            }
            # Pokud se nenašel přepis ani pro samotný první znak, okopírovat tento znak na výstup a odebrat ho.
            elsif($i==1)
            {
                $vysledek .= $1;
                $retezec =~ s/^.//s;
            }
        }
    }
    return $vysledek;
}



#------------------------------------------------------------------------------
# Převede řetězec z cizího písma do latinky.
# Tato stará verze funkce předpokládá, že převodní tabulka obsahuje převody
# jednotlivých znaků a dvojic znaků, ale ne delších řetězců.
#------------------------------------------------------------------------------
sub prevest0
{
    my $prevod = shift; # odkaz na hash s převodní tabulkou
    my $retezec = shift;
    my @znaky = split(//, $retezec);
    for(my $i = 0; $i<=$#znaky; $i++)
    {
        # Je-li převod dvojice znaků definovaný, použít ho.
        if($i<$#znaky)
        {
            my $src = $znaky[$i].$znaky[$i+1];
            if(exists($prevod->{$src}))
            {
                $znaky[$i] = $prevod->{$src};
                splice(@znaky, $i+1, 1);
                $i--;
                next;
            }
        }
        if(exists($prevod->{$znaky[$i]}))
        {
            $znaky[$i] = $prevod->{$znaky[$i]};
        }
    }
    my $vysledek = join("", @znaky);
    return $vysledek;
}



1;
