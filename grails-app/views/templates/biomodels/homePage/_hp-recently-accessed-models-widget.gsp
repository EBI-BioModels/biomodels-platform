<ul style="list-style: none inside none; padding: 0; margin-left: 0">
    <g:each in="${models}" var="it">
        <%
            String modelId = it.key
            String modelURI = g.createLink(controller: 'model', id: modelId, action: 'show')
            String modelName = it.value
        %>
        <li style="text-indent: -1.5em; padding-left: 1.5em">
            <span class='icon icon-functional' data-icon='4'>&nbsp;</span>
                <a href='${modelURI}'>${modelName}</a></li>
    </g:each>
</ul>
