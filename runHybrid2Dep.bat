REM This is how you transform to dependencies for syntax experiments.
perl -e "use LvCorporaTools::TreeTransf::Hybrid2Dep qw($XPRED $COORD $PMC $LABEL_ROOT transformFile); $COORD = 'DEFAULT'; $PMC = 'DEFAULT'; $XPRED = 'DEFAULT'; $LABEL_ROOT = 0; transformFile(@ARGV)" testdata\Hybrid2Dep zeens.a 0 zeens-synt.a

REM This is how you transform to dependencies for semantic experiments.
perl -e "use LvCorporaTools::TreeTransf::Hybrid2Dep qw($XPRED $COORD $PMC $LABEL_ROOT transformFile); $COORD = 'ROW'; $PMC = 'BASELEM'; $XPRED = 'BASELEM'; $LABEL_ROOT = 0; transformFile(@ARGV)" testdata\Hybrid2Dep zeens.a 0 zeens-sem.a

REM This ir how you perform batch transformations.
perl -e "use LvCorporaTools::TreeTransf::Hybrid2Dep qw(processDir); processDir(@ARGV)" testdata\Hybrid2Dep


pause