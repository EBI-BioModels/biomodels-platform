<%--
Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
Deutsches Krebsforschungszentrum (DKFZ)

This file is part of Jummp.

Jummp is free software; you can redistribute it and/or modify it under the
terms of the GNU Affero General Public License as published by the Free
Software Foundation; either version 3 of the License, or (at your option) any
later version.

Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
details.

You should have received a copy of the GNU Affero General Public License along
with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>

<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 01/04/2020
  Time: 12:27
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>Administrative Interface of Home Page settings | BioModels</title>
</head>

<body>
    <div class="row">
        <div class="small-12 medium-12 large-12 columns">
            <h1>Reports</h1>
            <g:if test="${models}">
                <p>List of the recently accessed models</p>
                <ul>
                    <g:each in="${models}" var="model">
                        <li>${model.key}: ${model.value}</li>
                    </g:each>
                </ul>
            </g:if>
            <g:else>
                <p>There has been an error when trying to update the list of the recently accessed models on Redis
                Server</p>
            </g:else>
        </div>
    </div>
</body>
</html>
