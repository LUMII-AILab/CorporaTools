#!/usr/bin/env xsh2
# -*- mode: cperl; coding: utf-8; -*-
#
# This is a XSH2 script - http://xsh.sourceforge.net
#
# Splits the Tiger Treebank PML instance
# into several reasonable sized files
#
# Usage: ./split_tiger.xsh tiger_data.pml
#

my $pmlns = 'http://ufal.mff.cuni.cz/pdt/pml/';
register-namespace pml $pmlns;

my $filename = string($ARGV[1]);
my $out_schema := create 'aux';
my $input := open $filename;
cd $input/pml:corpus;

xcp pml:head/pml:schema/* replace $out_schema/aux;

save --file 'tiger_pml_schema.xml' $out_schema;

switch-to-new-documents 0;
my $i = 0;
my $out_base = $filename;
perl { $out_base =~ s{^.*/}{}; $out_base=~s/\.pml$// };
while (pml:body[1]/pml:s[1]) {
  my $out := create 'corpus';
  for $out/* {
     declare-ns '' $pmlns;
     set-ns $pmlns;
  }
  xcp @* into $out/*;
  set $out/pml:corpus/pml:head/pml:schema/@href 'tiger_pml_schema.xml';
  xcp pml:meta into $out/*;
  my $body := insert element 'pml:body' append $out/*;
  xmv pml:body[1]/pml:s[position()<=50] into $body;
  save --file { sprintf("%s_%04d.pml", $out_base,$i++) } $out;
}
