<%--
  Created by IntelliJ IDEA.
  Author: Tung Nguyen <tnguyen@ebi.ac.uk>
  Date: 18/10/17
  Time: 13:23
--%>

<%@ page import="net.biomodels.jummp.deployment.biomodels.CurationNotesCategory" contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.deployment.biomodels.CurationNotesTransportCommand" contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.deployment.biomodels.CurationNotes" contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.model.Model" contentType="text/html;charset=UTF-8" %>
<%@ page import="groovy.json.JsonOutput" contentType="text/html;charset=UTF-8" %>
<%@ page import="java.util.Date" contentType="text/html;charset=UTF-8" %>
<%@ page import="java.text.SimpleDateFormat" contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>Update simulation results</title>
</head>

<body>
    <%
        def modelPerennialOrSubmissionId = params.model
        Model model = Model.findByPublicationIdOrSubmissionId(modelPerennialOrSubmissionId, modelPerennialOrSubmissionId)
        CurationNotes curationNotes = CurationNotes.findByModel(model)
        CurationNotesTransportCommand curationNotesTC
        use(CurationNotesCategory) {
            curationNotesTC = curationNotes.toCommandObject()
        }
        String curationImage
        curationImage = curationNotesTC.curationImage ? Base64.encoder.encodeToString(curationNotesTC.curationImage) : null
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    %>
    <div class="row">
        <h2>Update curation notes of the model
            <a href="${createLink(controller: "model", action: "show", id: modelPerennialOrSubmissionId)}"
               title="Back to the model display page">${modelPerennialOrSubmissionId}</a></h2>
        <div id="txtStatus" style="color: #550000; font-weight: 900; font-size: larger"></div>
        <form>
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
                                         title="The curation images are not available" />
                                </g:else><br/>
                            </label>
                            <label for="uploadCurationImage" class="button">Upload an image</label>
                            <input type="file" id="uploadCurationImage" class="show-for-sr"
                                   style="text-align: right; direction: ltr">
                        </div>
                    </div>
                </div>
            </div>
            <div class="small-12 medium-6 large-6 columns">
                <div class="grid-container">
                    <div class="grid-x grid-padding-x">
                        <div class="medium-12 large-12 cell">
                            <label>Comments
                                <textarea id="comment" placeholder="none" aria-multiline="true" rows="5">${curationNotes.comment}</textarea>
                            </label>
                        </div>

                        <div class="medium-12  large-12 cell">
                            <label>Submitter
                                <input type="text" id="submitter" placeholder="the submitter who had deposited the simulation results"
                                       value="${curationNotes.submitter.username}">
                            </label>
                        </div>
                        <div class="medium-12 large-12 large-12 cell">
                            <label>Last modifier
                                <input type="text" id="lastModifier" placeholder="the last modifier who is updating the simulation results"
                                       value="${curationNotes.lastModifier.username}">
                            </label>
                        </div>
                        <div class="row">
                            <div class="small-12 medium-6 large-6 columns">
                                <label>Date added
                                    <input type="text" id="txtDateAdded" placeholder="enter the date when the simulation results were added"
					   value="${dateFormat.format(curationNotes.dateAdded)}">
                                </label>
                            </div>
                            <div class="small-12 medium-6 large-6 columns">
                                <label>Last modified
                                    <input type="text" id="txtLastModified" placeholder="enter the latest date when the simulation results have been updated"
					   value="${dateFormat.format(curationNotes.lastModified)}">
                                </label>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
            <div class="small-12 medium-12 columns" style="text-align: right">
                <button type="button" class="button" id="btnSave">Save</button>
                <button type="button" class="button" id="btnReset">Reset</button>
            </div>
        </form>
    </div>
    <g:javascript>
        $('#txtDateAdded').datepicker({
            dateFormat: 'yy-mm-dd',
            onSelect: function(datetext) {
                datetext = datetext + updateOnSelect();
                $('#datepicker').val(datetext);
                console.log(datetext);
            }
        });

        $('#txtLastModified').datepicker({
            dateFormat: 'yy-mm-dd',
            onSelect: function(datetext) {
                datetext = datetext + updateOnSelect();
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

        function previewImage(input) {
            // reused sample codes from https://stackoverflow.com/a/4459419/865603
            if (input.files && input.files[0]) {
                var reader = new FileReader();
                var image = input.files[0];
                reader.onload = function (event) {
                    var imgSrc = event.target.result;
                    $('#curaImageHolder').attr('src', imgSrc);
                }
                reader.readAsDataURL(image);
            }
        }

        function saveImage(input) {
            if (input.files && input.files[0]) {
                var reader = new FileReader();
                reader.onload = function(event) {
                    var file = input.files[0];
                    var fileName = input.files[0].name;
                    // remove the prefix to only keep data, but it depends on the server
                    var data = event.target.result.replace("data:"+ file.type +";base64,", '');
                    $.ajax({
                        type: "POST",
                        url: $.jummp.createLink("curationNotes", "updateCurationImage"),
                        dataType: "text",
                        data: {
                            curaImg: data,
                            model: "${modelPerennialOrSubmissionId}"
                        },
                        cache: true,
                        async: true,
                        processData: true,
                        success: function(data) {
                            $('#txtStatus').text(data);
                        },
                        error: function(jqXHR, textStatus, errorThrown) {
                            $('#txtStatus').text("Error: ", jqXHR.responseText + "\n" + textStatus + ": " + errorThrown);
                        }
                    });
                }
                reader.readAsDataURL(input.files[0]);
            }
        }

        $("#uploadCurationImage").change(function(){
            previewImage(this);
            // save it to the database
            saveImage(this);
        });

        $('#btnSave').on("click", function(event) {
            var comment = $('#comment').val();
            var submitter = $('#submitter').val();
            var lastModifier = $('#lastModifier').val();
            var dateAdded = $('#txtDateAdded').val();
            var lastModified = $('#txtLastModified').val();
            var curationNotes = {
                'id': ${curationNotes.id},
                'comment': comment,
                'submitter': submitter,
                'lastModifier': lastModifier,
                'dateAdded': dateAdded,
                'lastModified': lastModified
            }
            curationNotes = JSON.stringify(curationNotes);
            "use strict";
            event.preventDefault();
            $.ajax({
                dataType: "text",
                type: "GET",
                url: $.jummp.createLink("curationNotes", "update"),
                cache: true,
                contentType: "application/json; charset=utf-8",
                data: {
                    curationNotes: curationNotes,
                    model: "${modelPerennialOrSubmissionId}"
                },
                processData: true,
                async: false,
                beforeSend: function() {
                    $('#txtStatus').text("The curation notes are being saved. Please wait...");
                },
                success: function(data) {
                    $('#txtStatus').text(data);
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    $('#txtStatus').text("Error: ", jqXHR.responseText + textStatus + errorThrown + JSON.stringify(jqXHR));
	            }
            });
        });

        $('#btnReset').on('click', function(event) {
            $('#comment').val("${curationNotes.comment}");
            $('#submitter').val("${curationNotes.submitter.username}");
            $('#lastModifier').val("${curationNotes.lastModifier.username}");
            $('#txtDateAdded').val("${dateFormat.format(curationNotes.dateAdded)}");
            $('#txtLastModified').val("${dateFormat.format(curationNotes.lastModified)}");
        });
    </g:javascript>
</body>
</html>
