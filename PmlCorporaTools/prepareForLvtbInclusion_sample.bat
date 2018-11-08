REM Step by step sample on how to prepare files for including on Latvian Treebank.

:: To add new file to Latvian Treebank, following things must be done:
:: * Convert all files to UTF-8 without BOM
:: * Do PMLUtils::Unite to unite file parts if necessary
:: * Do PMLUtils::CheckW and use newly-created w file
:: * Do PMLUtils::NormalizeSpaces to ensure no spaces are lost in m file
:: * Do PMLUtils::CheckLvPml and solve all identified problems
:: * Do PMLUtils::NormalizeIds to obtain sequential IDs

REM Unite files
::perl -e "use LvCorporaTools::PMLUtils::Unite qw(unite); unite(@ARGV)" data DIENA_intervija_28012013

REM Check W
::perl -e "use LvCorporaTools::PMLUtils::CheckW qw(processDir); processDir(@ARGV)" data
::perl -e "use LvCorporaTools::PMLUtils::CheckW qw(checkW); checkW(@ARGV)" data filename.w filename.txt

REM Normalize spaces
::perl -e "use LvCorporaTools::PMLUtils::NormalizeSpaces qw(processDir); processDir(@ARGV)" data 

REM Check consistency
::perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(processDir); processDir(@ARGV)" data A

REM Normalize IDs
::perl -e "use LvCorporaTools::PMLUtils::NormalizeIds qw(processDir); processDir(@ARGV)" data
::perl -e "use LvCorporaTools::PMLUtils::NormalizeIds qw(normalizeIds); normalizeIds(@ARGV)" data old_filename new_filename


:::::: AND Here are given some invocation samples for aditional things.

:::: Check consistency for w and m files, if no a file available
::perl -e "use LvCorporaTools::PMLUtils::CheckLvPml qw(processDir); processDir(@ARGV)" data\v M

:::: Make TrEd filelist from all data files.
::perl LvCorporaTools/GenericUtils/MakeFilelist.pm data LatvianTreebank

pause
