#!/bin/sh

# Usage:
# mdata2adata.sh input output [ [ --stringparam schema adata_schema.xml ] [ --stringparam desc description ] ]

stylesheet=mdata2adata.xsl
stylesheet_dir=`dirname $0`

mdata="$1"; shift
adata="$1"; shift

if [ -z "$adata" ]; then
  adata="-"
fi

xsltproc -o "$adata" --stringparam mdata "$mdata" "$@" "$stylesheet_dir/mdata2adata.xsl" "$mdata" 
