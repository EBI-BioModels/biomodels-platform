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
        <title>Model Listing</title>
        <meta name="layout" content="main" />
        <link rel="stylesheet" href="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: '/css', file: 'datatablestyle.css')}" />
        <g:javascript contextPath="" src="jquery/jquery-ui-v1.10.3.js"/>
    </head>
    <body activetab="search">
        <g:render template="/templates/datatable" model="['action': 'list']"/>
    </body>
    <content tag="sidebar">
        <g:if test="${history}">
            <div class="element" id="sidebar-element-last-accessed-models">
                <h3><g:message code="model.history.title"/></h3>
                <ul>
                <g:each in="${history}">
                    <li><a href="${createLink(controller: "model", action: "show", id: it.publicationId ?: it.submissionId)}">${it.name}</a><br/>
                        <g:message code="model.history.submitter"/>${it.submitter}</li>
                </g:each>
                </ul>
            </div>
        </g:if>
        <%--  GoTree code, disabled until it is useful again.
        <div class="element">
            <h2>Gene Ontology Tree</h2>
            <h3>Browse models using GO Tree</h3>
            <p>This is a tree view of the models in this Database based on <a href="http://www.geneontology.org/">Gene Ontology</a>.</p>
            <p><g:link controller="gotree">link</g:link></p>
        </div> --%>
    </content>
    <content tag="browse">
        selected
    </content>
    <content tag="title">
        <g:message code="model.list.heading"/>
    </content>
    <content tag="contexthelp">
        browse
    </content>
