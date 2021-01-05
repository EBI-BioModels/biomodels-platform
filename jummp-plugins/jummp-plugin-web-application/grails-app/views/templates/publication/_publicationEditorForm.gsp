<g:form useToken="true">
    <div class="dialog">
        <g:render template="/templates/publication/publicationEditableElements"
                  plugin="jummp-plugin-web-application"
                  model="[id: params.id, publication: publication, authorListContainerSize: 4]"/>
        <g:render template="/templates/publication/publicationButtonsForm"
                  plugin="jummp-plugin-web-application"/>
    </div>
</g:form>
