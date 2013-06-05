REM Unlabeled trees in "small" CoNLL with default POSTAG and CPOSTAG.
perl -e "use LvCorporaTools::FormatTransf::DepPml2Conll qw(processDir); processDir(@ARGV)" testdata\DepPml2Conll 0 0

REM Labeled trees in "small" CoNLL format with custom POSTAG and CPOSTAG.
perl -e "use LvCorporaTools::FormatTransf::DepPml2Conll qw(processDir POSTAG CPOSTAG); $POSTAG = 'FULL'; $CPOSTAG = 'FIRST'; processDir(@ARGV)" testdata\DepPml2Conll 1 0

pause