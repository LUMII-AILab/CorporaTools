REM For single dataset.
perl -e "use LvCorporaTools::PMLUtils::NormalizeSpaces qw(processFile); processFile(@ARGV)" testdata\NormalizeSpaces zeens

REM For multiple datasets.
perl -e "use LvCorporaTools::PMLUtils::NormalizeSpaces qw(processDir); processDir(@ARGV)" testdata\NormalizeSpaces

pause