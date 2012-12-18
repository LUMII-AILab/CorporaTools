:: Assuming that morphology repository can be found in the same folder as Morphocorpus.

mkdir lib
mkdir lib\morphology

copy "..\..\..\morphology\dist\Lexicon*.xml" "lib\morphology"
copy "..\..\..\morphology\dist\Statistics.xml" "lib\morphology"

java -Xmx1500m -classpath ".;..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator data/Balanseetais rez/Balanseetais

java -Xmx1500m -classpath ".;..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator data/Juridiskais rez/Juridiskais

::java -classpath ".;..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator data/Ledus rez/Ledus

java -Xmx1500m -classpath ".;..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator data/Veestnesis rez/Veestnesis

::java -Xmx1500m -classpath ".;..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator data/Treebank rez/Treebank

pause
