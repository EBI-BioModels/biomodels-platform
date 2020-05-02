<ul style="list-style-type: none; list-style-position: inside; padding: 0; margin-left: 0">
    <g:each in="${models}" var="model">
        <li style="text-indent: -1.5em; padding-left: 3em">
            <!-- show icon as bullet -->
            <%
                String modelId = model.key
                String modelURI = g.createLink(controller: 'model', id: modelId, action: 'show')
                String submitter = model.value.submitter
                // using createLink taglib encodes special characters twice
                /*String searchBySubmitterLink = g.createLink(controller: 'search', action: 'search',
                    params: [query: "domain=biomodels_all&*:* AND submitter:\"${submitter}\""])*/
                String searchBySubmitterLink =
                    "${serverURL}/search?query=*%3A*+AND+submitter%3A'${submitter}'&domain=biomodels_all"
            %>
            <span class='icon icon-functional' data-icon='U'>&nbsp;</span>
            <a href='${modelURI}'>${model.value.title}</a><br/>
            <span>Submitter: <a href="${searchBySubmitterLink}" target="_blank">${model.value.submitter}</a> |
            Published date: ${model.value.lastPublished}
            &nbsp;| Publication: ${model.value.pubTitle}, ${model.value.pubJournal} (${model.value.pubYear})</span>
        </li>
    </g:each>
</ul>
