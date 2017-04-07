::copy "TrEd extension\lv-treebank\resources\*.xml" "testdata\Knit\"
REM Knit-in single A file.
perl -e "use LvCorporaTools::PMLUtils::Knit qw(knit); knit(@ARGV)" testdata/Knit zeens.a zeens-a.pml "../TrEd extension/lv-treebank/resources"

REM Knit-in single M file.
perl -e "use LvCorporaTools::PMLUtils::Knit qw(knit); knit(@ARGV)" testdata/Knit zeens.m zeens-m.pml "../TrEd extension/lv-treebank/resources"

REM Process a folder with filesets containing A files.
perl -e "use LvCorporaTools::PMLUtils::Knit qw(processDir); processDir(@ARGV)" testdata/Knit/ a "../TrEd extension/lv-treebank/resources"

REM Process a folder with filesets containing M files.
perl -e "use LvCorporaTools::PMLUtils::Knit qw(processDir); processDir(@ARGV)" testdata/Knit/ m "../TrEd extension/lv-treebank/resources"

pause