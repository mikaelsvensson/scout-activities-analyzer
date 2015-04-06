<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/">
        <html>
            <head>
                <style>
                    body, table {
                    font-family: sans-serif;
                    font-size: 90%;
                    }
                    td {
                    vertical-align: top;
                    border: 1px solid #555;
                    padding: 0.2em;
                    }
                    thead {
                    font-weight: bold;
                    }
                    table {
                    border-collapse: collapse;
                    }
                    .relatedName {
                    padding-left: 1em;
                    white-space: nowrap;
                    }
                </style>
            </head>
            <body>
                <xsl:apply-templates select="configuration"/>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="configuration">
        <ul>
            <xsl:for-each select="child::*[local-name()]">
                <li><xsl:value-of select="local-name()"/>:
                    <xsl:value-of select="text()"/>
                </li>
            </xsl:for-each>
        </ul>
    </xsl:template>
</xsl:stylesheet>