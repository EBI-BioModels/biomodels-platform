<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 26/11/2018
  Time: 16:08
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title><g:message code="error.400.title"/> | BioModels</title>
</head>

<body>
    <h2><g:message code="error.400.title"/></h2>
    <p><g:message code="error.400.explanation"/>
    <p>${errorDescription}</p>
</body>
</html>
