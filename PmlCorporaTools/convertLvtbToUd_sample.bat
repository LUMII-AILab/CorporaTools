
:: Prepare LVTB data for transforming
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect --ord mode=TOKEN --knit

:: Copy all the data from data/knitted to the LVTB2UD data folder
:: (with default IntelliJ configuration it is in CorporaTools/LVTB2UD/out/production)
:: Run runUniversalizer.bat from CorporaTools/LVTB2UD/out/production
:: Copy results from CorporaTools/LVTB2UD/out/production/data/conll-u to data/complete
:: Create data splits in folders data/train, data/test, data/dev

:: Fold everything neatly
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/train --fold p=1 name=lv-ud-train
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/test --fold p=1 name=lv-ud-test
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/dev --fold p=1 name=lv-ud-dev
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/complete --fold p=1 name=lv-ud-full

:: Do not forget to convert everything to Linux line endings
:: Renaming files from .conll to .conllu also might be needed

pause
