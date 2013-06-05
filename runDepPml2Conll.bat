REM Unlabeled trees in "small" CoNLL with default POSTAG and CPOSTAG; single file.
perl -e "use LvCorporaTools::FormatTransf::DepPml2Conll qw(transformFile); transformFile(@ARGV)" testdata\DepPml2Conll zeens-dep.xml 0 zeens-unlabeled.conll 0

REM Labeled trees in "large" CoNLL format with custom POSTAG and CPOSTAG; single file.
perl -e "use LvCorporaTools::FormatTransf::DepPml2Conll qw(transformFile POSTAG CPOSTAG); $POSTAG = 'FULL'; $CPOSTAG = 'FIRST'; transformFile(@ARGV)" testdata\DepPml2Conll zeens-dep.xml 1 zeens-omit-reductions.conll 1

REM Unlabeled trees in "small" CoNLL with default POSTAG and CPOSTAG; entire folder.
perl -e "use LvCorporaTools::FormatTransf::DepPml2Conll qw(processDir); processDir(@ARGV)" testdata\DepPml2Conll 0 0

REM Labeled trees in "small" CoNLL format with custom POSTAG and CPOSTAG; entire folder.
perl -e "use LvCorporaTools::FormatTransf::DepPml2Conll qw(processDir POSTAG CPOSTAG); $POSTAG = 'FULL'; $CPOSTAG = 'FIRST'; processDir(@ARGV)" testdata\DepPml2Conll 1 0

pause