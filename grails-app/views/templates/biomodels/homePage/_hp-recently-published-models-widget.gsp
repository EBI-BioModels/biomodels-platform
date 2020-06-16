<div class="homepage_info_box"><h3>Recently published</h3></div>
<div class="widget-body-text" style="text-align: left">
<ul style="list-style-type: none; list-style-position: inside; padding: 0; margin-left: 0">
    <g:each in="${models}" var="model">
        <li style="text-indent: -1.5em; padding-left: 1.5em">
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
            <i class="icon icon-common icon-unlock">&nbsp;</i>
            <a href='${modelURI}'>${model.value.title}</a><br/>
            <div class="hide-for-small-only" style="text-indent: 0.0em; font-size: 90%">
                Submitter: <a href="${searchBySubmitterLink}" target="_blank">${model.value.submitter}</a>&nbsp;|
                Published date: ${model.value.lastPublished} </div>
            <div class="show-for-large hide-for-medium-only publication-info"
                 style="text-indent: 0.0em; font-size: 90%">
                Publication: ${model.value.pubTitle}, ${model.value.pubJournal} (${model.value.pubYear})</div>
        </li>
    </g:each>
</ul>
</div>
