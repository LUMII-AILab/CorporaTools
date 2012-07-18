<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet  xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
  xmlns="http://ufal.mff.cuni.cz/pdt/pml/"
  version='1.0'>
<xsl:output method="xml" encoding="utf-8"/>

<xsl:template match='/'>
  <xdata>
    <head>
      <schema href="xdata_schema.xml"/>
    </head>
    <root>
      <xsl:apply-templates select="*"/>
    </root>
  </xdata>
</xsl:template>

<xsl:template match='*'>
  <LM>
    <xsl:attribute name="type">element</xsl:attribute>
    <xsl:attribute name="name"><xsl:value-of select="name()"/></xsl:attribute>
    <!--
    <xsl:if test="namespace-uri()!=''">
      <xsl:attribute name="ns"><xsl:value-of select="namespace-uri()"/></xsl:attribute>
    </xsl:if>
    -->
    <xsl:if test="@*">
      <attributes>
        <xsl:apply-templates select="@*"/>
      </attributes>      
    </xsl:if>
    <xsl:if test="node()">
      <children>
        <xsl:apply-templates select="node()"/>
      </children>      
    </xsl:if>
  </LM>
</xsl:template>

<xsl:template match='@*'>
  <LM>
    <xsl:attribute name="name"><xsl:value-of select="name()"/></xsl:attribute>
    <!--
    <xsl:if test="namespace-uri()!=''">
      <xsl:attribute name="ns"><xsl:value-of select="namespace-uri()"/></xsl:attribute>
    </xsl:if>
    -->
    <xsl:element name="content"><xsl:value-of select="string(.)"/></xsl:element>
  </LM>
</xsl:template>

<xsl:template match='text()'>
  <!-- TODO: remove the following condition -->
  <xsl:if test="not(contains(' ',normalize-space(.)))">

  <LM>
    <xsl:attribute name="type">text</xsl:attribute>
    <xsl:if test="contains(' ',normalize-space(.))">
      <xsl:element name="hide">1</xsl:element>
    </xsl:if>
    <content><xsl:value-of select="string(.)"/></content>
  </LM>

  </xsl:if>

</xsl:template>

<xsl:template match='processing-instruction()'>
  <LM>
    <xsl:attribute name="type">processing-instruction</xsl:attribute>
    <content><xsl:value-of select="string(.)"/></content>
  </LM>
</xsl:template>

<xsl:template match='comment()'>
  <LM>
    <xsl:attribute name="type">comment</xsl:attribute>
    <content><xsl:value-of select="string(.)"/></content>
  </LM>
</xsl:template>

</xsl:stylesheet>
