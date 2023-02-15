<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 06/07/2020
  Time: 10:12
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>Reproducible simulation studies targeting COVID-19 | BioModels</title>
</head>

<body>
<g:if test="${content}">
    ${content}
</g:if>
<g:else>
    <h1>Under construction. Please come back later or contact us. Thanks!</h1>
</g:else>
</body>
</html>
<content tag="covid19">
    selected
</content>
