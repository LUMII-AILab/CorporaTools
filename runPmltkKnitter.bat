@echo off
set PERL5LIB=C:\PROGRA~1\tred\dependencies\lib\perl5;C:\PROGRA~1\tred\dependencies\lib\perl5\MSWin32-x86-multi-thread;C:\Program Files\tred\tredlib\libs\fslib;%PERL5LIB%
@echo on

copy "TrEd extension\lv-treebank\resources\*.xml" "testdata\pmltk-knitter\"
perl pmltk-1.1.5/tools/knit.pl testdata/pmltk-knitter/zeens.a testdata/pmltk-knitter/zeens.pml
pause