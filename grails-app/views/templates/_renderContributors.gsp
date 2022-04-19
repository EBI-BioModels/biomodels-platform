<div class="row">
    <div class="small-12 medium-2 large-2 columns">
        <span class="overview-tab-attribute"><g:message code="model.model.authors"/></span>
    </div>
    <div class="small-12 medium-10 large-10 columns">
        <em>Submitter of the first revision: </em>${revision.model.submitter}
        <br/>
        <em>Submitter of this revision: </em>${revision.owner}
        <br/>
        <em>Curators:</em>
        <g:join in="${curators}"/>
        <br/>
        <em>Modellers:</em>
        <g:join in="${modellers}"/>
        <br/>
        <em>Others:</em>
        <g:join in="${others}"/>
        <br/>
    </div>
</div>
