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
    <g:render template="/templates/head" plugin="jummp-plugin-web-application" />
    <script type="javascript">
        toastr.options = {
            "progressBar": true
        }
    </script>
    <title>Show Entry of Model of The Month ${entry?.formattedEntryDate}</title>
</head>

<body>
    <g:if test="${errMsg}">
        <h2 style="color: orange">The errors have been reported!</h2>
        <h3>${errMsg}</h3>
    </g:if>
    <g:else>
        <g:if test="${entry}">
            <h2>Entry of The Model of The Month: ${entry?.formattedEntryDate}</h2>
            <g:render template="/templates/modelOfTheMonthForm" />
        </g:if>
        <g:else>
            <h2>Could not find any entry of Model Of The Month associated with the identifier ${params.id}</h2>
        </g:else>
    </g:else>
</body>
</html>
