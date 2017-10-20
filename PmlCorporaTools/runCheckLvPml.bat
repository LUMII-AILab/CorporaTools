REM For single .w + .m dataset.
perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(checkLvPml); checkLvPml(@ARGV)" testdata\CheckLvPml zeens M

REM For multiple .w + .m + .a datasets.
perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(processDir); processDir(@ARGV)" testdata\CheckLvPml A


pause