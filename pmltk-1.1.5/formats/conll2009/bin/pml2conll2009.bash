#!/bin/bash

if [ ${BASH_VERSION%%.*} -lt 3 ] ; then
    echo Wrong Bash version >&2
    exit 1
fi

NAME=pml2conll2009.bash
VERSION=0.2

function ShowHelp () {
    echo $NAME version $VERSION
    usage
    cat <<HELP

DESCRIPTION

  This script converts the PML data created by conll2009-to-pml.sh
  back to CoNLL-ST-2009 format.

OPTIONS:

  -v|--version          - show version
  -u|--usage            - show usage
  -h|--help             - show this help
  -b|--btred            - path to btred


  -F|--compute-fillpred - set FILLPRED=Y where PRED is set

  -g|--group <strip-suffix> <add-suffix>

    While processing files, the files with names that differ only in
    the suffix will be output to the same output file. For examlpe

    $NAME -g '-??.pml' .conll cz-01.pml cz-02.pml en-??.pml

    will create two output files, cz.conll and en.conll.

AUTHORS:
    Copyright (c) 2009 by
      Jan Stepanek <jan.stepanek[at]matfyz.cz>
HELP
}

function usage () {
    cat <<USAGE
USAGE: $NAME [-b path_to_btred] [-g strip add] file.pml..
USAGE
}

STRIP=''
ADD=.conll
BTRED=btred
OPTS=()
args=()
while [ $# -gt 0 ]; do
    case "$1" in
	-u|--usage) usage; exit; ;;
	-h|--help) ShowHelp; exit; ;;
	-v|--version) echo $VERSION; exit; ;;
	-b|--btred) BTRED=$2; shift 2; break ;;
	-g|--group) STRIP=$2; ADD=$3; shift 3; break ;;
	-F|--compute-fillpred) OPTS=("${OPTS[@]}" "$1"); shift; break ;;
	--) shift ; break ;;
        -*) echo "Invalid command-line option: $1!" >&2 ; exit 1 ;;
	*) args=("${args[@]}" "$1"); shift ;;
    esac
done

eval set -- "$@" "${args[@]}"

bindir=$(readlink -f ${0%/*})
for file ; do
    : > ${file%$STRIP}$ADD 
done

for file ; do
    $BTRED -qI "$bindir"/pml2conll -o $OPTS -- "$file" >> ${file%$STRIP}$ADD 
done
