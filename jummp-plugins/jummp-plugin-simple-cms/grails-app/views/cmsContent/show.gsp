<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 06/07/2022
  Time: 17:35
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Show content | BioModels</title>
    <g:render template="/templates/head" />
</head>

<body>
    <g:render template="/templates/contentShow" />
    <g:render template="/templates/actionButtons" model='[id: "${id}"]'/>
</body>
</html>
