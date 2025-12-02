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
<h4><a href="${createLink(action: "info")}">System Info</a><br/>
    <a href="${createLink(action: "health")}">System Healthcheck</a><br/>
    <a href="${createLink(controller: "search", action: "check")}">Checked Indexed Data</a><br/>
    <a href="${createLink(controller: "system", action: "checkDownUpLoadServer")}">Down and Up Server</a><br/>
    <a href="${createLink(controller: "system", action: "checkFileServiceServer")}">File Service Server</a>
</h4>
</body>
</html>
