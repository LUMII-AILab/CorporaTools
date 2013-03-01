perl -e "use LvCorporaTools::TreeTransformations::DepPml2ConllBatch qw(transformFileBatch); transformFileBatch(@ARGV)" 0 testdata\DepPml2Conll none full

perl -e "use LvCorporaTools::TreeTransformations::DepPml2ConllBatch qw(transformFileBatch); transformFileBatch(@ARGV)" 1 testdata\DepPml2Conll none full

pause