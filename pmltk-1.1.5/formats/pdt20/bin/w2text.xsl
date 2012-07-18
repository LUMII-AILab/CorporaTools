<?xml version="1.0"?>

<xsl:stylesheet  xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
  xmlns:s="http://ufal.mff.cuni.cz/pdt/pml/"
  version='1.0'>
<xsl:output method="text"/>

<xsl:param name="id" select="'1'"/>
<xsl:param name="spaces" select="'1'"/>
<xsl:param name="para" select="'1'"/>

<xsl:template match="/">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match='s:*'>
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match='s:para'>
  <xsl:if test="$id=1">
    <xsl:text>{</xsl:text>
    <xsl:value-of select="s:w[1]/@id"/>
    <xsl:text>} </xsl:text>    
  </xsl:if>
  <xsl:apply-templates/>
  <xsl:if test="$para=1">
    <xsl:text>&#x0A;&#x0A;</xsl:text>    
  </xsl:if>
</xsl:template>

<xsl:template match='s:w'>
  <xsl:apply-templates/>
  <xsl:if test="$spaces!=1 or not(s:no_space_after=1)"><xsl:text>&#x20;</xsl:text></xsl:if>
</xsl:template>

<xsl:template match='s:token'>
  <xsl:value-of select="text()"/>
</xsl:template>

<xsl:template match='text()'>
</xsl:template>

</xsl:stylesheet>
