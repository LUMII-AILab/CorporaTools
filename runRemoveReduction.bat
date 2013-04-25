perl -e "use LvCorporaTools::TreeTransformations::RemoveReduction qw(transformFile); transformFile(@ARGV)" testdata\RemoveReduction zeens-synt.a zeens-synt-noRed.a
perl -e "use LvCorporaTools::TreeTransformations::RemoveReduction qw(transformFile); transformFile(@ARGV)" testdata\RemoveReduction zeens-sem.a zeens-sem-noRed.a

pause
