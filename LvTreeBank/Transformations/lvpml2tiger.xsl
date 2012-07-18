<?xml version="1.0" encoding="UTF-8" standalone="yes"?>

<!-- Lauma Pretkalniņa, AIlab, UL IMCS, 2012-01-11.
	Stylesheet for transforming data from Latvian Treebank PML (a - vers. 2.9.,
	m - vers. 1.0., w - vers. 1.0 ) to Tiger XML. PML data must be "knited-in".
	You can use PML-toolkit from http://ufal.mff.cuni.cz/jazz/pml/ to obtain
	knited file. It is suggested that PML data contains "ord" values only for
	nodes coresponding tokens, otherwise resulting TigerXML will look a bit
	confusing (but technicaly everything works fine either way).
	If you use TigerSearch and experience problems with character encoding,
	convert the resulting file in "ISO-8859-13" encoding. -->

<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:pml="http://ufal.mff.cuni.cz/pdt/pml/"
	version="2.0">
	<xsl:output method="xml" encoding="UTF-8" indent="yes"/>
	
	<xsl:template match="/">
		<corpus>
			<!-- Corpora ID is derived from the 1st sentence id. -->
			<xsl:attribute name="id">
				<xsl:value-of
					select="replace(replace(/pml:lvadata/pml:trees/pml:LM[1],'-[^-]+$',''),'^[^-]+-','')"/>
			</xsl:attribute>
			<!-- Head output. -->
			<head>
				<meta>
					<!-- Some meta data must be added or corrected manually. -->
					<name>
						<xsl:value-of
							select="replace(replace(/pml:lvadata/pml:trees/pml:LM[1],'-[^-]+$',''),'^[^-]+-','')"/>
					</name>
					<author><xsl:text>--</xsl:text></author>
					<date><xsl:text>--</xsl:text></date>
					<description>
						<xsl:value-of select="/pml:lvadata/pml:meta"/>
					</description>
					<format>
						<xsl:text>TigerXML from Latvian Treebank PML</xsl:text>
					</format>
				</meta>
				<annotation>
					<xsl:call-template name="annotation"/>
				</annotation>
			</head>
			<!-- Body output. -->
			<body>
				<!-- Each sentence from PML is translated to one Tiger graph. -->
				<xsl:for-each select="/pml:lvadata/pml:trees/pml:LM">
					<s>
						<!-- IDs are reused as much as possible. -->
						<xsl:attribute name="id">
							<xsl:value-of select="@id"/>
						</xsl:attribute>
						<graph>
							<xsl:attribute name="root">
								<xsl:value-of select="@id"/>
								<xsl:text>_sent</xsl:text>
							</xsl:attribute>
							<!-- Terminals coresponds to tokens or reductions. -->
							<terminals>
								<xsl:call-template name="terminals"/>
							</terminals>
							<!-- Should dependency be treated as a phrase from two elements? -->
							<nonterminals>
								<xsl:call-template name="nonterminals"/>
							</nonterminals>
						</graph>
					</s>
				</xsl:for-each>
			</body>
		</corpus>
	</xsl:template>
	
	
	<!-- Template (subroutine;) for creating list of terminal nodes.
		XPath '.' must be one of '/pml:lvadata/pml:trees/pml:LM' -->
	<xsl:template name="terminals">
		<!-- One terminal node for each morphological unit and for each
			reduction node. -->
		<xsl:for-each select=".//pml:node[pml:m.rf] | .//pml:node[pml:reduction]">
			<!-- Terminal nodes must be sorted by the order of the occurence in
				the sentence. -->
			<xsl:sort select="./pml:ord" data-type="number"/>
			<t>
				<!-- Here goes the long list of attributes. -->
				<xsl:attribute name="id">
					<xsl:value-of select="@id"/>
					<xsl:text>_terminal</xsl:text>
				</xsl:attribute>
				<xsl:attribute name="ord">
					<xsl:value-of select="./pml:ord"/>
				</xsl:attribute>
				<xsl:attribute name="form">
					<xsl:choose>
						<xsl:when test="./pml:m.rf/pml:form">
							<xsl:value-of select="./pml:m.rf/pml:form"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>--</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<xsl:attribute name="lemma">
					<xsl:choose>
						<xsl:when test="./pml:m.rf/pml:lemma">
							<xsl:value-of select="./pml:m.rf/pml:lemma"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>--</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<xsl:attribute name="morph">
					<xsl:choose>
						<xsl:when test="./pml:m.rf/pml:tag">
							<xsl:value-of select="./pml:m.rf/pml:tag"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>--</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<xsl:attribute name="tokens">
					<xsl:choose>
						<xsl:when test="./pml:m.rf/pml:w.rf">
							<xsl:choose>
								<xsl:when test="./pml:m.rf/pml:w.rf/pml:LM">
									<xsl:for-each select="./pml:m.rf/pml:w.rf/pml:LM">
										<xsl:value-of select="./pml:token"/>
										<xsl:if test="not(./pml:no_space_after>0)">
											<xsl:text> </xsl:text>
										</xsl:if>
									</xsl:for-each>
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="./pml:m.rf/pml:w.rf/pml:token"/>
									<xsl:if test="not(./pml:m.rf/pml:w.rf/pml:no_space_after>0)">
										<xsl:text> </xsl:text>
									</xsl:if>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>--</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<xsl:attribute name="reduction">
					<xsl:choose>
						<xsl:when test="./pml:reduction">
							<xsl:value-of select="./pml:reduction"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>--</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
			</t>
		</xsl:for-each>
	</xsl:template>
	
	<!-- Template (subroutine;) for creating list of nonterminal nodes.
		XPath '.' must be one of '/pml:lvadata/pml:trees/pml:LM' -->
	<xsl:template name="nonterminals">
		<!-- Root node. -->
		<nt>
			<!-- Here goes the long list of attributes. -->
			<xsl:attribute name="id">
				<xsl:value-of select="@id"/>
				<xsl:text>_sent</xsl:text>
			</xsl:attribute>
			<xsl:attribute name="ord">
				<xsl:choose>
					<xsl:when test="./pml:ord">
						<xsl:value-of select="./pml:ord"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>--</xsl:text>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:attribute>
			<xsl:attribute name="role">
				<xsl:text>--</xsl:text>
			</xsl:attribute>
			<xsl:attribute name="type">
				<xsl:call-template name="nt_type"/>
				<!--<xsl:choose>
					<xsl:when test="./pml:children/pml:pmcinfo/pml:pmctype">
						<xsl:value-of select="./pml:children/pml:pmcinfo/pml:pmctype"/>
					</xsl:when>
					<xsl:when test="./pml:children/pml:coordinfo">
						<xsl:text>coord</xsl:text>
					</xsl:when>
					<xsl:otherwise>
						<xsl:text>-e-</xsl:text>
					</xsl:otherwise>
				</xsl:choose>-->
			</xsl:attribute>
			<xsl:attribute name="tag">
				<xsl:text>--</xsl:text>
			</xsl:attribute>
			<!-- Edges. -->
			<xsl:call-template name="nt_edges"/>
		</nt>
		<!-- One nonterminal nonroot node for each node element. -->
		<xsl:for-each select=".//pml:node">
			<nt>
				<!-- Here goes the long list of attributes. -->
				<!-- Common attributes for all kinds of nodes -->
				<xsl:attribute name="id">
					<xsl:value-of select="@id"/>
				</xsl:attribute>
				<xsl:attribute name="ord">
					<xsl:choose>
						<xsl:when test="./pml:ord">
							<xsl:value-of select="./pml:ord"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>--</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<xsl:attribute name="role">
					<xsl:choose>
						<xsl:when test="./pml:role">
							<xsl:value-of select="./pml:role"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>--</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<!-- Type attribute. -->
				<xsl:attribute name="type">
					<xsl:call-template name="nt_type"/>
				</xsl:attribute>
				<!-- Tag attribute. -->
				<xsl:attribute name="tag">
					<xsl:choose>
						<xsl:when test="./pml:children/pml:xinfo/pml:tag">
							<xsl:value-of select="./pml:children/pml:xinfo/pml:tag"/>
						</xsl:when>
						<xsl:when test="./pml:children/pml:coordinfo/pml:tag">
							<xsl:value-of select="./pml:children/pml:xinfo/pml:tag"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>--</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:attribute>
				<!-- Edges. -->
				<xsl:call-template name="nt_edges"/>
			</nt>
		</xsl:for-each>
	</xsl:template>

	<!-- Template for creating edges from a freshly created nonterminal node to
		its children.
		XPath '.' must be one of '/pml:lvadata/pml:trees/pml:LM//pml:node' -->
	<xsl:template name="nt_edges">
		<!-- Edges to dependency children. -->
		<xsl:for-each select="./pml:children/pml:node">
			<edge>
				<xsl:attribute name="label">
					<xsl:text>dep</xsl:text>
				</xsl:attribute>
				<xsl:attribute name="idref">
					<xsl:value-of select="@id"/>
				</xsl:attribute>
			</edge>
		</xsl:for-each>
		<!-- Edges to x-word constituents. -->
		<xsl:for-each select="./pml:children/pml:xinfo/pml:children/pml:node">
			<edge>
				<xsl:attribute name="label">
					<xsl:text>x</xsl:text>
				</xsl:attribute>
				<xsl:attribute name="idref">
					<xsl:value-of select="@id"/>
				</xsl:attribute>
			</edge>
		</xsl:for-each>
		<!-- Edges to pmc constituents. -->
		<xsl:for-each select="./pml:children/pml:pmcinfo/pml:children/pml:node">
			<edge>
				<xsl:attribute name="label">
					<xsl:text>pmc</xsl:text>
				</xsl:attribute>
				<xsl:attribute name="idref">
					<xsl:value-of select="@id"/>
				</xsl:attribute>
			</edge>
		</xsl:for-each>
		<!-- Edges to coordination constituents. -->
		<xsl:for-each select="./pml:children/pml:coordinfo/pml:children/pml:node">
			<edge>
				<xsl:attribute name="label">
					<xsl:text>coord</xsl:text>
				</xsl:attribute>
				<xsl:attribute name="idref">
					<xsl:value-of select="@id"/>
				</xsl:attribute>
			</edge>
		</xsl:for-each>
		<!-- Edge to corresponding terminal. -->
		<xsl:if test="./pml:m.rf | ./pml:reduction">
			<edge>
				<xsl:attribute name="label">
					<xsl:text>token</xsl:text>
				</xsl:attribute>
				<xsl:attribute name="idref">
					<xsl:value-of select="@id"/>
					<xsl:text>_terminal</xsl:text>
				</xsl:attribute>
			</edge>
		</xsl:if>
	</xsl:template>

	<!-- Template for creating content to the "type" attribute for nonterminal
		node. This attribute contains xtype or pmctype or coordtype from PML.
		XPath '.' must be one of '/pml:lvadata/pml:trees/pml:LM//pml:node' -->
	<xsl:template name="nt_type">	
		<xsl:choose>
			<xsl:when test="./pml:children/pml:xinfo/pml:xtype">
				<xsl:value-of select="./pml:children/pml:xinfo/pml:xtype"/>
			</xsl:when>
			<xsl:when test="./pml:children/pml:pmcinfo/pml:pmctype">
				<xsl:value-of select="./pml:children/pml:pmcinfo/pml:pmctype"/>
			</xsl:when>
			<xsl:when test="./pml:children/pml:coordinfo">
				<xsl:value-of select="./pml:children/pml:coordinfo/pml:coordtype"/>
			</xsl:when>
			<!--<xsl:when test="./pml:m.rf | ./pml:reduction">
				<xsl:text>token</xsl:text>
			</xsl:when>-->
			<xsl:otherwise>
			<xsl:text>--</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	
	<!-- Template for creating \corpora\head\annotation content in the result
		file. Hardcoded creation. -->
	<xsl:template name="annotation">
		<feature name="form" domain="T"/>
		<feature name="lemma" domain="T"/>
		<feature name="morph" domain="T"/>
		<feature name="tokens" domain="T"/>
		<feature name="reduction" domain="T"/>
		
		<feature name="ord" domain="FREC"/>
		
		<feature name="role" domain="NT"/>
		<feature name="type" domain="NT"/>
		<feature name="tag" domain="NT"/>
		
		<edgelabel>
			<value name="dep">Dependency</value>
			<value name="x">X-word constituents.</value>
			<value name="pmc">PMC constituents.</value>
			<value name="coord">Coordination.</value>
			<value name="token">Corresponding token (technical edge).</value>
		</edgelabel>
	</xsl:template>
</xsl:stylesheet>
