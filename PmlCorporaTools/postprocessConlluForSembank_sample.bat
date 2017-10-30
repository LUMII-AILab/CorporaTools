
:: Obtain CoNLL-U files as for UD, but do not fold together in one file,
:: if "Verbu rindkopas" still uses the old naming convention for paragraphs,
:: e.g., c1_r15-p1, and thus, should not be transformed to file
:: c1_r15-p1.conllu from c1_r15.conllu.
:: Use only Treebank's normalizedIds branch.

::Let's assume all CoNLL-U files are in PmlCorporaTools/data.
::[Temporary] Separate "Verbu rindkopas" files.
::@mkdir .\data\verbPar >nul
::@move .\data\*_r*.conllu .\data\verbPar >nul

:: Call paragraph splitter on the rest of the files.
::@if exist .\data\oldCorpusPara rmdir .\data\oldCorpusPar /Q /S >nul
::perl -e "use LvCorporaTools::DataSelector::SplitConll2Para qw(processDir); processDir(@ARGV)" data
::@move .\data\res .\data\oldCorpusPar >nul

:: Use everything in verbPar and oldCorpusPar

pause