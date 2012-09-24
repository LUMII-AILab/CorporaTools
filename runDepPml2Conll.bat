perl -e "use LvTreeBank::Transformations::DepPml2Conll; LvTreeBank::Transformations::DepPml2Conll::transformFile(@ARGV)" 0 testdata\DepPml2Conll zeens-dep.xml zeens-unlabeled.conll

perl -e "use LvTreeBank::Transformations::DepPml2Conll; LvTreeBank::Transformations::DepPml2Conll::transformFile(@ARGV)" 1 testdata\DepPml2Conll zeens-dep.xml zeens-omit-reductions.conll

pause