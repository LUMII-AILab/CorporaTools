REM Step by step sample on how to prepare LVTB for publication, e.q. on LINDAT.

REM Collect LVTB data and remove AUTO/FIXME files
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect --clean --ord mode=NODE

REM Split off the UD skip-files
::perl -e "use LvCorporaTools::DataSelector::SplitByList qw(splitTDT); splitTDT(@ARGV)" data/ord ../../Treebank/Datasplits/testdevtrain.tsv data

REM If canonical TDT split is not needed, unite all folders except "skip".
::mkdir data\publish
::move data\train\*.* data\publish >nul
::rmdir data\train
::move data\test\*.* data\publish >nul
::rmdir data\test
::move data\dev\*.* data\publish >nul
::rmdir data\dev
::if exist data\not-mentioned move data\not-mentioned\*.* data\publish >nul
::if exist data\not-mentioned rmdir data\not-mentioned

REM To publish with LINDAT, a documentation in markdown and lv-treebank extension module is also needed.
REM NB! UFAL LINDAT server uses the same lv-treebank version for all LVTB versions, unless specifically asked to do otherwise. Thus, if backwards-uncompatible changes in lv-treebank are introduced, we must negotiate with UFAL before next release.

pause
