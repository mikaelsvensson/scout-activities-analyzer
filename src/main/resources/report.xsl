<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

    <xsl:template match="/report">
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
                    }
                </style>
            </head>
            <body>
                <xsl:apply-templates select="configuration"/>
                <table>
                    <thead>
                        <tr>
                            <td rowspan="2">
                                Activity
                                <br/>
                                <span class="relatedName">Related Activity</span>
                            </td>
                            <td colspan="{count(comparatorValuesLabels/v)}">Comparison Values</td>
                        </tr>
                        <tr>
                            <xsl:for-each select="comparatorValuesLabels/v">
                                <td>
                                    <xsl:value-of select="text()"/>
                                </td>
                            </xsl:for-each>
                        </tr>
                    </thead>
                    <tbody>
                        <xsl:apply-templates select="activities"/>
                    </tbody>
                </table>
            </body>
        </html>
    </xsl:template>

    <xsl:template match="configuration">
        <ul>
            <xsl:for-each select="child::*[local-name() != 'commonWords' and local-name() != 'translations']">
                <li><xsl:value-of select="local-name()"/>:
                    <xsl:value-of select="text()"/>
                </li>
            </xsl:for-each>
            <li>ignored words:
                <ul>
                    <xsl:for-each select="commonWords/v">
                        <xsl:sort select="text()"/>
                        <li>
                            <xsl:value-of select="text()"/>
                        </li>
                    </xsl:for-each>
                </ul>
            </li>
            <li>interchangable words:
                <ul>
                    <xsl:for-each select="translations/v">
                        <xsl:sort select="@to"/>
                        <li><xsl:value-of select="@to"/>:
                            <xsl:choose>
                                <xsl:when test="count(v) &gt; 5 or string-length(@to) &lt; 5">
                                    <ul>
                                        <xsl:for-each select="v">
                                            <xsl:sort select="text()"/>
                                            <li>
                                                <xsl:value-of select="text()"/>
                                            </li>
                                        </xsl:for-each>
                                    </ul>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:value-of select="."/>
                                </xsl:otherwise>
                            </xsl:choose>
                        </li>
                    </xsl:for-each>
                </ul>
            </li>
        </ul>
    </xsl:template>

    <xsl:template match="activities">
        <xsl:for-each select="relations">
            <xsl:if test="position()=1">
                <tr>
                    <td colspan="{1+count(comparatorValues/v)}">
                        <xsl:value-of select="../name"/>
                    </td>
                </tr>
            </xsl:if>
            <tr>
                <td>
                    <span class="relatedName">
                        <xsl:value-of select="name"/>
                    </span>
                </td>
                <xsl:for-each select="comparatorValues/v">
                    <td>
                        <xsl:value-of select="text()"/>
                    </td>
                </xsl:for-each>
            </tr>
        </xsl:for-each>
    </xsl:template>

</xsl:stylesheet>