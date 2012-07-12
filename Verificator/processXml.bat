:: Assuming that morphology repository can be found in the same folder as Morphocorpus.

copy "..\..\..\..\morphology\dist\Lexicon.xml" "lib\morphology"
copy "..\..\..\..\morphology\dist\Statistics.xml" "lib\morphology"

java -classpath ".;..\..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator data/Balanseetais rez/Balanseetais

java -classpath ".;..\..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator data/Juridiskais rez/Juridiskais

::java -classpath ".;..\..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator data/Ledus rez/Ledus

java -classpath ".;..\..\..\..\morphology\dist\morphology.jar" lv.morphology.corpora.CorpusVerificator data/Veestnesis rez/Veestnesis

pause
