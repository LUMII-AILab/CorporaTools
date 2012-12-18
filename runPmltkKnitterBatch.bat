@echo off
set PERL5LIB=C:\PROGRA~1\tred\dependencies\lib\perl5;C:\PROGRA~1\tred\dependencies\lib\perl5\MSWin32-x86-multi-thread;C:\Program Files\tred\tredlib\libs\fslib;%PERL5LIB%
@echo on

copy "TrEd extension\lv-treebank\resources\*.xml" "testdata\pmltk-knitter\"

perl -e "use LvCorporaTools::PMLUtils::PmltkKnitterBatch; LvCorporaTools::PMLUtils::PmltkKnitterBatch::runPmltkKnitterBatch(@ARGV)" testdata/pmltk-knitter pmltk-1.1.5

pause
