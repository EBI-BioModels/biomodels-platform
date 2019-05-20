<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-05-17
  Time: 20:09
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="grails.converters.JSON;" %>
<%
    def style = grailsApplication.config.jummp.branding.style
    def serverUrl = grailsApplication.config.grails.serverURL
%>
<html>
<head>
    <meta name="layout" content="biomodels/main" />
    <title>Add a new publication | BioModels</title>
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "/css/${style}", file: 'publicationPageStyle.css')}" />
</head>

<body>
    <div class="row">
        <h2>Add a new publication</h2>
    <g:render template="/templates/publication/publicationEditorForm" model="[publication: publication]"/>
    </div>
    <g:javascript>
        // Indeed, we don't need to check whether the authors is null or not because if case of the model has
        // no publication yet, we always create an empty PersonTransportCommand to maintain authors
        // during the submission flow.
        if (${publication.id}) {
            let authorMap = {"authors": ${publication?.authors?.collect {
                String userRealName = it.userRealName ?: ""
                String institution = it.institution ?: ""
                String orcid = it.orcid ?: ""
                [userRealName: userRealName, institution: institution, orcid: orcid]
            } as JSON }};
            let authorList = authorMap["authors"];
        } else {
            let authorMap = {"authors": [{"userRealName": "ABC", "institution": "Home", "orcid": "dsdsd-dsds-dsds-dsds"}]};
            let authorList = {};
        }
    </g:javascript>
    <g:javascript contextPath="" src="${style}/publicationSubmission.js"/>
</body>
</html>
