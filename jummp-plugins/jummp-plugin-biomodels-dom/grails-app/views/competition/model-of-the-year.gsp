<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 27/06/2022
  Time: 11:22
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>${titlePage}</title>
</head>

<body>
    ${content}
    <g:render template="/templates/actionButtons" plugin="jummp-plugin-biomodels-dom" />
</body>
</html>
