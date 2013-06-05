REM For single file.
perl -e "use LvCorporaTools::PMLUtils::CheckW qw(checkW); checkW(@ARGV)" testdata\CheckW wtest.w wtest.txt

REM For folder.
perl -e "use LvCorporaTools::PMLUtils::CheckW qw(processDir); processDir(@ARGV)" testdata\CheckW

pause