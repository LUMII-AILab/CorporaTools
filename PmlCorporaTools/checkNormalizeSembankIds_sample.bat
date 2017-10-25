REM Check if w file matches original text.
::perl -e "use LvCorporaTools::PMLUtils::CheckW qw(processDir); processDir(@ARGV)" data\original
::@rmdir .\data\checkedW /Q /S >nul
::@move .\data\original\res .\data\checkedW >nul
::@copy .\data\original\*.a .\data\checkedW >nul
::@copy .\data\original\*.m .\data\checkedW >nul
REM MANUAL: check if nothing failed!

:: Prepare for next step.
::@rmdir .\data\checkedAll /Q /S >nul
::@mkdir .\data\checkedAll >nul
::@copy .\data\checkedW\*.a .\data\checkedAll\ >nul
::@copy .\data\checkedW\*.m .\data\checkedAll\ >nul
::@copy .\data\checkedW\*.w .\data\checkedAll\ >nul
REM Check fileset IDs
::perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(processDir); processDir(@ARGV)" data\checkedAll A
REM MANUAL: check error messages and correct them!

REM Run ID normalization.
::perl -e "use LvCorporaTools::PMLUtils::NormalizeIds qw(processDir); processDir(@ARGV)" data\checkedAll
:: Move results to a suitable place
::@rmdir .\data\normalizedIds /Q /S >nul
::@move .\data\checkedAll\res .\data\normalizedIds >nul

REM Recheck that nothing is broken.
::perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(processDir); processDir(@ARGV)" data\normalizedIds A

REM MANUAL: have you CHECKED all error messages in the middle?

pause