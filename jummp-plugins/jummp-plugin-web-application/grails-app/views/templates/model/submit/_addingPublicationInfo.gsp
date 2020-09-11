<%@ page import="net.biomodels.jummp.model.PublicationLinkProvider" %>
<g:javascript contextPath="" src="enterPublicationLink.js"/>
<%
    List linkSourceTypes = PublicationLinkProvider.LinkType.
        values().collect { it.label }
%>
<style type="text/css">
    .hide {
        display: none;
    }
    .show {
        display: block;
    }
</style>
<div class="row">
    <div class="columns small-12 medium-10 large-10">
        <h2 class="fs-title">Add Publication Information</h2>
        <p><g:message code="submission.biomodels.submit.publication.explanation"/></p>
    </div>

    <div class="columns small-12 medium-2 large-2">
        <h2 class="steps">Step 3 - 5</h2>
    </div>
</div>

<div class="row">
    <div class="small-12 medium-12 large-12 columns">
        <h4><g:message code="submission.publicationLink.header"/>&nbsp;<a class="publink-whatisit"><i
            class="fa fa-question-circle" aria-hidden="true"></i>
        </a></h4>
        <div class="publink-explanation" style="display: none;"><g:message code="submission.publink.publication"/></div>
        %{--<g:if test="${publication}">
            <g:if test="${publication.title && (publication.affiliation || publication.synopsis)}">
                Currently, the model is associated with:
                <g:render  model="[model: model]" template="/templates/showPublication" />
            </g:if>
        </g:if>--}%
        <div class="row">
            <div class="columns small-12 medium-3 large-3">
                <g:if test="${publication}">
                    <g:select name="PubLinkProvider" id="pubLinkProvider"
                              from="${linkSourceTypes}"
                              value="${publication.linkProvider.linkType}"
                              noSelection="['':'- No publication available -']"/>
                </g:if>
                <g:else>
                    <g:select name="PubLinkProvider" id="pubLinkProvider"
                              from="${linkSourceTypes}"
                              noSelection="['':'- No publication available -']"/>
                    %{--<g:textField name="PublicationLink" id="publicationLink"
                                 placeholder="Enter PubMed identifier, DOI or web link"/>--}%
                </g:else>
            </div>
            <div class="columns small-12 medium-7 large-7">
                <g:textField name="PublicationLink" id="publicationLink" value="${publication.link}"
                             placeholder="Enter PubMed identifier, DOI or web link"/>
            </div>
            <div class="columns small-12 medium-2 large-2">
                <button type="button" class="button" id="refresh-pub-link" name="refreshPubLink">Refresh
                </button>
            </div>

        </div>

        %{--<g:submitButton name="Cancel" class="button"
                        value="${g.message(code: 'submission.common.cancelButton')}" />
        <g:submitButton name="Back" class="button"
                        value="${g.message(code: 'submission.common.backButton')}" />
        <g:submitButton name="Continue" class="button"
                        value="${g.message(code: 'submission.publink.continueButton')}"/>--}%
        <div id="publicationForm">
            <div class="dialog">
                <g:render template="/templates/publication/publicationEditableElements"
                          plugin="jummp-plugin-web-application"
                          model="[id: params.id, publication: publication, authorListContainerSize: 4]"/>
            </div>
        </div>
    </div>
</div>
<input type="button" name="next" class="next action-button" value="Next"/>
<input type="button" name="previous" class="previous action-button-previous" value="Previous"/>
<script type="text/javascript">
    $('.publink-whatisit').on("click", function () {
        $('.publink-explanation').toggle("slow");
    });
    $(document).on('click', '#refresh-pub-link', {}, function(e) {
        e.preventDefault();
    });

</script>
