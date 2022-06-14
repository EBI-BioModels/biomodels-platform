<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.core.model.RevisionTransportCommand" %>
<%@ page import="net.biomodels.jummp.core.model.ModelTransportCommand" %>
<%
    /*ModelTransportCommand model = workingMemory.get("ModelTC")
    RevisionTransportCommand revision = workingMemory.get("RevisionTC")*/
%>
<style type="text/css">
    .submission-prop {
        font-weight: bold;
        color: #0e0e0e;
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
<div class="row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            <g:message code="submission.summary.nameLabel"/></span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="detectedModelName"></div>
    </div>
</div>
<div class="row">
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
<div class="row">
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
<div class="row">
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
<div class="row">
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
<div class="row">
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
<div class="row">
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
<g:if test="${isUpdate}">
<div class="row">
    <div class="columns small-12 medium-2 large-2">
        <span class="submission-prop">
            Do you want to amend the revision?
        </span>
    </div>
    <div class="columns small-12 medium-10 large-10">
        <div id="amend-create-new-revision">
            <g:checkBox name="isAmend" id="is-amend" value="${false}" />
        </div>
    </div>
</div>
<div class="row">
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
<input type="button" name="next" class="next action-button" value="Submit" />
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
            data: {
                isUpdate: isUpdate,
                isAmend: isAmend,
                modelFile: JSON.stringify(modelFile),
                additionalFiles: JSON.stringify(additionalFiles),
                modelInfo: JSON.stringify(modelInfo),
                publication: JSON.stringify(publication),
                revisionComments: revisionComments,
                modelId: modelId,
                latestModelName: latestModelName,
                latestModelDescription: latestModelDescription,
                latestModelFormat: latestModelFormat,
                latestModelFormatNameAndVersion: latestModelFormatNameAndVersion,
                latestReadmeSubmission: latestReadmeSubmission,
                latestModellingApproach: latestModellingApproach,
                latestOtherInfo:latestOtherInfo,

                /**
                 * changesMade is a Set object. It is put here to pass to the server side as a list
                 * because it is one of the members of the params object. So, [...changesMade] is to
                 * convert a Set object to a List one.
                 */
                changesMade: [...changesMade],
                submissionFolder: "${submissionFolder}"
            },
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
                msg = "Finished the last validation of the submission data.\n";
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
    $("input[name=btnFinalCheck]").on("click", function() {
        // it can be called: validateData(4).done(function(response) {}); -- 4 means the step 4 of the submission flow
        submitData().done(function(response) {
            setCheckList(4, currentValidation);
        });
    });
</script>
