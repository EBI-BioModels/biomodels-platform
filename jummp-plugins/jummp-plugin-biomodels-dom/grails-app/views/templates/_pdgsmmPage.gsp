
<p>
    This section, called patient-derived genome scale metabolic models (PDGSMM), hosts models of
    metabolic pathways that are specific to individual patients. Models from this branch were built
    using gene expression data from tissue samples extracted from patients. To know more about the
    current available models in this section, please read the dedicated
    <a href="${grailsApplication.config.grails.serverURL}/content/model-of-the-month?year=2017&month=08"
       title="Model of the Month entry for August 2018">Model of the Month article</a>.
</p>

<p>If you have generated similar models and wish to submit them to BioModels, please
    <a href="contact">contact us</a>.</p>

<div id="categories">
    <biomd:renderPdgsmmDiseasesInTwoColumnsLayout
        categories="${categories}" diseases="${diseases}" />
</div>
