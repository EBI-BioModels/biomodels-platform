<%@ page import="net.biomodels.jummp.core.model.ModelState"%>

<div class="row">
    <div class="message" style="display: block"></div>

    <div style="float:left" class="columns medium-9 large-9 small-12">
        <h2>${revision.name}</h2>
        <biomd:renderModelOfMonth modelId="${revision.model.id}" />
    </div>

    <div style="float:right; text-align: right" class="columns medium-3 large-3 small-12">
        <h2>
        <g:if test="${!flags.empty}">
            <biomd:renderModelFlags flags="${flags}"/>
        </g:if>
        <g:if test="${revision.qcInfo != null}">
            <jummp:renderStarLevels flag="${revision.qcInfo.flag}" />
        </g:if>
            <a href="javascript:void(0)" onclick="linkServeOmex()"
               title="Click here to download OMEX format of this model">
                <i class="icon icon-common icon-download"></i></a>
            <a href="${g.createLink(controller: "model", action: "metadatardf", id: revision.identifier())}"
               target="_blank" title="Click here to view the metadata of this model in RDF/XML format">
                <i class="icon icon-fileformats icon-RDF_XML"></i></a>
            <a href="${g.createLink(controller: 'model', action: 'show', id: revision.identifier(), params: ['format': 'json'])}"
               target="_blank" title="Click here to view JSON format of this model">
                <i class="icon icon-fileformats icon-JSON"></i></a>
            <a href="${g.createLink(controller: 'model', action: 'show', id: revision.identifier(), params: ['format': 'xml'])}"
               target="_blank" title="Click here to view XML format of this model">
                <i class="icon icon-fileformats icon-XML"></i></a>
            <g:if test="${revision.model.isMetadataSubmission}">
                <i class="icon icon-common icon-code" title="This is a metadata-only submission"></i>
                <span>&nbsp;</span>
            </g:if>
            <g:if test="${revision.state==ModelState.PUBLISHED}">
                <i class="icon icon-common icon-unlock" title="This version of the model is public"></i>
            </g:if>
            <g:else>
                <i class="icon icon-common icon-lock" title="This version of the model is unpublished"></i>
            </g:else>
        </h2>
    </div>


</div>
<g:if test="${reactomeIds}">
<div class="row">
    <div class="columns">
    <!-- Render the selection box to choose Reactions which are visualised with Reactome Pathways Viewer -->
    <div id="reactome-dialog" title="Reactome pathway">
        <div id="diagramHolder"></div>
    </div>
    <div style="margin-right: 50%;">
        <g:select name="reactome_pathways"
                  id="opener"
                  onchange="setReactomeId(this.value);"
                  from="${reactomeIds}"
                  noSelection="['':'Choose Reactome Pathway']"
                  optionKey = "${{null != it && !((String)it).isEmpty()?((String)it).split('\\|')[1]:((String)it).split('\\|')[0]}}"
                  optionValue="${{((String)it).split('\\|')[0]}}" />
    </div>

    <script src="${resource(contextPath: serverURL, dir: 'js/biomodels', file: 'reactome.diagram.viewer.js')}"></script>
    </div>
</div>
</g:if>
