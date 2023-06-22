<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 27/04/2022
  Time: 22:36
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Technical System Overview | BioModels</title>
</head>

<body>
    <h3><a href="${createLink(action: "info")}">System Info</a> |
    <a href="${createLink(action: "health")}">System Healthcheck</a> |
    <a href="${createLink(controller: "search", action: "check")}">Checked Indexed Data</a>
    </h3>
</body>
</html>
