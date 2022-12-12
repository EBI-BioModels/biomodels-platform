<style>
#loading {
    margin: 10px auto 20px;
    display: block;
}
</style>
<div class="row">
    <div class="columns small-12 medium-7 large-7">
        <h2 class="fs-title">Completion</h2>
    </div>
    <div class="columns small-12 medium-5 large-5">
        <h2 class="steps">Step 5 - 5</h2>
    </div>
</div>

<div class="row align-center">
    <div class="columns small-12 medium-12 large-12">
        <div style="display: block;" id="completionMessage" class="animate-bottom text-center">
        </div>
    </div>
</div>

<script>
    function completeSubmission() {
        // perform an ajax call to complete the submission
        // the function should return true or false to indicate the state of submission
        // I suppose it fails meaning the currentValidation to be false
        // TODO: at this step, we don't need to pass parameters to the following AJAX call because all submission
        // data have been validated either from the previous step or pressing on the Final Check button. To ignore
        // this, the submission data should be saved on Redis after performing the last check/validation. The following
        // AJAX call just loads the submission data back and performs the further actions. To do so can prevent
        // cheats at the previous step to attempt to modify the submission data.
        // Just pass the submission session id
        const url = "${createLink(controller: "submission", action: "completeSubmission")}";
        $.ajax({
            type: "POST",
            url: url,
            async: false,
            data: {
                isUpdate: isUpdate,
                isAmend: isAmend,
                modelFile: JSON.stringify(modelFile),
                additionalFiles: JSON.stringify(additionalFiles),
                modelInfo: JSON.stringify(modelInfo),
                publication: JSON.stringify(publication),
                revisionComments: revisionComments,
                modelId: modelId,
                changesMade: changesMade,
                submissionFolder: "${submissionFolder}"
            },
            dataType: "json",
            beforeSend: function() {
                console.log("About completing the submission...");
            }
        }).done(function (response) {
            currentValidation = response.status === "Success";
            $('#completionMessage').html(response.message);
            setCheckList(5, currentValidation);
            console.log("Your submission has been deposited successfully.");
        }).fail(function (jXHR, textStatus, thrown) {
            const errMsg = "Status: " + jXHR.status + " - Causes: " + jXHR.responseText;
            console.log(errMsg);
            const msg = "There have been errors to prevent you from submitting or updating your model. Please try again or contact us for further help.";
            $('#completionMessage').html("<h3 style='color: red'>" + msg + "</h3>");
            setCheckList(5, false);
            console.log(msg + "<br/>" + errMsg);
        });
    }
</script>
