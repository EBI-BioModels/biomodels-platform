<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 20/05/2020
  Time: 13:51
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${layout}"/>
    <title>${title}</title>
</head>

<body>
    <div class="row">
        <div class="small-12 medium-12 large-12 columns">
            <h1>Report</h1>
            <h3>${message}</h3>
            <p><a href="${createLink(controller: "homePage", action: "index")}">Back Admin Board</a></p>
        </div>
    </div>
</body>
</html>
