<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 05/02/2022
  Time: 12:46
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>Create a reviewer account | BioModels</title>
</head>

<body>
<h1>Create a reviewer account</h1>
<h3>Below is the information of the reviewer account for your model ${modelId}.</h3>
<div id="reviewer-account-info" style="background-color: #A5A5C7">
    ${message}
</div>
</body>
</html>
