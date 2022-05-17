<div class="small-12 medium-6 large-6 columns">
    <g:if test="${curaRec.curationImage}">
        <img src="data:image/jpeg;base64,${curaRec.curationImage}"
             title="Click on the thumbnail to view the result(s)" />
    </g:if>
    <g:else>
        <img src="${grailsApplication.config.grails.serverURL}/images/biomodels/simulation-result-unavailable.png"
             title="The curation images are not available" />
    </g:else>
</div>
<div class="small-12 medium-6 large-6 columns">
    <strong>Curator's comment:</strong><br/>
    <em>(added: ${curaRec.dateAdded}, updated: ${curaRec.lastModified})</em><br/>
    <div style="white-space: pre-line">
    ${curaRec.comment}
    </div>
    <g:if test="${curaRec.hasCuratorRole && curaRec.internalComment}">
        <p><strong>Notes: </strong>${curaRec.internalComment}</p>
    </g:if>
</div>
