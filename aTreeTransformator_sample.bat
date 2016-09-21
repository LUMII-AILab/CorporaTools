REM Get help
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm

REM Collect data, reorder them and knit in
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect --ord mode=TOKEN --knit

REM Obtaining data for old parser experiments "syntax-style"
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect --unnest --dep xpred=DEFAULT coord=DEFAULT pmc=DEFAULT root=0 phdep=1 na=0 subrt=0 --red label=0 --knit --conll label=1 cpostag=FIRST postag=FULL conll09=0 --fold p=1
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect --unnest --dep xpred=DEFAULT coord=DEFAULT pmc=DEFAULT root=0 phdep=1 na=0 subrt=0 --red label=0 --knit --conll label=1 cpostag=FIRST postag=FULL conll09=0

REM Obtaining data for old parser experiments "semantics-style"
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect --unnest --dep xpred=BASELEM coord=ROW pmc=BASELEM root=0 phdep=1 na=0 subrt=0 --red label=0 --knit --conll label=1 cpostag=FIRST postag=FULL conll09=0 --fold p=1
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data --collect --unnest --dep xpred=BASELEM coord=ROW pmc=BASELEM root=0 phdep=1 na=0 subrt=0 --red label=0 --knit --conll label=1 cpostag=FIRST postag=FULL conll09=0

REM Count different roles in a conll file
::perl LvCorporaTools/RoleCounter.pm data/fold corpus.conll

REM Fold data sets
::perl LvCorporaTools/UIs/TreeTransformatorUI.pm --dir data/conll --fold p=1

REM Run generator producing all possible datasets
REM (see Generator file for default value-sets to be combined)
::perl LvCorporaTools/UIs/AllDatasetGenerator.pm data log.txt 1 1

REM This is how morphotagger is run
REM Afterwards line-endings and endcodings must be checked
REM Also, windows tends to adg aditional garbage lines in begin/end of the file
::::morphotagger -conll-in -conll-x -leta <sem-devel-20131007.conll >sem-pos-devel-20131007.conll



pause
