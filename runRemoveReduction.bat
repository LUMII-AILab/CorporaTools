REM For single file.
perl -e "use LvCorporaTools::TreeTransf::RemoveReduction qw(transformFile); transformFile(@ARGV)" testdata\RemoveReduction zeens-synt.a zeens-synt-noRed.a
perl -e "use LvCorporaTools::TreeTransf::RemoveReduction qw(transformFile); transformFile(@ARGV)" testdata\RemoveReduction zeens-sem.a zeens-sem-noRed.a

REM For folder.
perl -e "use LvCorporaTools::TreeTransf::RemoveReduction qw(processDir); processDir(@ARGV)" testdata\RemoveReduction


pause
