<div class="row">
    <div class="small-12 medium-6 large-4 columns">
        Tags
    </div>
    <div class="small-12 medium-6 large-8 columns">
        <g:each in="${bmTags}" var="bmTag">
            <%
                def href = g.createLink(controller: 'search', action: 'search', params: ['query': "${bmTag.name}"])
            %>
            <span class="model-tag" title="${bmTag?.description}"><a href="${href}">${bmTag.name}</a></span>
        </g:each>
    </div>
</div>
