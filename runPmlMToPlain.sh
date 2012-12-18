#!/bin/sh

if test "$1"
then filelist="$1"
else filelist="testdata/PMLToPlain/zeens.m"
fi

resultfile="`echo "$filelist" | sed -e 's/^\(.*\)\.m$/\1\.txt/g'`"

echo "PML->plain: Converting $filelist to $resultfile"

perl -e "use LvCorporaTools::GenericUtils::ApplyXSLT; LvCorporaTools::GenericUtils::ApplyXSLT::applyXSLT1_0(@ARGV)" "$filelist" LvCorporaTools/PMLToPlain/pmlM2plain.xsl "$resultfile"

