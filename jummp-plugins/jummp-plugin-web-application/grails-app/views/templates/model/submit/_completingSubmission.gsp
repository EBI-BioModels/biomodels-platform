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

<div class="row">
    <div class="columns small-12 large-12">
        <div class="text-center">
            <img src="${serverURL}/images/biomodels/loading.gif" id="loading" title="working..." />
        </div>
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
        // TODO: show a waiting prompt message
        // showWaitingIcon();

        // perform an ajax call to complete the submission
        // the function should return true or false to indicate the state of submission
        // I suppose it fails meaning the currentValidation to be false
        const url = "${createLink(controller: "submission", action: "completeSubmission")}";
        $.ajax({
            type: "POST",
            url: url,
            data: {
                isUpdate: isUpdate,
                isAmend: isAmend,
                modelFile: JSON.stringify(modelFile),
                additionalFiles: JSON.stringify(additionalFiles),
                modelInfo: JSON.stringify(modelInfo),
                publication: JSON.stringify(publication),
                revisionComments: revisionComments,
                modelId: modelId,
                changesMade: changesMade
            },
            dataType: "json"
        })
        .done(function (response) {
            console.log(response);
            currentValidation = response.status === "Success" ? true : false;
            $('#completionMessage').html(response.message);
            setCheckList(5, currentValidation);
            /*if (currentValidation) {
                $('#modelURL').attr("href", response.modelURL);
                $('#modelURL').text(response.modelIdentifier);
                $('#smgSuccess').css("display", "block");
                $('#smgFailure').css("display", "none");
            } else {
                $('#smgSuccess').css("display", "none");
                $('#smgFailure').css("display", "block");
            }*/
        // }).fail(function (jXHR, textStatus, thrown) {
        //     console.log("Status: " + jXHR.status + " - " + jXHR.statusText);
        //     $('#completionMessage').html("There have been errors to prevent you from submitting or updating your model. Please try again or contact us for further help.");
        //     // TODO: show cross icon
        });
    }
</script>
