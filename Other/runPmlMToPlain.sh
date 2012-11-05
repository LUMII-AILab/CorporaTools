#!/bin/sh

if test "$1"
then filelist="$1"
else filelist="testdata/PMLToPlain/zeens.m"
fi

resultfile="`echo "$filelist" | sed -e 's/^\(.*\)\.m$/\1\.txt/g'`"

echo "PML->plain: Converting $filelist to $resultfile"

perl -e "use LvMorphoCorpus::Utils::ApplyXSLT; LvMorphoCorpus::Utils::ApplyXSLT::applyXSLT1_0(@ARGV)" "$filelist" LvMorphoCorpus/PMLToPlain/pmlM2plain.xsl "$resultfile"

