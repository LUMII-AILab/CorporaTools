#!/bin/sh
set -o nounset
set -o errexit

if test "$1"
then pmlFolder="$1"
else pmlFolder="testdata/SplitData"
fi

echo "Converting $pmlFolder"

for n in $(find "$pmlFolder" -type f -name "*.m")
do
   ./runPmlMToPlain.sh "$n"
done
find "$pmlFolder" -type f -name "*.txt" -exec cat {} > "$pmlFolder.txt" \;


