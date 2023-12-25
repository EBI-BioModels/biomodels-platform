<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 30/10/2018
  Time: 23:37
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <g:render template="/templates/head" plugin="jummp-plugin-web-application" />
    <title>Create A New Entry of Model of The Month ${entry?.formattedEntryDate}</title>
</head>

<body>
    <h2>Create a new entry of The Model of The Month: ${entry?.formattedEntryDate}</h2>
    <g:render template="/templates/modelOfTheMonthForm" />
</body>
</html>
