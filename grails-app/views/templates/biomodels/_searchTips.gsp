<div class="reveal" id="searchTipsBox" data-reveal>
    <h3 id="messageTitle"
        style="border-bottom: 1px solid grey">Search Tips/Tricks</h3>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>

    <div class="search-tips-panel" id="searchTips_panel" data-toggler=".is-active">
        <ul>
            <li>Search is based on exact word matches. Make sure your <strong>spelling</strong> is correct.
            Example: the misspelled terms like <g:link
                controller="search" action="search" params="${[query: "CelCycle"]}" class="secondary label"
                title="Search by CelCycle term" target="_blank">CelCycle</g:link> (missing "l") will not find any results.</li>
            <li>Search for the phrase "Glucose" in the name field. Example: <g:link
                controller="search" action="search" params="${[query: "name:Glucose"]}" class="secondary label"
                title="Search by Glucose term in the 'name' field" target="_blank">name:"Glucose"</g:link></li>
            <li>Matches all models having model elements that were annotated with Gene Ontology term cell growth. Example:
                <g:link
                    controller="search" action="search" params="${[query: "GO:GO:0016049"]}" class="secondary label"
                    title="Search by Gene Ontology term cell growth" target="_blank">GO:GO:0016049</g:link></li>
        </ul>
        <p><a href="https://www.ebi.ac.uk/biomodels-static/jummp-biomodels-help/model_search.html"
              target="_blank">Learn more about searching models</a></p>
    </div>
</div>
