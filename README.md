Latvian Corpora Tools
=====================

Support tools for Latvian Treebank and Latvian morphologically annotated
corpora. Native data format of the Latvian corpora are adjusted PML.
Corresponding PML Schemas are available at
TrEd extension/lv-treebank/resources.

Contents
--------

* PmlCorporaTools - perl scripts and xslt-s for varios coprora data processing;
* LVTB2UD - transformator from native annotation format to Universal
Dependencies;
* TrEd extension - development snapshot for lv-treebank module for TrEd tool
(this enables TrEd to operate with lv-PML files);
* MorphoVerificator - java written tool implementing various heuristics for
searching human errors in morphological corpora (obselote).

For each script in PmlCorporaTools there is a .bat file showing invocation
sample on dummy data located in PmlCorporaTools/testdata. Also, it is possible
to launch these scripts without parameters to get information about expected
parameter values and meaning.

Some scripts have not been used several years and might be obselote, sorry.

Prerequisites
-------------
For PmlCorporaTools:
* Perl
* XSLT module (on Ubuntu, run 'sudo apt-get install libxml-libxslt-perl')

For LVTB2UD
* Java
* https://github.com/PeterisP/morphology


Main work-flows
---------------

* For converting teebank to UD, consult comments in
  PmlCorporaTools/convertLvtbToUd_sample.bat
* For converting treebank to old dependency formats used before UD, consult
  comments in PmlCorporaTools/aTreeTransformator_sample.bat
* For ID verification before including treebank files into SemBank, consult
  PmlCorporaTools/checkNormalizeSembankIds_sample.bat (Unite might be needed
  beforehand)
* For splitting whole-document CoNLL-U files to single-paragraph CoNLL-U files,
  consult PmlCorporaTools/postprocessConlluForSembank_sample.bat
* For parameter specifics to create a PML fileset accordingly to current
  naming conventions see PmlCorporaTools/LVK2LVTB-PML.readme.md
* Converting treebank to TigerXML is done as follows: Unite (if needed),
  TreeTransformatorUI with step --ord TOKEN, apply lvpml2tiger.xsl
* Preprocessing morphocorpus files before putting in repository: CheckLvPml
  with param M
* Preparing morphologically tagged data for training the LVTagger system:
  1.	download morphocorpus data from https://github.com/LUMII-AILab/Morphocorpus	
  2.	download treebank from https://github.com/LUMII-AILab/Treebank
  3.	run ./preparePOSTagData.sh
  4.	the results will be placed at ../Morphocorpus/Corpora/Merged/*.txt

License
-------

(c) Institute of Mathematics and Computer Science, University of Latvia, 2010-2017

This software is licensed under GNU General Public License.
Commercial licensing is available if necessary, contact us at lauma@ailab.lv.
