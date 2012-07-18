<?xml version="1.0"?>
<!--
============================================================
Abstract: process modularity instructions in a PML schema

Description:
  A XSLT 2.0 implementatin of the process of
  "PML schema simplification" as described in   
  The Prague Markup Language (Version 1.2),
  section Processing modular PML schemas

Author:
  Petr Pajas (pajas at ufal.mff.cuni.cz)
  Copyright (c) 2008 by Petr Pajas

LICENSE:
  This software is distributed under GPL - The General Public Licence
  Full text of the GPL can be found at http://www.gnu.org/copyleft/gpl.html

============================================================
-->

<xsl:stylesheet
    version="2.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"

    xmlns:p="http://ufal.mff.cuni.cz/pdt/pml/schema/"
    xmlns="http://ufal.mff.cuni.cz/pdt/pml/schema/"
    exclude-result-prefixes="p"
    xpath-default-namespace="http://ufal.mff.cuni.cz/pdt/pml/schema/">

  <!-- 
       Parameter: $search_paths
       Value: a comma separated list of paths to search for related PML schemas
  -->
  <xsl:param name="search_paths"/>
  <xsl:param name="search_path_list" select="tokenize($search_paths,',')"/>
  <!-- 
       Parameter: $no_derive
       Values: 0 or 1
         1 - don't process <derive> instructions
         0 - default (process <derive> instructions)
       Default: 1
  -->
  <xsl:param name="no_derive" select="0"/>
  <!-- 
       Parameter: $format
       Values: 0 or 1
         1 - reformat the output (adding ignorable whitespace)
         0 - no formatting
       Default: 1
  -->
  <xsl:param name="format" select="1"/>
  <!-- 
       Parameter: $comments
       Values: 0 or 1
         1 - insert comments describing the processing
         0 - no formatting
       Default: 1
  -->
  <xsl:param name="comments" select="1"/>
  <!-- 
       Parameter: $preserve_templates
       Values: 0 or 1
         1 - keep all <template> elements in the schema
             (while processing all <copy> instructions)
         0 - remove all <template> elements
       Default: 0
  -->
  <xsl:param name="preserve_templates" select="0"/>
  <xsl:param name="base_document" select="/"/>
  <xsl:output method="xml" indent="no"/>

  <xsl:strip-space elements="*"/>
  <xsl:preserve-space elements="description constant value delete "/>


  <!-- 
       The processing is implemented as an iterative application of
       the template matching pml_schema|template below.

       When the main processing of the document is finished, 
       post-processing can optionally remove all <template> elements
       and format the resulting PML schema by means of indentation
       and inserting new-lines (we do this in the stylesheet
       because <xsl:output indent="yes"/> does not produce
       reasonable output with saxon, most notably, there
       is no new-line after comments).
  -->
  <xsl:template match="/">
    <xsl:param name="toplevel" select="1" tunnel="yes"/>
    <xsl:choose>
      <xsl:when test="$toplevel">
	<!-- post-processing - removing templates, formatting -->
	<xsl:call-template name="postprocess">
	  <xsl:with-param name="result">
	    <!-- normal processing - first iteration, top-level schema -->
	    <xsl:apply-templates>
	      <xsl:with-param name="toplevel" select="0" tunnel="yes"/>
	    </xsl:apply-templates>
	  </xsl:with-param>
	</xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
	<!-- normal processing - iterations -->
	<xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <!-- processing the body (<pml_schema> or <template>) -->
  <xsl:template match="pml_schema|template">
    <!-- 
	 Iterative processing: at each pass, the first
	 <copy>,<import>,or <derive> child is processed and removed;
	 this iterates until there are none left on this level.

	 The $process_next variable (tunneled) controls
	 which child is to be processed, the rest is copied.

	 (Note that nested templates get processed during the first iteration
	 - but their processing iterates on its own -
	 which is ok, since instructions like <import> are allowed
	 to create a type that is homonymous with some type defined outside
	 the <template> enclosing the instruction)
         
    -->
    <xsl:variable name="process_next" select="(copy|import|derive|template[.//copy|.//import|.//derive])[1]"/>

    <xsl:choose>
      <xsl:when test="$process_next">
	<xsl:variable name="result" xml:base="{base-uri(/)}">
	  <xsl:copy>
	    <!-- process -->
	    <xsl:apply-templates select="@*"/>
	    <xsl:apply-templates>
	      <xsl:with-param name="process" tunnel="yes" select="$process_next"/>
	    </xsl:apply-templates>
	  </xsl:copy>
	</xsl:variable>
	<!-- iterate this stylesheet -->
	<xsl:apply-templates select="$result/*"/>
      </xsl:when>
      <xsl:otherwise>
	<!-- no more instructions to process: break from the recursion -->
	<xsl:copy>
	  <xsl:apply-templates select="@*"/>
	  <xsl:apply-templates/>
	</xsl:copy>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>  

  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!--
      ============================================================
      Processing of an <import> instruction
      ============================================================
  -->

  <xsl:template match="import">
    <xsl:param name="process" tunnel="yes"/>
    <xsl:choose>
      <xsl:when test=". is $process">
	<xsl:apply-templates select="." mode="process"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:copy-of select="."/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="lookup_schema">
    <xsl:param name="basename"/>
    <xsl:param name="i"/>
    <xsl:if test="count($search_path_list) &lt; $i">
      <xsl:message terminate="yes">
	<xsl:text>Could not retrieve imported schema </xsl:text>
	<xsl:value-of select="@schema"/>
	<xsl:text>&#xa;Search paths:</xsl:text>
	<xsl:value-of select="$search_paths"/>
      </xsl:message>
    </xsl:if>
    <xsl:variable name="schema-uri" select="resolve-uri($basename,concat($search_path_list[$i],'/'))"/>
    <xsl:choose>
      <xsl:when test="doc-available($schema-uri)">
	<xsl:apply-templates select="doc($schema-uri)"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:call-template name="lookup_schema">
	  <xsl:with-param name="basename" select="$basename"/>
	  <xsl:with-param name="i" select="$i+1"/>
	</xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>
  
  <xsl:template match="import" mode="process">
    <xsl:param name="process" tunnel="yes"/>
    <xsl:variable name="self" select="."/>

    <xsl:variable name="schema">
      <xsl:variable name="schema-uri" select="resolve-uri(@schema,base-uri($base_document))"/>
      <!-- <xsl:message>uri: <xsl:value-of select="$schema-uri"/></xsl:message> -->
      <xsl:choose>
      <xsl:when test="doc-available($schema-uri)">
	<xsl:apply-templates select="doc($schema-uri)"/>
      </xsl:when>
      <xsl:otherwise>
	<xsl:call-template name="lookup_schema">
	  <xsl:with-param name="basename" select="if (contains(@schema,'/')) then substring-after(@schema,'/') else @schema"/>
	  <xsl:with-param name="i" select="1"/>
	</xsl:call-template>
      </xsl:otherwise>
      </xsl:choose>
      <!-- apply-templates returns simplified version of the schema -->      
    </xsl:variable>

    <!-- verify revision constraints -->
    <xsl:if test="@revision and (@minimal_revision or @maximal_revision)">
      <xsl:message>
	WARNING: both revision and minimal_revision or maximal_revision
	are present on
	<copy/>
	This is not allowed in PML schema. Recovering: using revision.
      </xsl:message>
    </xsl:if>
    <xsl:if test="@revision or @minimal_revision or @maximal_revision">
      <xsl:variable name="min" select="(@revision|@minimal_revision)[1]"/>
      <xsl:variable name="max" select="(@revision|@maximal_revision)[1]"/>
      <xsl:variable name="revision" select="($schema//pml_schema)[1]/revision"/>
      <xsl:variable name="message">
	<xsl:text>ERROR: Constraints on PML Schema revision do not match </xsl:text>
	<xsl:value-of select="$revision"/>
	<xsl:text>&#xa;</xsl:text>
	<xsl:copy-of select="."/>
      </xsl:variable>
      <xsl:if test="$min">
	<xsl:call-template name="check_revisions">
	  <xsl:with-param name="lower" select="$min"/>
	  <xsl:with-param name="higher" select="$revision"/>
	  <xsl:with-param name="message" select="$message"/>
	</xsl:call-template>
      </xsl:if>
      <xsl:if test="$max">
	<xsl:call-template name="check_revisions">
	  <xsl:with-param name="lower" select="$revision"/>
	  <xsl:with-param name="higher" select="$max"/>
	  <xsl:with-param name="message" select="$message"/>
	</xsl:call-template>
      </xsl:if>
    </xsl:if>

    <!-- prepare the processing -->
    <xsl:variable name="type" select="@type"/>
    <xsl:variable name="template" select="@template"/>
    <xsl:if test="../self::pml_schema and 
		  (@root='1' or not(@root|$template|$type))
		  and 
		  not(../root)">
      <xsl:copy-of select="$schema/*/root"/>
    </xsl:if>
    <xsl:variable name="existing_types" select="../p:*[self::type|self::derive|self::param]/@name"/>
    <xsl:variable name="existing_templates" select="../template/@name"/>
    <xsl:variable name="types_to_copy"
		  select="if ($type='*' or not($type|$template|@root)) 
			  then $schema/*/type/@name
			  else $schema/*/type[@name=$type]/@name
			  "/>
    <xsl:variable name="templates_to_copy"
		  select="if ($template='*' or not($type|$template|@root)) 
			  then $schema/*/template/@name
			  else $schema/*/template[@name=$template]/@name
			  "/>
    <xsl:call-template name="do_import">
      <xsl:with-param name="existing_types" select="$existing_types"/>
      <xsl:with-param name="existing_templates" select="$existing_templates"/>
      <xsl:with-param name="types_to_copy" select="distinct-values($types_to_copy[not(.=$existing_types)])"/>
      <xsl:with-param name="templates_to_copy" select="distinct-values($templates_to_copy[not(.=$existing_templates)])"/>
      <xsl:with-param name="source" select="$schema/p:*"/>
    </xsl:call-template>
  </xsl:template>  

  <!-- compare revision numbers - assume $lower < $higher, otherwise terminate with $message -->
  <xsl:template name="check_revisions">
    <xsl:param name="lower"/>
    <xsl:param name="higher"/>
    <xsl:param name="message"/>
    <xsl:if test="string-length($lower) or string-length($higher)">
      <xsl:variable name="ll" select="if (string-length($lower)) then $lower else '0'"/>
      <xsl:variable name="hh" select="if (string-length($higher)) then $higher else '0'"/>
      <xsl:variable name="l" select="substring-before(concat($ll,'.0'),'.')"/>
      <xsl:variable name="h" select="substring-before(concat($hh,'.0'),'.')"/>
      <xsl:choose>
	<xsl:when test="$l &gt; $h">
	  <xsl:message terminate="yes">
	    <xsl:copy-of select="$message"/>
	  </xsl:message>
	</xsl:when>
	<xsl:when test="$l = $h">
	  <xsl:call-template name="check_revisions">
	    <xsl:with-param name="lower" select="substring-after($lower,'.')"/>
	    <xsl:with-param name="higher" select="substring-after($higher,'.')"/>
	    <xsl:with-param name="message" select="$message"/>
	  </xsl:call-template>
	</xsl:when>
      </xsl:choose>
    </xsl:if>
  </xsl:template>

  <!-- main part of processing of an <import> -->
  <xsl:template name="do_import">
    <xsl:param name="existing_types"/>
    <xsl:param name="existing_templates"/>
    <xsl:param name="types_to_copy"/>
    <xsl:param name="templates_to_copy"/>
    <xsl:param name="source"/>

    <xsl:if test="count($types_to_copy) or count($templates_to_copy)">
      <xsl:variable name="types" select="$source/type[@name=$types_to_copy]"/>
      <xsl:variable name="templates" select="$source/template[@name=$templates_to_copy]"/>
      <xsl:variable name="ref_types" select="$types//p:*/@type"/>
      <xsl:variable name="ref_templates" select="$templates//copy/@template"/> <!-- FIXME: there should be no <copy>; they are already processed! -->
      <xsl:variable name="now_existing_types" select="distinct-values(($existing_types, $types_to_copy))"/>
      <xsl:variable name="now_existing_templates" select="distinct-values(($existing_templates, $templates_to_copy))"/>
      <xsl:copy-of select="$types|$templates"/>
      <xsl:call-template name="do_import">
	<xsl:with-param name="existing_types" select="$now_existing_types"/>
	<xsl:with-param name="existing_templates" select="$now_existing_templates"/>
	<xsl:with-param name="types_to_copy" select="distinct-values($ref_types[not(.=$now_existing_types)])"/>
	<xsl:with-param name="templates_to_copy" select="distinct-values($ref_templates[not(.=$now_existing_templates)])"/>
	<xsl:with-param name="source" select="$source"/>
      </xsl:call-template>
    </xsl:if>
  </xsl:template>

  <!--
      ============================================================
      Processing a <derive> instruction
      ============================================================
  -->
  <!-- process a <type> -->
  <xsl:template match="type">
    <xsl:param name="process" tunnel="yes"/>
    <xsl:if test="not($process and $process/self::derive and
		  $process/@type=@name and
		  not($process/@name))">
      <xsl:copy-of select="."/>
    </xsl:if>
    <!-- otherwise the type was derived and we drop it here -->
  </xsl:template>

  <!-- process a <derive> -->
  <xsl:template match="derive">
    <xsl:param name="process" tunnel="yes"/>
    <xsl:choose>
      <xsl:when test="$no_derive=0 and . is $process">
	<xsl:variable name="type_ref" select="@type"/>
	<!-- find the referred type at the first ancestor that has some of that name -->
	<xsl:variable name="type" select="ancestor::*[type[@name=$type_ref]][1]/type[@name=$type_ref]"/>
	<xsl:variable name="name" select="if (@name) then @name else @type"/>
	<xsl:if test="not($type)">
	  <xsl:message terminate="yes">
	    <xsl:text>Did not find type </xsl:text><xsl:value-of select="$type_ref"/>
	    <xsl:text> reffered to by &#x0a;</xsl:text><xsl:copy-of select="."/>
	  </xsl:message>
	</xsl:if>
	<xsl:apply-templates select="$type" mode="derive">
	  <xsl:with-param name="derive" select="."/>
	  <xsl:with-param name="name" select="$name"/>
	</xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>
	<xsl:copy-of select="."/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- a mode for creating a derived type -->  
  <xsl:template match="type" mode="derive">
    <xsl:param name="derive"/>
    <xsl:param name="name"/>

    <xsl:if test="$comments=1">
      <xsl:comment>
	<xsl:text>derived from </xsl:text>
	<xsl:value-of select="$derive/@type"/>
      </xsl:comment>
    </xsl:if>
    <type name="{$name}">
      <xsl:copy-of select="@*[name()!='name']"/>
            <xsl:apply-templates select="node()" mode="derive">
	<xsl:with-param name="derive" select="$derive"/>
	<xsl:with-param name="name" select="$derive"/>
      </xsl:apply-templates>
    </type>
  </xsl:template>

  <xsl:template match="container" mode="derive">
    <xsl:param name="derive"/>
    <xsl:param name="name"/>
    <xsl:variable name="el" select="$derive/p:*[local-name(.)=local-name(current())]"/>
    <xsl:if test="not($el)">
      <xsl:message terminate="yes">
	<xsl:text>Cannot derive a</xsl:text>
	<xsl:value-of select="local-name($el)"/>
	<xsl:text> from a </xsl:text>
	<xsl:value-of select="local-name(.)"/>
	<xsl:text>as </xsl:text>
	<xsl:value-of select="$name"/>
	<xsl:text>!</xsl:text>
      </xsl:message>
    </xsl:if>
    <xsl:copy>
      <xsl:for-each select="@*[not(name()='type')]">
	<xsl:variable name="n" select="name()"/>
	<xsl:if test="not($el/@*[name()=$n])">
	  <xsl:copy-of select="."/>
	</xsl:if>
      </xsl:for-each>
      <xsl:apply-templates select="attribute" mode="derive">
	<xsl:with-param name="derive" select="$derive"/>
	<xsl:with-param name="derive_el" select="$el"/>
	<xsl:with-param name="name" select="$name"/>
      </xsl:apply-templates>
      <xsl:choose>
	<!-- we have a new content declation via @type in the derived container -->
	<xsl:when test="$el/@type">
	  <xsl:copy-of select="$el/@*[not(.='')]"/>
	  <xsl:copy-of select="$el/attribute"/>
	</xsl:when>
	<!-- we have a new content declation via type redefinition in the derived container -->
	<xsl:when test="$el/p:*[not(self::attribute|self::delete)]">
	  <xsl:copy-of select="$el/@*[not(.='')]"/>
	  <xsl:copy-of select="$el/attribute"/>
	  <xsl:copy-of select="$el/p:*[not(self::attribute|self::delete)]"/>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:copy-of select="@type"/>
	  <xsl:copy-of select="$el/@*[not(.='')]"/>
	  <xsl:copy-of select="$el/attribute"/>
	  <xsl:copy-of select="p:*[not(self::attribute|self::delete)]"/>
	</xsl:otherwise>
      </xsl:choose>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="structure|sequence|choice" mode="derive">
    <xsl:param name="derive"/>
    <xsl:param name="name"/>
    <xsl:variable name="el" select="$derive/p:*[local-name(.)=local-name(current())]"/>
    <xsl:if test="not($el)">
      <xsl:message terminate="yes">
	<xsl:text>Cannot derive a</xsl:text>
	<xsl:value-of select="local-name($el)"/>
	<xsl:text> from a </xsl:text>
	<xsl:value-of select="local-name(.)"/>
	<xsl:text>as </xsl:text>
	<xsl:value-of select="$name"/>
	<xsl:text>!</xsl:text>
      </xsl:message>
    </xsl:if>
    <xsl:copy>
      <xsl:copy-of select="@*"/>
      <xsl:copy-of select="$el/@* | $el/p:*[not(self::delete)]"/>
      <xsl:apply-templates select="*" mode="derive">
	  <xsl:with-param name="derive" select="$derive"/>
	  <xsl:with-param name="derive_el" select="$el"/>
	  <xsl:with-param name="name" select="$name"/>
	</xsl:apply-templates>
    </xsl:copy>
  </xsl:template>

<!--
      <xsl:if test="not(self::container) or $el/@type or (self::container and not($el/p:*[not(self::delete|self::attribute)]))">
      </xsl:if>
-->
  <xsl:template match="member|element|attribute" mode="derive">
    <xsl:param name="derive"/>
    <xsl:param name="derive_el"/>
    <xsl:variable name="name" select="@name"/>
    <xsl:variable name="element_name" select="name()"/>
    <xsl:if test="not($derive_el/p:*[self::delete=$name or @name=$name])">
      <xsl:copy-of select="."/>
    </xsl:if>
  </xsl:template>
  <xsl:template match="value" mode="derive">
    <xsl:param name="derive"/>
    <xsl:param name="derive_el"/>
    <xsl:if test="not(. = $derive_el/delete)">
      <xsl:copy-of select="."/>
    </xsl:if>
  </xsl:template>
  <xsl:template match="node()" mode="derive">
    <xsl:copy-of select="."/>
  </xsl:template>

  <!--
      ============================================================
      Processing a <copy> instruction
      ============================================================
  -->
  <xsl:template match="copy">
    <xsl:param name="process" tunnel="yes"/>
    <xsl:param name="name" select="@template"/>
    <xsl:choose>
      <xsl:when test=". is $process">
	<xsl:if test="$comments=1">
	  <xsl:comment>
	    <xsl:text>BEGIN copy from template </xsl:text><xsl:value-of select="$name"/>
	    <xsl:text> with prefix "</xsl:text><xsl:value-of select="@prefix"/><xsl:text>"</xsl:text>
	  </xsl:comment>
	</xsl:if>
	<xsl:variable name="template" select="ancestor::*[template[@name=$name]][1]/template[@name=$name]"/>
	<xsl:if test="not($template)">
	  <xsl:message terminate="yes">
	    <xsl:text>Could not find the template reffered to in &#xa;</xsl:text>
	    <xsl:copy-of select ="."/>
	  </xsl:message>
	</xsl:if>
	<xsl:if test="$template >> .">
	  <xsl:message terminate="yes">
	    <xsl:text>Error while processing: </xsl:text>
	    <xsl:copy-of select ="."/>
	    <xsl:text>A template declaration must precede a &lt;copy> instruction that refers to it!</xsl:text>
	  </xsl:message>
	</xsl:if>
    	<xsl:apply-templates mode="copy" select="$template/*[self::type or self::template]">
	  <xsl:with-param name="template" select="$template"/>
	  <xsl:with-param name="prefix" select="@prefix"/>
	  <xsl:with-param name="copy" select="."/>
	</xsl:apply-templates>
	<xsl:if test="$comments=1">
	  <xsl:comment>
	    <xsl:text> END copy from template </xsl:text><xsl:value-of select="$name"/>
	    <xsl:text>with prefix "</xsl:text><xsl:value-of select="@prefix"/><xsl:text>" </xsl:text>
	  </xsl:comment>
	</xsl:if>
      </xsl:when>
      <xsl:otherwise>
	<xsl:copy-of select="."/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- a mode for processing a template copy -->
  <xsl:template match="*|node()|@*" mode="copy">
    <xsl:param name="prefix"/>
    <xsl:param name="template"/>
    <xsl:param name="copy"/>
    <xsl:copy>
      <xsl:apply-templates select="@*[name()!='type']" mode="copy">
	<xsl:with-param name="prefix" select="$prefix"/>
	<xsl:with-param name="copy" select="$copy"/>
	<xsl:with-param name="template" select="$template"/>
      </xsl:apply-templates>
      <xsl:apply-templates select="node()" mode="copy">
	<xsl:with-param name="prefix" select="$prefix"/>
	<xsl:with-param name="copy" select="$copy"/>
	<xsl:with-param name="template" select="$template"/>
      </xsl:apply-templates>
      <xsl:apply-templates select="@type" mode="copy">
	<xsl:with-param name="prefix" select="$prefix"/>
	<xsl:with-param name="copy" select="$copy"/>
	<xsl:with-param name="template" select="$template"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>
  <xsl:template match="import|derive|copy" mode="copy">
    <xsl:param name="copy"/>
    <xsl:message terminate="yes">
      <xsl:text>Internal error while processing the following instruction:</xsl:text>
      <xsl:copy-of select="$copy"/>
      <xsl:text>An instruction </xsl:text>
      <xsl:copy/>
      <xsl:text>encountered while assuming all preceding instructions have been processed away!</xsl:text>
    </xsl:message>
  </xsl:template>
  <xsl:template match="type/@name|template/@name" mode="copy">
    <xsl:param name="prefix"/>
    <xsl:param name="template"/>
    <xsl:param name="copy"/>
    <xsl:choose>
      <xsl:when test="../../.. is $copy/..">
	<xsl:attribute name="name">
	  <xsl:value-of select="concat($prefix,.)"/>
	</xsl:attribute>
      </xsl:when>
      <xsl:otherwise>
	<xsl:copy-of select="."/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="@type" mode="copy">
    <xsl:param name="copy"/>
    <xsl:param name="prefix"/>
    <xsl:param name="template"/>
    <xsl:param name="name" select="string(.)"/>
    <xsl:param name="where_defined" select="ancestor::*[type[@name=$name]|param[@name=$name]|derive[@name=$name]][1]"/>
    <xsl:param name="let" select="$copy/let[@param=$name]"/>
    <xsl:choose>
      <xsl:when test="$where_defined is $template">
	<xsl:choose>
	  <xsl:when test="$template/param[@name=$name]">	  
	    <xsl:choose>
	      <xsl:when test="$let[* or @type]">
		<xsl:copy-of select="$let/@type|$let/node()"/>
	      </xsl:when>
	      <xsl:otherwise>
		<xsl:message terminate="yes">
		  <xsl:text>The parameter </xsl:text>
		  <xsl:value-of select="$name"/>
		  <xsl:text> of the template </xsl:text>
		  <xsl:value-of select="$template/@name"/>
		  <xsl:text> not bound to a type via </xsl:text>
		  <let name="{$name}" type="..."/>
		  <xsl:text> in 
		  </xsl:text>
		  <xsl:copy-of select="$copy"/>
		</xsl:message>
	      </xsl:otherwise>
	    </xsl:choose>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:attribute name="type">
	      <xsl:value-of select="concat($prefix,$name)"/>
	    </xsl:attribute>	
	  </xsl:otherwise>
	</xsl:choose>
      </xsl:when>
      <xsl:otherwise>
	<xsl:copy-of select="."/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="param" mode="copy">
  </xsl:template>

  <!--
      ============================================================
      Post-processing
      Part 1: removing <template> elements
      ============================================================
  -->

  <!-- removing templates -->
  <xsl:template name="postprocess">
    <xsl:param name="result"/>
    <xsl:choose>
      <xsl:when test="$preserve_templates=1">
	<xsl:call-template name="format">
	  <xsl:with-param name="result" select="$result"/>
	</xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
	<xsl:call-template name="format">
	  <xsl:with-param name="result">
	    <xsl:apply-templates select="$result" mode="remove_templates"/>
	  </xsl:with-param>
	</xsl:call-template>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

   <!-- a mode that removes all remaining <template> elements -->
   <xsl:template match="template" mode="remove_templates">
   </xsl:template>
   <xsl:template match="node()|@*" mode="remove_templates">
     <xsl:copy>
       <xsl:apply-templates select="node()|@*" mode="remove_templates"/>
     </xsl:copy>
   </xsl:template>


  <!--
      ============================================================
      Post-processing
      Part 2: indentation
      ============================================================
  -->
  <!-- formatting (indentation)  -->
  <xsl:template name="format">
    <xsl:param name="result"/>
    <xsl:choose>
      <xsl:when test="$format=1">
	<xsl:apply-templates select="$result" mode="format"/>
      </xsl:when>  
      <xsl:otherwise>
	<xsl:copy-of select="$result"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

   <!-- a mode that formats the schema - indentation and new-lines -->
   <xsl:template match="p:*|comment()" mode="format">
      <xsl:param name="depth" select="0"/>
      <xsl:variable name="previous" select="preceding-sibling::node()[1]"/>

      <xsl:if test="self::comment()">
	<xsl:text>&#xA;</xsl:text>
      </xsl:if>
      <xsl:text>&#xA;</xsl:text>
      <xsl:call-template name="indent">
         <xsl:with-param name="depth" select="$depth"/>
      </xsl:call-template>
      <xsl:copy>
	<xsl:if test="self::*">
	  <xsl:copy-of select="@*"/>
	  <xsl:apply-templates mode="format">
	    <xsl:with-param name="depth" select="$depth + 1"/>
	  </xsl:apply-templates>
	  <xsl:if test="count(*) &gt; 0">
	    <xsl:text>&#xA;</xsl:text>
	    <xsl:call-template name="indent">
	      <xsl:with-param name="depth" select="$depth"/>
	    </xsl:call-template>
	  </xsl:if>
	</xsl:if>
      </xsl:copy>
      <xsl:variable name="is_last" select="count(../..) = 0 and position() = last()"/>
      <xsl:if test="self::type|self::template|self::param[last()]">
	<xsl:text>&#xA;</xsl:text>
      </xsl:if>
      <xsl:if test="$is_last">
         <xsl:text>&#xA;</xsl:text>
      </xsl:if>
   </xsl:template>
   <xsl:template name="indent">
      <xsl:param name="depth" select="0"/>
      <xsl:if test="$depth &gt; 0">
	<xsl:value-of select="for $i in (1 to $depth) return ' '"/>
      </xsl:if>
   </xsl:template>
   <xsl:template match="text()" mode="format">
     <xsl:if test="normalize-space()!=''">
       <xsl:copy/>
     </xsl:if>
   </xsl:template>

   <xsl:template match="text()[not(ancestor::pml_schema)]" mode="format">
   </xsl:template>
   <xsl:template match="text()[not(ancestor::pml_schema)]">
   </xsl:template>
</xsl:stylesheet>
