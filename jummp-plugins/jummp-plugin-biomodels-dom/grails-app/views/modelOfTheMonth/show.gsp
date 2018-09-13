<%--
  Created by IntelliJ IDEA.
  User: tnguyen@ebi.ac.uk
  Date: 05/09/18
  Time: 17:15
--%>

<%@ page import="net.biomodels.jummp.deployment.biomodels.ModelOfTheMonthTransportCommand"
    contentType="text/html;charset=UTF-8" %>

<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <g:javascript src="toastr.min.js" contextPath=""/>
    <link rel="stylesheet"
          href="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: 'css', file: 'toastr.min.css')}"/>
    <title>Show Entry of Model of The Month ${entry?.date}</title>
</head>

<body>
    <g:if test="${entry}">
    <h2>Entry of The Model of The Month: ${entry?.date}</h2>
    <div id="txtStatus" style="color: #ED0000; font-weight: 500; font-size: larger"></div>
    <g:render template="/templates/modelOfTheMonthForm" />
    </g:if>
    <g:else>
        <h2>Could not find any entry of Model Of The Month associated with the identifier ${params.id}</h2>
    </g:else>
</body>
</html>
