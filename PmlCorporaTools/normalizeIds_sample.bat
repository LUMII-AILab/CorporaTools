REM Check if w file matches original text.
::perl -e "use LvCorporaTools::PMLUtils::CheckW qw(processDir); processDir(@ARGV)" data\original
::rmdir .\data\checkedW /Q /S
::move .\data\original\res .\data\checkedW
::copy .\data\original\*.a .\data\checkedW
::copy .\data\original\*.m .\data\checkedW
REM MANUAL: check error messages.

:: Prepare for next step.
::rmdir .\data\checkedAll /Q /S
::mkdir .\data\checkedAll
::copy .\data\checkedW\*.a .\data\checkedAll\
::copy .\data\checkedW\*.m .\data\checkedAll\
::copy .\data\checkedW\*.w .\data\checkedAll\
REM Check fileset IDs
::perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(processDir); processDir(@ARGV)" data\checkedAll A

REM MANUAL: check error messages and correct them.

REM Run ID normalization.
::perl -e "use LvCorporaTools::PMLUtils::NormalizeIds qw(processDir); processDir(@ARGV)" data\checkedAll
::move .\data\checkedAll\res .\data\normalizedIds

REM MANUAL: Fix m-files element order: open m files in Sublime Text, find (?s)(<s[ >].*</s>)(\s*)(<meta>.*</meta>) and replace with \3\2\1

REM Recheck that nothing is broken.
::perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(processDir); processDir(@ARGV)" data\normalizedIds A

pause