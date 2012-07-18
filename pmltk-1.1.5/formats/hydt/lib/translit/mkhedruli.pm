#!/usr/bin/perl
# Funkce pro přípravu transliterace z gruzínských písem asomtavruli a mchedruli do latinky.
# (c) 2008 Dan Zeman <zeman@ufal.mff.cuni.cz>
# Licence: GNU GPL

package translit::mkhedruli;
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
    my %mchedruli =
    (
        4304 => 'a',
        4305 => 'b',
        4306 => 'g',
        4307 => 'd',
        4308 => 'e',
        4309 => 'v',
        4310 => 'z',
        4311 => "t'", # tan
        4312 => 'i',
        4313 => 'k',  # kan
        4314 => 'l',
        4315 => 'm',
        4316 => 'n',
        4317 => 'o',
        4318 => 'p',
        4319 => 'ž',
        4320 => 'r',
        4321 => 's',
        4322 => 't',  # tar
        4323 => 'u',
        4324 => 'f',
        4325 => "k'", # khar
        4326 => 'gh', # ghan
        4327 => 'q',  # qar
        4328 => 'š',
        4329 => "č'", # chin
        4330 => "c'", # can
        4331 => 'dz', # jil
        4332 => 'c',  # cil
        4333 => 'č',  # char
        4334 => 'ch', # xan
        4335 => 'dž', # jhan
        4336 => 'h',  # hae
    );
    foreach my $kod (keys(%mchedruli))
    {
        $prevod->{chr($kod)} = $mchedruli{$kod};
    }
    return $prevod;
}



1;
