<div id="txtStatus" style="color: #ED0000; font-weight: 600; font-size: larger"></div>
<form id="curationNotesForm">
    <div class="row">
        <div class="small-12 medium-6 large-6 columns">
            <div class="grid-container">
                <div class="grid-x grid-padding-x">
                    <div class="small-12 medium-12 large-12 cell">
                        <label>Simulation results<br/>
                            <g:if test="${curationImage}">
                                <img src="data:image/jpeg;base64,${curationImage}"
                                     id="curaImageHolder"
                                     title="Click on the thumbnail to view the result(s)" />
                            </g:if>
                            <g:else>
                                <img src="${grailsApplication.config.grails.serverURL}/images/biomodels/No-Image-Available.jpg"
                                     id="curaImageHolder"
                                     title="The curation images are not available" />
                            </g:else><br/>
                        </label>
                    </div>
                </div>
            </div>
        </div>
        <div class="small-12 medium-6 large-6 columns">
            <div class="grid-container">
                <div class="grid-x grid-padding-x">
                    <div class="medium-12 large-12 cell">
                        <label>Comments
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
                                   value="${curationNotesTC.lastModifier?.username}">
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
                   style="text-align: right; direction: ltr" required>
        </div>
        <div class="small-12 medium-6 large-6 columns" style="text-align: right">
            <button type="button" class="button" id="btnSave">Save</button>
            <button type="button" class="button" id="btnReset">Reset</button>
        </div>
    </div>
</form>
<g:javascript>
    var imgUploadedStream;
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
        datetext = datetext + updateOnSelect();
        $('#datepicker').val(datetext);
        $(this).val(datetext);
    }
});

$('#txtLastModified').datepicker({
    dateFormat: 'yy-mm-dd',
    onSelect: function(datetext) {
        datetext = datetext + updateOnSelect();
        $('#datepicker').val(datetext);
        $(this).val(datetext);
    }
});

function updateOnSelect() {
    var d = new Date(); // for now
    var hour = d.getHours() < 10 ? "0" + d.getHours().toString() : d.getHours();
    var minute = d.getMinutes() < 10 ? "0" + d.getMinutes().toString() : d.getMinutes();
    var second = d.getSeconds() < 10 ? "0" + d.getSeconds().toString() : d.getSeconds();
    return "T" + hour + ":" + minute + ":"+ second;
}

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
    if (imgUploadedStream) {
        curationNotes['curationImage'] = imgUploadedStream;
    } else {
        var base64ImgStr = $('img#curaImageHolder').attr('src');
        if (base64ImgStr.indexOf('base64,')>=0) {
            base64ImgStr = base64ImgStr.substring(base64ImgStr.indexOf('base64,') + 'base64,'.length);
            curationNotes['curationImage'] = base64ImgStr;
        }
    }
    curationNotes = JSON.stringify(curationNotes);
    return curationNotes;
}

function previewImage(input) {
    // reused sample codes from https://stackoverflow.com/a/4459419/865603
    if (input.files && input.files[0]) {
        var reader = new FileReader();
        var image = input.files[0];
        reader.onload = function (event) {
            var imgSrc = event.target.result;
            imgUploadedStream = event.target.result.replace("data:"+ image.type +";base64,", '');
            $('#curaImageHolder').attr('src', imgSrc);
            $('#curaImageHolder').attr('title', 'This image has been uploaded or replaced');
        }
        reader.readAsDataURL(image);
    }
}

$("#uploadCurationImage").change(function(){
    var re = new RegExp('image\/');
    if (this.files && this.files[0]) {
        if (re.exec(this.files[0].type)) {
            previewImage(this);
            $('#txtStatus').text("");
        } else {
            $('#txtStatus').text("${g.message(code: "model.biomodels.curationNotes.editor.onlyAcceptImages")}");
        }

        /* validate file size */
        var MAX_SIZE = 1.44 * 1024 * 1024; // 1.44 MB ~ 1_500_000 is the allowed maximum size of the uploading image file
        if (imageFile.size > MAX_SIZE) {
            $('#txtStatus').text("${g.message(code: "curationNotesTransportCommand.curationImage.curationImageTooBig")}");
            "${g.message()}"
        }
    }
});

$('#btnSave').on("click", function(event) {
    if ($('#curationNotesForm')[0].checkValidity()) {
        var curationNotes = buildCurationNotesTC();
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
                $('#txtStatus').text("The curation notes are being saved. Please wait...");
            },
            success: function(response) {
                var href = window.location.href;
                if (href.indexOf("&cnId=") < 0 && typeof(response['cnId']) != 'undefined') {
                    var newHref = href + "&cnId=" + response['cnId'];
                    if (window.history.pushState) {
                        window.history.pushState({}, null, newHref);
                    } else {
                        document.location.hash = newHref;
                    }
                }
                $('#txtStatus').text(response['message']);
            },
            error: function(jqXHR, textStatus, errorThrown) {
                // TODO: the error message doesn't show properly
                $('#txtStatus').text("Error: ", jqXHR.responseText + textStatus + errorThrown + JSON.stringify(jqXHR));
            }
        });
    } else {
        $('#txtStatus').text("${g.message(code: "model.biomodels.curationNotes.editor.invalidForm")}");
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
