<%@
    page contentType="text/html;charset=UTF-8"
    expressionCodec = "none"
%>
<g:if test="${models}">
    <g:if test="${actionName == 'search'}">
        <div class="element">
            <h3>Make descriptive statistics</h3>
        </div>
    </g:if>
    <g:elseif test="${actionName == 'list'}">
        <g:if test="${history}">
            <div class="element" id="sidebar-element-last-accessed-models">
                <h3><g:message code="model.history.title"/></h3>
                <ul>
                    <g:each in="${history}">
                        <li><a href="${createLink(controller: "model", action: "show", id: it.publicationId ?: it.submissionId)}">${it.name}</a><br/>
                            <g:message code="model.history.submitter"/>${it.submitter}</li>
                    </g:each>
                </ul>
            </div>
        </g:if>
    <%--  GoTree code, disabled until it is useful again.
    <div class="element">
        <h2>Gene Ontology Tree</h2>
        <h3>Browse models using GO Tree</h3>
        <p>This is a tree view of the models in this Database based on <a href="http://www.geneontology.org/">Gene Ontology</a>.</p>
        <p><g:link controller="gotree">link</g:link></p>
    </div> --%>
    </g:elseif>
</g:if>
<g:else>
    <p></p>
</g:else>
