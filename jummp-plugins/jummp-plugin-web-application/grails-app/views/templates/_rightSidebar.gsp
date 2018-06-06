<%@
    page contentType="text/html;charset=UTF-8"
    expressionCodec = "none"
%>
<g:if test="${models}">
    <g:if test="${actionName == 'search'}">
        <div class="element" id="rightSidebar">
            <h4>Browse Path2Models content</h4>
            <p>Models from this branch are classified in 3 distinct categories:</p>
            <ul>
                <li><a href="//www.ebi.ac.uk/biomodels-main/path2models?cat=metabolic">metabolic models</a></li>
                <li><a href="//www.ebi.ac.uk/biomodels-main/path2models?cat=non-metabolic">non-metabolic models</a></li>
                <li><a href="//www.ebi.ac.uk/biomodels-main/path2models?cat=genome-scale">whole genome metabolism models</a></li>
            </ul>
            <p>One can also browse those models by organism:</p>
            <ul><li><a href="//www.ebi.ac.uk/biomodels-main/path2models?cat=organism">list of all organisms</a></li></ul>

            <p>Learn more about <a href="//www.ebi.ac.uk/biomodels-main/path2models">Path2Models</a></p>
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
        <g:link controller="goChart"><h3>Browse models using GO Chart</h3></g:link>
        <p>This is a chart view of the models in this Database based on <a href="http://www.geneontology.org/">Gene Ontology</a>.</p>
    </div>
</g:if>
<g:else>
    <p></p>
</g:else>
