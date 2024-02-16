<%--
  Created by IntelliJ IDEA.
  User: Tung Nguyen
  Date: 02/12/2023
  Time: 19:50
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main"/>
    <title>${pageTitle} | BioModels</title>
</head>

<body>
    <g:if test="${parentAliasURI == "news"}">
    <h1>${title}</h1>
        <p><em>created on: ${createdOn} by ${createdBy}, last updated on: ${lastChangedOn} by ${lastChangedBy}</em></p>
    </g:if>
    ${content}
    <g:if test="${canUpdate}">
    <p><input type="button" class="button" value="Click here to edit"/></p>
    </g:if>
</body>
</html>
