<%@
page contentType="text/html;charset=UTF-8"
    expressionCodec = "none"
%>
<g:if test="${models}">
    <div class="sidebar">
        <h4>Quick links</h4>
        <sec:ifLoggedIn>
            <h5><a href="${createLink(controller: "search", action: "archive")}">
                Browse Archived Models</a></h5>
        </sec:ifLoggedIn>
        <p>This link allows to access deleted models.</p>
        <g:if test="${history}">
            <div class="element" id="sidebar-element-last-accessed-models">
                <h5><g:message code="model.history.title"/></h5>
                <ul>
                    <g:each in="${history}">
                        <li><a href="${createLink(controller: "model", action: "show",
                            id: it.publicationId ?: it.submissionId)}">${it.name}</a><br/>
                            <g:message code="model.history.submitter"/>${it.submitter}</li>
                    </g:each>
                </ul>
            </div>
        </g:if>
    </div>
</g:if>
<g:else>
    <p></p>
</g:else>
