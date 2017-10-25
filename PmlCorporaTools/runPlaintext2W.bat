REM Process single file with default tokenization and inline metadata
perl -e "use LvCorporaTools::FormatTransf::Plaintext2W qw(transformFile); transformFile(@ARGV)" testdata\Plaintext2W zeens.txt zeens-singleton "<docmeta><title>This is sample inline metadata.</title></docmeta>" 1 zeens-singleton UTF-8 

REM Sample for importing a single paragraph from LVK.
perl -e "use LvCorporaTools::FormatTransf::Plaintext2W qw(transformFile); transformFile(@ARGV)" testdata\Plaintext2W t16_p21.txt t16 t16.meta 21 t16_p21

REM Process all txt files in given folder.
perl -e "use LvCorporaTools::FormatTransf::Plaintext2W qw(processDir); processDir(@ARGV)" testdata\Plaintext2W txt meta UTF-8 

pause