<?xml version="1.0" encoding="utf-8"?>
<!-- -*- mode: xsl; coding: utf8; -*- -->
<!-- Author: pajas@ufal.mff.cuni.cz -->

<xsl:stylesheet
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
  xmlns:p='http://ufal.mff.cuni.cz/pdt/pml/'
  exclude-result-prefixes="p"
  version='1.0'>
<xsl:output method="xml" encoding="utf-8" indent="yes"/>

<xsl:template match="/">
  <trees>
    <xsl:apply-templates/>
  </trees>
</xsl:template>

<xsl:template match="p:trees/p:LM">
  <xsl:apply-templates/>
</xsl:template>
<xsl:template match="p:children">
  <xsl:apply-templates/>
</xsl:template>
<xsl:template match="p:terminal[translate(p:pos,concat(&quot;'&quot;,'.,:;!?$#;`'),'')=p:pos and not(starts-with(p:pos,'-'))]">
  <xsl:element name="{translate(p:pos,'|=','')}">
    <xsl:attribute name="lex">
      <xsl:value-of select="p:form"/>    
    </xsl:attribute>
  </xsl:element>
</xsl:template>
<xsl:template match="p:nonterminal">
  <xsl:element name="{translate(p:cat,'|=','')}">
    <xsl:apply-templates/>
  </xsl:element>
</xsl:template>

<xsl:template match="text()">
</xsl:template>

</xsl:stylesheet>
