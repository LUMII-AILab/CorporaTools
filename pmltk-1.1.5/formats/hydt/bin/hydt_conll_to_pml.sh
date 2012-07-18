#!/bin/bash

CONLL2PML=${CONLL2PML:-"$HOME/.tred.d/extensions/conll2009/bin/conll2pml"}

if [ ! -f "$CONLL2PML" ]; then
    echo "The conll2009 TrEd extension must be installed or the CONLL2PML environment variable set to point to the conll2pml script; aborting!" >&2;
    exit 1
fi

if [ -z "$3" ]; then
    echo "Usage: $0 hindi|bangla|telugu output-dir hydt-conll-file"
    exit 1;
fi

readlink_nf () {
    perl -MCwd -e 'print Cwd::abs_path(shift)' "$1"
}
script_dir="$(dirname $(readlink_nf $0))"
base_dir="$(readlink_nf $script_dir/..)"

hydt_lang=$1
dir=$2
file=$3;
shift 3;

if [ $hydt_lang = "hindi" ]; then
    lang=hi
elif [ $hydt_lang = "bangla" ]; then
    lang=bn
elif [ $hydt_lang = "telugu" ]; then
    lang=te
else
    echo "Missing or unknown language (supported: hindi, bangla, telugu)." 2>&1 
    echo "Language information is needed to convert the WX encoding to the correct Indian script." 2>&1
    exit 1;
fi

perl -I"${base_dir}/lib" - $lang $file <<'EOF' | \
  "$CONLL2PML" -o "$dir/$hydt_lang" -I "$lang" -m 50 -i -c 'ID,WXFORM,FORM,WXLEMMA,LEMMA,CPOSTAG,POSTAG,FEATS,HEAD,DEPREL' -O order -r -R hydtconll -F -
use open qw(IO :utf8 :std);
use translit;
use translit::wc2utf;
my %p;
my $lang = shift @ARGV;
my $m = translit::wc2utf::inicializovat(\%p, $lang);
while (<>) {
  if (/\S/) {
    chomp;
    my @L=split(/\s+/,$_);
    $L[5] = join '|', map { my ($f,$v)=split('-',$_,2); $v eq '$' ? () : "$f=$v" } split(m{\|},$L[5]);
    print join("\t",map { ($L[$_],($_==1 or $_==2) ? ($L[$_] eq 'NULL' ? 'NULL' : translit::prevest(\%p,$L[$_],$m)) : ()) } 0..$#L)."\n";
  } else {
    print "\n";
  }
}
EOF
