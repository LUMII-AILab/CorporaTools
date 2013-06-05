REM For single file.
perl -e "use LvCorporaTools::PMLUtils::CalcStats qw(calcStats); calcStats(@ARGV)" testdata\CalcStats zeens.m

REM For folder.
perl -e "use LvCorporaTools::PMLUtils::CalcStats qw(processDir); processDir(@ARGV)" testdata\CalcStats

pause