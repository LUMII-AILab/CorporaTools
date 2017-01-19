::copy "TrEd extension\lv-treebank\resources\*.xml" "testdata\Knit\"
REM Knit-in single file.
perl -e "use LvCorporaTools::PMLUtils::Knit qw(knit); knit(@ARGV)" testdata/Knit zeens.a zeens.pml "TrEd extension/lv-treebank/resources"

REM Process a folder.
perl -e "use LvCorporaTools::PMLUtils::Knit qw(processDir); processDir(@ARGV)" testdata/Knit/ a "TrEd extension/lv-treebank/resources"

pause