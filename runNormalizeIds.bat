REM For single dataset.
perl -e "use LvCorporaTools::PMLUtils::NormalizeIds qw(normalizeIds); normalizeIds(@ARGV)" testdata\NormalizeIds bildes bildes

REM For multiple datasets.
perl -e "use LvCorporaTools::PMLUtils::NormalizeIds qw(processDir); processDir(@ARGV)" testdata\NormalizeIds

pause