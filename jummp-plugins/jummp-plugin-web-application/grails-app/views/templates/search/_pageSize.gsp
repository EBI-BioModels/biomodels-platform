<p style="float: right">
    <strong>Page size &nbsp;</strong>[
    <g:each in="${resultOptions}" status="i" var="op">
        <g:if test="${op == length}">
            <span style="font-style: italic; color: blue">${op}</span>
        </g:if>
        <g:else>
            <%
                Map customParams = [offset: params.offset, numResults: op, sort: params.sort]
                if (query) {
                    customParams['query'] = query
                }
                if (action == 'search') {
                    customParams['domain'] = domain
                }
            %>
            <a href="${createLink(controller: 'search', action: action,
                params: customParams)}">
                ${op}
            </a>
        </g:else>
        <g:if test="${i < resultOptions.size()-1}">|</g:if>
    </g:each>
    ]
</p>
