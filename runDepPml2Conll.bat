perl -e "use LvCorporaTools::TreeTransformations::DepPml2Conll; LvCorporaTools::TreeTransformations::DepPml2Conll::transformFile(@ARGV)" 0 testdata\DepPml2Conll zeens-dep.xml none full zeens-unlabeled.conll

perl -e "use LvCorporaTools::TreeTransformations::DepPml2Conll; LvCorporaTools::TreeTransformations::DepPml2Conll::transformFile(@ARGV)" 1 testdata\DepPml2Conll zeens-dep.xml none full zeens-omit-reductions.conll

pause