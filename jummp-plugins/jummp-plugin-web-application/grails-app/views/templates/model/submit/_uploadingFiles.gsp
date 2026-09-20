<link rel="stylesheet"
      href="https://cdn.jsdelivr.net/npm/pretty-checkbox@3.0/dist/pretty-checkbox.min.css"/>
<style>
#files {
    overflow-y: scroll !important;
    min-height: 320px;
    margin: 0; /* reset this property against ebi-global */
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

.media {
    margin: 5px;
    padding: 5px;
    list-style-type: none;
    background-color: #d8d8e6;
}
.media > hr {
    margin: 0;
}

.btn-remove-file {
    cursor: pointer;
    font-size: x-large;
    color: red;
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
                <h3 class="text-muted" style="margin-top: 1rem!important; margin-bottom: 1rem!important;">Drag
            &amp; drop files here</h3></div>

            <div class="padding-3 margin-3 btn button" style="margin-bottom: 1rem!important; display: block; width:
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
                <ul class="list-unstyled" id="files">
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
                    <div class="columns small-12 medium-4 large-4">
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
                                 style="width: 0"
                                 aria-valuenow="0" aria-valuemin="0" aria-valuemax="100">
                            </div>
                        </div>
                    </div>
                    <div class="columns small-12 medium-5 large-5">
                        <label>File Description
                            <input type="text" name="fileDescription" class="file-description"
                                   placeholder="Describe this file. For example: What is this file used for?">
                        </label>
                    </div>
                    <div class="columns small-12 medium-2 large-2">
                        <div class="pretty p-switch p-fill">
                            <input type="radio" name="isModelFile" class="is-model-file">
                            <div class="state p-success">
                                <label style="line-height: 0; margin-left: 5px">Main model file</label>
                            </div>
                        </div>
                    </div>
                    <div class="columns small-12 medium-1 large-1">
                        <i class="icon icon-common icon-times btn-remove-file" data-file-name="%{filename}%"></i>
                    </div>
                </div>
            </div>
        </li>
        </script>
    </div>
</div><!-- /file list -->
<input type="button" name="next" id="uploadFileNext" class="next action-button" value="Next" />
<script
    src="${resource(contextPath: serverURL, dir: '/js/biomodels/uploader-1.0.2', file: 'biomodels-ui.js')}">
</script>
<script type="text/javascript">
    const duplicateFilesMsg = "The file names in your submission should not be identical. " +
        "Please double-check the recently uploaded files having the name: ";
    const invalidFileNameMsg = "The file name is invalid (use only letters, digits, dots, hyphens and underscores).";
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
             * and refer it here. See more options and callbacks in README.md at
             * https://github.com/danielm/uploader/blob/master/README.md#options
             */
            // maxFileSize: 12000000, // 12 Megs
            extraData: {
                "submissionFolder": "${submissionFolder}"
            },
            multiple: true,
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
                checkIdenticalFileNames();

                ui_add_log('All pending transfers finished');
                console.log("Uploaded successfully");
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

    function retrieveUploadedFiles() {
        // All files in the queue are processed (success or error)
        return $('.file-name').map(function () {
            return this.innerHTML;
        }).get();
    }

    function buildUploadedFilesMap() {
        const allMediaElements = $('.media');
        return allMediaElements.map(function () {
            let filename = $(this).find("strong.file-name").html();
            let description = $(this).find("input.file-description").val();
            let isModelFile = $(this).find("input.is-model-file")[0].checked;
            let originalFilesize = $(this).find("a.original-file-size").text();
            return {
                id: $(this).prop("id"), filename: filename, description: description, isModelFile: isModelFile,
                originalFilesize: originalFilesize
            };
        }).get();
    }

    // the names of the files that break the rule of the acceptable characters
    function retrieveFileNamesBreakingTheRule() {
        return retrieveUploadedFiles().filter((filename) => !checkAcceptableCharactersForFileName(filename));
    }

    function checkIdenticalFileNames() {
        let uploadedFiles = retrieveUploadedFiles();
        let uploadedFilesMap = {};
        uploadedFiles.forEach(function(x) {
            uploadedFilesMap[x] = (uploadedFilesMap[x] || 0) + 1;
        });
        let messages = [];
        let msg = "";
        $.each(uploadedFilesMap, (filename, count) => {
            if (count > 1) {
                msg = duplicateFilesMsg + filename +".";
                messages.push(msg);
            }
        });
        return messages;
    }

    function validateFileUpload() {
        errorMessages = [];
        currentValidation = false;
        const ids = buildUploadedFilesMap();
        // a name that breaks the rule is not said in a message of its own: it goes in the line of its file, with the
        // other problems of that file, when the files have been checked
        const invalidNames = retrieveFileNamesBreakingTheRule();
        let messages = checkIdenticalFileNames();
        handleErrorMessages(messages);
        let msg = "";
        if (!currentValidation) {
            return $.ajax({
                type: "POST",
                url: "${createLink(controller: "submission", action: "renderFileUploadFailures")}",
                success: function() {
                    msg = "Failed to upload your files.";
                    showNotification(msg);
                    console.log(msg);
                },
                error: function() {
                    msg = "There have been internal errors when trying to upload your files.";
                    console.log(msg);
                    showNotification(msg);
                    toastr.error(msg);
                }
            });
        }

        return $.ajax({
            type: "POST",
            url: "${createLink(controller: "submission", action: "processUploadFiles")}",
            data: {
                submissionSessionId: "${submissionFolder}",
                submissionFolder: "${submissionFolder}",
                uploadingFiles: JSON.stringify(ids),
                files: JSON.stringify(existingFiles),
                isUpdate: isUpdate,
                changesMade: [...changesMade]
            },
            async: true,
            dataType: "JSON",
            beforeSend: function() {
                console.log("Validating files and extracting some information if it is available...");
            },
            success: function(response) {
                console.log("Updating objects and variables tighten to the form...");
                changesMade = response["changesMade"];
                let data = response["filesMap"];
                let msg = "";
                if (data.length) {
                    // what is wrong with each file, on one line that starts with the name of the file: the file is
                    // empty, its description is not filled in, its name is invalid
                    $.each(data, function (i, f) {
                        let summary = f["validateFileSummary"] || "";
                        if (invalidNames.includes(f["filename"]) && !f["validateFileName"]) {
                            // the server accepts more characters in a name than this page does
                            summary = (summary + " " + invalidFileNameMsg).trim();
                        }
                        if (summary) {
                            errorMessages.push(boldFileName(f["filename"]) + ": " + summary);
                        }
                    });
                    const haveAllDescriptions = data.filter(f => f["validateFileDescription"] &&
                        f["validateFileDescription"].length > 0).length === 0;
                    const hasOneModelFile = data.filter(e => e.isModelFile).length === 1;
                    let modelFileWithNoErrors = true;
                    let allFileNamesValid = true;
                    if (!hasOneModelFile) {
                        msg =
                            "Please verify the Main Model file radio box. A submission must have at least one main model file.";
                        errorMessages.push(msg);
                    } else {
                        modelFile = data.filter(e => e.isModelFile)[0];
                        if (!modelFile) {
                            modelFileWithNoErrors = false;
                        } else {
                            // model file

                            // populate the publication namespace and accession guessed when trying to detect model format
                            publication = modelFile["detectedModelFormat"]["pubURI"];
                            guessedPublicationNamespace = modelFile["detectedModelFormat"]["namespace"];
                            guessedPublicationCollectionLabel = modelFile["detectedModelFormat"]["collectionLabel"];
                            guessedPublicationAccession = modelFile["detectedModelFormat"]["accession"]

                            modelFileWithNoErrors = modelFile["validateFileErrors"].length === 0 && modelFile["validSyntax"]
                            if (!modelFile["validSyntax"]) {
                                consolidateErrorMessages(modelFile["filename"], modelFile["validateSyntaxErrors"]);
                            } else if (modelFile["validateSyntaxErrors"].length !== 0)  {
                                toastr.clear();
                                toastr.warning(modelFile["validateSyntaxErrors"])
                            }
                            // the name of the model file has to be valid
                            let isModelMainFileNameValid = !(modelFile["validateFileName"] &&
                                modelFile["validateFileName"].length > 0);

                            // additional files
                            additionalFiles = data.filter(e => !e.isModelFile);
                            let areAdditionalFileNamesValid = true;
                            if (additionalFiles.length > 0) {
                                // no file having errors
                                modelFileWithNoErrors = additionalFiles.filter(f =>
                                    f["validateFileErrors"].length > 0).length === 0;
                                $.each(additionalFiles, function (i, f) {
                                    // the name of each additional file has to be valid
                                    if (f["validateFileName"] && f["validateFileName"].length > 0) {
                                        areAdditionalFileNamesValid = false;
                                    }
                                });
                            }
                            // update the validation of all file names
                            allFileNamesValid = isModelMainFileNameValid && areAdditionalFileNamesValid;
                            // model info
                            modelInfo = modelFile["detectedModelInfo"];
                        }
                    }
                    currentValidation = hasOneModelFile && haveAllDescriptions && modelFileWithNoErrors &&
                        allFileNamesValid && invalidNames.length === 0;
                } else {
                    currentValidation = false;
                    errorMessages.push("A submission must have at least only one main model file.")
                }
            },
            error: function(jqXHR, textStatus, errorThrown) {
                let msg = JSON.parse(JSON.stringify(errorThrown));
                msg = "Error: " + msg;
                console.log(msg);
                toastr.error(msg);
                $('.flashNotificationDiv').html(msg).show();
            }
        });
    }

    $('#files').on("click", '.btn-remove-file', function () {
       let parent = $(this).parent();
       let grandparent = parent.parent();
       grandparent = grandparent.parent();
       parent = grandparent.parent();
       parent.remove();
    });

    function handleErrorMessages(messages) {
        if (messages.length) {
            console.log(messages);
            showFlashMessages(messages);
            toastr.clear();
            toastr.error(messages);
            currentValidation = false;
        } else {
            hideFlashMessages();
            currentValidation = true;
        }
    }

    /**
     * The name of a file in bold, for the messages that are shown as html, such as the ones of the notification box.
     * It is defined here, with the code that uses it, and not in helpers.js: a browser that has an old copy of that file
     * cached would lack it, and the messages would not be shown.
     *
     * @param filename  A String denoting the file name, which is escaped
     * @returns {string}    The html of the name in bold
     */
    function boldFileName(filename) {
        return "<strong>" + $("<div>").text(filename).html() + "</strong>";
    }

    function consolidateErrorMessages(filename, messages) {
        if (typeof messages === "undefined") { return false; }
        if (messages.length > 0) {
            $.each(messages, function (id, msg) {
                errorMessages.push(boldFileName(filename) + ": " + msg);
            });
            return true;
        }
        return false;
    }
</script>
