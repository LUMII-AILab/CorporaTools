<?xml version="1.0" encoding="utf-8"?>
<!-- -*- mode: xsl; coding: utf8; -*- -->
<!-- Author: pajas@ufal.mff.cuni.cz -->

<xsl:stylesheet
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
  xmlns='http://ufal.mff.cuni.cz/pdt/pml/'
  version='1.0'>
<xsl:output method="xml" encoding="utf-8" indent="yes"/>
<xsl:strip-space elements="alpino_ds node"/>

<xsl:template match="/">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="alpino_ds">
  <alpino_ds_pml>
    <head>
      <schema href="alpino_schema.xml" />
    </head>
    <version><xsl:value-of select="@version"/></version>
    <xsl:apply-templates select="sentence"/>
    <xsl:apply-templates select="comments"/>
    <trees>
      <xsl:apply-templates select="terminal|non-terminal"/>
    </trees>
  </alpino_ds_pml>
</xsl:template>

<xsl:template match="*">
  <xsl:element name="{name()}">
    <xsl:apply-templates select="@*"/>
    <xsl:apply-templates/>
  </xsl:element>
</xsl:template>


<!-- copy these attributes -->
<xsl:template match="@rel|@cat|@pos|@root|@word|@index">
  <xsl:copy/> 
</xsl:template>


<!-- replace @end with wordno -->
<xsl:template match="@end">
  <!-- only add wordno for word nodes (leaf nodes) -->
  <xsl:if test="../@word">
    <xsl:attribute name="wordno">
      <xsl:value-of select="."/>
    </xsl:attribute>
  </xsl:if>
</xsl:template>

<!-- skip any other attributes -->
<xsl:template match="@*">
</xsl:template>



</xsl:stylesheet>
