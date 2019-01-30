1. Dabūt PML-W
--------------
Izejas dati:
* meta informācija
* oriģinālteksts

Piemērs:
`perl -e "use LvCorporaTools::FormatTransf::Plaintext2W qw(transformFile); transformFile(@ARGV)" testdata\Plaintext2W t16_p21.txt t16 t16.meta 21 t16_p21`

Parametri:
1) datu mape
2) fails ar tekstu, no kura jāuzbūvē W fails
3) datu avota ID no LVK, piemēram, p2134 vai c60
4) fails ar metainformāciju no LVK
5) rinkopas numurs
6) jaunveidojamās PML failu kopas vārds. Sembank ietvaros parasti tas ir <avota ID>_p<rindkopas nr>
  
Metadati no LVK ir padodami failā šādā formā formā (bez XML header):
```
<docmeta>
  <title>Dokumenta nosaukums (obligāts).</title>
  <source>Nav obligāts.</source>
  <author>Nav obligāts.</author>
  <authorgender>Nav obligāts.</authorgender>
  <published>Nav obligāts.</published>
  <genre>Nav obligāts.</genre>
  <keywords>
    <LM>atslēgvārds</LM>
    <LM>atslēgasvārds</LM>
    <LM>neviens atslēgas vārds un viss šis elements nav obligāts</LM>
  </keywords>
  <misc>No LVK nekādai citai informācijai arī nevajadzētu nākt.</misc>
</docmeta>
```
Mazākie _dummy_ dati varētu būt apmēram šādi: `<docmeta><title>t16</title></docmeta>`.

2. Dabūt PML-M un PML-A
-----------------------
Izejas dati:
* PML-W
* CoNLL ar morfoloģiju un, ja var, tad sintaksi

Piemērs:
`perl -e "use LvCorporaTools::FormatTransf::Conll2MA qw(processFileSet); processFileSet(@ARGV)" t16_p21.w  result_folder t16_p21.conllu`
vai
`perl -e "use LvCorporaTools::FormatTransf::Conll2MA qw(processFileSet); processFileSet(@ARGV)" t16_p21.w  result_folder`

Parametri:
1) w fails
2) mape, kur likt rezultātu
3) CoNLL fails - ja šo nepadod, tad skripts ņem 1. parametru, nocērt tam no beigām ".w", pieliek ".conll" un mēģina lietot to kā CoNLL faila nosaukumu


