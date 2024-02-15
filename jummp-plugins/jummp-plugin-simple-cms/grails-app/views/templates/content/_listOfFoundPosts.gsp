<%@ page import="java.util.regex.Pattern" %>
<ul id="posts-list">
    <g:each in="${posts}" var="post">
        <%
            String label = post.label
            label = label.replaceAll("(?i)"+ Pattern.quote(searchTerm), "<span style='background-color: #ffff00'>${searchTerm}</span>")
        %>
        <li onclick='selectFoundPost("${post.aliasURI}")'>${label}</li>
    </g:each>
</ul>
