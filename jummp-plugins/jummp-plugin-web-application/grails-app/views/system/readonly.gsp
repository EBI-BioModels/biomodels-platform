<%--
  Created by IntelliJ IDEA.
  User: nvntung@gmail.com
  Date: 07/12/2025
  Time: 23:52
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>Readonly | BioModels</title>
</head>

<body>
    <h2>Read-Only Mode</h2>
<g:if test="${context}">
    <div>
        ${context}
    </div>
</g:if>
<g:else>
    <p>This instance is no longer receiving new submission and any updates.
    Please use our new site at <a href="https://biomodels.org">https://biomodels.org</a>. Thank you for your
    understanding.</p>
</g:else>
</body>
</html>
