#!/bin/sh
set -o nounset
set -o errexit

if [ $# -ge 1 ]
then sourceFolder="$1"
# should come from https://github.com/LUMII-AILab/Morphocorpus
else sourceFolder="../../Morphocorpus/Corpora" 
fi

#treebank is in a separate git repository https://github.com/LUMII-AILab/Treebank, and its morphological data is also usable for tagger training
treebankFolder="../../Treebank/Corpora"
morphologyFolder="../../morphology"
taggerFolder="../../LVTagger"

pmlFolder="$sourceFolder/Merged"
rm -rf $pmlFolder
mkdir $pmlFolder
mkdir $pmlFolder/dev
mkdir $pmlFolder/test
mkdir $pmlFolder/train

# This subset of corpora is double-checked and usable for tagger training
cp $sourceFolder/Balanseetais/Jaunaakais/*.[m,w] $pmlFolder
cp $sourceFolder/Latvijas\ Veestnesis/Jaunaakais/*.[m,w] $pmlFolder

tail -n +2 $treebankFolder/LatvianTreebankMorpho.fl | while read file
do
	if grep -q "<comment>AUTO</comment>" "$treebankFolder/${file%.m}.a"; then
		echo "skipping $file - unfinished"
	elif grep -q "<comment>FIXME" "$treebankFolder/${file%.m}.a"; then
		echo "skipping $file - fixme"
	else
		cp "$treebankFolder/$file" $pmlFolder
		cp "$treebankFolder/${file%.m}.w" $pmlFolder
	fi
done

# Knit the .m and .w files together
perl -e "use LvCorporaTools::PMLUtils::Knit qw(processDir); processDir(@ARGV)" $pmlFolder m "../TrEd extension/lv-treebank/resources"
rm $pmlFolder/*.[m,w]

# We use a predefined file split between train/dev/test
for file in $(<LvCorporaTools/corpus_devset.txt); do 
	# echo "copying $file to devset"
	mv "$pmlFolder/res/$file.pml" "$pmlFolder/dev";
done
for file in $(<LvCorporaTools/corpus_testset.txt); do 
	# echo "copying $file to testset"
	mv "$pmlFolder/res/$file.pml" "$pmlFolder/test";
done
for file in $pmlFolder/res/*.pml; do 
	mv "$file" "$pmlFolder/train/";
done
rm -rf "$pmlFolder/res"

# If the files would need automatic random splitting of sentences between dev/test/train, then the following lines would do it

# perl -e "use LvCorporaTools::DataSelector::SplitMorphoData; LvCorporaTools::DataSelector::SplitMorphoData::splitCorpus(@ARGV)" "$pmlFolder" 0.2 0
# mv "$pmlFolder/dev" "$pmlFolder/train"
# mv "$pmlFolder/test" "$pmlFolder/devtest"
# perl -e "use LvCorporaTools::DataSelector::SplitMorphoData; LvCorporaTools::DataSelector::SplitMorphoData::splitCorpus(@ARGV)" "$pmlFolder/devtest" 0.5 0
# mv "$pmlFolder/devtest/dev" "$pmlFolder/"
# mv "$pmlFolder/devtest/test" "$pmlFolder/"
# rm -rf "$pmlFolder/devtest"

echo "Converting $pmlFolder"

for n in $(find "$pmlFolder/train" -type f -name "*.pml")
do
   ./runPmlMToPlain.sh "$n"
done
find "$pmlFolder/train" -type f -name "*.txt" -exec cat {} > "$pmlFolder/train.txt" \;

for n in $(find "$pmlFolder/dev" -type f -name "*.pml")
do
   ./runPmlMToPlain.sh "$n"
done
find "$pmlFolder/dev" -type f -name "*.txt" -exec cat {} > "$pmlFolder/dev.txt" \;

for n in $(find "$pmlFolder/test" -type f -name "*.pml")
do
   ./runPmlMToPlain.sh "$n"
done
find "$pmlFolder/test" -type f -name "*.txt" -exec cat {} > "$pmlFolder/test.txt" \;

rm -rf "$pmlFolder/train"
rm -rf "$pmlFolder/dev"
rm -rf "$pmlFolder/test"

cat "$pmlFolder/train.txt" "$pmlFolder/dev.txt" "$pmlFolder/test.txt" > "$pmlFolder/all.txt"
cat "$pmlFolder/train.txt" "$pmlFolder/dev.txt" > "$pmlFolder/train_dev.txt"

cp "$pmlFolder/train.txt" "$morphologyFolder/src/main/resources/"
cp "$pmlFolder/all.txt" "$morphologyFolder/src/test/resources/"
cp "$pmlFolder/dev.txt" "$morphologyFolder/src/test/resources/"
cp "$pmlFolder/test.txt" "$morphologyFolder/src/test/resources/"
cp "$treebankFolder/../Docs/SemTi-Kamols_morphotags.xlsx" "$morphologyFolder/docs/"

cp "$pmlFolder/train.txt" "$taggerFolder/MorphoCRF/"
cp "$pmlFolder/train_dev.txt" "$taggerFolder/MorphoCRF/"
cp "$pmlFolder/test.txt" "$taggerFolder/MorphoCRF/"
cp "$pmlFolder/dev.txt" "$taggerFolder/MorphoCRF/"
cp "$pmlFolder/all.txt" "$taggerFolder/MorphoCRF/"
cp "$treebankFolder/../Docs/SemTi-Kamols_morphotags.xlsx" "$taggerFolder/docs/"

echo "Done!"
