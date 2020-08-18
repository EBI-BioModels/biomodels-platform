<%--
 Copyright (C) 2010-2017 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

<%@ page contentType="text/html;charset=UTF-8" %>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title><g:message code="${titleCode}" default="Developer's Zone | BioModels"/></title>
</head>

<body>
    <h2>Development with BioModels</h2>
    <p>Explore our web services and tools and learn how to integrate them into your applications.</p>
    <ul>
        <li>
            <a href="${createLink(uri: '/docs', absolute: true)}" target="_blank">RESTful Web Services API Documentation</a><br>
            BioModels provides programmatic access to its content via RESTful Web Services Interface. The Web Services API covers everything users can do on the Web interface.
        </li>
    </ul>
</body>
<content tag="developerZone">
    selected
</content>
<content tag="title">
    <g:message code="${titleCode}" default="Developer's Zone" />
</content>
