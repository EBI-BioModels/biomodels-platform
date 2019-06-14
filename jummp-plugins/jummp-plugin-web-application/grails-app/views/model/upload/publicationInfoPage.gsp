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
<%@ page import="grails.converters.JSON;" %>
<%@ page import="net.biomodels.jummp.core.model.ModelTransportCommand" %>
<%@ page import="net.biomodels.jummp.core.user.PersonTransportCommand;" %>
<%@ page import="net.biomodels.jummp.core.model.PublicationDetailExtractionContext" %>
<%@ page import="net.biomodels.jummp.core.model.RevisionTransportCommand" %>
<%@ page import="net.biomodels.jummp.core.model.PublicationTransportCommand" %>
<%
    def style = grailsApplication.config.jummp.branding.style
    def serverUrl = grailsApplication.config.grails.serverURL
%>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>
        <g:if test="${isUpdate}">
            <g:message code="submission.publicationInfoPage.update.title" args="${ [params.id] }" />
        </g:if>
        <g:else>
            <g:message code="submission.publicationInfoPage.create.title"/>
        </g:else>
    </title>
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "/css/${style}", file: 'publicationPageStyle.css')}" />
    <g:javascript>
        // Indeed, we don't need to check whether the authors is null or not because if case of the model has
        // no publication yet, we always create an empty PersonTransportCommand to maintain authors
        // during the submission flow.
        if (${workingMemory['Authors'] != null}) {
            var authorMap = {"authors": ${workingMemory['Authors'].collect {
                String userRealName = it.userRealName ?: ""
                String institution = it.institution ?: ""
                String orcid = it.orcid ?: ""
                def id = it.id ?: "undefined"
                if (id == "undefined") {
                    [userRealName: userRealName, institution: institution, orcid: orcid]
                } else {
                    [id: id, userRealName: userRealName, institution: institution, orcid: orcid]
                }
                } as JSON}
            };
            var authorList = authorMap["authors"];
        }
    </g:javascript>
    <g:javascript contextPath="" src="${style}/publicationSubmission.js"/>
</head>
<body>
    <%
        RevisionTransportCommand revision = workingMemory.get("RevisionTC") as RevisionTransportCommand
        PublicationTransportCommand publication  = revision?.model?.publication as PublicationTransportCommand
        def publicationMap = workingMemory.get('publication_objects_in_working') as HashMap<Object, PublicationDetailExtractionContext>
        PublicationDetailExtractionContext pubContext = publicationMap.get(workingMemory.get('SelectedPubLinkProvider'))
        if (pubContext && pubContext.publication) {
            publication = pubContext.publication
        }
        def nbAuthors = workingMemory.get("Authors")?.size()
        def authorListContainerSize = 4
        // If users strive to reload the page for any reason, the flash message would have been wiped.
        // Therefore, we need to re-populate it to display the message again
        boolean isReloaded = false
        if (pubContext.comesFromDatabase && null == flashMessage) {
            isReloaded = true
            flashMessage = message(code: "publication.editor.duplicateEntry.message")
        }
    %>
    <g:if test="${isReloaded}">
        <script>
            $('.flashNotificationDiv').remove();
        </script>
        <g:render template="/templates/notification/showNotificationDiv" contextPath=""/>
    </g:if>
    <div class="row">
    <h2>Update Publication Information</h2>
    <div id="publicationForm">
        <g:render plugin="jummp-plugin-web-application" template="/templates/publication/publicationEditorForm"
              model="['publication': publication, 'authorListContainerSize': authorListContainerSize]" />
    </div>
    </div>
</body>
<g:render template="/templates/decorateSubmission" />
