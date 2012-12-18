::perl -e "use LvCorporaTools::TestDataSelector::SplitData; LvCorporaTools::TestDataSelector::SplitData::splitFile(@ARGV)" testdata\SplitData zeens.m 0.2 0

perl -e "use LvCorporaTools::TestDataSelector::SplitData; LvCorporaTools::TestDataSelector::SplitData::splitCorpus(@ARGV)" testdata/SplitData 0.2 0

pause