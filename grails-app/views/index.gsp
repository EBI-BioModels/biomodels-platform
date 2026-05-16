<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>











<%
    def styleName = grailsApplication.config.jummp.branding.style
    response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate")
    response.setHeader("Pragma", "no-cache")
    response.setDateHeader("Expires", 0)
%>

<!doctype html>
<html>
    <head>
        <meta name="layout" content="${styleName}/main"/>
        <script type="text/javascript" src="https://d3js.org/d3.v4.min.js"></script>
    </head>
    <body>
        <g:render template="/templates/biomodels/homepage" />
    </body>
</html>
<content tag="title">
	BioModels repository
</content>
