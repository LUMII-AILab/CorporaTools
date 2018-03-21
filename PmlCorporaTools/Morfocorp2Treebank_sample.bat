REM build M & A file
::perl -e "use LvCorporaTools::FormatTransf::Conll2MA qw(processFileSet); processFileSet(@ARGV)" data\w\some_file.w  data\m-a data\conll\some_file.conll
::@copy .\data\w\some_file.* .\data\m-a\ >nul

REM ceck stuff
REM Check if w file matches original text.
::perl -e "use LvCorporaTools::PMLUtils::CheckW qw(processDir); processDir(@ARGV)" data\m-a
::@if exist .\data\checkedW rmdir .\data\checkedW /Q /S >nul
::@move .\data\m-a\res .\data\checkedW >nul
::@copy .\data\m-a\*.a .\data\checkedW >nul
::@copy .\data\m-a\*.m .\data\checkedW >nul
REM MANUAL: check if nothing failed!

:: Prepare for next step.
::@if exist .\data\checkedAll rmdir .\data\checkedAll /Q /S >nul
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
::@if exist .\data\normalizedIds rmdir .\data\normalizedIds /Q /S >nul
::@move .\data\checkedAll\res .\data\normalizedIds >nul

REM Recheck that nothing is broken.
::perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(processDir); processDir(@ARGV)" data\normalizedIds A

REM MANUAL: have you checked ALL error messages in the middle?

pause

