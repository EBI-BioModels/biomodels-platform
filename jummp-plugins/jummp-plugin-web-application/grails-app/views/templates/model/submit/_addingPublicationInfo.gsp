<%@ page import="net.biomodels.jummp.model.PublicationLinkProvider" %>
<g:javascript contextPath="" src="enterPublicationLink.js"/>
<g:javascript contextPath="" src="biomodels/publicationSubmission.js"/>
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
                              value="${publication?.linkProvider?.linkType}"
                              noSelection="['NoPub':'- No publication available -']"/>
                </g:if>
                <g:else>
                    <g:select name="PubLinkProvider" id="pubLinkProvider"
                              from="${linkSourceTypes}"
                              noSelection="['NoPub':'- No publication available -']"/>
                    %{--<g:textField name="PublicationLink" id="publicationLink"
                                 placeholder="Enter PubMed identifier, DOI or web link"/>--}%
                </g:else>
            </div>
            <div class="columns small-12 medium-7 large-7">
                <g:textField name="PublicationLink" id="publicationLink" value="${publication?.link}"
                             placeholder="Enter PubMed identifier, DOI or web link"/>
            </div>
            <div class="columns small-12 medium-2 large-2">
                <button type="button" class="button" id="refreshPubLinkBtn" name="refreshPubLink">Refresh
                </button>
            </div>

        </div>

        <div id="publicationForm">
            <div class="dialog">
                <g:render template="/templates/publication/publicationEditableElements"
                          plugin="jummp-plugin-web-application"
                          model="[id: params.id, publication: publication, authorListContainerSize: 4]"/>
            </div>
        </div>
    </div>
</div>
<div name="authorListTemp" id="authorListTemp"
     style="height: 50px; margin: auto; border: 3px solid #73AD21; display: none">
</div>
<input type="button" name="next" class="next action-button" value="Next" />
<input type="button" name="previous" class="previous action-button-previous" value="Previous"/>
<script type="text/javascript">
    $(document).ready(function () {
        console.log("${publication.dump()}");
        if ("${publication}") {
            $('#publicationForm').show();
        } else {
            $('#publicationForm').hide();
        }
    });
    $('.publink-whatisit').on("click", function () {
        $('.publink-explanation').toggle("slow");
    });
    $(document).on('click', '#refreshPubLinkBtn', {}, function(e) {
        e.preventDefault();
        let pubLinkProvider = $('#pubLinkProvider').val();
        let pubLink = $('#publicationLink').val();
        $.ajax({
            type: "POST",
            url: "${createLink(controller: "publication", action:"doVerifyPubLinkAndFetchData")}",
            data: {
                pubLinkProvider: pubLinkProvider,
                pubLink: pubLink
            },
            dataType: "json",
            async: false,
            success: function (data) {
                toastr.clear();
                publication = data.publication;
                if (data.status === "Failed") {
                    errorMessages.push(data["message"]);
                    toastr.error(data["message"])
                } else {
                    toastr.success(data["message"]);
                    reloadPublicationForm(data["publication"]);
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                console.log("inside error " + JSON.stringify(errorThrown));
                console.log(textStatus);
            }
        });
    });

    function reloadPublicationForm(publication) {
        $('#title').val(publication.title);
        $('#journal').val(publication.journal);
        $('#affiliation').val(publication.affiliation);
        $('#synopsis').val(publication.synopsis);
        $('#volume').val(publication.volume);
        $('#issue').val(publication.issue);
        $('#year').val(publication.year);
        $('#month').val(publication.month);
        $('#pages').val(publication.pages);
        $('#authorList').empty()
        if (publication.authors !== null) {
            $.each(publication.authors, function (index, it) {
                if (it === null) { return; } // skip nulls
                let option = '<option value="' + index + '|' + it.userRealName + '|' + (it.orcid !== "" ?
                    it.orcid : "") +
                    '|' + (it.institution !== null ? it.institution : "") + '"' +
                'data-person-id="' + index + '"' +
                'data-person-realname="' + it.userRealName + '"' +
                'data-person-orcid="' + (it.orcid !== null ? it.orcid : "") + '"' +
                'data-person-institution="' + (it.institution !== null ? it.institution : "") + '">' +
                it.userRealName + '</option>';
                $('#authorList').append(option);
            });
            authorList = publication.authors;
            updateData();
        }
    }

    function validatePublicationInfo() {
        errorMessages = [];
        let selectedPubLinkProvider = $('#pubLinkProvider').val();
        console.log(selectedPubLinkProvider);
        let withoutPub = selectedPubLinkProvider === "NoPub";
        if (withoutPub) {
            currentValidation = true;
            return;
        }
        let isPubTCValidated = true;
        let authors = $('textarea[name="authorListContainer"]').val();
        let pubDetails = {};
        pubDetails["linkProvider"] = $('#pubLinkProvider').val();
        pubDetails["link"] = $('#publicationLink').val();
        pubDetails["title"] = $('#title').val();
        pubDetails["journal"] = $('#journal').val();
        pubDetails["affiliation"] = $('#affiliation').val();
        pubDetails["synopsis"] = $('#synopsis').val();
        pubDetails["volume"] = $('#volume').val();
        pubDetails["issue"] = $('#issue').val();
        pubDetails["year"] = $('#year').val();
        pubDetails["month"] = $('#month').val();
        pubDetails["pages"] = $('#pages').val();
        pubDetails["authors"] = authorList;
        $.ajax({
            type: "POST",
            url: "${createLink(controller: "publication", action: "validatePublicationDetails")}",
            data: {
                pubDetails: JSON.stringify(pubDetails)
            },
            async: false,
            dataType: "json",
            success: function(res) {
                isPubTCValidated = res.status === "Error" ? false : true;
                $.each(res.errors, function(index, error) {
                    errorMessages.push(error);
                });
                console.log("Status: " + res.status);
            },
            error: function(jqXHR, textStatus, errorThrown) {
                const msg = JSON.stringify(errorThrown);
                console.log("msg: " + msg);
                console.log("textStatus: " + textStatus);
                errorMessages.push(msg);
                isPubTCValidated = false;
            }
        });
        currentValidation = withoutPub || isPubTCValidated;
    }
</script>
