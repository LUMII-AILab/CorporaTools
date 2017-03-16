#!/bin/sh
set -o nounset
set -o errexit

if [ $# -ge 1 ]
then sourceFolder="$1"
# should come from https://github.com/LUMII-AILab/Morphocorpus
else sourceFolder="../../Morphocorpus/Corpora" 
fi

pmlFolder="$sourceFolder/Merged"
rm -rf $pmlFolder
mkdir $pmlFolder
mkdir $pmlFolder/dev
mkdir $pmlFolder/test
mkdir $pmlFolder/train

# This subset of corpora is double-checked and usable for tagger training
cp $sourceFolder/Balanseetais/Jaunaakais/*.m $pmlFolder
cp $sourceFolder/Latvijas\ Veestnesis/Jaunaakais/*.m $pmlFolder

#treebank is in a separate git repository https://github.com/LUMII-AILab/Treebank, and its morphological data is also usable for tagger training
treebankFolder="../../Treebank/Corpora"
# shopt -s globstar
# cp $treebankFolder/Corpora/**/*.m $pmlFolder
find $treebankFolder/ -name "*.m" -exec cp {} $pmlFolder ";"

# We use a predefined file split between train/dev/test
for file in $(<LvCorporaTools/corpus_devset.txt); do 
	# echo "copying $file to devset"
	mv "$pmlFolder/$file.m" "$pmlFolder/dev";
done
for file in $(<LvCorporaTools/corpus_testset.txt); do 
	# echo "copying $file to testset"
	mv "$pmlFolder/$file.m" "$pmlFolder/test";
done
for file in $pmlFolder/*.m; do 
	mv "$file" "$pmlFolder/train/";
done

# If the files would need splitting between dev/test/train, then the following lines would do it

# perl -e "use LvCorporaTools::DataSelector::SplitMorphoData; LvCorporaTools::DataSelector::SplitMorphoData::splitCorpus(@ARGV)" "$pmlFolder" 0.2 0
# mv "$pmlFolder/dev" "$pmlFolder/train"
# mv "$pmlFolder/test" "$pmlFolder/devtest"
# perl -e "use LvCorporaTools::DataSelector::SplitMorphoData; LvCorporaTools::DataSelector::SplitMorphoData::splitCorpus(@ARGV)" "$pmlFolder/devtest" 0.5 0
# mv "$pmlFolder/devtest/dev" "$pmlFolder/"
# mv "$pmlFolder/devtest/test" "$pmlFolder/"
# rm -rf "$pmlFolder/devtest"

echo "Converting $pmlFolder"

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
cat "$pmlFolder/train.txt" "$pmlFolder/dev.txt" > "$pmlFolder/train_dev.txt"

echo "Done!"
