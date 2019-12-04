<div class="reveal" id="domainSwitcherExplanationBox" data-reveal>
    <h3 id="messageTitle"
        style="border-bottom: 1px solid grey">What are BioModels domains?</h3>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>

    <div class="explanation-panel" id="domainSwitcherExplanationPanel" data-toggler=".is-active">
        <p>This drop down box allows you to choose a pool (aka. domain) from where models are searched for.
        At the current stage, all auto generated models as well as ones belonging to the large submissions have been
        indexed to the separate domain called <strong>BioModels Autogen</strong> which currently consists of
        <g:link mapping="path2models" target="_blank">Path2Models</g:link> and
            <a href="${g.createLink(controller: 'pdgsmm', action: 'index')}" target="_blank">Patient-derived genome scale metabolic
        models (PDGSMM)</a>. You can consult more
        information about these submissions in BioModels by following the links we have pointed out earlier.</p>

        <p>By default, <strong>BioModels</strong> is added to your query if you do not specify any domain from
        the box . This domain contains all other models, except for auto generated models and ones deposited
        alongside large scale submissions. If you do not want to ignore them, please choose
            <strong>BioModels All</strong>.</p>

        <ul>
            <li>Search for the phrase "Metabolism" in the name field of models in BioModels Autogen, here is the
                query <code>name:Metabolism&domain=biomodels_autogen</code>. The sharable search link looks like<br/>
<code>${grailsApplication.config.grails.serverURL}/search?query=name:Metabolism&domain=biomodels_autogen</code>.
            Click <a
                href="${grailsApplication.config.grails.serverURL}/search?query=name:Metabolism&domain=biomodels_autogen" target="_blank">here</a> to see the search results for this query.
            <li><code>domain</code> field is either of the three following values: biomodels,
        biomodels_autogen, and biomodels_all.</li>
        </ul>

        <p><a href="https://www.ebi.ac.uk/biomodels-static/jummp-biomodels-help/model_search.html"
              target="_blank">Learn more about searching models</a></p>
    </div>
</div>
