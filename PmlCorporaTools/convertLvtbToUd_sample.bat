REM Step by step sample on how to convert LVTB to UD and how to prepare UD release.

:: In most releases sample data must be removed like this: 
::@if exist .\data\Corpora\Paraugi rmdir .\data\Corpora\Paraugi /Q /S >nul
:: However, for standard UD release, TDT file handles this.
:: For standard Sembank release sample files are handled by ignore list.

:: Prepare LVTB data for transforming (your data goes into folder "data")
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect --ord mode=TOKEN --knit

:: Copy all the data from data/knitted to the LVTB2UD data folder
:: (with default IntelliJ configuration it is CorporaTools/LVTB2UD/out/production/data)
:: Run runUniversalizer.bat from CorporaTools/LVTB2UD/out/production
:: Copy results from CorporaTools/LVTB2UD/out/production/data/conll-u to data/conll-u

:: Create data splits in folders data/train, data/test, data/dev
::perl -e "use LvCorporaTools::DataSelector::SplitByList qw(splitTDT); splitTDT(@ARGV)" data/conll-u ../../Treebank/Datasplits/testdevtrain.tsv data

:: Fold everything neatly (assumed files are separated in folders "train", "test", "dev")
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/train --fold p=1 name=lv_lvtb-ud-train
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/test --fold p=1 name=lv_lvtb-ud-test
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/dev --fold p=1 name=lv_lvtb-ud-dev
:: File for validation (nothing is ommited) can be made like this
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/conll-u --fold p=1 name=lv_lvtb-ud-everything
:: File for statistics (includes only sentences to be published) is made by concatenating tran, test, dev: lv_lvtb-ud-tb = lv_lvtb-ud-train + lv_lvtb-ud-test + lv_lvtb-ud-dev

:: Convert everything to Linux line endings
:: Rename files from .conll to .conllu
:: Copy lv_lvtb-ud-train.conll, lv_lvtb-ud-test.conll, lv_lvtb-ud-dev.conll to ../../UD_Latvian-LVTB
:: Copy lv_lvtb-ud-tb.conll to ../../tools
:: Get newest UD tools version from github to folder ../../tools

:: Validate files to be published, Python 3 needed.
::cd ../../tools
::python validate.py --lang=lv ../UD_Latvian-LVTB/lv_lvtb-ud-train.conllu
::python validate.py --lang=lv ../UD_Latvian-LVTB/lv_lvtb-ud-dev.conllu
::python validate.py --lang=lv ../UD_Latvian-LVTB/lv_lvtb-ud-test.conllu
:: If you want to validate full corpus located in ../../tools, do
::python validate.py --lang=lv lv_lvtb-ud-everything.conllu
::python validate.py --lang=lv --max-err=0 lv_lvtb-ud-everything.conllu > ud-validator.lv.log 2>&1
:: or to validate corpus to be published without ommited example sentences
::python validate.py --lang=lv lv_lvtb-ud-tb.conllu
::python validate.py --lang=lv --max-err=0 lv_lvtb-ud-tb.conllu > ud-validator.lv.log 2>&1

:: Get stats for UD readme - OBSELOTE and needs Python 2
::python conllu-stats.py --stats ../UD_Latvian-LVTB/lv_lvtb-ud-train.conllu ../UD_Latvian-LVTB/lv_lvtb-ud-dev.conllu ../UD_Latvian-LVTB/lv_lvtb-ud-test.conllu

:: ../../tools contains an old stats.xml, delete it.
:: Get stats.xml
::perl conllu-stats.pl < lv_lvtb-ud-tb.conllu > stats.xml
:: Convert stats.xml to Linux line endings.

:: Update UD readme with new TDT sentence counts and numbers in the first paragraph.


pause
