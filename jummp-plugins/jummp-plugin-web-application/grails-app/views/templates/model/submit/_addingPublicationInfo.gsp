<g:javascript contextPath="" src="biomodels/enterPublicationLink.js"/>
<g:javascript contextPath="" src="biomodels/publicationSubmission.js"/>

<style>
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
        <div id="publicationProviderSelection">
        <g:render template="/templates/publication/selectPublicationSource"
                  plugin="jummp-plugin-web-application"/>
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
<div id="authorListTemp" style="height: 50px; margin: auto; border: 3px solid #73AD21; display: none">
</div>
<input type="button" name="next" class="next action-button" value="Next" />
<input type="button" name="previous" class="previous action-button-previous" value="Previous"/>
<script type="text/javascript">
    // TODO: move some duplicate codes to helpers.js
    $(document).ready(function () {
        $('#loadingIcon').hide();
        if ("${publication}") {
            $('#publicationForm').show();
        } else {
            $('#publicationForm').hide();
        }
    });

    $('.publink-whatisit').on("click", function () {
        $('.publink-explanation').toggle("slow");
    });

    /**
     * Read the guessed namespace, collection label and accession of the publication annotation if
     * it is available. These things have been detected while detecting the model format in the phase
     * of uploading files. The function will be only invoked when the publication linked to the model
     * is empty. It could be the first submission or the update flow when the previous revision does
     * have any publication annotation.
     */
    function guessPublicationAndFillForm() {
        if (${!publication}) {
            console.log("Guess publication identifier from the main file and fill in the publication form");
            const namespace = guessedPublicationNamespace;
            const accession = guessedPublicationAccession;
            const label = guessedPublicationCollectionLabel;
            if (typeof accession  !== "undefined" && typeof label !== "undefined") {
                verifyAndFetchPublicationDetails(label, accession);
            }
        }
    }

    /**
     * jQuery's errorThrown is only populated for actual HTTP error responses (e.g. "Not Found");
     * for network-level failures where no response was received at all (server down, connection
     * refused/reset, ERR_EMPTY_RESPONSE) it is an empty string, so building a message from it
     * alone produces a blank toastr. Fall back to a message based on jqXHR.status/textStatus.
     */
    function buildAjaxErrorMessage(jqXHR, textStatus, errorThrown) {
        if (jqXHR.responseJSON && jqXHR.responseJSON.message) {
            return jqXHR.responseJSON.message;
        }
        if (errorThrown) {
            return jqXHR.status + " " + errorThrown;
        }
        if (jqXHR.status === 0) {
            return "Could not reach the server. Please check your connection and try again.";
        }
        return jqXHR.status + " " + (textStatus || "error") +
            ": the server did not return a response. Please try again later.";
    }

    function verifyAndFetchPublicationDetails(pubLinkProvider, pubLink) {
        verifyPublicationSource(pubLinkProvider, pubLink);
        clearErrorMessages();
        $.ajax({
            type: "POST",
            url: "${createLink(controller: "publication", action:"verifyPubLinkAndFetchData")}",
            data: {
                pubLinkProvider: pubLinkProvider,
                pubLink: pubLink
            },
            dataType: "json",
            async: true,
            beforeSend: function () {
                console.log("Before sending the request");
            },
            success: function (data) {
                toastr.clear();
                publication = data["publication"];
                if (data.status === "Failed") {
                    collectErrors(errorMessages, data["message"]);
                    toastr.error(data["message"]);
                    currentValidation = false;
                    showFlashMessages(errorMessages);
                } else {
                    let msg = data["message"];
                    if (data.status === "OK") {
                        toastr.success(msg);
                    } else {
                        toastr.warning(msg);
                    }
                    showFlashMessages(msg);
                    if (data["comesFromDB"]) {
                        msg = data["message"];
                        toastr.warning(msg);
                        showFlashMessages(msg);
                    }
                    if (publication) {
                        reloadPublicationForm(publication);
                        currentValidation = true;
                    } else {
                        msg = "The publication details of  " + pubLinkProvider + ": " + pubLink + " cannot be found."
                        showFlashMessages(msg);
                        toastr.error(msg);
                        currentValidation = false;
                    }
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                let errMsg = buildAjaxErrorMessage(jqXHR, textStatus, errorThrown);
                console.log("inside error " + errMsg);
                console.log(textStatus);
                toastr.clear();
                toastr.error(errMsg);
                errorMessages.push(errMsg);
                currentValidation = false;
                showFlashMessages(errorMessages);
            },
            complete: function () {
                console.log("Completed");
            }
        });
    };

    function verifyPublicationProviderAndLink(pubLinkProvider, pubLink) {
        verifyPublicationSource(pubLinkProvider, pubLink);
        clearErrorMessages();
        return $.ajax({
            type: "POST",
            url: "${createLink(controller: "publication", action:"doVerifyPublicationProviderAndLink")}",
            data: {
                pubLinkProvider: pubLinkProvider,
                pubLink: pubLink
            },
            dataType: "json",
            async: false,
            beforeSend: function () {
                $('#loadingIcon').show();
                setTimeout(function(){ console.log("Please wait for 3s..."); }, 3000);
            },
            success: function (data) {
                toastr.clear();
                if (data.status === "Failed") {
                    collectErrors(errorMessages, data["message"]);
                    toastr.error(data["message"]);
                    currentValidation = false;
                    showFlashMessages(errorMessages);
                } else {
                    currentValidation = true;
                    if (data.status === "OK") {
                        toastr.success(data["message"]);
                    } else {
                        toastr.warning(data["message"]);
                    }
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                let errMsg = buildAjaxErrorMessage(jqXHR, textStatus, errorThrown);
                console.log("inside error " + errMsg);
                toastr.clear();
                toastr.error(errMsg);
                errorMessages.push(errMsg);
                currentValidation = false;
                showFlashMessages(errorMessages);
            },
            complete: function () {
                $('#loadingIcon').css("display", "none");
                console.log("just complete");
                $('#loadingIcon').hide();
            }
        });
    };

    function reloadPublicationForm(publication) {
        $('#publicationLinkProviderBox').show();
        $('#publicationLink').show();
        $('#publicationLinkCol').show();
        $('#freshPublicationBtnCol').show();
        $('#publicationForm').show();

        $('#publicationLink').val(publication.link);
        $('#pubLinkProvider').val(publication.linkProvider.linkType);

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
            return ajax(() => {
                currentValidation = true;
            });
        } else {
            verifyPublicationProviderAndLink($('#pubLinkProvider').val(),
                $('#publicationLink').val()).then(function (r) {
                if (!currentValidation) {
                    return;
                }
                currentValidation = validateDataForm("publicationForm");
            });
            if (!currentValidation) {
                let msg =
                    "The publication form is invalid such as missing required values. Please check all the fields again!";
                toastr.error(msg);
                showFlashMessages(msg);
                return;
            }
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
        return $.ajax({
            type: "POST",
            url: "${createLink(controller: "publication", action: "validatePublicationDetails")}",
            data: {
                pubDetails: JSON.stringify(pubDetails),
                isUpdate: isUpdate,
                modelId: modelId,
                changesMade: [...changesMade]
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
                changesMade = res["changesMade"];
            },
            error: function(jqXHR, textStatus, errorThrown) {
                const msg = buildAjaxErrorMessage(jqXHR, textStatus, errorThrown);
                console.log("msg: " + msg);
                console.log("textStatus: " + textStatus);
                collectErrors(errorMessages, msg);
                isPubTCValidated = false;
            },
            complete: function (r) {
                currentValidation = withoutPub || isPubTCValidated;
            }
        });
    }

    function verifyPublicationSource(pubLinkProvider, pubLink) {
        if (!pubLinkProvider || (!pubLink && pubLinkProvider !== "Publication without link")) {
            toastr.clear();
            toastr.error("Either of publication provider or link is empty.");
            return;
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
    function ajax(callback) {
        return $.ajax({
            url: "${createLink(controller: "submission", action: "checkCurrentValidation")}",
            type: "POST",
            data: {
                isUpdate: isUpdate,
                changesMade: [...changesMade]
            },
            success: function (response) {
                changesMade = response["changesMade"];
                console.log(JSON.stringify(response));
                callback();
            }
        });
    }
</script>
