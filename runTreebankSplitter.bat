REM To seperate 1/5 into one data set and 2/5 into other (seed: 0).
perl -e "use LvCorporaTools::DataSelector::SplitTreebank qw(splitCorpus); splitCorpus(@ARGV)" testdata\TreebankSplitter 0.2 0

REM To make 4 aproximetly even-sized datasets (seed: 0).
perl -e "use LvCorporaTools::DataSelector::SplitTreebank qw(splitCorpus); splitCorpus(@ARGV)" testdata\TreebankSplitter 4 0

REM To concatenate multiple CoNLL files.
perl -e "use LvCorporaTools::DataSelector::SplitTreebank qw(splitCorpus); splitCorpus(@ARGV)" testdata\TreebankSplitter 1

pause