<div class="row">
    <div class="small-12 medium-6 large-4 columns">
        Tags
    </div>
    <div class="small-12 medium-6 large-8 columns">
        <g:each in="${tags}" var="tag">
            <%
                def href = g.createLink(controller: 'search', action: 'search', params: ['query': "$tag"])
            %>
            <span class="model-tag"><a href="${href}">${tag}</a></span>
        </g:each>
    </div>
</div>
