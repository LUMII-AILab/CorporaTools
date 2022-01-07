REM Process single file.
perl -e "use LvCorporaTools::FormatTransf::ConllU2Vert qw(processFile); processFile(@ARGV)" testdata\ConllU2Vert\lv_lvtb-ud-tb.conllu

pause