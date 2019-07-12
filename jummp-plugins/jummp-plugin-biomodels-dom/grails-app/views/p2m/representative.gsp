<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-06-28
  Time: 13:24
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>Representative Path2Models | BioModels</title></head>

<body>
<h2>The representative model info:</h2>
<ul>
    <li>Requested model identifier: ${requestedModelId}</li>
    <li>Representative model identifier: ${representativeModelId}</li>
</ul>
</body>
</html>
