:: Assuming that morphology repository can be found in the same folder as Morphocorpus.

mkdir lib
mkdir lib\morphology

copy "..\..\..\morphology\dist\Lexicon*.xml" "lib\morphology"
copy "..\..\..\morphology\dist\Statistics.xml" "lib\morphology"

java -Xmx1500m -classpath ".;..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator corpus1_source corpus1_result

java -Xmx1500m -classpath ".;..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator corpus2_source corpus2_result

pause
