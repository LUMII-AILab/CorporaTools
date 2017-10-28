REM Split the given folder according to given split list.
perl -e "use LvCorporaTools::DataSelector::SplitTrainDevTest qw(makeTDT); makeTDT(@ARGV)" testdata\SplitTrainDevTest\data testdata\SplitTrainDevTest\split.tsv testdata\SplitTrainDevTest

pause
