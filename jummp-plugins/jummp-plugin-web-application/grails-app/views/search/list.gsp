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
        <title>My Models | BioModels</title>
        <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main" />
        <link rel="stylesheet" href="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: '/css', file: 'datatablestyle.css')}" />
        <g:javascript contextPath="" src="jquery/jquery-ui-v1.10.3.js"/>
    </head>
    <body activetab="search">
        <g:render template="/templates/mainContent" model="['action': 'list']"/>
        <!-- TODO: could use other view for list action -->
    </body>
    <content tag="sidebar">
        <!-- quick links -->
        <g:render template="/templates/search/listRightSidebar" />
    </content>
    <content tag="facetsearch">
        <!-- show facets search on the left sidebar -->
        <g:render template="/templates/search/leftSidebar" />
    </content>
    <content tag="mymodels">
        selected
    </content>
    <content tag="title">
        <g:message code="model.list.heading"/>
    </content>
    <content tag="contexthelp">
        browse
    </content>
