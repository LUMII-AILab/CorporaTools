REM Count roles in conll file
::perl LvCorporaTools/RoleCounter.pm data treebank.conll

REM Compare corpora
::perl LvCorporaTools/CorporaComparator.pm data/coordROW-na0-phdep1-pmcBASELEM-root0-subrt0-xpredBASELEM/treebank.conll data/coordROW-na0-phdep1-pmcBASELEM-root0-subrt0-xpredBASELEM/treebank.conll
::perl LvCorporaTools/CorporaComparator.pm data/coordROW-na0-phdep1-pmcBASELEM-root0-subrt0-xpredDEFAULT/treebank.conll data/coordROW-na0-phdep1-pmcBASELEM-root0-subrt0-xpredDEFAULT/treebank.conll
::perl LvCorporaTools/CorporaComparator.pm data/coordROW-na0-phdep1-pmcDEFAULT-root0-subrt0-xpredBASELEM/treebank.conll data/coordROW-na0-phdep1-pmcDEFAULT-root0-subrt0-xpredBASELEM/treebank.conll
::perl LvCorporaTools/CorporaComparator.pm data/coordROW-na0-phdep1-pmcDEFAULT-root0-subrt0-xpredDEFAULT/treebank.conll data/coordROW-na0-phdep1-pmcDEFAULT-root0-subrt0-xpredDEFAULT/treebank.conll

REM Make TrEd filelist from all data files.
::perl LvCorporaTools/GenericUtils/MakeFilelist.pm data LatvianTreebank

pause
