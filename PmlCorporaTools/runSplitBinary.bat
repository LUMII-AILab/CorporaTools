REM Split the given folder according to given split list.
perl -e "use LvCorporaTools::DataSelector::SplitByList qw(splitOnOffList); splitOnOffList(@ARGV)" testdata\SplitBinary\data testdata\SplitBinary\split.tsv testdata\SplitBinary

pause
