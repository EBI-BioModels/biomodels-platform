<%--
  Created by IntelliJ IDEA.
  Author: Tung Nguyen <tnguyen@ebi.ac.uk>
  Date: 18/10/17
  Time: 13:23
--%>

<%@ page import="net.biomodels.jummp.deployment.biomodels.CurationNotesCategory" contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.deployment.biomodels.CurationNotesTransportCommand" contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.deployment.biomodels.CurationNotes" contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.model.Model" contentType="text/html;charset=UTF-8" %>
<%@ page import="groovy.json.JsonOutput" contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.Date" contentType="text/html;charset=UTF-8" %>
<%@ page import="java.text.SimpleDateFormat" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>Update simulation results</title>
</head>

<body>
    <div class="row">
        <h3>Update curation notes of the model
            <a href="${createLink(controller: "model", action: "show", id: id)}"
               title="Back to the model display page">[${modelName}]</a></h3>
        <g:render template="/templates/curationNotesEditor"
                  plugin="jummp-plugin-biomodels-dom"
                  model="['curationNotesTC': curationNotesTC,
                          'curationImage': curationImage,
                          'dateFormat': dateFormat]" />
    </div>
</body>
</html>
