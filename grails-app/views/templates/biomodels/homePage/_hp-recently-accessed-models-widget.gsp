<ul style="list-style-type: none; list-style-position: inside; padding: 0; margin-left: 0">
    <g:each in="${models}" var="model">
        <%
            String modelId = model.key
            String modelURI = g.createLink(controller: 'model', id: modelId, action: 'show')
            String submitter = model.value.submitter
            String searchBySubmitterLink =
                "${serverURL}/search?query=*%3A*+AND+submitter%3A'${submitter}'&domain=biomodels_all"
        %>
        <li style="text-indent: -1.5em; padding-left: 1.5em">
            <i class="icon icon-common icon-unlock">&nbsp;</i>
            <a href='${modelURI}'>${model.value.title}</a><br/>
            <div class="hide-for-small-only" style="text-indent: 0.0em; font-size: 90%">
                Submitter: <a href="${searchBySubmitterLink}" target="_blank">${submitter}</a>&nbsp;|
                Format: ${model.value.format}&nbsp;|
                Accessed: ${model.value.accessCount} time(s)</div>
            <div class="show-for-large hide-for-medium-only" style="text-indent: 0.0em; font-size: 90%">
                Submitted: ${model.value.submittedDate}&nbsp;|
                Published: ${model.value.publishedDate ?: 'N/A'}</div>
        </li>
    </g:each>
</ul>
