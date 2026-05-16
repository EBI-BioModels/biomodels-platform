<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 07/04/2021
  Time: 14:48
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${layout}">
    <title>${title}</title>
</head>

<body>
    <div class="row">
        <div class="columns small-12 medium-10 large-8">
            <h2>Specific Curation Projects</h2>
            <ul>
                <li>Documentation</li>
                <li><a href="${g.createLink(controller: 'curation', action: 'fbc')}">FBC</a></li>
            </ul>
        </div>
    </div>
</body>
</html>
<content tag="curationpage">
    selected
</content>
