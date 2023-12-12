<div class="row align-middle">
    <div class="small-12 medium-3 large-3 columns" id="external-resource-link-holder">
        <p><a href="${externalLink}" target="_blank" title="${linkTitle}">
            <img src="${externalResourceIcon}" width="80%"/></a></p>
    </div>
    <div class="small-12 medium-7 large-7 columns">
        <p class="ext-rsc-text">${shortDescription}</p>
    </div>
    <div class="small-12 medium-2 large-2 columns">
        <g:if test="${canRemoveGalaxyLink}">
        <p id="para-remove-galaxy-link"><button id="btn-remove-galaxy-link" class="button">X</button></p></g:if>
    </div>
</div>
