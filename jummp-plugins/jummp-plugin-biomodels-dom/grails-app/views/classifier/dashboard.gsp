<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 30/05/2022
  Time: 20:49
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Classifier Service | BioModels</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="layout" content="${session['branding.style']}/main" />
</head>

<body>
    <h1>Classifier Service Dashboard</h1>
<p>
    <a href="${createLink(controller: "classifierConfigure", action: "index")}" target="_blank">Index</a> |
    <a href="${createLink(controller: "classifierConfigure", action: "classifier")}" target="_blank">Classifier</a>
</p>
</body>
</html>
