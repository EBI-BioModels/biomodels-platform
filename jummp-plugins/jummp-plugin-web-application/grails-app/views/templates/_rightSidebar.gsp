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
        <h4>Shortcuts to browse models</h4>
        <h5><g:link controller="parameterSearch">Parameters Search</g:link></h5>
        <p>BioModels Parameters is a resource that facilitates easy search and retrieval of parameter values used in
        the SBML models stored in the BioModels repository.</p>
        <h5><g:link controller="goChart">Using GO Chart</g:link></h5>
        <p>This is a chart view of the models in this Database based on <a href="http://www.geneontology.org/">Gene Ontology</a>.</p>

        <h5><g:link controller="feature" action="agedbrain">Neurodegeneration models</g:link></h5>
        <p>Mechanistic models describing neurodegenerative disease processes
        </p>
        <h5><g:link controller="p2m">Path2Models</g:link></h5>
        <p>The approximately 140,000 models in the Path2Models project are now grouped taxonomically into 812 bundles,
        typically one per genus.</p>
        <h5><g:link controller="pdgsmm">Patient-derived genome metabolic models</g:link></h5>
        <p>This section hosts models of metabolic pathways that are specific to individual patients</p>
    </div>
</g:if>
<g:else>
    <p></p>
</g:else>
