::perl -e "use LvCorporaTools::TestDataSelector::SplitMorphoData qw(splitFile); splitFile(@ARGV)" testdata\SplitMorphoData zeens.m 0.2 0

perl -e "use LvCorporaTools::TestDataSelector::SplitMorphoData qw(splitCorpus); splitCorpus(@ARGV)" testdata/SplitMorphoData 0.2 0

pause