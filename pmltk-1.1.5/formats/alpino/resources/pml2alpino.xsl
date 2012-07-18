<?xml version="1.0" encoding="utf-8"?>
<!-- -*- mode: xsl; coding: utf8; -*- -->
<!-- Author: pajas@ufal.mff.cuni.cz -->

<xsl:stylesheet
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
  xmlns:pml='http://ufal.mff.cuni.cz/pdt/pml/'
  version='1.0'>
<xsl:output method="xml" encoding="iso-8859-1" indent="yes"/>
<xsl:namespace-alias stylesheet-prefix="pml" result-prefix="#default"/>
<xsl:strip-space elements="*"/>

<xsl:template match="/">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="pml:head">
</xsl:template>

<xsl:template match="pml:trees">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="pml:alpino_ds_pml">
  <alpino_ds version="{pml:version}">
    <xsl:apply-templates select="pml:trees"/>
    <xsl:apply-templates select="pml:sentence"/>
    <xsl:apply-templates select="pml:comments"/>
  </alpino_ds>
</xsl:template>

<xsl:template match="*">
  <xsl:element name="{name()}">
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates/>
  </xsl:element>
</xsl:template>


<xsl:template match="@*">
  <xsl:copy/>
</xsl:template>


<!-- create begin and end attributes based on @wordno 
     higher level begin/end attributes are added later
-->

<xsl:template match="@wordno">
  <xsl:attribute name="begin">
    <xsl:value-of select=". - 1"/>
  </xsl:attribute>
  <xsl:attribute name="end">
    <xsl:value-of select="."/>
  </xsl:attribute>
</xsl:template>


</xsl:stylesheet>
