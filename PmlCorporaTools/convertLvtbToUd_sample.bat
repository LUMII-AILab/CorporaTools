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
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/complete --fold p=1 name=lv_lvtb-ud-complete

:: Do not forget to convert everything to Linux line endings
:: Renaming files from .conll to .conllu also might be needed

pause
