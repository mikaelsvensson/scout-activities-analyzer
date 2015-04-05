<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:output indent="yes" method="html"/>

    <xsl:template match="/">
        <html>
            <head></head>
            <body>
                <ul>
                <xsl:apply-templates select="//variation">
                    <xsl:sort select="@suffix"/>
                </xsl:apply-templates>
                </ul>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="variation">
        <li>
            <xsl:value-of select="@suffix"/>
            <ul>
                <xsl:for-each select="prefix">
                    <li>
                        <xsl:value-of select="concat(text(), '|', ../@suffix)"/>
                    </li>
                </xsl:for-each>
            </ul>
        </li>
    </xsl:template>

</xsl:stylesheet>