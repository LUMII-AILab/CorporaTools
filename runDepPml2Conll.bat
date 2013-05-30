REM Unlabeled trees in "small" CoNLL with default POSTAG and CPOSTAG.
perl -e "use LvCorporaTools::TreeTransformations::DepPml2Conll qw(transformFile); transformFile(@ARGV)" testdata\DepPml2Conll zeens-dep.xml 0 zeens-unlabeled.conll 0

REM Labeled trees in "large" CoNLL format with custom POSTAG and CPOSTAG.
perl -e "use LvCorporaTools::TreeTransformations::DepPml2Conll qw(transformFile POSTAG CPOSTAG); $POSTAG = 'FULL'; $CPOSTAG = 'FIRST'; transformFile(@ARGV)" testdata\DepPml2Conll zeens-dep.xml 1 zeens-omit-reductions.conll 1

pause