<div class="small-12 medium-6 large-6 columns">
    <g:each in="${diseases}" var="disease">
        <%
            def firstModel = disease.value.first()
            def modelUrl = firstModel.searchableLink ?:
                g.createLink(controller: 'model', action: 'show', id: firstModel.modelIdentifier)
        %>
        <a href="${modelUrl}" target="_blank">
            ${disease.key}
        </a>(${disease.value.size()})<br/>
    </g:each>
</div>
