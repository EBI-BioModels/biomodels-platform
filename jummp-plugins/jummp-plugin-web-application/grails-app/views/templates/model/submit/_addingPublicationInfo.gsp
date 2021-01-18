<%@ page import="net.biomodels.jummp.model.PublicationLinkProvider" %>
<g:javascript contextPath="" src="biomodels/enterPublicationLink.js"/>
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
    #loadingIcon {
        margin: 10px auto 20px;
        display: block;
    }
</style>
<div class="row">
    <div class="columns small-12 medium-8 large-8">
        <h2 class="fs-title">Add Publication Information</h2>
        <p><g:message code="submission.biomodels.submit.publication.explanation"/></p>
    </div>
    <div class="columns small-12 medium-2 large-2">
        <div class="text-center">
            <img src="${serverURL}/images/biomodels/loading.gif" id="loadingIcon" title="Fetching data..."
                 alt="Please wait..."/>
        </div>
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
        <div class="row">
            <div class="columns small-12 medium-3 large-3">
                <label for="pubLinkProvider">Choose a publication source</label>
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
                </g:else>
            </div>
            <div class="columns small-12 medium-7 large-7">
                <div id="lblPublicationLink">
                    <label class="required" for="publicationLink">
                        Enter PubMed identifier or DOI, then press on the <strong>Update</strong> button
                    </label>
                </div>
                <g:textField name="PublicationLink" id="publicationLink" value="${publication?.link}"
                             placeholder="Enter PubMed identifier, DOI or web link"/>
            </div>
            <div class="columns small-12 medium-2 large-2">
                <label>&nbsp;</label>
                <button type="button" class="button" id="updatePubLinkBtn" name="updatePubLink">Update
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
        $('#loadingIcon').hide();
        if ("${publication}") {
            $('#publicationForm').show();
            //doShowHideUpdateBtn(true);
            let selectedPubLinkProvider = $('#pubLinkProvider').val();
            if (selectedPubLinkProvider === "Publication without link") {
                doShowHideUpdateBtn(false);
                $('#publicationLink').hide();
            }
        } else {
            $('#publicationForm').hide();
            doShowHideUpdateBtn(false);
        }
    });
    $('.publink-whatisit').on("click", function () {
        $('.publink-explanation').toggle("slow");
    });

    $(document).on('change', '#pubLinkProvider', {}, function(e) {
        let pubLinkProvider = $(this).val();
        let res = shouldWarnWhenUpdatingLinkProvider(pubLinkProvider);
        doShowHideUpdateBtn(res);
        if (res) {
            let message =
                "Please change the publication link on the next input and click on Refresh button to refresh the form";
            showWarningMessage(message);
        }
    });

    $(document).on('blue focusout', '#publicationLink', {}, function(e) {
        let pubLink = $(this).val();
        let res = shouldWarnWhenUpdatingPublicationLink(pubLink);
        if (res) {
            let message = "Click on Refresh button to refresh the publication details";
            showWarningMessage(message);
        }
    });

    $(document).on('click', '#updatePubLinkBtn', {}, function (e) {
        e.preventDefault();
        clearErrorMessages();
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
            async: true,
            beforeSend: function () {
                $('#loadingIcon').show();
            },
            success: function (data) {
                toastr.clear();
                publication = data["publication"];
                if (data.status === "Failed") {
                    collectErrors(errorMessages, data["message"]);
                    toastr.error(data["message"]);
                    showErrorMessages();
                } else {
                    if (data.status === "OK") {
                        toastr.success(data["message"]);
                    } else {
                        toastr.warning(data["message"]);
                    }
                    if (data["comesFromDB"]) {
                        toastr.warning("${g.message(code: "publication.editor.duplicateEntry.message")}");
                    }
                    reloadPublicationForm(publication);
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                let errMsg = JSON.stringify(errorThrown);
                console.log("inside error " + errMsg);
                console.log(textStatus);
                toastr.clear();
                toastr.error(errMsg);
                errorMessages.push(errMsg);
                showErrorMessages();
            },
            complete: function () {
                $('#loadingIcon').hide();
            }
        });
        return false;
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
        let withoutPub = selectedPubLinkProvider === "NoPub";
        if (withoutPub) {
            currentValidation = true;
            return;
        }
        let isPubTCValidated = true;
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
        pubDetails["authors"] = $('#authorListTemp').text();
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
                    collectErrors(errorMessages, error);
                });
                if (isPubTCValidated) {
                    publication = res.publication;
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                const msg = JSON.stringify(errorThrown);
                console.log("msg: " + msg);
                console.log("textStatus: " + textStatus);
                collectErrors(errorMessages, msg);
                isPubTCValidated = false;
            }
        });
        currentValidation = withoutPub || isPubTCValidated;
    }

    function shouldWarnWhenUpdatingLinkProvider(pubLinkProvider) {
        let Need2BeWarned = pubLinkProvider === "PubMed ID" || pubLinkProvider === "DOI";
        return Need2BeWarned;
    }

    function shouldWarnWhenUpdatingPublicationLink(update) {
        return update !== "${publication?.link}";
    }

    function showWarningMessage(message) {
        toastr.clear();
        toastr.warning(message);
    }

    function doShowHideUpdateBtn(flag) {
        flag ? $('#updatePubLinkBtn').show() : $('#updatePubLinkBtn').hide();
        let v1 = '<label class="required" for="publicationLink">\n' +
            'Enter PubMed identifier or DOI, then press on the <strong>Update</strong> button\n' +
            '</label>'
        let v2 = '&nbsp;';
        flag ? $('#lblPublicationLink').html(v1) : $('#lblPublicationLink').html(v2);
    }

    function showErrorMessages() {
        if (errorMessages.length) {
            let messages = "<ul>";
            for (i = 0; i < errorMessages.length; i++) {
                messages += "<li>" + errorMessages[i] + "</li>";
            };
            messages += "</ul>";
            $('.flashNotificationDiv').html(messages).show();
        }
    }

    function clearErrorMessages() {
        errorMessages = [];
        $('.flashNotificationDiv').html("").hide();
    }

    function collectErrors(errorMessages, errMsg) {
        let found = jQuery.inArray(errMsg, errorMessages);
        if (found < 0) {
            errorMessages.push(errMsg);
        }
    }
</script>
