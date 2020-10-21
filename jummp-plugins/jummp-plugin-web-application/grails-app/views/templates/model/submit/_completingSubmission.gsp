<style>
/*.loader {
    border: 16px solid #f3f3f3;
    border-radius: 50%;
    border-top: 16px solid blue;
    border-right: 16px solid green;
    border-bottom: 16px solid red;
    border-left: 16px solid pink;
    width: 120px;
    height: 120px;
    -webkit-animation: spin 2s linear infinite;
    animation: spin 2s linear infinite;
}

@-webkit-keyframes spin {
    0% { -webkit-transform: rotate(0deg); }
    100% { -webkit-transform: rotate(360deg); }
}

@keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}*/
#loader {
    position: absolute;
    left: 50%;
    top: 50%;
    z-index: 1;
    width: 150px;
    height: 150px;
    margin: -75px 0 0 -75px;
    border: 16px solid #f3f3f3;
    border-radius: 50%;
    border-top: 16px solid #3498db;
    width: 120px;
    height: 120px;
    -webkit-animation: spin 2s linear infinite;
    animation: spin 2s linear infinite;
}

@-webkit-keyframes spin {
    0% { -webkit-transform: rotate(0deg); }
    100% { -webkit-transform: rotate(360deg); }
}

@keyframes spin {
    0% { transform: rotate(0deg); }
    100% { transform: rotate(360deg); }
}

/* Add animation to "page content" */
.animate-bottom {
    position: relative;
    -webkit-animation-name: animatebottom;
    -webkit-animation-duration: 1s;
    animation-name: animatebottom;
    animation-duration: 1s
}

@-webkit-keyframes animatebottom {
    from { bottom:-100px; opacity:0 }
    to { bottom:0px; opacity:1 }
}

@keyframes animatebottom {
    from{ bottom:-100px; opacity:0 }
    to{ bottom:0; opacity:1 }
}

#myDiv {
    display: none;
    text-align: center;
}
#loading {
    display: none;
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
<div class="row align-center">
    <div class="columns small-12 large-12">
        <div style="display: none;" id="smgSuccess" class="animate-bottom text-center">
            <h4 class="purple-text"><strong>Model Created!</strong></h4>
            <h5>Your model has been deposited successfully!</h5>
            <img src="https://i.imgur.com/GwStPmg.png" class="fit-image" style="width: 15%">
            <p class="purple-text">
                It has been assigned perennial identifier <a href="" id="modelURL" target="_blank"></a>.
            </p>
            <p class="purple-text">
                Thank you for submitting your model.
            </p>
        </div>
    </div>
</div>

<div class="row align-center">
    <div class="columns small-12 medium-12 large-12">
        <div style="display: none;" id="smgFailure" class="animate-bottom text-center">
            <h4 class="purple-text"><strong>Model Creation Failed!</strong></h4>
            <h5>Your submisson has been unsuccessful!</h5>
        </div>
    </div>
</div>

<script>
    // TODO: remove showWaitingIcon
    var myVar;

    function showWaitingIcon() {
        myVar = setTimeout(showPage, 3000);
    }

    function showPage() {
        // depending on the status of the completion, it can show success or failure
        if (currentValidation) {

        } else {

        }
        document.getElementById("loader").style.display = "none";
        document.getElementById("myDiv").style.display = "block";
    }

    function completeSubmission() {
        // TODO: show a waiting prompt message
        // showWaitingIcon();

        // perform an ajax call to complete the submission
        // the function should return true or false to indicate the state of submission
        // I suppose it fails meaning the currentValidation to be false
        const url = "${createLink(controller: "submission", action: "completeSubmission")}";
        $('#loading').show();
        $.ajax({
            type: "POST",
            url: url,
            data: {
                isUpdate: isUpdate,
                modelFile: JSON.stringify(modelFile),
                additionalFiles: JSON.stringify(additionalFiles),
                modelInfo: JSON.stringify(modelInfo),
                publication: JSON.stringify(publication),
                revisionComments: revisionComments,
                modelId: modelId
            },
            dataType: "json"
        })
        .done(function (response) {
            console.log(response);
            currentValidation = response.status === "Success" ? true : false;
            $('#completionMessage').html(response.message);
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
