<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-05-17
  Time: 18:57
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="biomodels/main" />
    <title>Error 404 - Publication | BioModels</title>
</head>

<body>
    <h2>Error 404</h2>
    <h4>Publication (with id: ${params.id}) cannot be found in our database.</h4>
    <p><a href="${createLink(controller: "publication", action: "index")}">Back to the publication list page</a></p>
</body>
</html>
