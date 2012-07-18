<?xml version="1.0" encoding="utf-8"?>
<!-- -*- mode: xsl; coding: utf8; -*- -->
<!-- Author: pajas@ufal.mff.cuni.cz -->

<xsl:stylesheet
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
  xmlns="http://ufal.mff.cuni.cz/pdt/pml/"
  xmlns:p="http://perl.org/pod"
  xmlns:set="http://exslt.org/sets"
  xmlns:exsl="http://exslt.org/common"
  extension-element-prefixes="exsl set"
  version='1.0'>
<xsl:output method="xml" encoding="utf-8" indent="yes"/>

<p:pod>
=head1 latin2pml.xsl

A simple convertor from the XML format of the Latin Dependency Treebank
(http://nlp.perseus.tufts.edu/syntax/treeb) to PML.

=head1 SYNOPSIS

  xsltproc -o output.pml latin2pml.xsl treebank-1.3.xml
or
  xsltproc --stringparam chunk doc latin2pml.xsl treebank-1.3.xml
or
  xsltproc --stringparam chunk subdoc latin2pml.xsl treebank-1.3.xml

=head1 DESCRIPTION

This stylesheet converts the Latin Dependency Treebank XML data to PML
with these changes:

- the output can be optionally chunked by document_id or document_id+subdoc

- unique (within treebank) IDs are added to each node (including root)

- relation is renamed to afun and postag is renamed to tag for PDT compatibility

The output file uses the latin_pmlschema.xml schema which is based on
a generic tree_schema.xml.

=head1 AUTHOR

Petr Pajas &lt;pajas@matfyz.cz&gt;

Copyright 2007 Petr Pajas, All rights reserved.

=cut
</p:pod>

<xsl:template match="/">
  <xsl:apply-templates/>
</xsl:template>

<xsl:param name="chunk" select="'none'"/>

<xsl:template match="treebank">
  <xsl:choose>
    <xsl:when test="$chunk='none'">
      <xsl:call-template name="latin_treebank"/>
    </xsl:when>
    <xsl:when test="starts-with($chunk,'subdoc')">
      <xsl:call-template name="chunk_subdoc"/>
    </xsl:when>
    <xsl:when test="starts-with($chunk,'doc')">
      <xsl:call-template name="chunk_document"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:message terminate="1">
        Parameter $chunk must be one of: none, document, subdoc
      </xsl:message>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="chunk_document">
  <xsl:for-each select="set:distinct(/treebank/sentence/@document_id)">
    <xsl:call-template name="chunk">
      <xsl:with-param name="document_id" select="string(.)"/>
      <xsl:with-param name="href" select="concat(translate(.,':=','-_'),'_pml.xml')"/>
    </xsl:call-template>
  </xsl:for-each>
</xsl:template>

<xsl:template name="chunk_subdoc">
  <xsl:for-each select="set:distinct(/treebank/sentence/@document_id)">
    <xsl:call-template name="do_chunk_subdoc">
      <xsl:with-param name="document_id" select="string(.)"/>
      <xsl:with-param name="href" select="concat(translate(.,':=','-_'),'_pml.xml')"/>
    </xsl:call-template>
  </xsl:for-each>
</xsl:template>

<xsl:template name="do_chunk_subdoc">
  <xsl:param name="document_id"/>
  <xsl:param name="subdoc"/>
  <xsl:param name="href"/>  
  <xsl:for-each select="set:distinct(/treebank/sentence[@document_id=$document_id]/@subdoc)">
    <xsl:call-template name="chunk">
      <xsl:with-param name="document_id" select="$document_id"/>
      <xsl:with-param name="subdoc" select="string(.)"/>
      <xsl:with-param name="href" select="concat(translate($document_id,':=','-_'),'_',translate(.,':=','-_'),'_pml.xml')"/>
    </xsl:call-template>
  </xsl:for-each>
</xsl:template>

<xsl:template name="chunk">
  <xsl:param name="document_id"/>
  <xsl:param name="subdoc"/>
  <xsl:param name="href"/>
  <xsl:message>Writing chunk <xsl:value-of select="$href"/></xsl:message>
  <exsl:document href="{$href}" indent="yes" method="xml">
    <xsl:call-template name="latin_treebank">
      <xsl:with-param name="document_id" select="$document_id"/>
      <xsl:with-param name="subdoc" select="$subdoc"/>
    </xsl:call-template>
  </exsl:document>
</xsl:template>

<xsl:template name="latin_treebank">
  <xsl:param name="document_id" select="''"/>
  <xsl:param name="subdoc" select="''"/>
  <latin_treebank>
    <head>
      <schema href="latin_pmlschema.xml"/>
    </head>
    <meta>
      <annotation_info>
        <version_info><xsl:value-of select="@version"/></version_info>
      </annotation_info>
      <xsl:if test="$document_id!=''">
        <document_id><xsl:value-of select="$document_id"/></document_id>            
      </xsl:if>
      <xsl:if test="$subdoc!=''">
        <subdoc><xsl:value-of select="$subdoc"/></subdoc>            
      </xsl:if>
    </meta>
    <trees>
      <xsl:choose>
        <xsl:when test="$subdoc!=''">
          <xsl:apply-templates select="/*/sentence[@document_id=$document_id and @subdoc=$subdoc]"/>
        </xsl:when>
        <xsl:when test="$document_id!=''">
          <xsl:apply-templates select="/*/sentence[@document_id=$document_id]"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates/>
        </xsl:otherwise>
      </xsl:choose>
    </trees>
  </latin_treebank>
</xsl:template>

<xsl:template match="@subdoc">
  <xsl:if test="not(starts-with($chunk,'subdoc'))">
    <xsl:copy/>
  </xsl:if>
</xsl:template>

<xsl:template match="@document_id">
  <xsl:if test="$chunk='none'">
    <xsl:copy/>
  </xsl:if>
</xsl:template>

<xsl:template match="sentence">
  <root>
    <xsl:apply-templates select="@*"/>
    <children>
      <xsl:apply-templates select="word[@head='0']"/>
    </children>
  </root>
</xsl:template>

<xsl:template match="word">
  <node>
    <xsl:apply-templates select="@*"/>
    <children>
      <xsl:apply-templates select="../word[@head=current()/@id]"/>
    </children>
  </node>
</xsl:template>

<xsl:template match="@relation">
  <afun><xsl:value-of select="."/></afun>
</xsl:template>
<xsl:template match="@postag">
  <tag><xsl:value-of select="."/></tag>
</xsl:template>

<xsl:template match="@form|@lemma">
  <xsl:element name="{local-name()}">
    <xsl:value-of select="."/>
  </xsl:element>
</xsl:template>


<xsl:template match="sentence/@id">
  <xsl:attribute name="id">
    <xsl:for-each select="../@document_id">
      <xsl:call-template name="fix_id"/>
    </xsl:for-each>
    <xsl:text>-s</xsl:text>
    <xsl:value-of select="."/>
  </xsl:attribute>
</xsl:template>

<xsl:template match="word/@id">
  <xsl:attribute name="id">
    <xsl:for-each select="../../@document_id">
      <xsl:call-template name="fix_id"/>
    </xsl:for-each>
    <xsl:text>-s</xsl:text>
    <xsl:value-of select="../../@id"/>
    <xsl:text>w</xsl:text>
    <xsl:value-of select="."/>
  </xsl:attribute>
  <xsl:element name="order">
    <xsl:value-of select="."/>
  </xsl:element>
</xsl:template>

<xsl:template match="@head"/>

<xsl:template name="fix_id">
  <xsl:value-of select="translate(.,':=','-_')"/>
</xsl:template>

<xsl:template match="@*">
  <xsl:copy/>
</xsl:template>

</xsl:stylesheet>
