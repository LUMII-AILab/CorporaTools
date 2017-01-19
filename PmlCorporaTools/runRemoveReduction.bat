REM For single file with default settings.
perl -e "use LvCorporaTools::TreeTransf::RemoveReduction qw(transformFile); transformFile(@ARGV)" testdata\RemoveReduction zeens-synt.a zeens-synt-noRed.a
REM For single file without empty element labeling.
perl -e "use LvCorporaTools::TreeTransf::RemoveReduction qw($LABEL_EMPTY transformFile); $LABEL_EMPTY = 0; transformFile(@ARGV)" testdata\RemoveReduction zeens-sem.a zeens-sem-noRed.a

REM For folder.
perl -e "use LvCorporaTools::TreeTransf::RemoveReduction qw(processDir); processDir(@ARGV)" testdata\RemoveReduction


pause
