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
    <title>Missing Path2Models | BioModels</title></head>

<body>
<h2>All missing models</h2>
<ol>
    <g:each in="${missing}" var="m">
        <li>${m}</li>
    </g:each>
</ol>
</body>
</html>
