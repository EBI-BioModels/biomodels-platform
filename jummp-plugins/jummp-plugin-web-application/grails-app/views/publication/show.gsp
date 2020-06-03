<%--
 Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

<%--
  Created by IntelliJ IDEA.
  User: tnguyen@ebi.ac.uk
  Date: 2019-03-25
  Time: 11:40
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="grails.converters.JSON;" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>${title}</title>
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "css/${style}", file: 'publicationPageStyle.css')}" />
</head>

<body>
    <h2>Publication Details</h2>
    <g:if test="${publication}">
        <div id="publicationForm">
            <g:render template="/templates/publication/tableView"
                      plugin="jummp-plugin-web-application"
                      model="['publication': publication]"/>
            %{--<g:render template="/templates/publication/publicationButtonsForm"
                      plugin="jummp-plugin-web-application"/>--}%
        </div>
    </g:if>
    <g:else>
        <p>Could not find the publication ${params?.id}</p>
    </g:else>
</body>
</html>
