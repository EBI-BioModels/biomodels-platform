<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-07-01
  Time: 14:14
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>Missing required parameter | BioModels</title>
</head>

<body>
    <h2>Error:</h2>
<ul>
    <li>Status: ${status}</li>
    <li>Description: ${message}</li>
</ul>
</body>
</html>
