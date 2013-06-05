REM For single dataset.
perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(checkLvPml); checkLvPml(@ARGV)" testdata\CheckLvPml zeens

REM For multiple datasets.
perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(processDir); processDir(@ARGV)" testdata\CheckLvPml


pause