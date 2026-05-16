<div class="row"><div class="columns large-12 medium-12 small-12">
    <g:if test="${parentAliasURI == "news"}">
        <h1>${title}</h1>
        <p><em>created on: ${createdOn} by ${createdBy}, last updated on: ${lastChangedOn} by ${lastChangedBy}</em></p>
    </g:if>
    ${content}
</div></div>
