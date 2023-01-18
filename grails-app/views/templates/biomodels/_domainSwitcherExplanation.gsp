<div class="reveal" id="domainSwitcherExplanationBox" data-reveal>
    <h3 id="messageTitle"
        style="border-bottom: 1px solid grey">What are BioModels domains?</h3>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>

    <div class="explanation-panel" id="domainSwitcherExplanationPanel" data-toggler=".is-active">
        <p>This drop down box allows you to filter which models appear in the search results:
        <ul>
            <li><strong><g:message code="net.biomodels.jummp.domain.name.BioModelsAutogen"/></strong>: large-scale or automatically-generated submissions such as <g:link mapping="path2models" target="_blank">Path2Models</g:link> or
            <a href="${g.createLink(controller: 'pdgsmm', action: 'index')}" target="_blank">Patient-derived genome scale metabolic models (PDGSMM)</a> </li>
            <li><strong><g:message code="net.biomodels.jummp.domain.name.BioModels"/></strong>: models described in the literature that have been submitted individually to BioModels</li>
            <li><strong><g:message code="net.biomodels.jummp.domain.name.BioModelsAll"/></strong>: the whole BioModels content</li>
        </ul>
        </p>
        <p>The default search domain is <strong><g:message code="net.biomodels.jummp.domain.name.BioModels"/></strong>.</p>

        <p><a href="${grailsApplication.config.jummp.context.help.root}/model_search.html"
              target="_blank">Learn more about searching models</a></p>
    </div>
</div>
