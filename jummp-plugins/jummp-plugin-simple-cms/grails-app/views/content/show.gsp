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
    <g:render template="/templates/content/cntBody" />
    <g:if test="${canUpdate}">
    <g:render template="/templates/actionButtons" />
    </g:if>
</body>
</html>
