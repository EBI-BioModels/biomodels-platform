<%--
  Created by IntelliJ IDEA.
  User: nvntung@gmail.com
  Date: 05/02/2024
  Time: 11:06
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="biomodels/main"/>
    <title>${titlePage}</title>
    <g:render template="/templates/head" />
</head>

<body>
    <h1>All Items</h1>
    <ul>
    <g:each in="${items}" var="item" status="i">
        <li><a href="${createLink(controller: "cmsContent", action: "show", id: item.id)}" target="_blank">
            ${item.title}</a></li>
    </g:each>
    </ul>
</body>
</html>
