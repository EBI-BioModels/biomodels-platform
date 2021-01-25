<style>
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
    /*overflow-y: scroll; */
    /*height: 279px*/
}
</style>
<div class="row">
    <div class="columns small-12 medium-10 large-10">
        <h2 class="fs-title">${uploadingFilesHeading}</h2>
        <p style="padding-bottom:1em"><g:message code="submission.biomodels.upload.explanation"/></p>
    </div>

    <div class="columns small-12 medium-2 large-2">
        <h2 class="steps">Step 1 - 5</h2>
    </div>
</div>
<div class="row">
    <div class="columns small-12 medium-12 large-12">
        <!-- Our markup, the important part here! -->
        <div id="drag-and-drop-zone" class="dm-uploader">
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
</div>
<div class="row">
    <div class="columns small-12 medium-12 large-12">
        <div class="card">
            <div class="card-header">
                <h3>Model File List <span style="font-size: small; color: red">Important: </span><span
                    style="font-size: small">Please
                make sure only one main model file by checking the corresponding the Main Model file
                radio box</span></h3>
            </div>
            <div class="card-section">
                <ul class="list-unstyled" id="files" style="margin-right: 1.25rem">
                <g:if test="${files}">
                    <jummp:renderExistingFiles files="${files}" />
                </g:if>
                <g:else>
                    <li class="empty" style="margin-left: 0">No files uploaded.</li>
                </g:else>
                </ul>
            </div>
        </div>
        <!-- File item template -->
        <script type="text/html" id="files-template">
        <li class="media">
            <div class="media-body mb-1">
                <div class="row">
                    <div class="columns small-12 medium-5 large-5">
                        <p class="mb-2">
                            <strong class="file-name">%%filename%%</strong> - Size: <strong
                        class="file-size">%%filesize%%</strong>, Status: <span
                            class="text-muted">
                            Waiting</span>
                            <a class="original-file-size" style="display: none">%%originalFilesize%%</a>
                        </p>
                        <div class="progress mb-2">
                            <div class="progress progress-bar progress-bar-striped progress-bar-animated bg-primary"
                                 role="progressbar"
                                 style="width: 0%"
                                 aria-valuenow="0" aria-valuemin="0" aria-valuemax="100">
                            </div>
                        </div>
                    </div>
                    <div class="columns small-12 medium-6 large-6">
                        <label>File Description
                            <input type="text" name="fileDescription" class="file-description"
                                   placeholder="Describe this file. For example: What is this file used for?">
                        </label>
                    </div>
                    <div class="columns small-12 medium-1 large-1">
                        <label>Main model file
                            <input type="radio" name="isModelFile" class="is-model-file"></label>
                    </div>
                </div>
            </div>
            <button type="button" name="removeFile" class="button btn-remove-file">Remove</button>
            <hr class="mt-1 mb-1" style="color: lightgrey; max-width: 100%"/>
        </li>
        </script>
    </div>
</div><!-- /file list -->
<input type="button" name="next" id="uploadFileNext" class="next action-button" value="Next" />
<script
    src="${resource(contextPath: serverURL, dir: '/js/biomodels/uploader-1.0.2', file: 'biomodels-ui.js')}">
</script>
<script type="text/javascript">
    $(function () {
        /*
         * For the sake keeping the code clean and the examples simple this file
         * contains only the plugin configuration & callbacks.
         *
         * UI functions ui_* can be located in: biomodels-ui.js
         */
        $('#drag-and-drop-zone').dmUploader({ //
            url: '${createLink(controller: "model", action: "uploadFile")}',
            /**
             * We have no max size limit. Notes: the default is 0 meaning no size limit.
             * If we want to use this customisable property, please externalise its value in Config.groovy
             * and refer it here.
             */
            // maxFileSize: 12000000, // 12 Megs
            extraData: {
                "submissionFolder": "${submissionFolder}"
            },
            onDragEnter: function () {
                // Happens when dragging something over the DnD area
                this.addClass('active');
            },
            onDragLeave: function () {
                // Happens when dragging something OUT of the DnD area
                this.removeClass('active');
            },
            onInit: function () {
                // Plugin is ready to use
                ui_add_log('Penguin initialized :)', 'info');
            },
            onComplete: function () {
                // All files in the queue are processed (success or error)
                ui_add_log('All pending transfers finished');
            },
            onNewFile: function (id, file) {
                // When a new file is added using the file selector or the DnD area
                ui_add_log('New file added #' + id);
                ui_multi_add_file(id, file);
            },
            onBeforeUpload: function (id) {
                // about tho start uploading a file
                ui_add_log('Starting the upload of #' + id);
                ui_multi_update_file_status(id, 'uploading', 'Uploading...');
                ui_multi_update_file_progress(id, 0, '', true);
            },
            onUploadCanceled: function (id) {
                // Happens when a file is directly canceled by the user.
                ui_multi_update_file_status(id, 'warning', 'Canceled by User');
                ui_multi_update_file_progress(id, 0, 'warning', false);
            },
            onUploadProgress: function (id, percent) {
                // Updating file progress
                ui_multi_update_file_progress(id, percent);
            },
            onUploadSuccess: function (id, data) {
                // A file was successfully uploaded
                ui_add_log('Server Response for file #' + id + ': ' + JSON.stringify(data));
                ui_add_log('Upload of file #' + id + ' COMPLETED', 'success');
                ui_multi_update_file_status(id, 'success', 'Upload Complete');
                ui_multi_update_file_progress(id, 100, 'success', false);
            },
            onUploadError: function (id, xhr, status, message) {
                ui_multi_update_file_status(id, 'danger', message);
                ui_multi_update_file_progress(id, 0, 'danger', false);
            },
            onFallbackMode: function () {
                // When the browser doesn't support this plugin :(
                ui_add_log('Plugin cant be used here, running Fallback callback', 'danger');
            },
            onFileSizeError: function (file) {
                ui_add_log('File \'' + file.name + '\' cannot be added: size excess limit', 'danger');
            }
        });
    });

    function validateFileUpload() {
        errorMessages = [];
        currentValidation = false;
        let nbModelFiles = 0;
        const allMediaElements = $('.media');
        const ids = allMediaElements.map(function () {
            let filename = $(this).find("strong.file-name").html();
            let description = $(this).find("input.file-description").val();
            let isModelFile = $(this).find("input.is-model-file")[0].checked;
            let originalFilesize = $(this).find("a.original-file-size").text();
            return { id: $(this).prop("id"), filename: filename , description: description, isModelFile: isModelFile,
                            originalFilesize: originalFilesize };
        }).get();
        $.ajax({
            type: "POST",
            url: "${createLink(controller: "submission", action: "processUploadFiles")}",
            data: {
                submissionSessionId: "${submissionSessionId}",
                submissionFolder: "${submissionFolder}",
                uploadingFiles: JSON.stringify(ids),
                files: JSON.stringify(existingFiles),
                isUpdate: isUpdate
            },
            async: false,
            dataType: "JSON",
            success: function(response) {
                changesMade = response.changesMade;
                let data = response["filesMap"];
                let msg = "";
                if (data.length) {
                    const haveAllDescriptions = data.filter(e => e.description === "").length === 0;
                    if (!haveAllDescriptions) {
                        msg = "Please check the file description text boxes. They are not allowed empty.";
                        errorMessages.push(msg);
                    }
                    const hasOneModelFile = data.filter(e => e.isModelFile).length === 1;
                    let modelFileWithNoErrors = true;
                    if (!hasOneModelFile) {
                        msg =
                            "Please verify the Main Model file radio box. A submission must have at least only one main model file.";
                        errorMessages.push(msg);
                    } else {
                        modelFile = data.filter(e => e.isModelFile)[0];
                        if (!modelFile) {
                            modelFileWithNoErrors = false;
                        } else {
                            // model file
                            modelFileWithNoErrors = modelFile["validateFileErrors"].length === 0 && modelFile["validSyntax"]
                            consolidateErrorMessages(modelFile["filename"], modelFile["validateFileErrors"]);
                            if (!modelFile["validSyntax"]) {
                                consolidateErrorMessages(modelFile["filename"], modelFile["validateSyntaxErrors"]);
                            } else if (modelFile["validateSyntaxErrors"].length !== 0)  {
                                toastr.clear();
                                toastr.warning(modelFile["validateSyntaxErrors"])
                            }
                            // additional files
                            additionalFiles = data.filter(e => !e.isModelFile);
                            if (additionalFiles.length > 0) {
                                // there is no file having errors
                                modelFileWithNoErrors = additionalFiles.filter(f => f["validateFileErrors"].length
                                    > 0).length === 0;
                                $.each(additionalFiles, function (i, f) {
                                    consolidateErrorMessages(f["filename"], f["validateFileErrors"]);
                                });
                            }
                        }
                    }
                    currentValidation = hasOneModelFile && haveAllDescriptions && modelFileWithNoErrors;
                } else {
                    currentValidation = false;
                    errorMessages.push("A submission must have at least only one main model file.")
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                console.log("inside error " + JSON.stringify(errorThrown));
                console.log(textStatus);
            }
        });
    }

    $('#files').on("click", '.btn-remove-file', function () {
       let parent = $(this).parent();
       parent.remove();
    });

    function consolidateErrorMessages(filename, messages) {
        if (messages.length > 0) {
            $.each(messages, function (id, msg) {
                errorMessages.push(filename + ": " + msg);
            });
        }
    }
</script>
