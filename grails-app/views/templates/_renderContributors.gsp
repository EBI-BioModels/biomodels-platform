<div class="row">
    <div class="small-12 medium-2 large-2 columns">
        <span class="overview-tab-attribute"><g:message code="model.model.authors"/></span>
    </div>
    <div class="small-12 medium-10 large-10 columns">
        <em>Submitter of the first revision: </em>${revision.model.submitter}
        <br/>
        <em>Submitter of this revision: </em>${revision.owner}

        <g:if test="${curators}"><br/><em>Curators:</em>
        <g:join in="${curators}"/></g:if>

        <g:if test="${modellers}">
        <br/><em>Modellers:</em>
        <g:join in="${modellers}"/></g:if>

        <g:if test="${others}"><br/>
        <em>Others:</em>
        <g:join in="${others}"/></g:if>
    </div>
</div>
