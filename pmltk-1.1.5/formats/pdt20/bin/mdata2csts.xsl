<?xml version="1.0" encoding="utf-8"?>
<!--

=head1 mdata2csts.xsl

A simple convertor for mdata PML instances to CSTS.

=head1 SYNOPSIS

  xsltproc -o file.csts mdata2csts.xsl file.m
or
  saxon -o file.csts file.m mdata2csts.xsl

=head1 DESCRIPTION

This stylesheet converts an mdata PML instance to a corresponding
CSTS instance with the following simplifications:

- no csts header is created

- only one of each of C<doc>,C<c>,C<p> elements is created

- only a dummy header is created for C<doc>

- all elements of wdata layer except for C<w> elements referenced from the mdata are ignored

- C<D> elements are created as correctly as possible (based on wdata)

- only one source of morphological annotation is supporeted
  and it is dumped as C<l> and C<t> regardless of its true source

=head1 AUTHOR

Petr Pajas <pajas@matfyz.cz>

Copyright 2005 Petr Pajas, All rights reserved.

=cut

-->

<xsl:stylesheet
  xmlns:xsl='http://www.w3.org/1999/XSL/Transform' 
  xmlns:m='http://ufal.mff.cuni.cz/pdt/pml/'
  version='1.0'>
  <xsl:output method="text" encoding="iso-8859-2"/>
  
  <xsl:variable name="wdata" select="document(string(/m:mdata/m:head/m:references/m:reffile[@name='wdata']/@href),/)"/>
  <xsl:variable name="wprefix" select="/m:mdata/m:head/m:references/m:reffile[@name='wdata']/@id"/>

  <xsl:key name="w" match="m:w" use="@id"/>

  <xsl:template match="/">
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="m:mdata">
    <xsl:text>&lt;csts lang="</xsl:text>
    <xsl:value-of select="string(/m:mdata/m:meta/m:lang)"/>
    <xsl:text>">&#xa;</xsl:text>
    <xsl:text>&lt;doc file=mdata id=000>&#xa;</xsl:text>
    <xsl:text><![CDATA[<a>
<mod>x
<txtype>x
<genre>x
<med>x
<temp>1992
<authname>x
<opus>ln
<id>x
</a>
<c>
<p n=0>
]]></xsl:text>
    <xsl:apply-templates/>
    <xsl:text>&lt;/c>&#xa;</xsl:text>
    <xsl:text>&lt;/doc>&#xa;</xsl:text>
    <xsl:text>&lt;/csts>&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="m:s">
    <xsl:text>&lt;s id=</xsl:text>
    <xsl:value-of select="substring-after(@id,'m-')"/>
    <xsl:text>>&#xa;</xsl:text>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="m:m" mode="tag">
    <xsl:param name="gen">0</xsl:param>
    <xsl:choose>
      <xsl:when test="m:form!='-' and (string(m:form)=0 or number(string(m:form)))">
        <xsl:text>&lt;f num</xsl:text>
        <xsl:if test="$gen"><xsl:text>.gen</xsl:text></xsl:if>
      </xsl:when>
      <xsl:when test="contains(concat(&quot;'&quot;,'.-,;!?:&lt;&gt;&amp;()`_&quot;/\=+#@$%^*~|{}ˇ\\'),substring(m:form,1,1))">
        <xsl:text>&lt;d</xsl:text>
        <xsl:if test="$gen"><xsl:text> gen</xsl:text></xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:text>&lt;f</xsl:text>
        <xsl:if test="$gen"><xsl:text> gen</xsl:text></xsl:if>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="m:m" mode="after-tag">
    <xsl:param name="form">
      <xsl:call-template name="csts-scape">
        <xsl:with-param name="string" select="string(m:form)"/>
      </xsl:call-template>
    </xsl:param> 
    <xsl:text> id=</xsl:text>
    <xsl:value-of select="substring-after(@id,'m-')"/>
    <xsl:text>&gt;</xsl:text>
    <xsl:value-of select="$form"/>
<!--
    <xsl:choose>
      <xsl:when test="m:form='`'">
        <xsl:text>&amp;grave;</xsl:text>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="m:form"/>
      </xsl:otherwise>
    </xsl:choose>

    <xsl:value-of select="m:form"/>
-->
    <!-- hack: what characters do we escape in lemma? -->
    <xsl:text>&lt;l&gt;</xsl:text>
    <xsl:choose>
      <xsl:when test="m:lemma = substring(m:lemma,1,1)">
        <xsl:call-template name="csts-scape">
          <xsl:with-param name="string" select="string(m:lemma)"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:call-template name="sgml-scape">
          <xsl:with-param name="string" select="string(m:lemma)"/>
        </xsl:call-template>        
      </xsl:otherwise>
    </xsl:choose>
    <xsl:text>&lt;t&gt;</xsl:text>
    <xsl:value-of select="m:tag"/>
    <xsl:text>&#xa;</xsl:text>
  </xsl:template>

  <xsl:template match="m:m">
    <xsl:choose>
      <xsl:when test="m:w.rf/m:LM">
        <xsl:apply-templates select="m:w.rf/m:LM">
          <xsl:with-param name="m" select="."/>
          <xsl:with-param name="more_w" select="count(m:w.rf/m:LM)>1"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:when test="not(m:w.rf)">
        <xsl:text>&lt;w ins>&#xa;</xsl:text>
        <xsl:apply-templates select="." mode="tag"/>
        <xsl:apply-templates select="." mode="after-tag"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="m:w.rf">
          <xsl:with-param name="m" select="."/>
          <xsl:with-param name="more_w" select="0"/>
        </xsl:apply-templates>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="m:w.rf|m:w.rf/m:LM">
    <xsl:param name="m"/>
    <xsl:param name="more_w"/>
    <xsl:param name="ref" select="substring-after(current(),concat($wprefix,'#'))"/>
    <xsl:param name="last" select="not(following-sibling::m:LM)"/>
    <xsl:for-each select="$wdata">
      <xsl:apply-templates select="key('w',$ref)">
        <xsl:with-param name="m" select="$m"/>
        <xsl:with-param name="more_w" select="$more_w"/>
        <xsl:with-param name="last" select="$last"/>
      </xsl:apply-templates>
    </xsl:for-each>
  </xsl:template>

  <xsl:template match="m:w">
    <xsl:param name="m"/>
    <xsl:param name="more_w"/>
    <xsl:param name="last"/>
    <xsl:if test="$more_w or $m/m:form!=string(m:token)">
      <xsl:text>&lt;w</xsl:text>
      <xsl:choose>
        <xsl:when test="$m/m:form_change=''">
        </xsl:when>
        <xsl:when test="$m/m:form_change='insert'">
          <xsl:text> ins</xsl:text>
        </xsl:when>
        <xsl:when test="$m/m:form_change='num_normalization'">
          <xsl:text> num.orig</xsl:text>
        </xsl:when>
        <xsl:when test="$m/m:form_change!=''">
          <xsl:text> </xsl:text>
          <xsl:value-of select="$m/m:form_change"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:message>
            <xsl:text>Warning: empty m/formchange while w/token!=m/form at </xsl:text>
            <xsl:text>m/@id=</xsl:text>
            <xsl:value-of select="$m/@id"/>
            <xsl:text>, w/@id=</xsl:text>
            <xsl:value-of select="@id"/>
          </xsl:message>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>&gt;</xsl:text>
      <xsl:call-template name="csts-scape">
        <xsl:with-param name="string">
          <xsl:value-of select="string(m:token)"/>
        </xsl:with-param>
      </xsl:call-template>
      <xsl:text>&#xa;</xsl:text>
    </xsl:if>
    <xsl:if test="$last">
      <xsl:apply-templates select="$m" mode="tag">
        <xsl:with-param name="gen" select="$more_w or $m/m:form!=string(m:token)"/>
      </xsl:apply-templates>
      <xsl:apply-templates select="$m" mode="after-tag"/>
    </xsl:if>
    <xsl:if test="m:no_space_after=1">
      <xsl:text>&lt;D>&#xa;</xsl:text>
    </xsl:if>
  </xsl:template>

  <xsl:template match="m:head|m:meta|text()">
  </xsl:template>

  
  <xsl:template name="string-replace">
    <xsl:param name="string"/>
    <xsl:param name="from"/>
    <xsl:param name="to"/>
    
    <xsl:choose>
      <xsl:when test="contains($string, $from)">
        
        <xsl:variable name="before" select="substring-before($string, $from)"/>
        <xsl:variable name="after" select="substring-after($string, $from)"/>
        <xsl:variable name="prefix" select="concat($before, $to)"/>
        
        <xsl:value-of select="$before"/>
        <xsl:value-of select="$to"/>
        <xsl:call-template name="string-replace">
          <xsl:with-param name="string" select="$after"/>
          <xsl:with-param name="from" select="$from"/>
          <xsl:with-param name="to" select="$to"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="$string"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>  

  <xsl:template name="sgml-scape">
    <xsl:param name="string"/>
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">&lt;</xsl:with-param>
      <xsl:with-param name="to">&amp;lt;</xsl:with-param>
    <xsl:with-param name="string">
      <xsl:call-template name="string-replace">
        <xsl:with-param name="from">&amp;</xsl:with-param>
        <xsl:with-param name="to">&amp;amp;</xsl:with-param>
      <xsl:with-param name="string">
        <xsl:call-template name="string-replace">
          <xsl:with-param name="from">%</xsl:with-param>
          <xsl:with-param name="to">&amp;percnt;</xsl:with-param>
          <xsl:with-param name="string" select="$string"/>
        </xsl:call-template>
      </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="csts-scape">
    <xsl:param name="string"/>
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">{</xsl:with-param>
      <xsl:with-param name="to">&amp;lcub;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">}</xsl:with-param>
      <xsl:with-param name="to">&amp;rcub;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">&lt;</xsl:with-param>
      <xsl:with-param name="to">&amp;lt;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">]</xsl:with-param>
      <xsl:with-param name="to">&amp;rsqb;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">\</xsl:with-param>
      <xsl:with-param name="to">&amp;bsol;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">^</xsl:with-param>
      <xsl:with-param name="to">&amp;circ;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">[</xsl:with-param>
      <xsl:with-param name="to">&amp;lsqb;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">_</xsl:with-param>
      <xsl:with-param name="to">&amp;lowbar;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">$</xsl:with-param>
      <xsl:with-param name="to">&amp;dollar;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">></xsl:with-param>
      <xsl:with-param name="to">&amp;gt;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">@</xsl:with-param>
      <xsl:with-param name="to">&amp;commat;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">|</xsl:with-param>
      <xsl:with-param name="to">&amp;verbar;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">ˇ</xsl:with-param>
      <xsl:with-param name="to">&amp;macron;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">`</xsl:with-param>
      <xsl:with-param name="to">&amp;grave;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">#</xsl:with-param>
      <xsl:with-param name="to">&amp;num;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">*</xsl:with-param>
      <xsl:with-param name="to">&amp;ast;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">%</xsl:with-param>
      <xsl:with-param name="to">&amp;percnt;</xsl:with-param>
    <xsl:with-param name="string">
    <xsl:call-template name="string-replace">
      <xsl:with-param name="from">&amp;</xsl:with-param>
      <xsl:with-param name="to">&amp;amp;</xsl:with-param>
      <xsl:with-param name="string" select="$string"/>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
    </xsl:with-param>
    </xsl:call-template>
  </xsl:template>
      
</xsl:stylesheet>
