<div class="small-12 medium-6 large-6 columns">
    <g:each in="${diseases}" var="disease">
        <a href="${g.createLink(controller: 'model', action: 'show', id: "${disease.value.first().modelIdentifier}")}"
           target="_blank">
            ${disease.key}
        </a>(${disease.value.size()})<br/>
    </g:each>
</div>
