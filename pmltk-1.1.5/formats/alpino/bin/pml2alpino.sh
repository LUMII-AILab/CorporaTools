#!/bin/bash
# pml2alpino.sh     pajas@ufal.mff.cuni.cz     2010/03/15 13:06:48

readlink_nf () {
    perl -MCwd -e 'print Cwd::abs_path(shift)' "$1"
}
script_dir=$(readlink_nf "$(dirname "$0")")
xsl="$script_dir"/../resources/pml2alpino.xsl

if [ ! -f "$xsl" ]; then
    echo "Didn't find $xsl\n" 1>&2
    exit 1;
fi

OUTPUT_DIR=.
VERSION=0.1
PRINT_USAGE=0
PRINT_HELP=0
PRINT_VERSION=0
DEBUG=0
QUIET=0

args=()
while [ $# -gt 0 ]; do
    case "$1" in
	-D|--debug) DEBUG=1; shift ;;
	-q|--quiet) QUIET=1; shift ;;
	-u|--usage) PRINT_USAGE=1; shift ;;
	-h|--help) PRINT_HELP=1; shift ;;
	-v|--version) PRINT_VERSION=1; shift ;;
	-o|--output-dir) OUTPUT_DIR=$2; shift 2 ; break ;;
	--) shift ; break ;;
        -*) echo "Invalid command-line option: $1!" >&2 ; exit 1 ;;
	*) args+=("$1"); shift ;;
    esac
done

eval set -- "$@" "${args[@]}"

function usage () {
    echo "$(basename $0) version $VERSION" 
    cat <<USAGE
$(basename $0) [--output-dir path] [options] alpino_file.pml [...]
or
$(basename $0) [-h|--help]|[-u|--usage]|[-v|--version]
USAGE
}

function help () {
    echo "$(basename $0) version $VERSION" 
    usage
    cat <<HELP

  DESCRIPTION:

  OPTIONS:
      -o|--output-dir path - directory for output files
                             (defaults to .)

      -h|--help    - print this help and exit
      -u|--usage   - print a short usage and exit
      -v|--version - print version and exit

      -D|--debug - turn on debugging output
      -q|--quiet - turn off informative messages

  AUTHOR:
      Copyright by pajas@ufal.mff.cuni.cz
HELP
}

if [ $PRINT_VERSION = 1 ]; then echo Version: $VERSION; exit; fi
if [ $PRINT_HELP = 1 ]; then help; exit; fi
if [ $PRINT_USAGE = 1 ]; then usage; exit; fi

if [ -z "$1" ]; then usage; exit; fi

if [ -d "$OUTPUT_DIR" ]; then
    mkdir -p "$OUTPUT_DIR" || exit 1
fi

for input in "$@"; do
    output=$OUTPUT_DIR/$(basename "${input%.pml}")".xml";

    [ $QUIET = 0 ] && echo "$input => $output" 1>&2

    xsltproc "$xsl" "$input" > "$output"
done
