#!/usr/bin/perl
# Funkce pro přípravu převodu indického textu z kódování WX do UTF-8. Je nutné si uvědomit následující:
# - WX zapisuje indický text pomocí latinky. Pokud do indického textu měla být vložena skutečná latinka, nepoznáme to.
# - WX je mapování obecného repertoáru hlásek, které lze zapsat v indických písmech. Pro převod do UTF-8 musíme vědět, které indické písmo se má použít.
# Copyright © 2009 Dan Zeman <zeman@ufal.mff.cuni.cz>
# Licence: GNU GPL

package translit::wc2utf;
use utf8;

# Tabulka jazyků a odkazy na sloupce v následující tabulce.
%jazyky =
(
    'hi' => 1,
    'bn' => 2,
    'te' => 3
);
# Tabulka korespondence znaků WX a písmen v dévanágarí, bengálském a telugském písmu.
@wx =
(
    ['a',   "\x{905}", "\x{985}", "\x{C05}"],
    ['-a',  "",        "",        ""       ],
    ['A',   "\x{906}", "\x{986}", "\x{C06}"],
    ['-A',  "\x{93E}", "\x{9BE}", "\x{C3E}"],
    ['i',   "\x{907}", "\x{987}", "\x{C07}"],
    ['-i',  "\x{93F}", "\x{9BF}", "\x{C3F}"],
    ['I',   "\x{908}", "\x{988}", "\x{C08}"],
    ['-I',  "\x{940}", "\x{9C0}", "\x{C40}"],
    ['u',   "\x{909}", "\x{989}", "\x{C09}"],
    ['-u',  "\x{941}", "\x{9C1}", "\x{C41}"],
    ['U',   "\x{90A}", "\x{98A}", "\x{C0A}"],
    ['-U',  "\x{942}", "\x{9C2}", "\x{C42}"],
    ['q',   "\x{90B}", "\x{98B}", "\x{C0B}"],
    ['-q',  "\x{943}", "\x{9C3}", "\x{C43}"],
    ['Q',   "\x{960}", "\x{9E0}", "\x{C60}"],
    ['-Q',  "\x{944}", "\x{9C4}", "\x{C44}"],
    ['eV',  "\x{90E}", "\x{98E}", "\x{C0E}"],
    ['-eV', "\x{946}", "\x{9C6}", "\x{C46}"],
    ['e',   "\x{90F}", "\x{98F}", "\x{C0F}"],
    ['-e',  "\x{947}", "\x{9C7}", "\x{C47}"],
    ['E',   "\x{910}", "\x{990}", "\x{C10}"],
    ['-E',  "\x{948}", "\x{9C8}", "\x{C48}"],
    ['OY',  "\x{911}", "\x{991}", "\x{C11}"],
    ['-OY', "\x{949}", "\x{9C9}", "\x{C49}"],
    ['oV',  "\x{912}", "\x{992}", "\x{C12}"],
    ['-oV', "\x{94A}", "\x{9CA}", "\x{C4A}"],
    ['o',   "\x{913}", "\x{993}", "\x{C13}"],
    ['-o',  "\x{94B}", "\x{9CB}", "\x{C4A}"],
    ['O',   "\x{914}", "\x{994}", "\x{C14}"],
    ['-O',  "\x{94C}", "\x{9CC}", "\x{C4C}"],
    ['M',   "\x{902}", "\x{982}", "\x{C02}"],
    ['H',   "\x{903}", "\x{983}", "\x{C03}"],
    ['z',   "\x{901}", "\x{981}", "\x{C01}"],
    ['EY',  "\x{901}", "\x{981}", "\x{C01}"],
    ['-',   "\x{94D}", "\x{9CD}", "\x{C4D}"], # virám
    ['Z',   "\x{93C}", "\x{9BC}", "\x{C3C}"], # telugu ve skutečnosti nuktu nemá, ale možná je potřeba některé souhlásky s nuktou namapovat jinam
    ['k',   "\x{915}", "\x{995}", "\x{C15}"],
    ['K',   "\x{916}", "\x{996}", "\x{C16}"],
    ['g',   "\x{917}", "\x{997}", "\x{C17}"],
    ['G',   "\x{918}", "\x{998}", "\x{C18}"],
    ['f',   "\x{919}", "\x{999}", "\x{C19}"],
    ['c',   "\x{91A}", "\x{99A}", "\x{C1A}"],
    ['C',   "\x{91B}", "\x{99B}", "\x{C1B}"],
    ['j',   "\x{91C}", "\x{99C}", "\x{C1C}"],
    ['J',   "\x{91D}", "\x{99D}", "\x{C1D}"],
    ['F',   "\x{91E}", "\x{99E}", "\x{C1E}"],
    ['t',   "\x{91F}", "\x{99F}", "\x{C1F}"],
    ['T',   "\x{920}", "\x{9A0}", "\x{C20}"],
    ['d',   "\x{921}", "\x{9A1}", "\x{C21}"],
    ['D',   "\x{922}", "\x{9A2}", "\x{C22}"],
    ['N',   "\x{923}", "\x{9A3}", "\x{C23}"],
    ['w',   "\x{924}", "\x{9A4}", "\x{C24}"],
    ['W',   "\x{925}", "\x{9A5}", "\x{C25}"],
    ['x',   "\x{926}", "\x{9A6}", "\x{C26}"],
    ['X',   "\x{927}", "\x{9A7}", "\x{C27}"],
    ['n',   "\x{928}", "\x{9A8}", "\x{C28}"],
    ['p',   "\x{92A}", "\x{9AA}", "\x{C2A}"],
    ['P',   "\x{92B}", "\x{9AB}", "\x{C2B}"],
    ['b',   "\x{92C}", "\x{9AC}", "\x{C2C}"],
    ['B',   "\x{92D}", "\x{9AD}", "\x{C2D}"],
    ['m',   "\x{92E}", "\x{9AE}", "\x{C2E}"],
    ['y',   "\x{92F}", "\x{9AF}", "\x{C2F}"],
    ['r',   "\x{930}", "\x{9B0}", "\x{C30}"],
    ['l',   "\x{932}", "\x{9B2}", "\x{C32}"],
    ['lY',  "\x{933}", "\x{9B3}", "\x{C33}"],
    ['v',   "\x{935}", "\x{9B5}", "\x{C35}"],
    ['S',   "\x{936}", "\x{9B6}", "\x{C36}"],
    ['R',   "\x{937}", "\x{9B7}", "\x{C37}"],
    ['s',   "\x{938}", "\x{9B8}", "\x{C38}"],
    ['h',   "\x{939}", "\x{9B9}", "\x{C39}"],
    ['0',   "\x{966}", "\x{9E6}", "\x{C66}"],
    ['1',   "\x{967}", "\x{9E7}", "\x{C67}"],
    ['2',   "\x{968}", "\x{9E8}", "\x{C68}"],
    ['3',   "\x{969}", "\x{9E9}", "\x{C69}"],
    ['4',   "\x{96A}", "\x{9EA}", "\x{C6A}"],
    ['5',   "\x{96B}", "\x{9EB}", "\x{C6B}"],
    ['6',   "\x{96C}", "\x{9EC}", "\x{C6C}"],
    ['7',   "\x{96D}", "\x{9ED}", "\x{C6D}"],
    ['8',   "\x{96E}", "\x{9EE}", "\x{C6E}"],
    ['9',   "\x{96F}", "\x{9EF}", "\x{C6F}"],
);



#------------------------------------------------------------------------------
# Uloží do hashe přepisy souhlásek a slabik. Odkaz na cílový hash převezme jako
# parametr. Vrátí délku nejdelšího řetězce, jehož přepis je v hashi definován.
#------------------------------------------------------------------------------
sub inicializovat
{
    # Odkaz na hash, do kterého se má ukládat převodní tabulka.
    my $prevod = shift;
    my $jazyk = shift;
    if(!exists($jazyky{$jazyk}))
    {
        print STDERR ("Known WX languages: ", join(' ', sort(keys(%jazyky))), "\n");
        die("Unknown language '$jazyk'.\n");
    }
    my $maxl;
    # Vytáhnout si z hlavní tabulky aktuální písmo a předělat ho na přepisovací hash.
    foreach my $radek (@wx)
    {
        my $wx = $radek->[0];
        my $utf = $radek->[$jazyky{$jazyk}];
        if($wx =~ s/^-//)
        {
            push(@diasamohlasky, [$wx, $utf]);
        }
        elsif($wx =~ m/^(a|A|i|I|u|U|q|Q|eV|e|E|OY|oV|o|O|M|H|z|EY|Z|[0-9])$/)
        {
            $prevod->{$wx} = $utf;
            $maxl = length($wx) if(length($wx)>$maxl);
        }
        else
        {
            push(@souhlasky, [$wx, $utf]);
        }
    }
    foreach my $s (@souhlasky)
    {
        foreach my $d (@diasamohlasky)
        {
            my $wx = $s->[0].$d->[0];
            my $utf = $s->[1].$d->[1];
            $prevod->{$wx} = $utf;
            $maxl = length($wx) if(length($wx)>$maxl);
            # Výše uvedené zatím nepokrývá správně slabiky s nuktou.
            # Např. "PZu" (= ph+nukta+u, tedy "fu") se nám přepíše se samostatným znakem pro "u", nikoli se znaménkem pro "u".
            # Proto tady tyto případy zachytíme ještě zvlášť.
            $wx = $s->[0].'Z'.$d->[0];
            $utf = $s->[1].$prevod->{'Z'}.$d->[1];
            $prevod->{$wx} = $utf;
            $maxl = length($wx) if(length($wx)>$maxl);
        }
    }
    return $maxl;
}
