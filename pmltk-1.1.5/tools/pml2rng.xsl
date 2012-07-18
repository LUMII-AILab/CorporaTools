<?xml version="1.0"?>

<!-- Abstract: convert PML Schema to a RelaxNG grammar -->

<xsl:stylesheet  xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
  xmlns:s="http://ufal.mff.cuni.cz/pdt/pml/schema/"
  xmlns="http://relaxng.org/ns/structure/1.0"
  xmlns:a="http://relaxng.org/ns/annotation/1.0"
  version='1.0'>
<xsl:output method="xml" indent="yes"/>

<xsl:param name="standalone">0</xsl:param>
<xsl:param name="DEBUG">0</xsl:param>
<xsl:variable name="SUPPORTED_VERSIONS">1.1.0</xsl:variable>
<xsl:variable name="SUPPORTED_FORMATS">,string,normalizedString,token,base64Binary,hexBinary,integer,positiveInteger,negativeInteger,nonNegativeInteger,nonPositiveInteger,long,unsignedLong,int,unsignedInt,short,unsignedShort,byte,unsignedByte,decimal,float,double,boolean,duration,dateTime,date,time,gYear,gYearMonth,gMonth,gMonthDay,gDay,Name,NCName,anyURI,language,IDREF,IDREFS,NMTOKEN,NMTOKENS,</xsl:variable>

<xsl:template match="/">
  <xsl:apply-templates select="(//s:pml_schema)[1]"/>
</xsl:template>

<xsl:template match='*'>
  <xsl:apply-templates/>
  <xsl:message>Error: Unknown PML element <xsl:value-of select="name()"></xsl:value-of></xsl:message>
</xsl:template>

<xsl:template match='s:pml_schema'>
  <xsl:if test="contains(concat(' ',$SUPPORTED_VERSIONS,' '), concat(' ',@version, ' '))">
    <xsl:message terminate="no">Warning: Unsupported PML schema version <xsl:value-of select="@version"/>. Supported versions are:
    <xsl:value-of select="$SUPPORTED_VERSIONS"/>. The result may be unaccurete.
  </xsl:message>
  </xsl:if>
  <xsl:if test="//s:import|//s:derive">
    <xsl:message terminate="yes">Error: Modular PML schemas are not supported by this stylesheet. 
Please convert the PML schema to a simplified PML schema first (e.g. using the 'pml_simplify' tool).
  </xsl:message>
  </xsl:if>
  <grammar xmlns="http://relaxng.org/ns/structure/1.0"
    xmlns:a="http://relaxng.org/ns/annotation/1.0"
    xmlns:pml="http://ufal.mff.cuni.cz/pdt/pml/"
    datatypeLibrary="http://www.w3.org/2001/XMLSchema-datatypes">
      <xsl:choose>
	<xsl:when  test="$standalone=1">
	  <xsl:choose>
	  <xsl:when test="not(/s:pml_schema)">
	    <xsl:copy-of select="document('../rng/pml_internal.rng')/*/*[local-name()='define']"/>
	    <xsl:copy-of select="document('../rng/pml_schema.rng')/*/*[local-name()='define']"/>
	  </xsl:when>
	  <xsl:otherwise>
	    <xsl:copy-of select="document('../rng/pml_common.rng')/*/*[local-name()='define']"/>
	  </xsl:otherwise>
	  </xsl:choose>
	</xsl:when>
	<xsl:otherwise>
	  <xsl:choose>
	    <xsl:when test="/s:pml_schema">
	      <include href="../rng/pml_common.rng"/>
	    </xsl:when>
	    <xsl:otherwise>
	      <include href="../rng/pml_internal.rng"/>
	    </xsl:otherwise>
	  </xsl:choose>
	</xsl:otherwise>
      </xsl:choose>
    <xsl:apply-templates/>
  </grammar>
</xsl:template>

<xsl:template match='s:revision'>
  <a:documentation>PML schema Revision: <xsl:apply-templates/></a:documentation>
</xsl:template>

<xsl:template match='s:description'>
  <a:documentation>RelaxNG schema for PML described as: <xsl:apply-templates/></a:documentation>
</xsl:template>

<xsl:template match='s:reference'></xsl:template>

<xsl:template name="resolve_type">
  <xsl:choose>
    <xsl:when test="@type">
      <xsl:value-of select="/s:pml_schema/s:type[@name=current()/@type]"/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="."/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="follow_type">
  <xsl:param name="type">
    <xsl:call-template name="resolve_type"/>
  </xsl:param>
  <xsl:choose>
    <xsl:when test="count(*)>1">
      <group>
        <xsl:apply-templates select="@type|*"/>
      </group>
    </xsl:when>
    <xsl:otherwise>
      <xsl:apply-templates select="@type|*"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match='s:structure'>
  <xsl:choose>
    <xsl:when test="s:member">
      <interleave>
	<xsl:apply-templates/>
      </interleave>
    </xsl:when>
    <xsl:otherwise>
      <empty/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match='s:sequence[not(@content_pattern)]'>
  <zeroOrMore>
    <xsl:choose>
      <xsl:when test="count(s:element)>1">
        <choice>
          <xsl:apply-templates/>
        </choice>        
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </zeroOrMore>
</xsl:template>

<xsl:template match='s:sequence[@content_pattern]'>
  <xsl:call-template name="parse_re">
    <xsl:with-param name="re">
      <xsl:value-of select="normalize-space(@content_pattern)"/>
    </xsl:with-param>
  </xsl:call-template>
</xsl:template>

<xsl:template name="parse_re"> 
  <xsl:param name="re"/>
  <xsl:param name="depth">0</xsl:param>
  <xsl:param name="sep"/>
  <xsl:param name="token">
    <xsl:call-template name="get_token_re">
      <xsl:with-param name="re" select="$re"/>
    </xsl:call-template>
  </xsl:param>
  <xsl:param name="next" select="normalize-space(substring($re,string-length($token)+1))"/>
  <xsl:param name="next_char" select="substring($next,1,1)"/>
  <xsl:param name="tag">
    <xsl:choose>
      <xsl:when test="$next_char='?'">optional</xsl:when>
      <xsl:when test="$next_char='*'">zeroOrMore</xsl:when>
      <xsl:when test="$next_char='+'">oneOrMore</xsl:when>
    </xsl:choose>
  </xsl:param>
  <xsl:param name="rest">
    <xsl:choose>
      <xsl:when test="$tag!=''">
        <xsl:value-of select="normalize-space(substring($next,2))"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$next"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:param>
  <xsl:if test="$DEBUG=1">
  <xsl:message>[parse: RE:<xsl:value-of select="$re"/>, TOKEN:<xsl:value-of select="$token"/>, NEXT:<xsl:value-of select="$next"/>, DEPTH:<xsl:value-of select="$depth"/>, TAG:<xsl:value-of select="$tag"/>, REST:<xsl:value-of select="$rest"/>]</xsl:message>    
  </xsl:if>
  <xsl:if test="$token=''">
    <xsl:message terminate="yes">Unexpected end of content_pattern='<xsl:value-of select="@content_pattern"/>'</xsl:message>
  </xsl:if>
  <xsl:if test="$DEBUG=1">
    <xsl:comment>
      <xsl:value-of select="$re"/>
    </xsl:comment>
  </xsl:if>
  <xsl:choose>
    <xsl:when test="$tag!=''">
      <xsl:element name="{$tag}">
        <xsl:call-template name="process_token_re">
          <xsl:with-param name="depth" select="$depth"/>
          <xsl:with-param name="token" select="$token"/>
        </xsl:call-template>        
      </xsl:element>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="process_token_re">
        <xsl:with-param name="depth" select="$depth"/>
        <xsl:with-param name="token" select="$token"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
  <xsl:choose>
    <xsl:when test="starts-with($rest,',')">
      <xsl:if test="$sep='|'">
        <xsl:message terminate="yes">Cannot use '|' and ',' in one group in @content_pattern='<xsl:value-of select="@content_pattern"/>'</xsl:message>
      </xsl:if>
      <xsl:call-template name="parse_re">
        <xsl:with-param name="sep" select="','"/>
        <xsl:with-param name="re" select="normalize-space(substring($rest,2))"/>
        <xsl:with-param name="depth" select="$depth"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="starts-with($rest,'|')">
      <xsl:if test="$sep=','">
        <xsl:message terminate="yes">Cannot use ',' and '|' in one group in @content_pattern='<xsl:value-of select="@content_pattern"/>'</xsl:message>
      </xsl:if>
      <xsl:call-template name="parse_re">
        <xsl:with-param name="sep" select="'|'"/>
        <xsl:with-param name="re" select="normalize-space(substring($rest,2))"/>
        <xsl:with-param name="depth" select="$depth"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$rest=''">
    </xsl:when>
    <xsl:otherwise>
      <xsl:message terminate="yes">Unexpected character near '<xsl:value-of select="$rest"/>' in @content_pattern='<xsl:value-of select="@content_pattern"/>'</xsl:message>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="process_token_re"> 
  <xsl:param name="depth"/>
  <xsl:param name="token"/>
  <xsl:if test="$DEBUG=1">
    <xsl:message>[process: TOKEN: <xsl:value-of select="$token"/>, DEPTH: <xsl:value-of select="$depth"/>]</xsl:message>
  </xsl:if>
  <xsl:choose>
    <xsl:when test="substring($token,1,1)='('">
      <!-- find if it is ( | ) or ( , ) -->
      <xsl:call-template name="process_bracket_re">
        <xsl:with-param name="bracket" select="normalize-space(substring($token,2,string-length($token)-2))"/>
        <xsl:with-param name="depth" select="$depth"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$token='#TEXT'">
      <xsl:if test="not(s:text)">
        <xsl:message terminate="yes">No &lt;text> declared for #TEXT in @content_pattern='<xsl:value-of select="@conent_re"/>'</xsl:message>
      </xsl:if>
      <text/>
    </xsl:when>
    <xsl:when test="$token!=''">
      <xsl:if test="not(s:element[@name=$token])">
        <xsl:message terminate="yes">Undefined element '<xsl:value-of select="$token"/>' in @content_pattern='<xsl:value-of select="@content_pattern"/>'</xsl:message>
      </xsl:if>
      <xsl:if test="count(s:element[@name=$token])>1">
        <xsl:message terminate="yes">Repeated sequence element '<xsl:value-of select="$token"/>'</xsl:message>
      </xsl:if>
      <xsl:apply-templates select="s:element[@name=$token]"/>
    </xsl:when>
  </xsl:choose>
</xsl:template>

<xsl:template name="process_bracket_re">
  <xsl:param name="bracket"/>
  <xsl:param name="depth">0</xsl:param>
  <xsl:param name="token">
    <xsl:call-template name="get_token_re">
      <!-- FIXME: if  {n,m} are going to be allowed, we need to eat them carefully here -->
      <xsl:with-param name="re" select="$bracket"/>
    </xsl:call-template>
  </xsl:param>
  <xsl:param name="next" select="normalize-space(translate(substring($bracket,string-length($token)+1),'*+?',''))"/>
  <xsl:param name="next_char" select="substring($next,1,1)"/>
  <xsl:if test="$DEBUG=1">
    <xsl:message>[process: BRACKET:<xsl:value-of select="$bracket"/>, NORM:<xsl:value-of select="normalize-space(translate($bracket,'*?+','      '))"/>, DEPTH: <xsl:value-of select="$depth"/></xsl:message>
  </xsl:if>
  <xsl:if test="$DEBUG=1">
    <xsl:message>... TOKEN:<xsl:value-of select="$token"/>, NCHAR:<xsl:value-of select="$next_char"/>]</xsl:message>
  </xsl:if>
  <xsl:choose>
    <xsl:when test="$next_char=','">
      <group>
        <xsl:call-template name="parse_re">
          <!-- FIXME: pass , so that parse_re can check after every token -->
          <xsl:with-param name="sep" select="','"/>
          <xsl:with-param name="re" select="$bracket"/>
          <xsl:with-param name="depth" select="$depth + 1"/>
          <xsl:with-param name="token" select="$token"/>
        </xsl:call-template>
      </group>
    </xsl:when>
    <xsl:when test="$next_char='|'">
      <choice>
        <xsl:call-template name="parse_re">
          <!-- FIXME: pass | so that parse_re can check after every token -->
          <xsl:with-param name="sep" select="'|'"/>
          <xsl:with-param name="re" select="$bracket"/>
          <xsl:with-param name="depth" select="$depth + 1"/>
          <xsl:with-param name="token" select="$token"/>
        </xsl:call-template>
      </choice>
    </xsl:when>
    <xsl:when test="$next_char=''">
      <xsl:call-template name="parse_re">
        <!-- FIXME: pass | so that parse_re can check after every token -->
        <xsl:with-param name="re" select="$bracket"/>
        <xsl:with-param name="depth" select="$depth"/>
        <xsl:with-param name="token" select="$token"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:message>Unexpected character '<xsl:value-of select="$next_char"/>' in @content_pattern='<xsl:value-of select="@content_pattern"/>'</xsl:message>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="get_token_re">
  <xsl:param name="re"/>
  <xsl:param name="token">
    <xsl:value-of select="substring-before(concat(translate($re,'),|*?+{','      '),' '),' ')"/>
  </xsl:param>
  <xsl:choose>
    <xsl:when test="$token=''">
      <xsl:message terminate="yes">Unexpected content in @content_pattern='<xsl:value-of select="@content_pattern"/>' near '<xsl:value-of select="$re"/>'</xsl:message>
    </xsl:when>
    <xsl:when test="substring($token,1,1)='('">
      <xsl:call-template name="match_bracket_re">
        <xsl:with-param name="re" select="$re"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="$token"/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template name="match_bracket_re"> 
  <xsl:param name="re"/>
  <xsl:param name="pos">1</xsl:param>
  <xsl:param name="depth">0</xsl:param>
  <xsl:param name="next" select="substring($re,$pos,1)"/>
  <xsl:if test="$DEBUG=1">
    <xsl:message>[match_bracket: RE:<xsl:value-of select="$re"/>, DEPTH: <xsl:value-of select="$depth"/>, POS: <xsl:value-of select="$pos"/>, NEXT: <xsl:value-of select="$next"/>]</xsl:message>
  </xsl:if>
  <!-- output char -->
  <xsl:value-of select="$next"/>
  <xsl:choose>
    <xsl:when test="$next=''">
      <xsl:message terminate="yes">Unmatched '(' near '<xsl:value-of select="$re"/>' in @content_pattern='<xsl:value-of select="@content_pattern"/>'</xsl:message>
    </xsl:when>
    <xsl:when test="$next='('">
      <xsl:call-template name="match_bracket_re">
        <xsl:with-param name="re" select="$re"/>
        <xsl:with-param name="depth" select="$depth+1"/>
        <xsl:with-param name="pos" select="$pos+1"/>
      </xsl:call-template>
    </xsl:when>
    <xsl:when test="$next=')'">
      <xsl:if test="$depth>1">
        <xsl:call-template name="match_bracket_re">
          <xsl:with-param name="re" select="$re"/>
          <xsl:with-param name="depth" select="$depth - 1"/>
          <xsl:with-param name="pos" select="$pos+1"/>
        </xsl:call-template>
      </xsl:if>
    </xsl:when>
    <xsl:otherwise>
      <xsl:if test="$depth&lt;1">
        <xsl:message terminate="yes">Expected '(' near: '<xsl:value-of select="substring($re,$pos)"/>' in @content_pattern='<xsl:value-of select="@content_pattern"/>'</xsl:message>
      </xsl:if>
      <xsl:call-template name="match_bracket_re">
        <xsl:with-param name="re" select="$re"/>
        <xsl:with-param name="depth" select="$depth"/>
        <xsl:with-param name="pos" select="$pos+1"/>
      </xsl:call-template>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match='s:container'>
  <xsl:apply-templates/>
  <xsl:apply-templates select="@type"/>
  <xsl:if test="not(@type|s:alt|s:list|s:choice|s:constant|s:sequence|s:cdata)">
    <empty/>
  </xsl:if>
</xsl:template>

<xsl:template match='s:member[@as_attribute=1 and @required=1]'>
  <attribute name="{@name}">
    <xsl:choose>
      <xsl:when test="@role='#KNIT'">
        <text/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="follow_type"/>
      </xsl:otherwise>
    </xsl:choose>
  </attribute>
</xsl:template>

<xsl:template match='s:member[@as_attribute=1 and not(@required=1)]'>
  <optional>
    <attribute name="{@name}">
      <xsl:choose>
        <xsl:when test="@role='#KNIT'">
          <text/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="follow_type"/>
        </xsl:otherwise>
      </xsl:choose>
    </attribute>
  </optional>
</xsl:template>


<xsl:template match='s:member[not(@as_attribute=1) and @required=1]'>
  <element name="pml:{@name}">
    <xsl:choose>
      <xsl:when test="@role='#KNIT'">
        <text/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="follow_type"/>
      </xsl:otherwise>
    </xsl:choose>
  </element>
</xsl:template>

<xsl:template match='s:member[not(@as_attribute=1) and not(@required=1)]'>
  <optional>
    <element name="pml:{@name}">
      <xsl:choose>
        <xsl:when test="@role='#KNIT'">
          <text/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:call-template name="follow_type"/>
        </xsl:otherwise>
      </xsl:choose>
    </element>
  </optional>
</xsl:template>

<xsl:template match='s:element'>
  <element name="pml:{@name}">
    <xsl:call-template name="follow_type"/>
  </element>
</xsl:template>

<xsl:template match='s:text'>
  <text/>
</xsl:template>

<xsl:template match='s:sequence'>
  <oneOrMore>
    <choice>
      <xsl:apply-templates select="*"/>
    </choice>
  </oneOrMore>
</xsl:template>

<xsl:template match='s:attribute[@required=1]'>
  <attribute name="{@name}">
    <xsl:apply-templates select="@type|*"/>
  </attribute>
</xsl:template>

<xsl:template match='s:attribute'>
  <optional>
    <attribute name="{@name}">
      <xsl:apply-templates select="@type|*"/>
    </attribute>
  </optional>
</xsl:template>

<xsl:template match='s:list'>
  <choice>
    <zeroOrMore>
      <element name="pml:LM">
        <xsl:choose>
          <xsl:when test="@role='#KNIT'">
            <text/>
          </xsl:when>
          <xsl:otherwise>
            <xsl:call-template name="follow_type"/>
          </xsl:otherwise>          
        </xsl:choose>
      </element>
    </zeroOrMore>
    <xsl:choose>
      <xsl:when test="@role='#KNIT'">
        <text/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="follow_type"/>
      </xsl:otherwise>          
    </xsl:choose>
  </choice>
</xsl:template>

<xsl:template match='s:alt'>
  <choice>
    <oneOrMore>
      <element name="pml:AM">
        <xsl:call-template name="follow_type"/>
      </element>
    </oneOrMore>
    <xsl:call-template name="follow_type"/>
  </choice>
</xsl:template>

<!--
<xsl:template match='s:pml_schema/s:type'>
  <define name="{@name}">
    <xsl:apply-templates/>
  </define>
</xsl:template>
-->


<xsl:template match='s:type'>
  <define name="type-{@name}">
    <xsl:call-template name="follow_type"/>
  </define>
</xsl:template>

<xsl:template match="s:cdata[@format='ID']">
  <data type="ID"/>
</xsl:template>

<xsl:template match="s:cdata[@format='PMLREF']">
  <ref name="PMLREF"/>
</xsl:template>

<xsl:template match="s:cdata[@format='any']">
  <text/>
</xsl:template>

<xsl:template match="s:cdata">
  <xsl:choose>
    <xsl:when test="contains($SUPPORTED_FORMATS,concat(',',@format,','))">
      <data type="{@format}"/>
    </xsl:when>
    <xsl:otherwise>
        <xsl:message terminate="yes">cdata format '<xsl:value-of select="@format"/>' not supported!&#x0a;Supported formats are : <xsl:value-of select="substring-after($SUPPORTED_FORMATS,',')"/>any,ID,PMLREF.</xsl:message>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match='s:value'>
  <value datatypeLibrary="" type="string"><xsl:value-of select="string(.)"/></value>
</xsl:template>

<xsl:template match='s:constant'>
  <choice>
    <value datatypeLibrary="" type="string"><xsl:value-of select="string(.)"/></value>
  </choice>
</xsl:template>

<xsl:template match='s:choice'>
  <choice>
    <xsl:apply-templates/>
  </choice>
</xsl:template>

<xsl:template match="@type">
  <ref name="type-{string(.)}"/>
</xsl:template>

<xsl:template match='s:pml_schema/s:root'>
  <start>
    <element name="pml:{@name}">
      <ref name="head.element"/>
      <xsl:call-template name="follow_type"/>          
    </element>
  </start>
</xsl:template>

</xsl:stylesheet>
