#!/usr/bin/perl
# Funkce pro přípravu transliterace z arménského písma do latinky.
# Copyright © 2009 Dan Zeman <zeman@ufal.mff.cuni.cz>
# Licence: GNU GPL

package translit::armen;
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
    my %armen =
    (
        1329 => 'A',
        1330 => 'B',
        1331 => 'G',
        1332 => 'D',
        1333 => 'E',
        1334 => 'Z',
        1335 => "É", # eh
        1336 => "Ă", # et (šva se v Putty nezobrazí)
        1337 => "T'",      # to
        1338 => 'Ž',
        1339 => 'I',
        1340 => 'L',
        1341 => 'X',
        1342 => 'C',
        1343 => 'K',       # ken
        1344 => 'H',
        1345 => 'DZ',       # ja; možná se čte "dž" nebo "dz"?
        1346 => "\x{11E}",       # ghat
        1347 => 'Č',       # cheh
        1348 => 'M',
        1349 => "Y",
        1350 => 'N',
        1351 => 'Š',
        1352 => 'O',
        1353 => "Č'",      # cha
        1354 => 'P',       # peh
        1355 => "Ś", # jheh; zdá se, že v zeměpisných názvech se to přepisuje jako š
        1356 => "Ř",# rra (R s tečkou nahoře se v Putty nezobrazí)
        1357 => 'S',
        1358 => 'V',
        1359 => 'T',       # tiwn
        1360 => 'R',       # reh
        1361 => "C'",      # co
        1362 => 'W',       # yiwn, hiun
        1363 => "P'",      # piwr, piur
        1364 => "K'",      # keh
        1365 => "Ó", # oh
        1366 => 'F',
        1377 => 'a',
        1378 => 'b',
        1379 => 'g',
        1380 => 'd',
        1381 => 'e',
        1382 => 'z',
        1383 => "é", # eh
        1384 => "ă", # et (šva se v Putty nezobrazí)
        1385 => "t'",      # to
        1386 => 'ž',
        1387 => 'i',
        1388 => 'l',
        1389 => 'x',
        1390 => 'c',
        1391 => 'k',       # ken
        1392 => 'h',
        1393 => 'dz',       # ja; možná se čte "dž" nebo "dz"?
        1394 => "\x{11F}",       # ghat
        1395 => 'č',       # cheh
        1396 => 'm',
        1397 => "y",
        1398 => 'n',
        1399 => 'š',
        1400 => 'o',
        1401 => "č'",      # cha
        1402 => "p",       # peh
        1403 => "ś",       # jheh; zdá se, že v zeměpisných názvech se to přepisuje jako š
        1404 => "ř",# rra (r s tečkou nahoře se v Putty nezobrazí)
        1405 => 's',
        1406 => 'v',
        1407 => 't',       # tiwn
        1408 => 'r',       # reh
        1409 => "c'",      # co
        1410 => 'w',       # yiwn, hiun
        1411 => "p'",      # piwr, piur
        1412 => "k'",      # keh
        1413 => "ó", # oh
        1414 => 'f',
        1415 => 'eu', # ligatura ECH-YIWN, tedy e+w
    );
    foreach my $kod (keys(%armen))
    {
        $prevod->{chr($kod)} = $armen{$kod};
    }
    return $prevod;
}



1;
