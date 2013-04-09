perl -e "use LvCorporaTools::PMLUtils::CalcStats qw(calcStats); calcStats(@ARGV)" testdata\CalcStats zeens.m
REM or
perl -e "use LvCorporaTools::PMLUtils::CalcStats qw(calcStatsBatch); calcStatsBatch(@ARGV)" testdata\CalcStats

pause