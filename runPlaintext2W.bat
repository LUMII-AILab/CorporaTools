REM Process single file with default tokenization and inline metadata
perl -e "use LvCorporaTools::FormatTransf::Plaintext2W qw(transformFile); transformFile(@ARGV)" testdata\Plaintext2W zeens.txt zeens-bruteTok.w 0 UTF-8 "This is sample inline metadata."

REM Process all txt files in given folder.
perl -e "use LvCorporaTools::FormatTransf::Plaintext2W qw(processDir); processDir(@ARGV)" testdata\Plaintext2W txt 0 UTF-8 metadata.meta

pause