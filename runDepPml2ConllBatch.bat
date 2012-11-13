perl -e "use LvTreeBank::Transformations::DepPml2ConllBatch; LvTreeBank::Transformations::DepPml2ConllBatch::transformFileBatch(@ARGV)" 0 testdata\DepPml2Conll none full

perl -e "use LvTreeBank::Transformations::DepPml2ConllBatch; LvTreeBank::Transformations::DepPml2ConllBatch::transformFileBatch(@ARGV)" 1 testdata\DepPml2Conll none full

pause