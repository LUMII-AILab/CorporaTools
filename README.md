# Latvian Corpora Tools

Support tools for Latvian Treebank and Latvian morphologically annotated
corpora. Native data format of the Latvian corpora are adjusted PML.
Corresponding PML Schemas are available at
TrEd extension/lv-treebank/resources.


## Contents

* PmlCorporaTools - perl scripts and xslt-s for varios coprora data processing;
* LVTB2UD - transformator from native annotation format to Universal
Dependencies;
* TrEd extension - development snapshot for lv-treebank module for TrEd tool
(this enables TrEd to operate with lv-PML files);
* MorphoVerificator - *obsolete* - java written tool implementing various heuristics for
searching human errors in morphological corpora;
* ParserTools - *obsolete* - tools used for preUD parser experiments.

For each script in PmlCorporaTools there is a .bat file showing invocation
sample on dummy data located in PmlCorporaTools/testdata. Also, it is possible
to launch these scripts without parameters to get information about expected
parameter values and meaning.

Some scripts have not been used several years and might be obsolete, sorry.

Folder Docs containing datasplits moved to Treebank repo in 2017-12-11.


## Prerequisites

For PmlCorporaTools:
* Perl
* `XML::Simple`
* XSLT module (on Ubuntu, run 'sudo apt-get install libxml-libxslt-perl')
* Treex::Core according to https://ufal.mff.cuni.cz/treex/install.html

For LVTB2UD
* Java
* Morphological https://github.com/PeterisP/morphology

### OSX install
* install homebrew
* brew install cpanm
* brew install libxml2
* sudo cpanm XML::LibXML
* sudo cpanm -n PerlIO::Util
* sudo cpanm Moose
* moose-outdated | cpanm
* sudo cpanm Treex::Core
* treex -h

## Main work-flows

Files `PmlCorporaTools/*_sample.bat` contains general descriptions and 
commented-off Windows comand samples for main workflows. To follow through a
workflow on Windows machine, create a copy of the necassary `sample.bat` and
update it accordign to your needs. For convenience of Windows users
`.gitignore` blocks scripts named `/PmlCorporaTools/* - Copy.bat` :wink:
To follow through a workflow on a Unix machine, you have to create similar
shell script, but it should be relatively easy as the all interesting data
processing is done in platform independent (hopefully) perl scripts.


### Publishing LVTB

* `PmlCorporaTools/prepareForLvtbPublication_sample.bat` - create dataset for publishing LVTB
  in the native hybrid/PML format.
* `PmlCorporaTools/convertLvtbToUd_sample.bat` - converting teebank to UD \&
  conllu. Also, this contains notes on what checkups and preparation steps
  should be done for an UD release.
* `PmlCorporaTools/postprocessConlluForSembank_sample.bat` - create UD data
  for FullStack project Sembank.

### Others

* `PmlCorporaTools/checkNormalizeSembankIds_sample.bat` - ID verification
  before including treebank files into SemBank - this is what is done in
  _Treebank moratorium_.
* `PmlCorporaTools/PmlCorporaTools/aTreeTransformator_sample.bat` - convert
  treebank to old dependency formats used before UD.
* For parameter specifics to create a PML fileset accordingly to current
  naming conventions see `PmlCorporaTools/LVK2LVTB-PML.readme.md`
* `PmlCorporaTools/prepareForLvtbInclusion_sample.bat` - to add completely
  new, hand annotated file to LVTB.
* Converting treebank to TigerXML is done as follows: `Unite` (if needed),
  TreeTransformatorUI with step `--ord TOKEN`, apply `lvpml2tiger.xsl`
* Preprocessing morphocorpus files before putting in repository: `CheckLvPml`
  with param `M` (currently not used).
* `./preparePOSTagData.sh` is used for preparing morfological data for
  [LVTagger](https://github.com/PeterisP/LVTagger)


## License

(c) Institute of Mathematics and Computer Science, University of Latvia, 2010-2017

This software is licensed under GNU General Public License.
Commercial licensing is available if necessary, contact us at lauma@ailab.lv.
