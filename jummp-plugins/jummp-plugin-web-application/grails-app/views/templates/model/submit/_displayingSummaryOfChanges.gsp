<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.core.model.RevisionTransportCommand" %>
<%@ page import="net.biomodels.jummp.core.model.ModelTransportCommand" %>

<style>
    .submission-prop {
        font-weight: bold;
        color: #0e0e0e;
    }
    .spacing-row {
        padding: 10px 0 10px 0;
    }
    .even-row {
        background-color: lightgrey;
    }
    .odd-row {
        background-color: lightcyan;
    }
</style>
<div class="row">
    <div class="columns small-12 medium-7 large-7">
        <h2 class="fs-title"><g:message code="submission.summary.header"/></h2>
    </div>

    <div class="columns small-12 medium-5 large-5">
        <h2 class="steps">Step 4 - 5</h2>
    </div>
</div>
<div class="row spacing-row odd-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            <g:message code="submission.summary.nameLabel"/></span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="detectedModelName"></div>
    </div>
</div>
<div class="row spacing-row even-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            <jummp:displayModelDescriptionLabel>
                ${description}
            </jummp:displayModelDescriptionLabel>
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="detectedModelDescription"></div>
    </div>
</div>
<div class="row spacing-row odd-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            Model format
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="detectedModelFormat"></div>
        <div id="detectedModelFormatReadme"></div>
    </div>
</div>
<div class="row spacing-row even-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            Modelling approach
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="detectedModellingApproach"></div>
        <div id="detectedModellingOtherInfo"></div>
    </div>
</div>
<div class="row spacing-row odd-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            Model file
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="detectedModelFileName" style="font-weight: bold"></div>
        <div id="detectedModelFileDescription"></div>
    </div>
</div>
<div class="row spacing-row even-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            Additional files
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="detectedAdditionalFiles">
            <ol id="listAdditionalFiles"></ol>
        </div>
    </div>
</div>
<div class="row spacing-row odd-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            Publication
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="detectedPublicationDetails">
        </div>
    </div>
</div>
<div class="row spacing-row even-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            Is this a metadata submission?
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="is-metadata-submission">
            <g:checkBox name="isMetadataSubmission" id="chk-is-metadata-submission" value="${isMetadataSubmission}"
                        title="Tick the check box if this is a metadata submission"/>
            <span>&nbsp;
            (<strong>Note</strong>: Only check this box if you are making a metadata-only submission. Read more about
            <a href="${serverURL}/faq#how-to-submit-only-metadata" target="_blank">How to make metadata-only submissions?</a>)
        </span>
        </div>
    </div>
</div>
<g:if test="${isUpdate}">

<div class="row spacing-row odd-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            Do you want to amend the revision?
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="amend-create-new-revision">
        <g:if test="${amendable}">
            <g:checkBox name="isAmend" id="is-amend" value="${false}"
                        title="You can overwrite the current updates on this version"/>
        </g:if>
        <g:else>
            <g:checkBox name="isAmend" id="is-amend" value="${false}" disabled="true"
                        title="This version is not overwritten."/>
        </g:else>
        </div>
    </div>
</div>

<div class="row spacing-row even-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            Is this a minor revision?
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="is-minor-revision-row">
            <g:checkBox name="isMinorRevision" id="is-minor-revision" value="${false}"
                        title="Minor revisions can later be deleted by the submitter, a curator, or an administrator without removing the rest of the model's history"/>
            <span>&nbsp;(<strong>Note</strong>: only check this box for a small, non-scientific correction - e.g.
                fixing a typo or a file that shouldn't have been included. A minor revision can be removed later
                from the model's history without affecting subsequent revisions.)</span>
        </div>
    </div>
</div>

<div class="row spacing-row odd-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            Contributor Role
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="detected-contributor-role">

        </div>
    </div>
</div>

<div class="row spacing-row even-row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            <g:message code="submission.summary.revisionLabel"/>
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="whatYouHaveUpdated">
            <g:textArea name="RevisionComments" id="revisionComments" rows="5" cols="70"
                        placeholder="Explain what you have updated"/>
        </div>
    </div>
</div>
</g:if>
<input type="button" name="next" class="next action-button" value="Submit" id="btnSubmit" />
<input type="button" name="previous" class="previous action-button-previous" value="Previous" />
<input type="button" name="btnFinalCheck" class="action-button" value="Final Check" style="float: left" />
<script type="text/javascript">
    function populateSummaryData() {
        console.log("Displaying the summary of submission/changes");
        $('#detectedModelName').html(modelInfo.detectedName);
        $('#detectedModelDescription').html(modelInfo.detectedDescription);

        $('#detectedModelFormat').text(modelInfo.detectedModelFormat.name);
        $('#detectedModelFormatReadme').text(modelInfo.detectedModelFormat.readme);

        $('#detectedModellingApproach').text(modelInfo.detectedModelling.approach);
        $('#detectedModellingOtherInfo').text(modelInfo.detectedModelling.otherInfo);

        // Files uploaded
        $('#detectedModelFileName').text(modelFile.filename);
        $('#detectedModelFileDescription').text(modelFile.description);
        if (additionalFiles.length > 0) {
            $('#listAdditionalFiles').empty();
            $.each(additionalFiles, function (index, file) {
                const item = "<li><strong>" + file.filename + "</strong><br/>" + file.description + "</li>";
                $('#listAdditionalFiles').append(item);
            });
        } else {
            $('#detectedAdditionalFiles').text("No additional files provided");
        }

        $('#detected-contributor-role').text(latestContributorRole);

        // Publication details
        // invoke an ajax call to the server to render _publication template
        return $.ajax({
            url: "${createLink(controller: "publication", action: "renderPublicationDetails")}",
            type: 'POST',
            data: {
                pubDetails: JSON.stringify(publication)
            },
            success: function(response) {
                $('#detectedPublicationDetails').html(response);
            },
            error: function() {
                console.log("Failed loading content");
            }
        });
    }

    // This function implements for the Submit button on the final form
    function submitData() {
        // TODO: validate the working map again before invoking the following AJAX call and rename the method
        // if the validation is true, hit the callback. The callback will save all the data in the redis
        let msg = "";
        revisionComments = $('#revisionComments').val();
        return $.ajax({
            url: "${createLink(controller: "submission", action: "doLastValidateSubmissionData")}",
            type: "POST",
            data: submissionParameters(),
            beforeSend: function () {
                msg = "Doing the final verification of  your submission data...";
                console.log(msg);
                toastr.info(msg);
            },
            success: function (response) {
                JSON.stringify(response);
                currentValidation = response["currentValidation"];
                // Explain what you have updated
                revisionComments = $('#revisionComments').val();
                msg = "Finished the last validation of the submission data.";
                if (!currentValidation) {
                    msg += response["errMsg"];
                } else {
                    msg += " Your submission data have no error."
                }
                showNotification(msg);
                console.log(msg);
                toastr.success(msg);
            },
            error: function (error) {
                currentValidation = false;
                msg = "There have been some errors in your submission data. Please do verify all steps again.";
                console.log(msg);
                console.log(error.responseText);
                showNotification(msg);
                toastr.error(msg);
            }
        });
    }

    $('#is-amend').on("click", function () {
        isAmend = $(this).is(":checked");
        if (isAmend) {
            // TODO: get the last commit message
        } else {

        }
    });

    $('#is-minor-revision').on("click", function () {
        isMinorRevision = $(this).is(":checked");
    });

    $('#chk-is-metadata-submission').on("click", function () {
        isMetadataSubmission = $(this).is(":checked");
        if (isMetadataSubmission) {
            // TODO: what should be done?
        } else {

        }
    });

    $("input[name=btnFinalCheck]").on("click", function() {
        // it can be called: validateData(4).done(function(response) {}); -- 4 means the step 4 of the submission flow
        submitData().done(function(response) {
            setCheckList(4, currentValidation);
        });
    });

    $("#btnSubmit").on("click", function(e) {
        // stop submitting the form to see the disabled button effect
        e.preventDefault();
        // $(this).addClass("disabled");
        $(this).attr("disabled", true);
        $(this).attr("style", "background-color: #616161 !important");
        console.log("Please waiting for saving your submission data in our system. Don't press any button or close this window!");
    });

</script>
