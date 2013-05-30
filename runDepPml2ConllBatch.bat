REM Unlabeled trees in "small" CoNLL with default POSTAG and CPOSTAG.
perl -e "use LvCorporaTools::TreeTransformations::DepPml2Conll qw(transformFileBatch); transformFileBatch(@ARGV)" testdata\DepPml2Conll 0 0

REM Labeled trees in "small" CoNLL format with custom POSTAG and CPOSTAG.
perl -e "use LvCorporaTools::TreeTransformations::DepPml2Conll qw(transformFileBatch POSTAG CPOSTAG); $POSTAG = 'FULL'; $CPOSTAG = 'FIRST'; transformFileBatch(@ARGV)" testdata\DepPml2Conll 1 0

pause