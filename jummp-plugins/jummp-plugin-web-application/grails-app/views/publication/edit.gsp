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
    <g:render template="/templates/head" plugin="jummp-plugin-web-application" />
    <g:javascript src="helpers.js" contextPath=""/>
    <g:javascript>
        // Indeed, we don't need to check whether the authors is null or not because if case of the model has
        // no publication yet, we always create an empty PersonTransportCommand to maintain authors
        // during the submission flow.
        if (${publication != null}) {
            var authorMap = {"authors": ${publication?.authors?.collect {
                String userRealName = it.userRealName ?: ""
                String institution = it.institution ?: ""
                String orcid = it.orcid ?: ""
                def id = it.id ?: "undefined"
                if (id == "undefined") {
                    [userRealName: userRealName, institution: institution, orcid: orcid]
                } else {
                    [id: id, userRealName: userRealName, institution: institution, orcid: orcid]
                }
            } as JSON}};
            var authorList = authorMap["authors"];
        } /*else {
            var authorMap = {"authors": []};
            var authorList = {};
        }*/
    </g:javascript>
    <g:javascript src="${style}/publicationSubmission.js" contextPath="" />
</head>

<body>
    <h2>Edit Publication Details</h2>
    <g:if test="${publication}">
        <g:render template="/templates/publication/selectPublicationSource"
                  plugin="jummp-plugin-web-application"
                  model="['publication': publication, 'controller': controller,
                          'operation': operation,
                          'linkSourceTypes': linkSourceTypes]" />
        <div id="publicationForm">
            <g:render template="/templates/publication/publicationEditableElements"
                      plugin="jummp-plugin-web-application"
                      model="['publication': publication, 'authorListContainerSize': authorListContainerSize]"/>
            <g:render template="/templates/publication/publicationButtonsForm"
                      plugin="jummp-plugin-web-application"/>
        </div>

        <g:render template="/templates/publication/loadCommonActions"
                  plugin="jummp-plugin-web-application"/>
    </g:if>
    <g:else>
        <p>Could not find the publication ${params?.id}</p>
    </g:else>
</body>
</html>
