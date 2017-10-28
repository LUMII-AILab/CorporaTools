
:: Prepare LVTB data for transforming (your data goes into folder "data")
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect --ord mode=TOKEN --knit

:: Copy all the data from data/knitted to the LVTB2UD data folder
:: (with default IntelliJ configuration it is CorporaTools/LVTB2UD/out/production/data)
:: Run runUniversalizer.bat from CorporaTools/LVTB2UD/out/production
:: Copy results from CorporaTools/LVTB2UD/out/production/data/conll-u to data/complete

:: Create data splits in folders data/train, data/test, data/dev
::perl -e "use LvCorporaTools::DataSelector::SplitTrainDevTest qw(makeTDT); makeTDT(@ARGV)" data/complete ../Docs/testdevtrain.tsv data

:: Fold everything neatly (assumed files are separated in folders "train", "test", "dev" and "complete")
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/train --fold p=1 name=lv-ud-train
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/test --fold p=1 name=lv-ud-test
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/dev --fold p=1 name=lv-ud-dev
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/complete --fold p=1 name=lv-ud-full

:: Do not forget to convert everything to Linux line endings
:: Renaming files from .conll to .conllu also might be needed

pause
