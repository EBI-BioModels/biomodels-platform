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
    <g:javascript src="toastr.min.js" contextPath=""/>
    <link rel="stylesheet"
          href="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: 'css', file: 'toastr.min.css')}"/>
    <title>Create A New Entry of Model of The Month ${entry?.formattedEntryDate}</title>
</head>

<body>
    <g:render template="/templates/modelOfTheMonthForm" />
</body>
</html>
