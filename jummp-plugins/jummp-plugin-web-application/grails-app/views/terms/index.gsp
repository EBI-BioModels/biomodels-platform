<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 01/10/2024
  Time: 15:54
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main">
    <title>Terminology | BioModels</title>
    <style>
        .terms dt {
            font-weight: bold !important;
        }

        dl,
        dd {
            font-size: 0.9rem;
        }

        dd {
            margin-bottom: 1em;
        }
    </style>
</head>

<body>
<div class="row">
<div class="columns small-12 medium-12 large-12 terms">
    ${content}
</div>
</div>
</body>
</html>
