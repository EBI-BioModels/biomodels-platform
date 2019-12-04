<%@
    page contentType="text/html;charset=UTF-8"
    expressionCodec = "none"
%>
<g:if test="${models}">
    <g:if test="${actionName == 'search'}">
        <div class="element" id="rightSidebar">
        </div>
    </g:if>
    <g:elseif test="${actionName == 'list'}">
        <g:if test="${history}">
            <div class="element" id="sidebar-element-last-accessed-models">
                <h4><g:message code="model.history.title"/></h4>
                <ul>
                    <g:each in="${history}">
                        <li><a href="${createLink(controller: "model", action: "show",
                            id: it.publicationId ?: it.submissionId)}">${it.name}</a><br/>
                            <g:message code="model.history.submitter"/>${it.submitter}</li>
                    </g:each>
                </ul>
            </div>
        </g:if>
    </g:elseif>
    <div class="element">
        <g:link controller="goChart"><h4>Browse models using GO Chart</h4></g:link>
        <p>This is a chart view of the models in this Database based on <a href="http://www.geneontology.org/">Gene Ontology</a>.</p>
    </div>
</g:if>
<g:else>
    <p></p>
</g:else>
