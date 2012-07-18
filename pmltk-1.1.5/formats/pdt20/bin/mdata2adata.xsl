<?xml version="1.0" encoding="utf-8"?>
<!--

=head1 mdata2adata.xsl

Create dummy adata PML instance from an mdata PML instance.

=head1 SYNOPSIS

  xsltproc -o file.a [ -stringparam param value ... ] mdata2adata.xsl file.m

or

  saxon -o file.a file.m mdata2adata.xsl [param="'value'" ...]

=head1 DESCRIPTION

Based on an mdata PML instance, this stylesheets creates a simple
adata instance with one tree per mdata sentence and one node per mdata
word in the sentence; all trees are flat.

Stylesheet parameters:

=over 4

=item schema 

PML schema file for adata (defaults to adata_schema.xml)

=item mdata 

filename of the mdata instance

=item wdata 

filename of wdata instance (if different from the one specified in mdata instance)

=item desc 

text for meta/annotation_info/desc element (defaults to "Automatically generated")

=back

=head1 AUTHOR

Petr Pajas <pajas@matfyz.cz>

Copyright 2006 Petr Pajas, All rights reserved.

=cut

-->

<xsl:stylesheet
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
  xmlns='http://ufal.mff.cuni.cz/pdt/pml/'
  xmlns:m='http://ufal.mff.cuni.cz/pdt/pml/'
  version='1.0'>
  <xsl:output method="xml" encoding="utf-8" indent="yes"/>
  
  <xsl:param name="schema" select="'adata_schema.xml'"/>
  <xsl:param name="mdata" select="''"/>
  <xsl:param name="wdata" select="''"/>
  <xsl:param name="desc" select="'Automatically generated'"/>

  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="m:mdata">
    <adata>
      <head>
        <schema href="{$schema}"/>
        <references>
          <reffile id="m" name="mdata" href="{$mdata}" />
          <xsl:choose>
            <xsl:when test="$wdata!=''">
              <reffile id="{m:head/m:references/m:reffile[@name='wdata']/@id}" name="wdata" href="{$mdata}" />
            </xsl:when>
            <xsl:otherwise>
              <xsl:apply-templates select="m:head/m:references/m:reffile[@name='wdata']"/>
            </xsl:otherwise>
          </xsl:choose>
        </references>
      </head>
      <meta>
        <annotation_info>
          <desc><xsl:value-of select="$desc"/></desc>
        </annotation_info>
      </meta>
      <trees>
        <xsl:apply-templates select="m:s"/>
      </trees>
    </adata>
  </xsl:template>
  <xsl:template match="m:reffile">
    <reffile id="{@id}" name="{@name}" href="{@href}"/>
  </xsl:template>
  <xsl:template match="m:s">
    <LM id="{concat('a-',substring-after(@id,'m-'))}">
      <s.rf>m#<xsl:value-of select="@id"/></s.rf>
      <ord>0</ord>
      <children>
        <xsl:apply-templates select="m:m"/>
      </children>
    </LM>
  </xsl:template>
  <xsl:template match="m:m">
    <LM id="{concat('a-',substring-after(@id,'m-'))}">
      <m.rf>m#<xsl:value-of select="@id"/></m.rf>
      <afun>ExD</afun>
      <ord><xsl:value-of select="position()"/></ord>
    </LM>
  </xsl:template>

  <xsl:template match="node()">
  </xsl:template>

</xsl:stylesheet>
