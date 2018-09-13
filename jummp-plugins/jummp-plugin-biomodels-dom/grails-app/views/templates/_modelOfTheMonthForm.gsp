<%
    def previewImage = null
    if (entry?.previewImage) {
        previewImage = Base64.encoder.encodeToString(entry.previewImage)
    }
    def modelIds = ""
    if (entry?.models) {
        Collection<String> ids = entry.models.values()
        List list = new ArrayList(ids);
        modelIds = String.join(", ", list)
    }
%>
<div id="MoMEntryEditorForm">
    <form id="momEntryForm">
        <div class="row">
            <div class="small-12 medium-6 large-6 columns">
                <label for="authors" class="required">Authors</label>
                <input type="text" id="authors" name="authors" required
                       placeholder="Authors of MoM"
                       value="${entry?.authors}">
                <label for="title" class="required">Title</label>
                <input type="text" id="title" name="title" required
                       placeholder="Title of MoM"
                       value="${entry?.title}">
                <label>Short Description</label>
                <textarea id="shortDescription"
                          placeholder="Enter a short description for this entry"
                          aria-multiline="true" rows="5"
                          style="white-space: pre-wrap">${entry?.shortDescription}</textarea>
                <textarea id="tmpShortDescription"
                          aria-multiline="true" rows="5"
                          style="white-space: pre-wrap; display: none">
                    ${entry?.shortDescription}</textarea>
                <label for="models" class="required">Models associated with (separated by commas)</label>
                <input type="text" id="models" name="models" required
                       placeholder="Model identifiers associated with this entry separated by commas"
                       value="${modelIds}">
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
                <g:if test="${previewImage}">
                    <img src="data:image/jpeg;base64,${previewImage}"
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
                <label for="uploadPreviewImage" class="button">Upload an image</label>
                <input type="file" id="uploadPreviewImage" class="show-for-sr" accept="image/*"
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
        $('#curaImageHolder').attr('src', imgSrc);
        $('#curaImageHolder').attr('title', 'This format is not acceptable');
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
        /* check whether the curation image is available or not */
        var previewImage = $('#previewImage').attr('src');
        var re = new RegExp('data:image\/');
        var isPreviewImageAvailable = re.exec(previewImage);
        /* combine with the built-in validation check */
        var isValid = $('#momEntryForm')[0].checkValidity() && isPreviewImageAvailable;
        if (isValid) {
            delete messages["invalidForm"];
        } else {
            set(messages, "invalidForm", "${g.message(code: "model.biomodels.curationNotes.editor.invalidForm")}");
        }
        return isValid;
    }

    function buildMoMEntryTC() {
        var authors = $('#authors').val();
        var title = $('#title').val();
        var shortDescription = $('#shortDescription').val();
        var publicationDate = $('#publicationDate').val();
        var lastUpdated = $('#lastUpdated').val();
        var models = $('#models').val();
        var momEntryTC = {
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
        momEntryTC = JSON.stringify(momEntryTC);
        return momEntryTC;
    }

    $("#uploadPreviewImage").change(function(){
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
                set("onlyAcceptImages",
                        "${g.message(code: "model.biomodels.curationNotes.editor.onlyAcceptImages")}");
                showWarningMessage();
            }

            /* validate file size */
            var MAX_SIZE = 1.44 * 1024 * 1024; // 1.44 MB ~ 1_500_000 is the allowed maximum size of the uploading image file
            if (imageFile.size > MAX_SIZE) {
                set("curationImageTooBig",
                        "${g.message(code: "curationNotesTransportCommand.curationImage.curationImageTooBig")}");
            } else {
                delete messages["curationImageTooBig"];
            }
            $('#txtStatus').html(values().join("<br/>"));
        }
    });

    $('#btnSave').on("click", function(event) {
        var shouldSubmit =  true; //checkRequiredValidity() && checkCustomValidity();
        if (shouldSubmit) {
            var tmpEntry = buildMoMEntryTC();
            "use strict";
            event.preventDefault();
            $.ajax({
                dataType: "json",
                type: "POST",
                url: $.jummp.createLink("modelOfTheMonth", "save"),
                cache: true,
                data: {
                    momEntryTC: tmpEntry,
                    id: "${entry.id}"
                },
                processData: true,
                async: true,
                beforeSend: function() {
                    toastr.info("The entry is being saved. Please wait...");
                },
                success: function(response) {
                    var href = window.location.href;
                    if ("${params.id}" == "" && typeof(response['id']) != 'undefined') {
                        var newHref = href + "/" + response['id'];
                        if (window.history.pushState) {
                            window.history.pushState({}, null, newHref);
                        } else {
                            document.location.hash = newHref;
                        }
                    }
                    toastr.clear();
                    toastr.success(response['message']);
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    // TODO: the error message doesn't show properly
                    toastr.clear();
                    toastr.error("Error: ", jqXHR.responseText + textStatus + errorThrown + JSON.stringify(jqXHR));
                }
            });
        } else {
            $('#txtStatus').html(values().join("<br/>"));
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
