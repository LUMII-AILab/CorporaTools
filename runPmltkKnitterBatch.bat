@echo off
set PERL5LIB=C:\PROGRA~1\tred\dependencies\lib\perl5;C:\PROGRA~1\tred\dependencies\lib\perl5\MSWin32-x86-multi-thread;C:\PROGRA~1\tred\tredlib\libs\fslib;C:\PROGRA~2\tred\dependencies\lib\perl5;C:\PROGRA~2\tred\dependencies\lib\perl5\MSWin32-x86-multi-thread;C:\PROGRA~2\tred\tredlib\libs\fslib;%PERL5LIB%
@echo on

copy "TrEd extension\lv-treebank\resources\*.xml" "testdata\pmltk-knitter\"

perl -e "use LvCorporaTools::PMLUtils::PmltkKnitterBatch qw(processDir); processDir(@ARGV)" testdata/pmltk-knitter pmltk-1.1.5

pause
