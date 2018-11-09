REM Step by step sample on how prepare UD data for Sembank.

:: Obtain CoNLL-U files as for UD, but do not split TDT and do not fold together in one file.
:: Use only Treebank's normalizedIds branch.

:: Let's assume all CoNLL-U files are in PmlCorporaTools/data/conll-u.
:: Let's assume Treebank repository is right next to CorporaTools.

REM Split according to Sembank ignore list.
::perl -e "use LvCorporaTools::DataSelector::SplitByList qw(splitOnOffList); splitOnOffList(@ARGV)" data\conll-u ..\..\Treebank\Datasplits\SemBank-ignored.tsv data
::@move .\data\on-list .\data\ignore >nul
::@move .\data\off-list .\data\good >nul

:: If "Verbu rindkopas" still uses the old naming convention for paragraphs,
:: e.g., c1_r15-p1, they should not undergo paragraph splitting transformation,
:: as it would make file names and IDs wrong for these files, e.g.,
:: c1_r15.conllu to c1_r15-p1.conllu.
:: To separate "Verbu rindkopas" one can dom something like this:
::@mkdir .\data\verbPar >nul
::@move .\data\good\*_r*.conllu .\data\verbPar >nul

REM Call paragraph splitter on the files.
::@if exist .\data\splitedPar rmdir .\data\splitedPar /Q /S >nul
::perl -e "use LvCorporaTools::DataSelector::SplitConll2Para qw(processDir); processDir(@ARGV)" data\good
::@move .\data\good\res .\data\splitedPar >nul

:: If you separated "Verbu rindkopas" earlier, use data both in verbPar and
:: splitedPar.

pause