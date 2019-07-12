<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-06-20
  Time: 16:38
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>${title} | BioModels</title>
</head>

<body>
    <g:render template="/templates/p2mProjectPage" plugin="jummp-plugin-biomodels-dom"/>
</body>
</html>
