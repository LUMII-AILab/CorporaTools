#!/usr/bin/env xsh2
# -*- mode: cperl; coding: utf-8; -*-

quiet;
$INPUT := open $ARGV[1];

# change children/LM to children/N
def LM2N { 
  if (self::pml:children) {
    cd &{wrap :i N .};
  } elsif (self::pml:LM) {
    rename pml:N .
  }
  for (pml:children[not(pml:LM)]|pml:children/pml:LM) {
    LM2N 
  }
}

# complete skip the children sub-level
# and turn */children/LM to */N
def children2N {
  for (pml:children) {
    if (pml:LM) {
      rename pml:N pml:LM;
      for &{xmove pml:N replace .} {
	children2N;
      }
    } else {
      rename pml:N .;
      children2N;
    }
  }
}

# for /*/pml:trees/* LM2N;
for (/*/pml:trees[not(pml:LM)]) wrap :i LM .;

for (/*/pml:trees/pml:LM) {
  rename pml:N .;
  children2N;
}

#for //*[namespace::*] change-ns-uri '';
for //* change-ns-uri '';

ls /;

#ls /;
