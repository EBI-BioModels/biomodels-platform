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
        <p><em>Created on: ${createdOn}, Last Updated on: ${lastChangedOn}</em></p>
    </g:if>
    ${content}
</body>
</html>
