<%
    String serverURL = grailsApplication.config.grails.serverURL
%>
<div id="txtStatus" style="color: #ED0000; font-weight: 500; font-size: larger"></div>
<form id="curationNotesForm">
    <div class="row">
        <div class="small-12 medium-6 large-6 columns">
            <div class="grid-container">
                <div class="grid-x grid-padding-x">
                    <div class="small-12 medium-12 large-12 cell">
                        <label>Simulation results <small style="color: red">required</small>                        </label>
<br/>                   <div style="text-align: center">
                            <g:if test="${curationImage}">
                                <img src="data:image/jpeg;base64,${curationImage}"
                                     id="curaImageHolder"
                                     title="Click on the thumbnail to view the result(s)" />
                            </g:if>
                            <g:else>
                                <img src="${serverURL}/images/biomodels/simulation-result-unavailable.png"
                                     id="curaImageHolder"
                                     title="The curation images are not available" />
                                <p>Please press the button below to upload your curation figure.</p>
                            </g:else><br/>
                        </div>
                    </div>
                </div>
            </div>
        </div>
        <div class="small-12 medium-6 large-6 columns">
            <div class="grid-container">
                <div class="grid-x grid-padding-x">
                    <div class="medium-12 large-12 cell">
                        <label>Comments <small style="color: red">required</small>
                            <textarea id="comment" placeholder="Enter your curation comments here"
                                      aria-multiline="true" rows="5" required
                                      style="white-space: pre-wrap">${curationNotesTC.comment}</textarea>
                            <textarea id="tmpComment"
                                      aria-multiline="true"
                                      rows="5" style="display: none">${curationNotesTC.comment}</textarea>
                        </label>
                        <label>Internal comments used for curators
                            <textarea id="internalComment" placeholder="Enter your additional curation comments here"
                                      aria-multiline="true" rows="5"
                                      style="white-space: pre-wrap">${curationNotesTC.internalComment}</textarea>
                            <textarea id="tmpInternalComment"
                                      aria-multiline="true"
                                      rows="5" style="display: none">${curationNotesTC.internalComment}</textarea>
                        </label>
                    </div>

                    <div class="medium-12  large-12 cell">
                        <label>Initially curated by <small style="color: red">required</small>
                            <input type="text" id="submitter" required
                                   placeholder="the submitter who had deposited the simulation results"
                                   value="${curationNotesTC.submitter?.username}" readonly>
                        </label>
                    </div>
                    <div class="medium-12 large-12 large-12 cell">
                        <label>Last modified by <small style="color: red">required</small>
                            <input type="text" id="lastModifier" required
                                   placeholder="the last modifier who is updating the simulation results"
                                   value="${curationNotesTC.lastModifier?.username}" readonly>
                        </label>
                    </div>
                    <div class="row">
                        <div class="small-12 medium-6 large-6 columns">
                            <label>Date added <small style="color: red">required</small>
                                <input type="text" id="txtDateAdded" required
                                       placeholder="enter the date when the simulation results were added"
                                       value="${dateFormat.format(curationNotesTC.dateAdded)}">
                            </label>
                        </div>
                        <div class="small-12 medium-6 large-6 columns">
                            <label>Last modified <small style="color: red">required</small>
                                <input type="text" id="txtLastModified" required
                                       placeholder="enter the latest date when the simulation results have been updated"
                                       value="${dateFormat.format(curationNotesTC.lastModified)}">
                            </label>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <div class="row">
        <div class="small-12 medium-6 large-6 columns">
            <label for="uploadCurationImage" class="button">Upload an image</label>
            <input type="file" id="uploadCurationImage" class="show-for-sr" accept="image/*"
                   style="text-align: right; direction: ltr">
        </div>
        <div class="small-12 medium-6 large-6 columns" style="text-align: right">
            <a href="${createLink(controller: "model", action: "show", id: id)}" class="button"
               title="Back to the model display page">Back</a>
            <button type="button" class="button" id="btnSave">Save</button>
            <button type="button" class="button" id="btnReset">Reset</button>
        </div>
    </div>
</form>
<g:javascript>
    var imgUploadedStream;
    var mimeType = 'unknown';
    var messages = {}; // or: new Object(); or: new Map(); but not supported in IE

    $('#submitter, #lastModifier').on('keydown', function() {
        $(this).autocomplete({
            source: function(request, response) {
                $.ajax({
                    url: $.jummp.createLink('usermanagement', 'fetchUsers'),
                    type: 'POST',
                    dataType: 'json',
                    data: {
                        search: request.term,
                        request: 1
                    },
                    success: function(data) {
                        response(data);
                    }
                });
            },
            select: function(event, ui) {
                $(this).val(ui.item.username);   // display the selected text
                var username = ui.item.username; // selected value
                $.ajax({
                    url: $.jummp.createLink('usermanagement', 'fetchUsers'),
                    type: 'POST',
                    data: {
                        username: username,
                        request: 2
                    },
                    dataType: 'json',
                    success: function(response) {
                        var len = response.length;
                        if(len > 0){
                            var id = response[0]['id'];
                            var username = response[0]['username'];
                            var email = response[0]['email'];
                        }
                    }
                });
                return false;
            }
        });
    });

    $('#txtDateAdded').datepicker({
        dateFormat: 'yy-mm-dd',
        onSelect: function(datetext) {
            datetext = datetext + getTimeStamp();
            $('#datepicker').val(datetext);
            $(this).val(datetext);
        }
    });

    $('#txtLastModified').datepicker({
        dateFormat: 'yy-mm-dd',
        onSelect: function(datetext) {
            datetext = datetext + getTimeStamp();
            $('#datepicker').val(datetext);
            $(this).val(datetext);
        }
    });

    function buildCurationNotesTC() {
        var comment = $('#comment').val();
        var internalComment = $('#internalComment').val();
        var submitter = $('#submitter').val();
        var lastModifier = $('#lastModifier').val();
        var dateAdded = $('#txtDateAdded').val();
        var lastModified = $('#txtLastModified').val();
        var curationNotes = {
            'comment': comment,
            'internalComment': internalComment,
            'submitter': submitter,
            'lastModifier': lastModifier,
            'dateAdded': dateAdded,
            'lastModified': lastModified,
            'updated': ${curationNotesTC.updated}
        };
        let base64ImgStr;
        if (imgUploadedStream) {
            base64ImgStr = imgUploadedStream;
        } else {
            base64ImgStr = $('img#curaImageHolder').attr('src');
            if (base64ImgStr.indexOf('base64,')>=0) {
                mimeType = base64ImgStr.substring(5, base64ImgStr.indexOf(";"));
                base64ImgStr = base64ImgStr.substring(base64ImgStr.indexOf('base64,') + 'base64,'.length);
            } else {
                console.log("The curation notes is not uploaded the curation image");
                // allow the curation figure to be empty, but it will be populated a dummy figure later
                // load the dummy curation figure
                let dummyFigure = "${serverURL}/images/biomodels/simulation-result-unavailable.png";
                mimeType = "image/jpeg";
                // The convertImageURL2Data is defined in the common.js file
                base64ImgStr = convertImageURL2Data(dummyFigure);
            }
        }
        curationNotes['curationImage'] = base64ImgStr;
        curationNotes['mimeType'] = mimeType;
        curationNotes = JSON.stringify(curationNotes);
        return curationNotes;
    }

    function showWarningMessage() {
        var imgSrc = "${serverURL}/images/biomodels/unacceptable.png";
        $('#curaImageHolder').attr('src', imgSrc);
        $('#curaImageHolder').attr('title', 'This format is not acceptable');
    }

    $("#uploadCurationImage").change(function(){
        var re = new RegExp('image\/');
        //var re = new RegExp('(.*?)'); accept everything
        if (this.files && this.files[0]) {
            /* validate file type */
            var imageFile = this.files[0];
            mimeType = imageFile.type;
            if (re.exec(mimeType)) {
                // use the previewImage function defined in common.js to render the preview of the figure
                imgUploadedStream = previewImage(this, '#curaImageHolder');
                delete messages["onlyAcceptImages"];
            } else {
                set(messages, "onlyAcceptImages",
                        "${g.message(code: "model.biomodels.curationNotes.editor.onlyAcceptImages")}");
                showWarningMessage();
            }

            /* validate file size */
            var MAX_SIZE = 1.44 * 1024 * 1024; // 1.44 MB ~ 1_500_000 is the allowed maximum size of the uploading image file
            if (imageFile.size > MAX_SIZE) {
                set(messages, "curationImageTooBig",
                        "${g.message(code: "curationNotesTransportCommand.curationImage.curationImageTooBig")}");
            } else {
                delete messages["curationImageTooBig"];
            }
            $('#txtStatus').html(values(messages).join("<br/>"));
        }
    });

    function checkCustomValidity() {
        return Object.keys(messages).length === 0;
    }

    function checkRequiredValidity() {
        /* check whether the curation image is available or not */
        var curationImage = $('#curaImageHolder').attr('src');
        var re = new RegExp('data:image\/');
        var isCurationImageAvailable = re.exec(curationImage);
        // allow the curation figure to be empty but it will be populated a dummy figure later
        isCurationImageAvailable = true;
        /* combine with the built-in validation check */
        var isValid = $('#curationNotesForm')[0].checkValidity() && isCurationImageAvailable;
        if (isValid) {
            delete messages["invalidForm"];
        } else {
            set(messages, "invalidForm", "${g.message(code: "model.biomodels.curationNotes.editor.invalidForm")}");
        }
        return isValid;
    }

    $('#btnSave').on("click", function(event) {
        var shouldSubmit =  checkRequiredValidity() && checkCustomValidity();
        if (shouldSubmit) {
            const curationNotes = buildCurationNotesTC();
            "use strict";
            event.preventDefault();
            $.ajax({
                dataType: "json",
                type: "POST",
                url: $.jummp.createLink("curationNotes", "doAddOrUpdate"),
                cache: true,
                data: {
                    curationNotes: curationNotes,
                    model: "${id}"
                },
                processData: true,
                async: true,
                beforeSend: function() {
                    toastr.info("The curation notes are being saved. Please wait...");
                },
                success: function(response) {
                    if (response['error']) {
                        toastr.error(response['message']);
                        return;
                    }
                    var href = window.location.href;
                    if (href.indexOf("&cnId=") < 0 && typeof(response['cnId']) != 'undefined') {
                        var newHref = href + "&cnId=" + response['cnId'];
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
            $('#txtStatus').html(values(messages).join("<br/>"));
        }
    });

    $('#btnReset').on('click', function(event) {
        var orgComment = $('#tmpComment').val();
        $('#comment').val(orgComment);
        orgComment = $('#tmpInternalComment').val();
        $('#internalComment').val(orgComment);
        $('#submitter').val("${curationNotesTC.submitter?.username}");
        $('#lastModifier').val("${curationNotesTC.lastModifier?.username}");
        $('#txtDateAdded').val("${dateFormat.format(curationNotesTC.dateAdded)}");
        $('#txtLastModified').val("${dateFormat.format(curationNotesTC.lastModified)}");
    });
</g:javascript>
