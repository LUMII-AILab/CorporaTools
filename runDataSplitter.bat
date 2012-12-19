::perl -e "use LvCorporaTools::TestDataSelector::SplitData qw(splitFile); splitFile(@ARGV)" testdata\SplitData zeens.m 0.2 0

perl -e "use LvCorporaTools::TestDataSelector::SplitData qw(splitCorpus); splitCorpus(@ARGV)" testdata/SplitData 0.2 0

pause