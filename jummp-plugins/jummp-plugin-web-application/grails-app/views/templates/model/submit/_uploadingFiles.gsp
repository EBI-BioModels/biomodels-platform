<style>
/*
A couple styles to make the demo page look good
*/
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
</style>
<div class="row">
    <div class="columns small-12 medium-7 large-7">
        <h2 class="fs-title">Select and upload your model files</h2>
    </div>

    <div class="columns small-12 medium-5 large-5">
        <h2 class="steps">Step 1 - 5</h2>
    </div>
</div>
<div class="row">
    <div class="columns small-12 medium-6 large-offset-2 large-4">
        <!-- Our markup, the important part here! -->
        <div id="drag-and-drop-zone" class="dm-uploader p-5">
            <div class="padding-3 margin-3"><h3 class="text-muted">Drag &amp; drop files here</h3></div>

            <div class="">
                <span>Open the file Browser</span>
                <input type="file" title='Click to add Files' class="btn btn-primary" name="modelFiles[]" multiple />
            </div>
        </div><!-- /uploader -->

    </div>
    <div class="columns small-12 medium-6 large-4">
        <div class="card">
            <div class="card-header">
                <h3>Model File List</h3>
            </div>
            <div class="card-section">
                <ul class="list-unstyled p-2 d-flex flex-column col" id="files">
                    <li class="">No files uploaded.</li>
                </ul>
            </div>
        </div>
    </div>
</div><!-- /file list -->
<input type="button" name="next" class="next action-button" value="Next"/>
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
            $("#status_" + fileId).text(event.target.responseText);
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

        ajax.open("POST", "${createLink(controller: "model", action: "upload2")}", true); // Your API .net, php

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
