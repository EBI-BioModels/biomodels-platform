<div id="hp-features-data-submission-update" class="large-3 medium-3 small-12 columns">
    <a href="${createLink(controller: "model", action: "create")}"
       title="Go to submission or update flow">
        <span class="size-400 icon icon-common icon-spacer hide-for-small-only" data-icon="&#xf093;"></span></a>
    <h4><a href="${createLink(controller: "model", action: "create")}" title="Go to submission or update flow">
        Submission/Update</a></h4>
    <h5>3,456</h5>
</div>
<div id="hp-features-browse-models" class="large-6 medium-6 small-12 columns">
    <div class="row">
        <div class="large-4 medium-4 small-12 columns">
            <a href="${createLink(controller: "search", action: "search", params:
                [query: "*:*&domain=biomodels"])}"
               title="Browse all literature models">
                <span class="size-400 icon icon-common icon-resource icon-spacer hide-for-small-only"></span></a>
            <h4><a href="${createLink(controller: "search", action: "search", params: [query: "*:*&domain=biomodels"])}"
                   title="Browse all literature models">Individual models</a></h4>
            <h5>2,345</h5>
        </div>
        <div class="large-4 medium-4 small-12 columns">
            <a href="${createLink(controller: "search", action: "search", params:
                [query: "*:*&domain=biomodels_autogen"])}"
               title="Browse all auto generated models">
                <span class="size-400 icon icon-common icon-clone icon-spacer hide-for-small-only"></span></a>
            <h4><a href="${createLink(controller: "search", action: "search", params:
                [query: "*:*&domain=biomodels_autogen"])}"
                   title="Browse all auto generated models">Auto gen</a></h4>
            <h5>833</h5>
        </div>
        <div class="large-4 medium-4 small-12 columns">
            <a href="${createLink(controller: "goChart", action: "index")}"
               title="Browse all literature models using GO chart">
                <span class="size-400 icon icon-conceptual icon-ontology icon-spacer hide-for-small-only"></span></a>
            <h4><a href="${createLink(controller: "goChart", action: "index")}"
                   title="Browse all literature models using GO chart">GO Chart</a></h4>
        </div>
    </div>
</div>
<div id="hp-features-parameters-search" class="large-3 medium-3 small-12 columns">
    <a href="${createLink(controller: "parameterSearch", action: "index")}"
       title="Search parameters interest"><span
        class="size-400 icon icon-common icon-sliders-h icon-spacer hide-for-small-only"></span></a>
    <h4><a href="${createLink(controller: "parameterSearch", action: "index")}"
           title="Search parameters interest">Parameters search</a></h4>
    <h5>1,657</h5>
</div>
