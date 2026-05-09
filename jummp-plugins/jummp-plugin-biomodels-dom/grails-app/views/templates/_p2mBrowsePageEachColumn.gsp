<div class="small-12 medium-2 large-2 columns">
    <ul class="list" style="list-style: none;">
        <g:each in="${group}" var="c">
            <%
                def modelUrl = c.searchableLink ?:
                    createLink(controller: 'model', action: 'show', id: c.modelIdentifier)
            %>
            <li class="category-name"><a href="${modelUrl}">${c.categoryName}</a></li>
        </g:each>
    </ul>
</div>
