<div id="txtStatus" style="color: #ED0000; font-weight: 500; font-size: larger"></div>
<div id="MoMEntryEditorForm">
    <form id="momEntryForm">
        <div class="row">
            <div class="small-12 medium-6 large-6 columns">
                <label for="authors" class="required">Authors</label>
                <input type="text" id="authors" name="authors" required
                       placeholder="Authors of the MoM entry who is mainly creating and editing it"
                       value="${entry?.authors}">
                <label for="title" class="required">Title</label>
                <input type="text" id="title" name="title" required
                       placeholder="Title of the MoM entry which will be displayed on the MoM page"
                       value="${entry?.title}">
                <label for="shortDescription" class="required">Short Description</label>
                <textarea id="shortDescription" required
                          placeholder="Enter a short description for this MoM entry.
                          This description will be shown on the MoM widget. It shouldn't be left empty."
                          aria-multiline="true" rows="5"
                          style="white-space: pre-wrap">${entry?.shortDescription}</textarea>
                <textarea id="tmpShortDescription"
                          aria-multiline="true" rows="5"
                          style="white-space: pre-wrap; display: none">
                    ${entry?.shortDescription}</textarea>
                <label for="models" class="required">Models associated with (separated by commas)</label>
                <input type="text" id="models" name="models" required
                       placeholder="Model identifiers associated with this entry separated by commas.
                       These identifiers must be determined to create backlinks to be shown underneath the model name on the model display page"
                       value="${entry?.models}">
                <div class="row">
                    <div class="small-12 medium-6 large-6 columns">
                        <label for="publicationDate" class="required">Publication Date</label>
                        <input type="text" id="publicationDate" name="publicationDate" required
                               placeholder="Publication Date of MoM"
                               value="${dateFormat.format(entry?.publicationDate)}">
                    </div>
                    <div class="small-12 medium-6 large-6 columns">
                        <label for="lastUpdated" class="required">Last Updated Date</label>
                        <input type="text" id="lastUpdated" name="lastUpdated" required
                               placeholder="Latest Updated Date of MoM"
                               value="${dateFormat.format(entry?.lastUpdated)}">
                    </div>
                </div>
            </div>
            <div class="small-12 medium-6 large-6 columns">
                <label for="previewImage">Preview Image</label>
                <g:if test="${entry?.previewImage}">
                    <img src="data:image/jpeg;base64,${entry?.previewImage}"
                         id="previewImage"
                         title="Click on the thumbnail to view the result(s)" />
                </g:if>
                <g:else>
                    <img src="${grailsApplication.config.grails.serverURL}/images/biomodels/No-Image-Available.jpg"
                         id="previewImage"
                         title="The preview image is unavailable" />
                </g:else>
            </div>
        </div>

        <div class="row">
            <div class="small-12 medium-6 large-6 columns" style="text-align: left">
                <a class="button" onclick="window.history.back()"
                   title="Back to the model display page">Back</a>
                <button type="button" class="button" id="btnSave">Save</button>
                <button type="button" class="button" id="btnReset">Reset</button>
            </div>
            <div class="small-12 medium-6 large-6 columns" style="text-align: right">
                <label for="btnUploadPreviewImage" class="button">Upload an image</label>
                <input type="file" id="btnUploadPreviewImage" class="show-for-sr" accept="image/*"
                       style="text-align: right; direction: ltr">
            </div>
        </div>
    </form>
</div>

<g:javascript>
    var imgUploadedStream;
    var mimeType = 'unknown';
    var messages = {}; // or: new Object(); or: new Map(); but not supported in IE

    function showWarningMessage() {
        var imgSrc = "${grailsApplication.config.grails.serverURL}/images/biomodels/unacceptable.png";
        $('#previewImage').attr('src', imgSrc);
        $('#previewImage').attr('title', 'This format is not acceptable');
    }

    $('#publicationDate').datepicker({
        dateFormat: 'yy-mm-dd',
        onSelect: function(datetext) {
            datetext = datetext + getTimeStamp();
            $('#datepicker').val(datetext);
            $(this).val(datetext);
        }
    });

    $('#lastUpdated').datepicker({
        dateFormat: 'yy-mm-dd',
        onSelect: function(datetext) {
            datetext = datetext + getTimeStamp();
            $('#datepicker').val(datetext);
            $(this).val(datetext);
        }
    });

    function checkCustomValidity() {
        return Object.keys(messages).length === 0;
    }

    function checkRequiredValidity() {
        /* check whether the preview image is available or not */
        var previewImage = $('#previewImage').attr('src');
        var isPreviewImageAvailable = true;
        if (previewImage.indexOf("http") >= 0) {
            isPreviewImageAvailable = true;
        } else {
            var re = new RegExp('data:image\/');
            isPreviewImageAvailable = re.exec(previewImage);
        }
        /* combine with the built-in validation check */
        var isValid = $('#momEntryForm')[0].checkValidity() && isPreviewImageAvailable;
        if (isValid) {
            delete messages["invalidForm"];
        } else {
            set(messages, "invalidForm", "${g.message(code: "model.biomodels.modelOfTheMonth.editor.invalidForm")}");
        }
        return isValid;
    }

    function buildMoMEntryTC() {
        var id = "${entry?.id}";
        var authors = $('#authors').val();
        var title = $('#title').val();
        var shortDescription = $('#shortDescription').val();
        var publicationDate = $('#publicationDate').val();
        var lastUpdated = $('#lastUpdated').val();
        var models = $('#models').val();
        var momEntryTC = {
            'id': id,
            'authors': authors,
            'title': title,
            'shortDescription': shortDescription,
            'publicationDate': publicationDate,
            'lastUpdated': lastUpdated,
            'updated': ${entry?.updated},
            'models': models
        };
        if (imgUploadedStream) {
            momEntryTC['previewImage'] = imgUploadedStream;
        } else {
            var base64ImgStr = $('img#previewImage').attr('src');
            if (base64ImgStr.indexOf('base64,')>=0) {
                mimeType = base64ImgStr.substring(5, base64ImgStr.indexOf(";"));
                base64ImgStr = base64ImgStr.substring(base64ImgStr.indexOf('base64,') + 'base64,'.length);
                momEntryTC['previewImage'] = base64ImgStr;
            } else {
                console.log("The entry of Model Of The Month does not have the preview image");
            }
        }
        momEntryTC['mimeType'] = mimeType;
        return momEntryTC;
    }

    $("#btnUploadPreviewImage").change(function(){
        var re = new RegExp('image\/');
        //var re = new RegExp('(.*?)'); accept everything
        if (this.files && this.files[0]) {
            /* validate file type */
            var imageFile = this.files[0];
            mimeType = imageFile.type;
            if (re.exec(mimeType)) {
                imgUploadedStream = previewImage(this, '#previewImage');
                delete messages["onlyAcceptImages"];
            } else {
                set(messages, "onlyAcceptImages",
                        "${g.message(code: "model.biomodels.modelOfTheMonth.editor.onlyAcceptImages")}");
                showWarningMessage();
            }

            /* validate file size */
            var MAX_SIZE = 2 * 1024 * 1024; // 2MB ~ 2_100_000 is the allowed maximum size of the uploading image file
            if (imageFile.size > MAX_SIZE) {
                set(messages, "previewImageTooBig",
                        "${g.message(code: "modelOfTheMonthTransportCommand.previewImage.previewImageTooBig")}");
                showWarningMessage();
            } else {
                delete messages["previewImageTooBig"];
            }
            $('#txtStatus').html(values(messages).join("<br/>"));
        }
    });

    $('#btnSave').on("click", function(event) {
        var shouldSubmit =  checkRequiredValidity() && checkCustomValidity();
        if (shouldSubmit) {
            var updatedEntry = buildMoMEntryTC();
            "use strict";
            event.preventDefault();
            $.ajax({
                // dataType: "json",
                type: "POST",
                url: $.jummp.createLink("modelOfTheMonth", "save"),
                cache: true,
                data: updatedEntry,
                processData: true,
                async: true,
                beforeSend: function() {
                    toastr.info("The entry is being saved. Please wait...");
                },
                success: function(data, txtStatus, jqXHR) {
                    // case: jqXHR.status === 200
                    var href = window.location.href;
                    if ("${params.id}" === "" && typeof(data['id']) !== 'undefined') {
                        var newHref = href + "/" + data['id'];
                        if (window.history.pushState) {
                            window.history.pushState({}, null, newHref);
                        } else {
                            document.location.hash = newHref;
                        }
                    }
                    var msg = jqXHR.responseJSON.message;
                    toastr.clear();
                    toastr.success(msg);
                    messages = {};
                    $('#txtStatus').html("");
                },
                error: function(jqXHR, exception, errorThrown) {
                    var msg = "";
                    if (jqXHR.status === 0) {
                        msg = 'Not connect.<br/>Verify Network.<br/>'  + errorThrown;
                    } else if (jqXHR.status == 404) {
                        msg = '404 - Requested page not found.<br/>'  + errorThrown;
                    } else if (jqXHR.status == 500) {
                        msg = '500 - Internal Server Error.<br/>'  + errorThrown;
                    } else if (jqXHR.status === 422 || jqXHR.staus === 400) {
                        msg = jqXHR.status + " - " + jqXHR.statusText + "<br/>" + jqXHR.responseJSON.message;
                    } else if (exception === 'parsererror') {
                        msg = 'Requested JSON parse failed.<br/>'  + errorThrown;
                    } else if (exception === 'timeout') {
                        msg = 'Time out error.' + errorThrown;
                    } else if (exception === 'abort') {
                        msg = 'Ajax request aborted.<br/>' + errorThrown;
                    } else {
                        msg = 'Uncaught Error.<br/>' + jqXHR.responseText;
                    }
                    $('#txtStatus').html(msg);
                    toastr.clear();
                    toastr.error(msg);
                }
            });
        } else {
            $('#txtStatus').html(values(messages).join("<br/>"));
        }
    });

    $('#btnReset').on('click', function(event) {
        event.preventDefault();
        $('#authors').val("${entry?.authors}");
        $('#title').val("${entry?.title}");
        var description = $('#tmpShortDescription').val();
        $('#shortDescription').val(description);
        $('#publicationDate').val("${dateFormat.format(entry?.publicationDate)}");
        $('#lastUpdated').val("${dateFormat.format(entry?.lastUpdated)}");
    });
</g:javascript>
