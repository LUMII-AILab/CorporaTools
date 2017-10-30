REM For single file.
perl -e "use LvCorporaTools::DataSelector::SplitConll2Para qw(transformFile); transformFile(@ARGV)" testdata\SplitConll2Para tenis3.conllu

REM For folder.
perl -e "use LvCorporaTools::DataSelector::SplitConll2Para qw(processDir); processDir(@ARGV)" testdata\SplitConll2Para

pause