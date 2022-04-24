<ul id="users-list">
    <g:each in="${users}" var="user">
    <%
        String label = user.label
        label = label.replaceAll(searchTerm, "<span style='background-color: #ffff00'>${searchTerm}</span>")
    %>
    <li onclick='selectFoundUser("${user.email}")'>${label}</li>
    </g:each>
</ul>
