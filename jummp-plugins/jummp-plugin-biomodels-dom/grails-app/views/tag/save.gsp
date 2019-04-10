<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 2019-04-07
  Time: 10:21
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <title>${title} | BioModels</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
</head>

<body>
    <h2>Message</h2>
    <p>${message}</p>
    <p><a href="${g.createLink(controller: "tag", action: "index")}">Back to list of all tags</a> </p>
</body>
</html>
