<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 08/03/2018
  Time: 11:42
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title><g:if test="${curationNotesTC.updated}">Update</g:if><g:else>Add</g:else> curation notes</title>
    <g:javascript src="toastr.min.js" contextPath=""/>
    <link rel="stylesheet"
          href="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: 'css', file: 'toastr.min.css')}"/>
</head>

<body>
    <g:render template="/templates/curationNotesEditorHeadingLine"
              plugin="jummp-plugin-biomodels-dom"
              model="['curationNotesTC': curationNotesTC, 'model': id]" />
    <g:render template="/templates/curationNotesEditorForm"
        plugin="jummp-plugin-biomodels-dom"
        model="['curationNotesTC': curationNotesTC, 'curationImage': curationImage,
                'dateFormat': dateFormat, 'id': id]" />
</body>
</html>
