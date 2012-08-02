::perl -e "use LvMorphoCorpus::TestDataSelector::SplitData; LvMorphoCorpus::TestDataSelector::SplitData::splitFile(@ARGV)" testdata\SplitData zeens.m 0.2 0

perl -e "use LvMorphoCorpus::TestDataSelector::SplitData; LvMorphoCorpus::TestDataSelector::SplitData::splitCorpus(@ARGV)" testdata/SplitData 0.2 0

pause