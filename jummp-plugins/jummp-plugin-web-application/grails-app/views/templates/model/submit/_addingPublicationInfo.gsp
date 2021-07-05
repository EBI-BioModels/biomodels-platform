<g:javascript contextPath="" src="biomodels/enterPublicationLink.js"/>
<g:javascript contextPath="" src="biomodels/publicationSubmission.js"/>

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
        %{--<div class="text-center">
            <img src="${serverURL}/images/biomodels/loading.gif" id="loadingIcon" title="Fetching data..."
                 alt="Please wait..."/>
        </div>--}%
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

        <g:render template="/templates/publication/selectPublicationSource"
                  plugin="jummp-plugin-web-application"/>

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

    function verifyAndFetchPublicationDetails(pubLinkProvider, pubLink) {
        if (!pubLinkProvider || !pubLink) {
            toastr.clear();
            toastr.error("Either of publication provider or link is empty");
            return;
        }
        clearErrorMessages();
        $.ajax({
            type: "POST",
            url: "${createLink(controller: "publication", action:"verifyPubLinkAndFetchData")}",
            data: {
                pubLinkProvider: pubLinkProvider,
                pubLink: pubLink
            },
            dataType: "json",
            async: false,
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
                    if (data.status === "OK") {
                        toastr.success(data["message"]);
                    } else {
                        toastr.warning(data["message"]);
                    }
                    if (data["comesFromDB"]) {
                        toastr.warning("${g.message(code: "publication.editor.duplicateEntry.message")}");
                    }
                    if (publication) {
                        reloadPublicationForm(publication);
                        currentValidation = true;
                    } else {
                        let msg = "The publication details of  " + pubLinkProvider + ": " + pubLink + " cannot be found."
                        showFlashMessages(msg);
                        currentValidation = false;
                    }
                }
            },
            error: function (jqXHR, textStatus, errorThrown) {
                let errMsg = JSON.parse(JSON.stringify(errorThrown));
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
        if (!pubLinkProvider || !pubLink) {
            toastr.clear();
            toastr.error("Either of publication provider or link is empty");
            return;
        }
        clearErrorMessages();
        $.ajax({
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
                setTimeout(function(){ console.log("Please wait for 10s..."); }, 10000);
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
                let errMsg = JSON.parse(JSON.stringify(errorThrown));
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
        } else {
            verifyPublicationProviderAndLink($('#pubLinkProvider').val(), $('#publicationLink').val());
            if (!currentValidation) {
                return;
            }
            currentValidation = validateDataForm("publicationForm");
        }
        if (!currentValidation) {
            let msg =
                "The publication form is invalid such as missing required values. Please check all the fields again!";
            toastr.error(msg);
            showFlashMessages(msg);
            return;
        } else {

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
                const msg = JSON.parse(JSON.stringify(errorThrown));
                console.log("msg: " + msg);
                console.log("textStatus: " + textStatus);
                collectErrors(errorMessages, msg);
                isPubTCValidated = false;
            }
        });
        currentValidation = withoutPub || isPubTCValidated;
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
