<g:if test="${revision.model.publication}">
    <%
        def model = revision.model
    %>

    <div class="row">
        <div class="small-12 medium-2 large-2 columns">
            <span class="overview-tab-attribute"><g:message code="model.model.publication"/></span>
        </div>
        <div class="small-12 medium-10 large-10 columns">
            <g:render  model="[publication: model?.publication]"
                       template="/templates/showPublication" />
        </div>
    </div>
</g:if>
