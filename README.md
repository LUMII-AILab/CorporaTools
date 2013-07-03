Latvian Corpora Tools
=====================

Support tools for Latvian Treebank and Latvian morphologically annotated
corpora. Native data format of the Latvian corpora are adjusted PML.
Corresponding PML Schemas are available at
"TrEd extension/lv-treebank/resources".

Contents
--------

* LvCorporaTools - perl scripts and xslt-s for varios coprora data processing;
* testdata - data samples for stuff in LvCorporaTools;
* TrEd extension - development snapshot for lv-treebank module for TrEd tool
(this enables TrEd to operate with lv-PML files);
* *.bat files - invocation examples for LvCorporaTools;
* MorphoVerificator - java written tool implementing various heuristics for
searching human errors in morphological corpora.

Main work-flows
---------------

* Converting treebank to CONLL format: TreeTransformatorUI with steps --dep
--red --knit --conll (application example [current folder is CorporaTools]:
perl LvCorporaTools/TreeTransf/TreeTransformatorUI.pm --dir data_folder
--collect --dep BASELEM ROW BASELEM 0 --red --knit --conll 1 FIRST FULL 0
--fold 1, for more information see TreeTransformatorUI.pm or launch it without
parameters).
* Converting treebank to TigerXML: Unite (if needed), TreeTransformatorUI with
step --ord TOKEN, apply lvpml2tiger.xsl
* Preprocessing treebank files before putting in repository: Unite (if needed),
CheckW, NormalizeIds, CheckLvPml with param A
* Preprocessing morphocorpus files before putting in repository: CheckLvPml
with param M

License
-------

(c) Institute of Mathematics and Computer Science, University of Latvia, 2010-2013

This software is licensed under GNU General Public License.
Commercial licensing is available if necessary, contact us at lauma@ailab.lv.
