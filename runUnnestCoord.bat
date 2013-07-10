REM For single file.
perl -e "use LvCorporaTools::TreeTransf::UnnestCoord qw(transformFile); transformFile(@ARGV)" testdata\UnnestCoord zeens.a zeens-flatCoord.a

REM For folder.
perl -e "use LvCorporaTools::TreeTransf::UnnestCoord qw(processDir); processDir(@ARGV)" testdata\UnnestCoord


pause
