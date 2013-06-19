REM For single .w + .m + .a dataset.
perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(checkLvPml); checkLvPml(@ARGV)" testdata\CheckLvPml zeens A

REM For multiple .w + .m datasets.
perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(processDir); processDir(@ARGV)" testdata\CheckLvPml M


pause