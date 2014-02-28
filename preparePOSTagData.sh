#!/bin/sh
set -o nounset
set -o errexit

if [ $# -ge 1 ]
then sourceFolder="$1"
# should come from https://github.com/LUMII-AILab/Morphocorpus
else sourceFolder="../Morphocorpus/Corpora" 
fi

pmlFolder="$sourceFolder/Merged"
rm -rf $pmlFolder
mkdir $pmlFolder
# This subset of corpora is double-checked and usable for tagger training
cp $sourceFolder/Balanseetais/Jaunaakais/*.m $pmlFolder
cp $sourceFolder/Latvijas\ Veestnesis/Jaunaakais/*.m $pmlFolder

#treebank is in a separate git repository https://github.com/LUMII-AILab/Treebank, and its morphological data is also usable for tagger training
treebankFolder="../Treebank/Corpora"
# shopt -s globstar
# cp $treebankFolder/Corpora/**/*.m $pmlFolder
find $treebankFolder/ -name "*.m" -exec cp {} $pmlFolder ";"

echo "Converting $pmlFolder"

perl -e "use LvCorporaTools::DataSelector::SplitMorphoData; LvCorporaTools::DataSelector::SplitMorphoData::splitCorpus(@ARGV)" "$pmlFolder" 0.2 0

mv "$pmlFolder/dev" "$pmlFolder/train"
mv "$pmlFolder/test" "$pmlFolder/devtest"

perl -e "use LvCorporaTools::DataSelector::SplitMorphoData; LvCorporaTools::DataSelector::SplitMorphoData::splitCorpus(@ARGV)" "$pmlFolder/devtest" 0.5 0

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
rm $pmlFolder/*.m

cat "$pmlFolder/train.txt" "$pmlFolder/dev.txt" "$pmlFolder/test.txt" > "$pmlFolder/all.txt"

echo "Done!"
