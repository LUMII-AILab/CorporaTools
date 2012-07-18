#!/usr/bin/perl
# Funkce pro přípravu transliterace z urdsko-arabského písma do latinky.
# (c) 2008 Dan Zeman <zeman@ufal.mff.cuni.cz>
# Licence: GNU GPL

package translit::urdu;
use utf8;



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
    my %urdu =
    (
        1548 => ',', # comma
        1563 => ';', # semicolon
        1567 => '?', # question
        1569 => "'", # hamza (samotná bývá někdy na konci slova)
        1570 => 'Á', # alef madda
        1571 => 'Á', # alef hamza above
        1572 => 'Ú', # hamza waw
        1573 => 'Í', # alef hamza below
        1574 => 'j', # hamza yeh
        1575 => 'á', # alef
        1576 => 'b', # beh
        1577 => 'eh', # teh marbuta (používá se v arabštině; v urdštině asi spíš jen omylem)
        1578 => 't', # teh
        1579 => $alt{'th'}[$alt], # theh
        1580 => $alt{'dž'}[$alt], # jeem
        1581 => $alt{'harab'}[$alt], # hah
        1582 => $alt{'ch'}[$alt], # khah
        1583 => 'd', # dal
        1584 => $alt{'dh'}[$alt], # thal
        1585 => 'r', # reh
        1586 => 'z', # zain
        1587 => 's', # seen
        1588 => $alt{'š'}[$alt], # sheen
        1589 => $alt{'sarab'}[$alt], # sad
        1590 => $alt{'darab'}[$alt], # dad
        1591 => $alt{'tarab'}[$alt], # tah
        1592 => $alt{'zarab'}[$alt], # zah
        1593 => $alt{'ajn'}[$alt], # ain
        1594 => $alt{'ghajn'}[$alt], # ghain
        1600 => '_', # tatweel (plnidlo mezi znaky na typografické prodloužení slova)
        1601 => 'f', # feh
        1602 => 'q', # qaf
        1603 => 'k', # kaf (urdština používá jiný tvar (keheh), ale omylem se může objevit i tenhle arabský)
        1604 => 'l', # lam
        1605 => 'm', # meem
        1606 => 'n', # noon
        1607 => 'h', # heh (urdština používá jiný tvar, ale omylem se může objevit i tenhle arabský)
        1608 => 'ú', # waw
        1609 => 'í', # alef maksura (objevuje se spíš omylem, správně by asi mělo být farsi yeh)
        1610 => 'í', # yeh (urdština používá jiný tvar, ale omylem se může objevit i tenhle arabský)
        1611 => 'an', # fathatan (diakritika pro krátké a s "nunací")
        1612 => 'un', # dammatan (diakritika pro krátké u s "nunací")
        1613 => 'in', # kasratan (diakritika pro krátké i s "nunací")
        1614 => 'a', # fatha (diakritika pro krátké a)
        1615 => 'u', # damma (diakritika pro krátké u)
        1616 => 'i', # kasra (diakritika pro krátké i)
        1617 => ':', # shadda (zdvojená souhláska)
        1618 => '',  # sukun (žádná samohláska)
        1642 => '%', # percent
        1643 => ',', # decimal separator
        1644 => chr(160), # thousands separator
        1648 => 'á', # superscript alef
        1649 => '-', # alef wasla (arabština Koránu; uvnitř slova; značí, že kvůli spojování se nevyslovuje ráz ani samohláska alefu)
        1657 => $alt{'tind'}[$alt], # tteh
        1662 => 'p', # peh
        1670 => $alt{'č'}[$alt], # tcheh
        1672 => $alt{'dind'}[$alt], # ddal
        1681 => $alt{'rind'}[$alt], # rreh
        1688 => 'ž', # jeh
        1705 => 'k', # keheh
        1711 => 'g', # gaf
        1722 => $alt{'anusvár'}[$alt], # noon ghunna
        1726 => 'h', # heh doachashmee
        1728 => 'h', # heh with yeh above
        1729 => 'h', # heh goal
        1730 => 'h', # heh goal hamza
        1740 => 'í', # farsi yeh
        1746 => 'é', # yeh barree
        1747 => 'é', # yeh barree hamza
        1748 => '.', # full stop
        1776 => '0', # zero
        1777 => '1', # one
        1778 => '2', # two
        1779 => '3', # three
        1780 => '4', # four
        1781 => '5', # five
        1782 => '6', # six
        1783 => '7', # seven
        1784 => '8', # eight
        1785 => '9', # nine
        2404 => '.', # danda (oddělovač vět v dévanágarí, v urdštině může být jen omylem)
        8204 => '', # zero width non-joiner
        8205 => '', # zero width joiner
        8206 => '', # left-to-right mark
        8207 => '', # right-to-left mark
        8234 => '', # left-to-right embedding
        8235 => '', # right-to-left embedding
        8236 => '', # pop directional formatting
        8237 => '', # left-to-right override
        8238 => '', # right-to-left override
    );
    foreach my $kod (keys(%urdu))
    {
        $prevod->{chr($kod)} = $urdu{$kod};
    }
    return $prevod;
}



1;
