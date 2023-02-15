<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 07/04/2021
  Time: 14:53
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${layout}">
    <title>${title}</title>
</head>

<body>
<g:if test="${content}">
    ${content}
</g:if>
<g:else>
    <h1>Under construction. Please come back later or contact us. Thanks!</h1>
</g:else>
</body>
</html>
<content tag="fbcpage">
    selected
</content>
