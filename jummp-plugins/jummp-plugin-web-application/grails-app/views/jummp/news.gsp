<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 24/09/2025
  Time: 10:13
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title><g:message code="${titleCode}" default="All News Articles"/> | BioModels</title>
    <style>
        .news-item-indent {
            margin-left: 10px;
        }
    </style>
</head>

<body>
    <h2>News</h2>
<g:each var="news" in="${newsEntries}">
<div class="faq-section-box">
    <h3 class="news-item-indent" style="margin-top: 5px">
        <a href="${serverURL}/content/news/${news.aliasURI}">${news.title}</a></h3>
    <div class="news-item-indent">
        ${news.description}
    </div>
    <div class="news-item-indent" style="margin-bottom: 5px">
        <a href="${serverURL}/content/news/${news.aliasURI}">Read more...</a></div>
</div>
</g:each>
</body>
</html>