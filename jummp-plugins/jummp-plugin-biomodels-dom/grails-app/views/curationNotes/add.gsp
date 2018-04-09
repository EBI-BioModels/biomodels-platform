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
    <title>Add simulation results</title>
</head>

<body>
    <div class="row">
        <h3>Add curation notes of the model
            <a href="${createLink(controller: "model", action: "show", id: id)}"
               title="Back to the model display page">[${modelName}]</a></h3>
        <g:render template="/templates/curationNotesEditor"
            plugin="jummp-plugin-biomodels-dom"
            model="['curationNotesTC': curationNotesTC, 'curationImage': curationImage,
                    'dateFormat': dateFormat, 'id': id]" />
    </div>
</body>
</html>
