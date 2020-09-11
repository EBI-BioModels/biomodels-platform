<style>
body {
    padding-bottom: 2rem;
    padding-top: 4rem;
}
.row {
    margin-bottom: 1rem;
}
[class*="col-"] {
    padding-top: 1rem;
    padding-bottom: 1rem;
}
hr {
    margin-top: 2rem;
    margin-bottom: 2rem;
}
#files {
    overflow-y: scroll !important;
    min-height: 320px;
}
@media (min-width: 768px) {
    #files {
        min-height: 0;
    }
}
#debug {
    overflow-y: scroll !important;
    height: 180px;
}

.dm-uploader {
    border: 0.25rem dashed #A5A5C7;
    text-align: center;
}
.dm-uploader.active {
    border-color: red;

    border-style: solid;
}
.card-section {
    border: 0.25rem solid #A5A5C7;
    overflow: scroll;
}
</style>
<div class="row">
    <div class="columns small-12 medium-10 large-10">
        <h2 class="fs-title">Select and upload your model files</h2>
        <p style="padding-bottom:1em"><g:message code="submission.biomodels.upload.explanation"/></p>
    </div>

    <div class="columns small-12 medium-2 large-2">
        <h2 class="steps">Step 1 - 5</h2>
    </div>
</div>
<div class="row">
    <div class="columns small-12 medium-6 large-4">
        <!-- Our markup, the important part here! -->
        <div id="drag-and-drop-zone" class="dm-uploader" style="padding: 3rem!important;">
            <div class="padding-3 margin-3">
                <h3 class="text-muted" style="margin-top: 3rem!important; margin-bottom: 3rem!important;">Drag
            &amp; drop files here</h3></div>

            <div class="padding-3 margin-3 btn button" style="margin-bottom: 3rem!important; display: block; width:
            100%">
                <span>Open the file Browser</span>
                <input type="file" title='Click to add Files' class="btn btn-primary" name="modelFiles[]" multiple
                       style="opacity: 0" />
            </div>
        </div><!-- /uploader -->

    </div>
    <div class="columns small-12 medium-6 large-8">
        <div class="card">
            <div class="card-header">
                <h3>Model File List</h3>
            </div>
            <div class="card-section" style="overflow-scrolling: auto">
                <ul class="list-unstyled" id="files">
                    <li class="empty">No files uploaded.</li>
                </ul>
            </div>
        </div>
        <!-- File item template -->
        <script type="text/html" id="files-template">
        <li class="media">
            <div class="media-body mb-1">
                <p class="mb-2">
                    <strong class="file-name">%%filename%%</strong> - Status: <span class="text-muted">Waiting</span>
                </p>
                <div class="progress mb-2">
                    <div class="progress progress-bar progress-bar-striped progress-bar-animated bg-primary"
                         role="progressbar"
                         style="width: 0%"
                         aria-valuenow="0" aria-valuemin="0" aria-valuemax="100">
                    </div>
                </div>
                <div class="row">
                    <div class="small-12 medium-11 large-11 columns">
                        <label>File Description
                            <input type="text" name="fileDescription" class="file-description"
                                   placeholder="Describe this file. For example: What is this file used for?">
                        </label>
                    </div>
                    <div class="small-12 medium-1 large-1 columns">
                        <label>Model file
                            <input type="checkbox" name="isModelFile" class="is-model-file"></label>
                    </div>
                </div>
                <hr class="mt-1 mb-1" />
            </div>
        </li>
        </script>
    </div>
</div><!-- /file list -->
<input type="button" name="next" id="uploadFileNext" class="next action-button" value="Next" onclick="clickNextOnFileUpload()"/>
<script
    src="${resource(contextPath: serverURL, dir: '/js/biomodels/uploader-1.0.2', file: 'biomodels-ui.js')}"></script>
<script
    src="${resource(contextPath: serverURL, dir: '/js/biomodels/uploader-1.0.2', file: 'biomodels-config.js')}"></script>
<script type="text/javascript">
    function uploadFiles() {
        const file = document.getElementById("fileUploader"); // All files
        for (let i = 0; i < file.files.length; i++) {
            uploadSingleFile(file.files[i], i);
        }
    }
    function uploadSingleFile(file, i) {
        const fileId = i;
        const ajax = new XMLHttpRequest();
        // Progress Listener
        ajax.upload.addEventListener("progress", function (e) {
            const percent = (e.loaded / e.total) * 100;
            $("#status_" + fileId).text(Math.round(percent) + "% uploaded, please wait...");
            $('#progressbar_' + fileId).css("width", percent + "%")
            $("#notify_" + fileId).text("Uploaded " + (e.loaded / 1048576).toFixed(2) + " MB of " + (e.total / 1048576).toFixed(2) + " MB ");
        }, false);
        // Load Listener
        ajax.addEventListener("load", function (e) {
            $("#status_" + fileId).text(e.target.responseText);
            $('#progressbar_' + fileId).css("width", "100%")

            // Hide cancel button
            const _cancel = $('#cancel_' + fileId);
            _cancel.hide();
        }, false);
        // Error Listener
        ajax.addEventListener("error", function (e) {
            $("#status_" + fileId).text("Upload Failed");
        }, false);
        //Abort Listener
        ajax.addEventListener("abort", function (e) {
            $("#status_" + fileId).text("Upload Aborted");
        }, false);

        ajax.open("POST", "${createLink(controller: "model", action: "uploadFile")}", true); // Your API .net, php

        const uploaderForm = new FormData(); // Create new FormData
        uploaderForm.append("file", file); // append the next file for upload
        ajax.send(uploaderForm);

        // Cancel button
        const _cancel = $('#cancel_' + fileId);
        _cancel.show();

        _cancel.on('click', function () {
            ajax.abort();
        })
    }

    function clickNextOnFileUpload() {
        const allMediaElements = $('.media');
        const ids = allMediaElements.map(function () {
            let filename = $(this).find("strong.file-name").html();
            let description = $(this).find("input.file-description").val();
            let isModelFile = $(this).find("input.is-model-file")[0].checked;
            return { id: $(this).prop("id"), filename: filename , description: description, isModelFile: isModelFile };
        }).get();
        $.ajax({
            type: "POST",
            url: "${createLink(action: "reconcileUploadingFiles")}",
            data: {
                uploadingFiles: JSON.stringify(ids)
            },
            dataType: "application/json",
            success: function (data) {
                console.log("Data loaded: " + data);
            }
        });
    }

    $(document).ready(function () {
        $('input[type=file]').change(function () {
            $('#btnUpload').show();
            $('#divFiles').html('');
            for (let i = 0; i < this.files.length; i++) {
                // Progress bar and status label's for each file generate dynamically
                const fileId = i;
                $("#divFiles").append('<div class="col-md-12">' +
                    '<div class="progress-bar progress-bar-striped active" id="progressbar_' + fileId + '" role="progressbar" aria-valuemin="0" aria-valuemax="100" style="width:0%"></div>' +
                    '</div>' +
                    '<div class="col-md-12">' +
                    '<div class="col-md-6">' +
                    '<input type="button" class="btn btn-danger" style="display:none;line-height:6px;height:25px" id="cancel_' + fileId + '" value="cancel">' +
                    '</div>' +
                    '<div class="col-md-6">' +
                    '<p class="progress-status" style="text-align: right;margin-right:-15px;font-weight:bold;color:saddlebrown" id="status_' + fileId + '"></p>' +
                    '</div>' +
                    '</div>' +
                    '<div class="col-md-12">' +
                    '<p id="notify_' + fileId + '" style="text-align: right;"></p>' +
                    '</div>');
            }
        });
    });
</script>
