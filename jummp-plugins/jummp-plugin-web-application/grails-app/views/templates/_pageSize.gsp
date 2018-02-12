<ul>
    <g:each in="${resultOptions}">
        <li>
            <g:if test="${it == length}">
                ${it}
            </g:if>
            <g:else>
                <a href="${createLink(controller: 'search', action: action,
                    params: [query: query, offset: 0, numResults: it, sort: params.sort])}">
                    ${it}
                </a>
            </g:else>
        </li>
    </g:each>
    <li>Page size </li>
</ul>
