<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 11/06/2020
  Time: 22:08
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${layout}">
    <title>${title}</title>
    <style type="text/css">
    .accordion-title {
        font-size: xx-large;
    }
    .accordion-content p {
        font-size: large;
        color: darkorange;
    }
    </style>
</head>

<body>
<div class="row">
    <div class="small-12 medium-12 large-12 columns">
        <h1>Curation Dashboard</h1>
        <ul class="accordion"
            data-accordion data-multi-expand="true"
            data-allow-all-closed="true">
            <g:render template="/templates/admin/board-curation" plugin="jummp-plugin-web-application"/>
        </ul>
    </div>
</div>
</body>
</html>
<content tag="curatorboard">
    selected
</content>
