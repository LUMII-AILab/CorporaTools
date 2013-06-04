#!/bin/sh

if test "$1"
then filelist="$1"
else filelist="testdata/LegacyToPml/zeens.m"
fi

resultfile="`echo "$filelist" | sed -e 's/^\(.*\)\.m$/\1\.txt/g'`"

echo "PML->plain: Converting $filelist to $resultfile"

perl -e "use LvCorporaTools::GenericUtils::ApplyXSLT; LvCorporaTools::GenericUtils::ApplyXSLT::applyXSLT1_0(@ARGV)" "$filelist" LvCorporaTools/FormatTransf/pmlM2plain.xsl "$resultfile"

