#!/bin/bash
# conll2009-to-pml.sh     pajas@ufal.mff.cuni.cz     2009/04/21 12:22:25

# readlink -f does not work on Mac OSX, so here is a Perl-based workaround:
readlink_nf () {
    perl -MCwd -e 'print Cwd::abs_path(shift)' "$1"
}

VERSION=1.0
PRINT_USAGE=0
PRINT_HELP=0
PRINT_VERSION=0
btred=btred
sentences_per_file=50
gzip=0
out_dir=

args=()
while [ $# -gt 0 ]; do
    case "$1" in
	-u|--usage) PRINT_USAGE=1; shift; break ;;
	-h|--help) PRINT_HELP=1; shift; break ;;
	-v|--version) PRINT_VERSION=1; shift; break ;;
	-t|--trees-per-file) sentences_per_file=$2; shift 2 ;  ;;
	-b|--btred) btred=$2; shift 2 ;  ;;
	-z|--gzip) gzip=1; shift 1 ;  ;;
	-o|--out-dir) out_dir=$(readlink_nf "$2"); shift 2 ;  ;;
	--) shift ; break ;;
	-*) echo "Unknown command-line option: $1" ; exit 1 ;;
        *) args+=("$1"); shift ;;
    esac
done
eval set -- "${args[@]}"

function usage () {
    cat <<USAGE
conll2009-to-pml.sh [-t|--trees-per-file N] [-z|--gzip] [-b|--btred path_to_btred] file(s).conll
  or
conll2009-to-pml.sh [-h|--help]|[-u|--usage]|[-v|--version]
USAGE
}

function help () {
    echo "conll2009-to-pml.sh version $VERSION" 
    usage
    cat <<HELP

DESCRIPTION:

	This script converts data in CoNLL 2009 Shared Task format to
 	the Prague Markup Language format suitable for use with the Tree
 	Editor Tred and for querying with PML-TQ.

OPTIONS:
 -h|--help    - print this help and exit
 -u|--usage   - print a short usage and exit
 -v|--version - print version and exit
 
 -t|--trees-per-file <N> 

	number of trees per resulting PML file (default is $sentences_per_file)

 -b|--btred <path> 

	path to btred or start_btred executable (default is '$btred')
 
 -z|--gzip

	do not gzip resulting PML files

AUTHORS:
    Copyright (c) 2009 by
      Jan Stepanek <stepanek@ufal.mff.cuni.cz>
      Petr Pajas <pajas@ufal.mff.cuni.cz>
HELP
}

if [ $PRINT_VERSION = 1 ]; then echo Version: $VERSION; exit; fi
if [ $PRINT_HELP = 1 ]; then help; exit; fi
if [ $PRINT_USAGE = 1 ]; then usage; exit; fi

apreds='<member name="apreds" xmlns="http://ufal.mff.cuni.cz/pdt/pml/schema/">
  <list ordered="0">
    <structure>
      <member name="target.rf">
        <cdata format="PMLREF"/>
      </member>
     <member name="label">
       <cdata format="any"/>
     </member>
    </structure>
  </list>
</member>
<member role="#ID" name="xml:id" as_attribute="1" xmlns="http://ufal.mff.cuni.cz/pdt/pml/schema/">
  <cdata format="ID"/>
</member>'
apreds=$(echo "$apreds" | tr '\n' ' ')

columns="ID,FORM,LEMMA,PLEMMA,POS,PPOS,FEAT,PFEAT,HEAD,PHEAD,DEPREL,PDEPREL,FILLPRED,PRED"
bin=$(readlink_nf "${0%/*}")

while (( $# )) ; do
    if [[ -f "$1" ]] ; then
        max=$(perl -e 'while (<>){@num = /(\t)/g ; $max = scalar(@num) if @num > $max } print "$max\n"' "$1")
        ((max-=13))
        # if there are no ARGS in the conll data, set max to 1
        if ((!max)) ; then max=1 ; fi
        arglist=''
        for n in $(seq 1 $max) ; do arglist=$arglist,APRED_$n ;done
	out_prefix="${1%.txt}"
	if [ -n "$out_dir" ]; then
	    out_prefix="$out_dir"/$(basename "$out_prefix")
	fi
        "$bin"/conll2pml --feat-columns FEAT,PFEAT -R conll2009 -r -o "$out_prefix" -m $sentences_per_file -c $columns$arglist "$1"
        sed -i~ 's%\(<s:member name="apred_1">\)%'"$apreds"'\1%' "${out_prefix}_schema.xml"
        "$btred" -q -S -I "$bin"/args.btred "$out_prefix"_*.pml
	rm "${out_prefix}_schema.xml"
	if [ $gzip = 1 ]; then
	    gzip -9 "$out_prefix"_*.pml
	fi
    else
        echo Skipping "$1" >&2
    fi
    shift
done
