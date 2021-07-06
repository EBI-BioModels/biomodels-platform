<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-05-17
  Time: 20:09
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.model.PublicationLinkProvider; grails.converters.JSON;" %>
<%
    def style = grailsApplication.config.jummp.branding.style
    def serverUrl = grailsApplication.config.grails.serverURL
%>
<html>
<head>
    <meta name="layout" content="biomodels/main" />
    <title>${title}</title>
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "/css/${style}", file: 'publicationPageStyle.css')}" />
    <link rel="stylesheet"
          href="${resource(contextPath: "${serverUrl}", dir: "css", file: 'toastr.min.css')}"/>

    <g:javascript>
        let authorMap = {"authors": []};
        let authorList = [];
    </g:javascript>
    <g:javascript contextPath="" src="${style}/publicationSubmission.js"/>
    <g:javascript src="toastr.min.js" contextPath=""/>
    <g:javascript src="helpers.js" contextPath=""/>
</head>

<body>
    <div class="row">
        <h2>Add a new publication</h2>

        <g:render template="/templates/publication/selectPublicationSource"
                  plugin="jummp-plugin-web-application"/>
        <div id="publicationForm">
            <g:render template="/templates/publication/publicationEditableElements"
                      plugin="jummp-plugin-web-application"
                      model="['publication': publication, 'authorListContainerSize': authorListContainerSize]"/>
            <g:render template="/templates/publication/publicationButtonsForm"
                      plugin="jummp-plugin-web-application"/>
        </div>
    </div>

    <g:render template="/templates/publication/loadCommonActions"
              plugin="jummp-plugin-web-application"/>
</body>
</html>
