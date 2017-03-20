Latvian Corpora Tools
=====================

Support tools for Latvian Treebank and Latvian morphologically annotated
corpora. Native data format of the Latvian corpora are adjusted PML.
Corresponding PML Schemas are available at
"TrEd extension/lv-treebank/resources".

Contents
--------

* LvCorporaTools - perl scripts and xslt-s for varios coprora data processing;
* LVTB2UD - transformator from native annotation format to Universal
Dependencies;
* testdata - data samples for stuff in LvCorporaTools;
* TrEd extension - development snapshot for lv-treebank module for TrEd tool
(this enables TrEd to operate with lv-PML files);
* *.bat files - invocation examples for LvCorporaTools;
* MorphoVerificator - java written tool implementing various heuristics for
searching human errors in morphological corpora.

Prerequisites
------------
Perl
XSLT module (on Ubuntu, run 'cpan XML::libXSLT')

Main work-flows
---------------

* Converting treebank to CONLL format: TreeTransformatorUI with steps --dep
--red --knit --conll (application example [current folder is CorporaTools]:
perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect
--unnest --dep xpred=BASELEM_NO_RED coord=ROW pmc=BASELEM --red --knit
--conll label=1 cpostag=FIRST postag=FULL --fold p=1, for more information see
TreeTransformatorUI.pm or launch it without parameters).
* Converting treebank to Universal Dependencies: TreeTransformatorUI with
steps --collect --ord mode=TOKEN --knit, compile LVTB2UD, run
runUniversalizer.bat from compiled LVTB2UD distribution (data must be in
"data" folder in the compiled distribution), TreeTransformatorUI with --fold
step (optional - if single file needed)
* Converting treebank to TigerXML: Unite (if needed), TreeTransformatorUI with
step --ord TOKEN, apply lvpml2tiger.xsl
* Preprocessing treebank files before putting in repository: Unite (if needed),
CheckW, NormalizeIds, CheckLvPml with param A
* Preprocessing morphocorpus files before putting in repository: CheckLvPml
with param M
* Preparing morphologically tagged data for training the LVTagger system:
**	download morphocorpus data from https://github.com/LUMII-AILab/Morphocorpus	
**	download treebank from https://github.com/LUMII-AILab/Treebank
**	run ./preparePOSTagData.sh
**	the results will be placed at ../Morphocorpus/Corpora/Merged/*.txt

License
-------

(c) Institute of Mathematics and Computer Science, University of Latvia, 2010-2016

This software is licensed under GNU General Public License.
Commercial licensing is available if necessary, contact us at lauma@ailab.lv.
