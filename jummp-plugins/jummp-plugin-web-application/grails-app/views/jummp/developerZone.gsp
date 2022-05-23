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
    <title><g:message code="${titleCode}" default="Developer's Zone"/> | BioModels</title>
</head>

<body>
    <h2>Development with BioModels</h2>
    <p>Explore our web services and tools and learn how to integrate them into your applications.</p>
    <ul>
        <li>
            <a href="${createLink(uri: '/docs', absolute: true)}" target="_blank">RESTful Web Services API Documentation</a><br>
            BioModels provides programmatic access to its content via RESTful Web Services Interface. The Web Services API covers everything users can do on the Web interface.
        </li>
        <li>
            <a href="${grailsApplication.config.jummp.ws.client.japi.docs}" target="_blank">Java based API client
            to consume BioModels' RESTful Web Services</a><br>
            The Java-based API client provides a convenient way to consume a few of the API endpoints requested by
            BioModels' existing users.
            <ul>
                <li><a href="https://bitbucket.org/biomodels/testbiomodelswsclient/src/master/" target="_blank">A Java program
                to demonstrate how to use the library</a><br>
                    The toy program written in Java to demonstrate the usages of the library.</li>
            </ul>
        </li>
        <li>
            <a href="${grailsApplication.config.jummp.ws.client.pyapi.docs}"
               target="_blank"
               style="pointer-events: none; cursor: default; opacity: 0.8">Python based client library
            to consume BioModels' RESTful Web Services</a> (<span style="color: red">coming soon</span>)<br/>
            The Python-based API client provides a convenient way to consume a few of the API endpoints requested by
            BioModels' existing clients.
            <ul>
                <li><a href="https://bitbucket.org/biomodels/testbiomodelswsclient/src/master/"
                       target="_blank"
                       style="pointer-events: none; cursor: default; opacity: 0.8">A Python script
                to demonstrate how to use the library</a> (<span style="color: red">coming soon</span>)<br/>
                    The toy program written in Python to demonstrate the usages of the library.</li>
            </ul>
        </li>
    </ul>
</body>
<content tag="developer-zone">
    selected
</content>
<content tag="title">
    <g:message code="${titleCode}" default="Developer's Zone" />
</content>
