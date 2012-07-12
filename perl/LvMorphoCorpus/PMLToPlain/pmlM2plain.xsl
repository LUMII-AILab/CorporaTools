<?xml version="1.0" encoding="UTF-8" standalone="yes"?>

<!-- Lauma Pretkalnina, AIlab, UL IMCS, 2012-01-10.
	 Stylesheet for transforming data from Latvian Treebank PML-M to plain
	 text (one token per row, <s></s> as sentence delimiters). -->

<xsl:stylesheet
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:pml="http://ufal.mff.cuni.cz/pdt/pml/"
	version="1.0">
	<xsl:output method="text" encoding="UTF-8" indent="no"/>
		<xsl:template match="/">
			<xsl:for-each select="/pml:lvmdata/pml:s">
				<xsl:text>&lt;s&gt;&#10;</xsl:text>
				<xsl:for-each select="./pml:m">
					<xsl:value-of select="./pml:form"/>
					<xsl:text>&#09;</xsl:text>
					<xsl:value-of select="./pml:tag"/>
					<xsl:text>&#09;</xsl:text>
					<xsl:value-of select="./pml:lemma"/>
					<xsl:text>&#10;</xsl:text>
				</xsl:for-each>
				<xsl:text>&lt;/s&gt;&#10;</xsl:text>
			</xsl:for-each>
		</xsl:template>
</xsl:stylesheet>