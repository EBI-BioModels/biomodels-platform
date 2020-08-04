<ul>
    <g:each in="${resultOptions}">
        <li>
            <g:if test="${it == length}">
                ${it}
            </g:if>
            <g:else>
                <%
                    Map customParams = [offset: 0, numResults: it, sort: params.sort.encodeAsHTML()]
                    if (action == 'search') {
                        customParams['query'] = query
                        customParams['domain'] = domain
                    }
                %>
                <a href="${createLink(controller: 'search', action: action,
                    params: customParams)}">
                    ${it}
                </a>
            </g:else>
        </li>
    </g:each>
    <li>Page size </li>
</ul>
