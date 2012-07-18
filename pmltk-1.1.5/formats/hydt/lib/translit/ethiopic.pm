#!/usr/bin/perl
# Funkce pro přípravu transliterace z etiopského písma ge'ez do latinky.
# (c) 2008 Dan Zeman <zeman@ufal.mff.cuni.cz>
# Licence: GNU GPL

package translit::ethiopic;
use utf8;



#!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
# Následující funkce je okopírovaná z modulu pro urdštinu, ale tady se zatím
# nijak nevyužívá.
#------------------------------------------------------------------------------
# Problematické hlásky nabízejí několik možných přepisů.
# Vysvětlivky:
# - český = Preferuje se zápis výslovnosti podle češtiny. To neznamená, že
#   přepis dobře modeluje výslovnost (ani v mezích možností české abecedy).
#   Ke správnému zápisu výslovnosti by bylo potřeba znát pravidla pravopisu
#   zdrojového jazyka, která mohou být někdy velmi složitá.
# - anglický = Preferuje se zápis výslovnosti podle angličtiny. To neznamená,
#   že přepis dobře modeluje výslovnost (viz stejná poznámka u českého).
# - bezztrátový = Přepis se snaží odlišit zdrojové znaky tak, aby bylo možné
#   rekonstruovat původní pravopis. "Český", resp. "anglický" přepis tak musí
#   být rozšířen o diakritická znaménka, psaní velkých písmen místo malých aj.
# - putty = Bezztrátový přepis, který se ale vyhýbá využití některých
#   prostředků, které nejsou vidět v terminálu Putty (a asi ani jinde, kde se
#   používá neproporcionální písmo). Jde zejména o znaky třídy "combining", tj.
#   samostatně kódovanou diakritiku.
# - ztrátový = Vyhýbá se použití zvláštních znaků, u kterých není výslovnost
#   na první pohled patrná.
# - technický = Preferuje bezztrátovost, přepis 1:1 (jeden znak za jeden znak)
#   a přepis pouze do ASCII znaků. Cenou je snížená čitelnost, ale zase mohou
#   odpadnout některé technické problémy se zobrazováním unikódové latinky.
#   Hodí se také pro opačný přepis při zadávání cizího písma z klávesnice (IME).
# Alternativní přepisy jsou uvedené v tomto pořadí (index do pole):
# 0 ... český bezztrátový
# 1 ... český putty
# 2 ... český ztrátový
# 3 ... anglický ztrátový
# 4 ... technický
#------------------------------------------------------------------------------
%alt =
(
    'dž' => ['dž', 'dž', 'dž', 'j',  'j'],
    'j'  => ['j',  'j',  'j',  'y',  'y'],
    'š'  => ['š',  'š',  'š',  'sh', 'sh'],
    'č'  => ['č',  'č',  'č',  'ch', 'ch'],
    'ch' => ['ch', 'ch', 'ch', 'kh', 'x'],
    # indické zvláštní hlásky
    # chr(803) je COMBINING DOT BELOW
    # chr(355) je LATIN SMALL LETTER T WITH CEDILLA
    # chr(273) je LATIN SMALL LETTER D WITH STROKE
    # chr(326) je LATIN SMALL LETTER N WITH CEDILLA
    # chr(771) je COMBINING TILDE
    # chr(241) je LATIN SMALL LETTER N WITH TILDE
    # chr(331) je LATIN SMALL LETTER ENG
    # chr(343) je LATIN SMALL LETTER R WITH CEDILLA
    'tind'    => ['t'.chr(803), chr(355), 't', 't', 'T'],
    'dind'    => ['d'.chr(803), chr(273), 'd', 'd', 'D'],
    'nind'    => ['n'.chr(803), chr(326), 'n', 'n', 'N'],
    'anusvár' => ['n'.chr(771), chr(241), 'n', 'n', 'M'],
    'ng'      => [chr(331),     chr(331), 'ng', 'ng', 'ng'],
    'ň'       => ['ň',          'ň',      'ň', 'ny', 'ny'],
    'rind'    => ['r'.chr(803), chr(343), 'r', 'r', 'R'],
    # arabské zvláštní hlásky (z urdštiny je nepřepisujeme stejně jako z arabštiny, protože některá znaménka tu potřebujeme na něco jiného)
    # Například nemůžeme přepsat ghajn jako "gh", protože by se to pletlo s indickým aspirovaným g.
    # chr(289) je LATIN SMALL LETTER G WITH DOT ABOVE
    # chr(703) je MODIFIER LETTER LEFT HALF RING
    # chr(8216) je LEFT SINGLE QUOTATION MARK
    # chr(807) je COMBINING CEDILLA
    # chr(254) je LATIN SMALL LETTER THORN
    # chr(240) je LATIN SMALL LETTER ETH
    'harab' => ['h'.chr(803), 'H',  'h',  'h',  'h'],
    'ghajn' => [chr(289), chr(289), 'gh', 'gh', 'G'],
    'ajn'   => [chr(703), '`', "`", "`", 'c'],
    'sarab' => ['s'.chr(807), 'S',  's',  's',  'S'],
    'darab' => ['d'.chr(807), 'D',  'd',  'd',  'd'],
    'tarab' => ['t'.chr(807), 'T',  't',  't',  't'],
    'zarab' => ['z'.chr(807), 'Z',  'z',  'z',  'Z'],
    'th'    => [chr(254), chr(254), 'th', 'th', 'th'],
    'dh'    => [chr(240), chr(240), 'dh', 'dh', 'dh']
);



#------------------------------------------------------------------------------
# Uloží do hashe přepisy znaků.
#------------------------------------------------------------------------------
sub inicializovat
{
    # Odkaz na hash, do kterého se má ukládat převodní tabulka.
    my $prevod = shift;
    # Má se do latinky přidávat nečeská diakritika, aby se neztrácela informace?
    my $bezztrat = 1;
    my $alt = 1; # český přepis pro putty
    my @souhlasky = ('h', 'l', "\x{127}", 'm', 'ś', 'r', 's', 'š', 'q', 'qw', 'qh', 'qhw', 'b', 'v', 't', 'č', 'ch',
                     'chw', 'n', 'ň', "'", 'k', 'kw', 'kch', 'kchw', 'w', '`', 'z', 'ž', 'j', 'd', "\x{111}", 'dž',
                     'g', 'gw', "\x{14B}", 'ţ', 'ć', 'ph', 'c', 'dz', 'f', 'p');
    my @samohlasky = ('ä', 'u', 'i', 'a', 'e', "\x{259}", 'o', 'ă');
    for(my $i = 0; $i<=$#souhlasky; $i++)
    {
        for(my $j = 0; $j<=$#samohlasky; $j++)
        {
            my $kod = 4608+$i*8+$j;
            $prevod->{chr($kod)} = $souhlasky[$i].$samohlasky[$j];
        }
    }
    $prevod->{chr(4952)} = 'rjä';
    $prevod->{chr(4953)} = 'mjä';
    $prevod->{chr(4954)} = 'fjä';
    $prevod->{chr(4962)} = '.';
    $prevod->{chr(4963)} = ',';
    $prevod->{chr(4964)} = ';';
    $prevod->{chr(4965)} = ':';
    $prevod->{chr(4967)} = '?';
    for(my $i = 1; $i<=9; $i++)
    {
        $prevod->{chr(4968+$i)} = $i;
        $prevod->{chr(4977+$i)} = '('.($i*10).')';
    }
    $prevod->{chr(4987)} = '(100)';
    $prevod->{chr(4988)} = '(10000)';
    return $prevod;
}



1;
