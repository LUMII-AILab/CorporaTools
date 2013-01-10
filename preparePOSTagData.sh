#!/bin/sh
set -o nounset
set -o errexit

if test "$1"
then pmlFolder="$1"
else pmlFolder="testdata/SplitMorphoData"
fi

echo "Converting $pmlFolder"

perl -e "use LvCorporaTools::TestDataSelector::SplitMorphoData; LvCorporaTools::TestDataSelector::SplitMorphoData::splitCorpus(@ARGV)" "$pmlFolder" 0.2 0

mv "$pmlFolder/dev" "$pmlFolder/train"
mv "$pmlFolder/test" "$pmlFolder/devtest"

perl -e "use LvCorporaTools::TestDataSelector::SplitMorphoData; LvCorporaTools::TestDataSelector::SplitMorphoData::splitCorpus(@ARGV)" "$pmlFolder/devtest" 0.5 0

mv "$pmlFolder/devtest/dev" "$pmlFolder/"
mv "$pmlFolder/devtest/test" "$pmlFolder/"
rm -rf "$pmlFolder/devtest"

for n in $(find "$pmlFolder/train" -type f -name "*.m")
do
   ./runPmlMToPlain.sh "$n"
done
find "$pmlFolder/train" -type f -name "*.txt" -exec cat {} > "$pmlFolder/train.txt" \;

for n in $(find "$pmlFolder/dev" -type f -name "*.m")
do
   ./runPmlMToPlain.sh "$n"
done
find "$pmlFolder/dev" -type f -name "*.txt" -exec cat {} > "$pmlFolder/dev.txt" \;

for n in $(find "$pmlFolder/test" -type f -name "*.m")
do
   ./runPmlMToPlain.sh "$n"
done
find "$pmlFolder/test" -type f -name "*.txt" -exec cat {} > "$pmlFolder/test.txt" \;

rm -rf "$pmlFolder/train"
rm -rf "$pmlFolder/dev"
rm -rf "$pmlFolder/test"

cat "$pmlFolder/train.txt" "$pmlFolder/dev.txt" "$pmlFolder/test.txt" > "$pmlFolder/all.txt"

echo "Done!"
