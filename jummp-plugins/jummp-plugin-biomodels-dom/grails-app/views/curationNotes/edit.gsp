B<%--
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
                                <input type="text" id="submitter" placeholder=".medium-6.cell"
                                       value="${curationNotes.submitter.username}">
                            </label>
                        </div>
                        <div class="medium-12 large-12 large-12 cell">
                            <label>Last modifier
                                <input type="text" id="lastModifier" placeholder=".medium-6.cell"
                                       value="${curationNotes.lastModifier.username}">
                            </label>
                        </div>
                        <div class="row">
                            <div class="small-12 medium-6 large-6 columns">
                                <label>Date added
                                    <input type="text"id="txtDateAdded" value="${curationNotes.dateAdded}">
                                </label>
                            </div>
                            <div class="small-12 medium-6 large-6 columns">
                                <label>Last modified
                                    <input type="text" id="txtLastModified" value="${curationNotes.lastModified}">
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
        function readURL(input) {
            if (input.files && input.files[0]) {
                var reader = new FileReader();

                reader.onload = function (e) {
                    $('#curaImageHolder').attr('src', e.target.result);
                }
            }
        }

        function saveImage(input) {
            if (input.files && input.files[0]) {
                console.log(input.files[0]);
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
	                       console.log(data);
	                    },
	                    error: function(jqXHR, textStatus, errorThrown) {
	                        console.error("Error: ", jqXHR.responseText + "\n" + textStatus + ": " + errorThrown);
	                        console.log(JSON.stringify(jqXHR));
	                    }
                    });
                }
                reader.readAsDataURL(input.files[0]);
            }
        }

        $("#uploadCurationImage").change(function(){
            readURL(this);
            // save it to the database
            saveImage(this);
            reloadEditFormSilent();
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
	        console.log(curationNotes);
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
	                console.log("The curation notes are being saved. Please wait...");
                },
                success: function(data) {
	                console.log(data);
                },
                error: function(jqXHR, textStatus, errorThrown) {
	                console.error("Error: ", jqXHR.responseText + textStatus + errorThrown);
	                console.log(JSON.stringify(jqXHR));
	            }
            });
        });


    </g:javascript>
</body>
</html>
