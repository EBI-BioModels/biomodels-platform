<%@ page import=" net.biomodels.jummp.model.PublicationLinkProvider" %>
<%
    linkSourceTypes = PublicationLinkProvider.LinkType.values().collect { it.label }
%>
<div class="editablePart">
    <g:render template="/templates/publication/publicationDetailForm"
              plugin="jummp-plugin-web-application" model="[linkSourceTypes: linkSourceTypes]"/>
</div>
