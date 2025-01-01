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











<%@ page contentType="text/html;charset=UTF-8" %>
    <head>
        <title>Model Search | BioModels</title>
        <meta name="layout" content="${session['branding.style']}/main" />
        <meta name="robots" content="nofollow" />
         <link rel="stylesheet" href="${resource(contextPath: "${serverURL}", dir: '/css', file: 'datatablestyle.css')}" />
         <g:javascript contextPath="" src="jquery/jquery-ui-v1.10.3.js"/>
    </head>
    <content tag="facetsearch">
        <!-- show facets search on the left sidebar -->
        <g:render template="/templates/search/leftSidebar" />
    </content>
    <body activetab="search">
        <g:render template="/templates/mainContent" model="['action': 'search']"/>
    </body>
    <content tag="sidebar">
        <!-- show descriptive statistics, GO chart, etc. -->
        <g:render template="/templates/search/rightSidebar" model="['action': 'search']"/>
    </content>
    <content tag="search">
        ${query}
    </content>

    <content tag="title">
        Search Model Repository
    </content>
    <content tag="contexthelp">
        search
    </content>
