<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 18/10/17
  Time: 13:23
--%>

<%@ page import="net.biomodels.jummp.deployment.biomodels.CurationNotesCategory; net.biomodels.jummp.deployment.biomodels.CurationNotesTransportCommand; net.biomodels.jummp.deployment.biomodels.CurationNotes; net.biomodels.jummp.model.Model"
         contentType="text/html;charset=UTF-8" %>
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
    %>
    <div class="row">
        <h2>Update curation notes of the model ${modelPerennialOrSubmissionId}</h2>
        <form>
            <div class="small-12 medium-6 large-6 columns">
                <div class="grid-container">
                    <div class="grid-x grid-padding-x">
                        <div class="small-12 medium-12 large-12 cell">
                            <label><h3>Simulation results</h3><br/>
                                <g:if test="${curationNotesTC.curationImage}">
                                    <img src="data:image/jpeg;base64,${curationNotesTC.curationImage}"
                                         title="Click on the thumbnail to view the result(s)" />
                                </g:if>
                                <g:else>
                                    <img src="${grailsApplication.config.grails.serverURL}/images/biomodels/No-Image-Available.jpg"
                                         title="The curation images are not available" />
                                </g:else><br/>
                                <label for="exampleFileUpload" class="button">Upload File</label>
                                <input type="file" id="exampleFileUpload" class="show-for-sr">
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
                                <textarea placeholder="none" aria-multiline="true">${curationNotes.comment}</textarea>
                            </label>
                        </div>

                        <div class="medium-12  large-12 cell">
                            <label>Submitter
                                <input type="text" placeholder=".medium-6.cell"
                                       value="${curationNotes.submitter.username}">
                            </label>
                        </div>
                        <div class="medium-12 large-12 large-12 cell">
                            <label>Last modifier
                                <input type="text" placeholder=".medium-6.cell"
                                       value="${curationNotes.lastModifier.username}">
                            </label>
                        </div>
                        <div class="medium-12 large-12 cell">
                            <label>Date added
                                <input type="text" placeholder=".medium-6.cell"
                                       value="${curationNotes.dateAdded}">
                            </label>
                        </div>
                        <div class="medium-12 large-12 cell">
                            <label>Last modified
                                <input type="text" placeholder=".medium-6.cell"
                                       value="${curationNotes.lastModified}">
                            </label>
                        </div>
                    </div>
                </div>
            </div>
            <div class="small-12 medium-12 columns">
                <button class="button">Save</button>
                <button class="button">Reset</button>
            </div>
        </form>
    </div>

</body>
</html>
