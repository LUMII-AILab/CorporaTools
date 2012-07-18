<?xml version="1.0" encoding="utf-8"?>
<!-- -*- mode: xsl; coding: utf-8; -*- -->
<!-- Author: pajas@ufal.mff.cuni.cz -->

<xsl:stylesheet
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
  xmlns="http://ufal.mff.cuni.cz/pdt/pml/"
  xmlns:s="http://ufal.mff.cuni.cz/pdt/pml/schema/"
  version='1.0'>
<xsl:output method="xml" encoding="utf-8" indent="yes"/>
<!-- <xsl:namespace-alias stylesheet-prefix='s' result-prefix='#default'/> -->
<xsl:template match="/">
  <xsl:apply-templates/>
</xsl:template>

<xsl:template match="s:pml_schema">
  <pml_schema_tree>
    <xsl:apply-templates select="@*"/>
    <head>
      <schema>
	<s:pml_schema version="1.2">
	  <s:description>PML Schema for PML Schema converted to a PML tree</s:description>
	  <s:root name="pml_schema_tree">
	    <s:container>
	      <s:attribute name="version"><s:cdata format="any"/></s:attribute>
	    <s:sequence role="#TREES">
	      <s:element name="revision"><s:cdata format="any"/></s:element>
	      <s:element name="description"><s:cdata format="any"/></s:element>
	      <s:element name="reference">
		<s:structure>
		  <s:member name="name" as_attribute="1"><s:cdata format="any"/></s:member>
		  <s:member name="readas" as_attribute="1">
		    <s:choice>
		      <s:value>trees</s:value>
		      <s:value>dom</s:value>
		      <s:value>pml</s:value>
		    </s:choice>
		  </s:member>
		</s:structure>
	      </s:element>
	      <!--
	      <s:element name="import">
		<s:structure>
		  <s:member name="schema"><s:cdata format="anyURI"/></s:member>
		  <s:member name="type"><s:cdata format="NMTOKEN"/></s:member>
		  <s:member name="minimal_revision"><s:cdata format="any"/></s:member>
		  <s:member name="maximal_revision"><s:cdata format="any"/></s:member>
		  <s:member name="revision"><s:cdata format="any"/></s:member>
		</s:structure>
	      </s:element>
	      -->
	      <s:element name="declarations">
		<s:container role="#NODE">
		  <s:sequence role="#CHILDNODES">
		    <s:element name="root" type="pmlschema-root.type"/>
		    <s:element name="type" type="pmlschema-type.type"/>
		  </s:sequence>
		</s:container>
	      </s:element>
	    </s:sequence>
	    </s:container>
	  </s:root>
	  <s:type name="pmlschema-type.type">
	    <s:container role="#NODE" type="pmlschema-decl.type">
	      <s:attribute name="name" role="#ID"><s:cdata format="any"/></s:attribute>
	    </s:container>
	  </s:type>
	  <s:type name="pmlschema-root.type">
	    <s:container role="#NODE" type="pmlschema-decl.type">
	      <s:attribute name="name" role="#ID"><s:cdata format="any"/></s:attribute>
	      <s:attribute name="type"><s:cdata format="PMLREF"/></s:attribute>
	    </s:container>
	  </s:type>
	  <s:type name="pmlschema-decl.type">
	    <s:sequence role="#CHILDNODES">
	      <s:element name="cdata" type="pmlschema-cdata.type"/>
	      <s:element name="choice" type="pmlschema-choice.type"/>
	      <s:element name="constant">
		<s:container role="#NODE">
		  <s:cdata format="any"/>
		</s:container>
	      </s:element>
	      <s:element name="list" type="pmlschema-list.type"/>
	      <s:element name="alt" type="pmlschema-alt.type"/>
	      <s:element name="structure" type="pmlschema-struct.type"/>
	      <s:element name="container" type="pmlschema-container.type"/>
	      <s:element name="sequence" type="pmlschema-sequence.type"/>
	    </s:sequence>
	  </s:type>
	  <s:type name="pmlschema-choice.type">
	    <s:container role="#NODE">
	      <s:sequence role="#CHILDNODES">
		<s:element name="value">
		  <s:container role="#NODE">
		    <s:cdata format="any"/>
		  </s:container>
		</s:element>
	      </s:sequence>
	    </s:container>
	  </s:type>
	  <s:type name="pmlschema-constant.type">
	    <s:container role="#NODE">
	      <s:cdata format="any"/>
	    </s:container>
	  </s:type>
	  <s:type name="pmlschema-cdata.type">
	    <s:structure role="#NODE" >
	      <s:member name="format" as_attribute="1"><s:cdata format="any"/></s:member>
	    </s:structure>
	  </s:type>
	  <s:type name="pmlschema-alt.type">
	    <s:container type="pmlschema-decl.type" role="#NODE" >
	      <s:attribute name="role"><s:cdata format="any"/></s:attribute>
	      <s:attribute name="type"><s:cdata format="PMLREF"/></s:attribute>
	    </s:container>
	  </s:type>
	  <s:type name="pmlschema-list.type">
	    <s:container type="pmlschema-decl.type" role="#NODE" >
	      <s:attribute name="role"><s:cdata format="any"/></s:attribute>
	      <s:attribute name="type"><s:cdata format="PMLREF"/></s:attribute>
	      <s:attribute name="ordered"><s:cdata format="boolean"/></s:attribute>
	    </s:container>
	  </s:type>
	  <s:type name="pmlschema-sequence.type">
	    <s:container role="#NODE" >
	      <s:attribute name="role"><s:cdata format="any"/></s:attribute>
	      <s:attribute name="type"><s:cdata format="PMLREF"/></s:attribute>
	      <s:attribute name="content_pattern"><s:cdata format="any"/></s:attribute>
	      <s:sequence role="#CHILDNODES" >
		<s:element name="element">
		  <s:container type="pmlschema-decl.type" role="#NODE" >
		    <s:attribute name="name"><s:cdata format="NMTOKEN"/></s:attribute>
		    <s:attribute name="role"><s:cdata format="any"/></s:attribute>
		    <s:attribute name="type"><s:cdata format="PMLREF"/></s:attribute>
		    <s:attribute name="as_attribute"><s:cdata format="boolean"/></s:attribute>
		    <s:attribute name="required"><s:cdata format="boolean"/></s:attribute>
		  </s:container>
		</s:element>
	      </s:sequence>
	    </s:container>
	  </s:type>
	  <s:type name="pmlschema-struct.type">
	    <s:container role="#NODE" >
	      <s:attribute name="role"><s:cdata format="any"/></s:attribute>
	      <s:attribute name="type"><s:cdata format="PMLREF"/></s:attribute>
	      <s:attribute name="name"><s:cdata format="NMTOKEN"/></s:attribute>
	      <s:sequence role="#CHILDNODES" >
		<s:element name="member">
		  <s:container type="pmlschema-decl.type" role="#NODE" >
		    <s:attribute name="name"><s:cdata format="NMTOKEN"/></s:attribute>
		    <s:attribute name="role"><s:cdata format="any"/></s:attribute>
		    <s:attribute name="type"><s:cdata format="PMLREF"/></s:attribute>
		    <s:attribute name="as_attribute"><s:cdata format="boolean"/></s:attribute>
		    <s:attribute name="required"><s:cdata format="boolean"/></s:attribute>
		  </s:container>
		</s:element>
	      </s:sequence>
	    </s:container>
	  </s:type>
	  <s:type name="pmlschema-container.type">
	    <s:container role="#NODE" >
	      <s:attribute name="role"><s:cdata format="any"/></s:attribute>
	      <s:attribute name="type"><s:cdata format="PMLREF"/></s:attribute>
	      <s:attribute name="name"><s:cdata format="NMTOKEN"/></s:attribute>
	      <s:sequence role="#CHILDNODES" content_pattern="attribute*, (cdata|choice|constant|list|alt|structure|sequence)?">
		<s:element name="attribute">
		  <s:container role="#NODE" >
		    <s:attribute name="name"><s:cdata format="NMTOKEN"/></s:attribute>
		    <s:attribute name="role"><s:cdata format="any"/></s:attribute>
		    <s:attribute name="type"><s:cdata format="PMLREF"/></s:attribute>
		    <s:attribute name="required"><s:cdata format="boolean"/></s:attribute>
		    <s:sequence role="#CHILDNODES" content_pattern="(cdata|choice|constant)">
		      <s:element name="cdata" type="pmlschema-cdata.type"/>
		      <s:element name="choice" type="pmlschema-choice.type"/>
		      <s:element name="constant">
			<s:container role="#NODE">
			  <s:cdata format="any"/>
			</s:container>
		      </s:element>
		    </s:sequence>
		  </s:container>
		</s:element>
		<s:element name="cdata" type="pmlschema-cdata.type"/>
		<s:element name="choice" type="pmlschema-choice.type"/>
		<s:element name="choice" type="pmlschema-constant.type"/>
		<s:element name="list" type="pmlschema-list.type"/>
		<s:element name="alt" type="pmlschema-alt.type"/>
		<s:element name="structure" type="pmlschema-struct.type"/>
		<s:element name="sequence" type="pmlschema-sequence.type"/>		
	      </s:sequence>
	    </s:container>
	  </s:type>
	</s:pml_schema>
      </schema>
    </head>
    <xsl:apply-templates select="node()[not(self::s:type|self::s:root)]"/>
    <declarations>
      <xsl:apply-templates select="s:type|s:root"/>
    </declarations>
  </pml_schema_tree>
</xsl:template>

<xsl:template match="s:import">
  <xsl:message>WARNING: found 'import' element. The schema should be simplified first!</xsl:message>
</xsl:template>

<xsl:template match="s:derive">
  <xsl:message>WARNING: found 'derive' element. The schema should be simplified first!</xsl:message>
</xsl:template>

<xsl:template match="s:template">
  <xsl:message>WARNING: found 'template' element. The schema should be simplified first!</xsl:message>
</xsl:template>

<xsl:template match="s:copy">
  <xsl:message>WARNING: found 'copy' element. The schema should be simplified first!</xsl:message>
</xsl:template>

<xsl:template match="*">
  <xsl:element name="{local-name()}" namespace="http://ufal.mff.cuni.cz/pdt/pml/">
    <xsl:apply-templates select="@*|node()"/>
  </xsl:element>
</xsl:template>

<xsl:template match="@*">
  <xsl:copy/>
</xsl:template>


</xsl:stylesheet>
