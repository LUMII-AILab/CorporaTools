REM Split the given folder according to given split list.
perl -e "use LvCorporaTools::DataSelector::SplitByList qw(splitTDT); splitTDT(@ARGV)" testdata\SplitTrainDevTest\data testdata\SplitTrainDevTest\split.tsv testdata\SplitTrainDevTest

pause
