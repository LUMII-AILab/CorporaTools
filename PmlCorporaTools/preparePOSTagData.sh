#!/bin/sh
set -o nounset
set -o errexit

# FIXME! now nothing is taken from Morphocorpus anymore, but its used as the temporary location for merging all the data...
if [ $# -ge 1 ]
then sourceFolder="$1"
# should come from https://github.com/LUMII-AILab/Morphocorpus
else sourceFolder="../../Morphocorpus/Corpora" 
fi

#Treebank is in a separate git repository https://github.com/LUMII-AILab/Treebank, and its morphological data is also usable for tagger training
treebankFolder="../../Treebank/Corpora"
morphologyFolder="../../morphology"
taggerFolder="../../LVTagger"

# We'll use the Morphocorpus Corpora/Merged folder as the temporary location for merging all the data
pmlFolder="$sourceFolder/Merged"
rm -rf $pmlFolder
mkdir $pmlFolder
mkdir $pmlFolder/dev
mkdir $pmlFolder/test
mkdir $pmlFolder/train

# For treebank, we're filtering out files that are marked as not yet finished (AUTO) or needing corrections (FIXME)
tail -n +2 $treebankFolder/LatvianTreebankMorpho.fl | while read file
do
	if [ -z "$file" ]; then
		echo "empty line" 
	elif [[ $file =~ .*Verbu_rindkopas.* ]] && grep -q "<comment>AUTO" "$treebankFolder/${file%.m}.a"; then
		echo "skipping $file - unfinished"
	elif grep -q "<comment>FIXME" "$treebankFolder/${file%.m}.a"; then
		echo "skipping $file - fixme"
	else
		cp "$treebankFolder/$file" $pmlFolder
		cp "$treebankFolder/${file%.m}.w" $pmlFolder
	fi
done

# Knitting merges the .m and .w files together
echo "Knitting - start"
time perl -e "use LvCorporaTools::PMLUtils::Knit qw(processDir); processDir(@ARGV)" $pmlFolder m "../TrEd extension/lv-treebank/resources" >/dev/null
echo "Knitting - done"
rm $pmlFolder/*.m
rm $pmlFolder/*.w

# We use a predefined file split between train/dev/test
while IFS=$'\t' read -r -a entry
do
	type=${entry[0]} 
	file=${entry[1]}
	if [ $type = "dev" ]; then
		# echo "copying $file to devset"
		mv "$pmlFolder/res/$file.pml" "$pmlFolder/dev" || true;
	elif [ $type = "test" ]; then
		# echo "copying $file to testset"
		mv "$pmlFolder/res/$file.pml" "$pmlFolder/test" || true;
	elif [ $type = "train" ]; then
		# echo "copying $file to trainset"
		mv "$pmlFolder/res/$file.pml" "$pmlFolder/train" || true;
	elif [ $type = "skip" ]; then
		echo "skipping $file"
		rm "$pmlFolder/res/$file.pml" || true
	else 
		echo "$file has bad type"
	fi
done < "$treebankFolder/../Datasplits/testdevtrain.tsv"

shopt -s nullglob
for file in $pmlFolder/res/*.pml; do 
	echo "$file has no defined type";
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

# Convert all the data from PML to tab-delimited vert format
echo "Converting $pmlFolder"
for n in $(find "$pmlFolder/train" -type f -name "*.pml")
do
   ./runPmlMToPlain.sh "$n" >/dev/null
done
find "$pmlFolder/train" -type f -name "*.txt" -exec cat {} > "$pmlFolder/train.txt" \;

for n in $(find "$pmlFolder/dev" -type f -name "*.pml")
do
   ./runPmlMToPlain.sh "$n" >/dev/null
done
find "$pmlFolder/dev" -type f -name "*.txt" -exec cat {} > "$pmlFolder/dev.txt" \;

for n in $(find "$pmlFolder/test" -type f -name "*.pml")
do
   ./runPmlMToPlain.sh "$n" >/dev/null
done
find "$pmlFolder/test" -type f -name "*.txt" -exec cat {} > "$pmlFolder/test.txt" \;
echo "Converting done"
rm -rf "$pmlFolder/train"
rm -rf "$pmlFolder/dev"
rm -rf "$pmlFolder/test"

cat "$pmlFolder/train.txt" "$pmlFolder/dev.txt" "$pmlFolder/test.txt" > "$pmlFolder/all.txt"
cat "$pmlFolder/train.txt" "$pmlFolder/dev.txt" > "$pmlFolder/train_dev.txt"

train_sent=`awk '/^<s>/{a++}END{print a}' "$pmlFolder/train.txt"`
test_sent=`awk '/^<s>/{a++}END{print a}' "$pmlFolder/test.txt"`
dev_sent=`awk '/^<s>/{a++}END{print a}' "$pmlFolder/dev.txt"`
all_sent=`awk '/^<s>/{a++}END{print a}' "$pmlFolder/all.txt"`
train_tok=`awk '/^[^<]/{a++}END{print a}' "$pmlFolder/train.txt"`
test_tok=`awk '/^[^<]/{a++}END{print a}' "$pmlFolder/test.txt"`
dev_tok=`awk '/^[^<]/{a++}END{print a}' "$pmlFolder/dev.txt"`
all_tok=`awk '/^[^<]/{a++}END{print a}' "$pmlFolder/all.txt"`
printf 'Training set:    %5s sentences, %6s tokens\n' $train_sent $train_tok
printf 'Development set: %5s sentences, %6s tokens\n' $dev_sent $dev_tok
printf 'Test set:        %5s sentences, %6s tokens\n' $test_sent $test_tok
printf 'TOTAL:           %5s sentences, %6s tokens\n' $all_sent $all_tok

cp "$pmlFolder/train.txt" "$morphologyFolder/src/main/resources/"
cp "$pmlFolder/all.txt" "$morphologyFolder/src/test/resources/"
cp "$pmlFolder/dev.txt" "$morphologyFolder/src/test/resources/"
cp "$pmlFolder/test.txt" "$morphologyFolder/src/test/resources/"
cp "$treebankFolder/../Docs/Annotation how-to/SemTi-Kamols_morphotags.xlsx" "$morphologyFolder/docs/"

cp "$pmlFolder/train.txt" "$taggerFolder/MorphoCRF/"
cp "$pmlFolder/train_dev.txt" "$taggerFolder/MorphoCRF/"
cp "$pmlFolder/test.txt" "$taggerFolder/MorphoCRF/"
cp "$pmlFolder/dev.txt" "$taggerFolder/MorphoCRF/"
cp "$pmlFolder/all.txt" "$taggerFolder/MorphoCRF/"
cp "$treebankFolder/../Docs/Annotation how-to/SemTi-Kamols_morphotags.xlsx" "$taggerFolder/docs/"

echo "Done!"
