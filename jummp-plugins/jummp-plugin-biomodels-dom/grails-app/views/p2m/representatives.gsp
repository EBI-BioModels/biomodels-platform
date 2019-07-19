<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-07-01
  Time: 17:11
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>All representative models | BioModels</title>
</head>

<body>
<h2>All representative models as your request</h2>
    <ol>
        <g:each in="${representatives}" var="rep">
            <li>${rep.key} represented by ${rep.value}</li>
        </g:each>
    </ol>
</body>
</html>
