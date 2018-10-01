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

# We'll use the Morphocorpus Corpora/Merged folder as the temporary location for merging all the data
pmlFolder="$sourceFolder/Merged"
rm -rf $pmlFolder
mkdir $pmlFolder

# For treebank, we're filtering out files that are marked as not yet finished (AUTO) or needing corrections (FIXME)
tail -n +2 $treebankFolder/LatvianTreebankMorpho.fl | while read file
do
	if [ -z "$file" ]; then
		echo "empty line" 
	elif grep -q "<comment>AUTO" "$treebankFolder/${file%.m}.a"; then
		echo "skipping $file - unfinished"
	elif grep -q "<comment>FIXME" "$treebankFolder/${file%.m}.a"; then
		echo "skipping $file - fixme"
	else
		cp "$treebankFolder/$file" $pmlFolder
		cp "$treebankFolder/${file%.m}.w" $pmlFolder
		cp "$treebankFolder/${file%.m}.a" $pmlFolder
	fi
done

# We use a predefined file split between train/dev/test
while IFS=$'\t' read -r -a entry
do
	type=${entry[0]} 
	file=${entry[1]}
	if [ $type = "skip" ]; then
		echo "skipping $pmlFolder/$file"
		rm "$pmlFolder/$file.m"
		rm "$pmlFolder/$file.w"
		rm "$pmlFolder/$file.a"
	fi
done < "$treebankFolder/../Datasplits/testdevtrain.tsv"

echo "Done!"
