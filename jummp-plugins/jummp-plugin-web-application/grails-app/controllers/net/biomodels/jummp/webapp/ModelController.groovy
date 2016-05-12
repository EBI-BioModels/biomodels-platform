/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Spring Framework, Spring Security (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Spring Framework, Spring Security used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.webapp

import com.wordnik.swagger.annotations.*
import eu.ddmore.publish.service.PublishContext
import eu.ddmore.publish.service.PublishException
import grails.converters.JSON
import grails.plugins.springsecurity.Secured
import net.biomodels.jummp.model.PublicationLinkProvider
import org.apache.commons.lang3.exception.ExceptionUtils
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import net.biomodels.jummp.core.model.ModelAuditTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PermissionTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.model.audit.*
import net.biomodels.jummp.plugins.security.PersonTransportCommand
import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.web.json.JSONObject
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.multipart.MultipartFile
import net.biomodels.jummp.plugins.security.Team

@Api(value = "/model", description = "Operations related to models")
class ModelController {
    /**
     * Flag that checks whether the dynamically-inserted logger is set to DEBUG or higher.
     */
    private final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Dependency injection of springSecurityService.
     */
    def springSecurityService
    /**
     * Dependency injection of modelDelegateService.
     **/
    def modelDelegateService
    /**
     * Dependency injection of modelFileFormatService
     **/
    def modelFileFormatService
    /**
     * Dependency injection of teamService.
     */
    def teamService
    /**
     * Dependency injection of sbmlService.
     */
    def sbmlService
    /**
     * Dependency injection of submissionService
     */
    def submissionService
    /**
     * Dependency Injection of grailsApplication
     */
    def grailsApplication
    /**
    * Dependency injection of pubMedService
    */
    def pubMedService
    /*
    * Dependency injection of mailService
    */
    def mailService

    /**
     * The list of actions for which we should not automatically create an audit item.
     */
    final List<String> AUDIT_EXCEPTIONS = ['updateFlow', 'createFlow', 'uploadFlow',
                'showWithMessage', 'share', 'getFileDetails','submitForPublication']

    def beforeInterceptor = [action: this.&auditBefore, except: AUDIT_EXCEPTIONS]

    def afterInterceptor = [ action: this.&auditAfter, except: AUDIT_EXCEPTIONS]

    private String getUsername() {
        String username="anonymous"
        def principal = springSecurityService.principal
        if (principal instanceof String) {
            username = principal
        }
        else if (principal) {
            username = principal.username
        }
        return username
    }

    // if this method returns false, the controller method is no longer called.
    private boolean auditBefore() {
        try {
            String modelIdParam = params.id
            String revisionIdParam = params.revisionId
            String modelId = null
            String username = getUsername()
            String accessType = actionUri
            String formatType = params.format ?: "html"
            String changesMade = null

            final boolean HAS_ONLY_DIGITS = isPositiveNumber(modelIdParam)
            //perennial model identifiers include literals
            final boolean IS_REVISION_ID = !revisionIdParam && HAS_ONLY_DIGITS
            if (IS_REVISION_ID) {
                // publish uses revision ids, annoyingly enough.
                if (accessType.contains("publish")) {
                    def rev = modelDelegateService.getRevisionDetails(
                                new RevisionTransportCommand(id: modelIdParam))
                    if (rev) {
                        modelId = rev.model.publicationId ?: rev.model.submissionId
                    }
                }
            }
            ModelTransportCommand model
            if (!modelId) {
                model = modelDelegateService.findByPerennialIdentifier(modelIdParam)
            }
            if (model) {
                modelId = (model.publicationId) ?: model.submissionId
                int historyItem = updateHistory(modelId, username, accessType, formatType, changesMade)
                session.lastHistory = historyItem
            } else {
                log.error "Ignoring invalid request for $actionUri with params $params."
                forward(controller: "errors", action: "error403")
                return false
            }
        } catch(Exception e) {
            log.error(e.message, e)
            forward(controller: "errors", action: "error403")
            return false
        }
        return true
    }

    private void auditAfter(def model) {
        try {
            if (session.lastHistory) {
                modelDelegateService.updateAuditSuccess(session.lastHistory, true)
                session.removeAttribute("lastHistory")
            }
        } catch(Exception e) {
            log.error e.message, e
        }
    }

    private int updateHistory(String modelId, String user, String accessType,
                String formatType, String changesMade, boolean success=false) {
        accessType = accessType.replace("/model/","")
        AccessFormat format = AccessFormat.HTML
        try {
            format = AccessFormat.valueOf(formatType.toUpperCase())
        }
        catch(Exception ignore) {
        }
        ModelTransportCommand model = modelDelegateService.findByPerennialIdentifier(modelId)
        ModelAuditTransportCommand audit = new ModelAuditTransportCommand(
                    model: model,
                    username: user,
                    format: format,
                    type: AccessType.fromAction(accessType),
                    changesMade: changesMade,
                    success: success)
        long returned = modelDelegateService.createAuditItem(audit)
        return returned
    }

    def showWithMessage = {
        flash["giveMessage"] = params.flashMessage
        StringBuilder modelId = new StringBuilder(params.id)
        if (params.revisionId) {
            modelId.append('.').append(params.revisionId)
        }
        redirect(action: "show", id: modelId.toString())
    }

    @ApiOperation(value = "Show a model.", httpMethod = "GET",
                response = net.biomodels.jummp.webapp.rest.model.show.Model.class,
                notes = "Pass the expected media type of the request as a parameter e.g. /model/id?format=json")
    @ApiImplicitParam(name = "modelId", value = "The model identifier", required = true, allowMultiple = false)
    def show() {
        RevisionTransportCommand rev = modelDelegateService.getRevisionFromParams(params.id,
                    params.revisionId)
        if (!params.format || (params.format != "json" && params.format != "xml") ) {
            if (!rev) {
                forward(controller: 'errors', action: 'error403')
                return
            }
            final String PERENNIAL_ID = (rev.model.publicationId) ?: (rev.model.submissionId)
            boolean showPublishOption = modelDelegateService.canPublish(PERENNIAL_ID)
            boolean canSubmitForPublication = modelDelegateService.canSubmitForPublication(PERENNIAL_ID)
            boolean show = modelDelegateService.canPublish(PERENNIAL_ID)
            boolean canUpdate = modelDelegateService.canAddRevision(PERENNIAL_ID)
            boolean canDelete = modelDelegateService.canDelete(PERENNIAL_ID)
            boolean canShare = modelDelegateService.canShare(PERENNIAL_ID)

            String flashMessage = ""
            if (flash.now["giveMessage"]) {
                flashMessage = flash.now["giveMessage"]
            }
            List<RevisionTransportCommand> revs =
                        modelDelegateService.getAllRevisions(PERENNIAL_ID)


            def model = [revision: rev,
                        authors: rev.model.creators,
                        allRevs: revs,
                        flashMessage: flashMessage,
                        canUpdate: canUpdate,
                        canDelete: canDelete,
                        canShare: canShare,
                        showPublishOption: showPublishOption,
                        canSubmitForPublication: canSubmitForPublication,
                        validationLevel: rev.getValidationLevelMessage()
            ]
            if (rev.id == modelDelegateService.getLatestRevision(PERENNIAL_ID).id) {
                flash.genericModel = model
                forward controller: modelFileFormatService.getPluginForFormat(rev.model.format),
                            action: "show", id: PERENNIAL_ID
            } else { //showing an old version, with the default page. Do not allow updates.
                model["canUpdate"] = false
                model["showPublishOption"] = false
                model["oldVersion"] = true
                model["canDelete"] = false
                model["canShare"] = false
                return model
            }
        } else {
            if (!rev) {
                respond net.biomodels.jummp.webapp.rest.error.Error("Invalid Id",
                        "An invalid model id was specified")
            } else {
                respond new net.biomodels.jummp.webapp.rest.model.show.Model(rev)
            }
        }
    }

    def files = {
        try {
            def revisionFiles = modelDelegateService.getRevisionFromParams(params.id, params.revisionId).files
            def responseFiles = revisionFiles.findAll { !it.hidden }
            respond new net.biomodels.jummp.webapp.rest.model.show.ModelFiles(responseFiles)
        } catch(Exception err) {
            log.error err.message, err
            respond net.biomodels.jummp.webapp.rest.error.Error("Invalid Id",
            "An invalid model id was specified")
        }
    }

    @Secured(["isAuthenticated()"])
    def publish = {
        RevisionTransportCommand rev
        try {
            rev = modelDelegateService.getRevisionFromParams(params.id, params.revisionId)
            PublishContext publishContext = modelDelegateService.publishModelRevision(rev)
            def currentUser = springSecurityService.currentUser
            if (currentUser) {
                def notification = [
                    revision: rev,
                    user: currentUser,
                    perms: modelDelegateService.getPermissionsMap(rev.model.submissionId)]
                sendMessage("seda:model.publish", notification)
            }

            redirect(action: "showWithMessage",
                        id: rev.identifier(),
                        params: [flashMessage: "Model has been published." + publishContext.getMessage()])
        } catch(AccessDeniedException e) {
            log.error(e.message, e)
            forward(controller: "errors", action: "error403")
        } catch(IllegalArgumentException e) {
            log.error(e.message)
            redirect(action: "showWithMessage",
                    id: rev.identifier(),
                    params: [flashMessage: "Model has not been published because there is a " +
                            "problem with this version of the model. Sorry!"])
        } catch(PublishException e) {
            log.error(e.message)
            redirect(action: "showWithMessage",
                id: rev.identifier(),
                params: [flashMessage: e.message])
        }
    }

    @Secured(["isAuthenticated()"])
    def submitForPublication = {
        try {
            def rev = modelDelegateService.getRevisionFromParams(params.id)
            modelDelegateService.submitModelRevisionForPublication(rev)

            def notification = [revision:rev, user:getUsername(), perms: modelDelegateService.getPermissionsMap(rev.model.submissionId)]
            sendMessage("seda:model.submitForPublication", notification)

            redirect(action: "showWithMessage",
                id: rev.identifier(),
                params: [flashMessage: "Model has been submitted to the curators for publication."])
        } catch (Exception e) {
            log.error(e.message, e)
            String message = "Sorry, there was a problem. Please try again later."
            redirect(action: "showWithMessage",
                id: modelDelegateService.getRevisionFromParams(params.id).identifier(),
                params: [flashMessage: message])
        }
    }

    @Secured(["isAuthenticated()"])
    def delete = {
        try {
            boolean deleted = modelDelegateService.deleteModel(params.id)
            def currentUser = springSecurityService.currentUser
            if (currentUser) {
                def notification = [
                    model: modelDelegateService.getModel(params.id),
                    user: currentUser,
                    perms: modelDelegateService.getPermissionsMap(params.id)]
                sendMessage("seda:model.delete", notification)
            }
            redirect(action: "showWithMessage", id: params.id,
                        params: [ flashMessage: deleted ?
                                    "Model has been deleted, and moved into archives." :
                                    "Model could not be deleted"])
        } catch(Exception e) {
            log.error e.message, e
            forward(controller: "errors", action: "error403")
        }
    }

    // uses revision id and filename
    def getFileDetails = {
        try {
            final RevisionTransportCommand REVISION =
                        modelDelegateService.getRevisionFromParams(params.id, params.revisionId)
            def retval = modelDelegateService.getFileDetails(REVISION.id, params.filename)
            if (IS_DEBUG_ENABLED) {
                log.debug("Permissions for ${REVISION.identifier()}: ${retval as JSON}")
            }
            render retval as JSON
        } catch(Exception e) {
            log.error e.message, e
            return "INVALID ID"
        }
    }

    @Secured(["isAuthenticated()"])
    def share = {
        try {
            def rev = modelDelegateService.getRevisionFromParams(params.id)
            def perms = modelDelegateService.getPermissionsMap(rev.model.submissionId)
            def teams = getTeamsForCurrentUser()
            return [revision: rev, permissions: perms as JSON, teams: teams]
        } catch(Exception error) {
            log.error error.message, error
            forward(controller: "errors", action: "error403")
        }
    }

    private List<Team> getTeamsForCurrentUser() {
        def user = springSecurityService.getCurrentUser()
        if (user) {
            return teamService.getTeamsForUser(user)
        }
        return []
    }

    @Secured(["isAuthenticated()"])
    def shareUpdate = {
        boolean valid = params.collabMap
        if (valid) {
            try {
                def map = JSON.parse(params.collabMap)
                List<PermissionTransportCommand> collabsNew = new LinkedList<PermissionTransportCommand>()
                for (int i = 0; i < map.length(); i++) {
                    JSONObject perm = map.getJSONObject(i)
                    PermissionTransportCommand ptc = new PermissionTransportCommand(
                                id: perm.getInt("id"),
                                username: perm.getString("username"),
                                name: perm.getString("name"),
                                read: perm.getBoolean("read"),
                                write: perm.getBoolean("write"))
                    collabsNew.add(ptc)
                }
                modelDelegateService.setPermissions(params.id, collabsNew)
                JSON result = ['success': true, 'permissions':
                            modelDelegateService.getPermissionsMap(params.id)]
                render result
            } catch(Exception e) {
                log.error e.message, e
                valid = false
            }
        }
        if (!valid) {
            render (['success': false, 'message': "Could not update permissions"] as JSON)
        }
    }

    @Secured(["isAuthenticated()"])
    def updateFlow = {
        start {
            action {
                try {
                    if (!modelDelegateService.canAddRevision(params.id)) {
                        return accessDenied()
                    }
                    conversation.model_id = params.id
                }
                catch(Exception err) {
                    return error()
                }
            }
            on("success").to "uploadPipeline"
            on("error") {
                session.updateMissingId = "True"
            }.to "displayErrorPage"
            on("accessDenied") {
                def model = modelDelegateService.findByPerennialIdentifier(params.id)
                Long MODEL_ID = model?.id
                if (params.id) {
                    updateHistory(MODEL_ID, "something wrong", "update", "html", null, false)
               }
            }.to "displayAccessDenied"
        }
        uploadPipeline {
            subflow(controller: "model", action: "upload", input: [isUpdate: true])
            on("abort").to "abort"
            on("displayConfirmationPage"){
                String update = conversation.changesMade.join(". ")
                String model = conversation.model_id
                def currentUser = springSecurityService.currentUser
                String username = currentUser?.username ?: 'anonymous'
                updateHistory(session.result_submission, username, "update", "html", update, true)
                if (!currentUser) {
                    def notification = [
                            model: modelDelegateService.getModel(model),
                            user: currentUser,
                            update: conversation.changesMade,
                            perms : modelDelegateService.getPermissionsMap(model, false)]
                    sendMessage("seda:model.update", notification)
                }
            }.to "displayConfirmationPage"
            on("displayErrorPage").to "displayErrorPage"
        }
        abort()
        displayConfirmationPage()
        displayErrorPage()
        displayAccessDenied()
   }

    @Secured(["isAuthenticated()"])
    def createFlow = {
        uploadPipeline {
            subflow(controller: "model", action: "upload", input: [isUpdate:false])
            on("abort").to "abort"
            on("displayConfirmationPage") {
                final String USERNAME = getUsername()
                final String AUDIT_ID = session.result_submission
                updateHistory(AUDIT_ID, USERNAME, "create", "html", null, true)
            }.to "displayConfirmationPage"
            on("displayErrorPage").to "displayErrorPage"
        }
        abort()
        displayConfirmationPage()
        displayErrorPage()
    }

    /*
     * The flow maintains the 'params' as flow.workingMemory (just to distinguish
     * between request.params and our params. Flow scope requires all objects
     * to be serializable. If this is not possible, there are two solutions:
     *   1) store in session scope
     *   2) evict the objects in question from the Hibernate session before the
     * end of the session using <tt>flow.persistenceContext.evict(it)</tt>.
     * See http://grails.org/grails/latest/doc/guide/theWebLayer.html#flowScopes
     */
    @Secured(["isAuthenticated()"])
    def uploadFlow = {
        input {
            isUpdate(required: true)
        }
        start {
            action {
                Map<String, Object> workingMemory = new HashMap<String,Object>()
                flow.workingMemory = workingMemory
                flow.workingMemory.put("isUpdateOnExistingModel",flow.isUpdate)
                conversation.changesMade = new TreeSet<String>()
                if (flow.isUpdate) {
                    String model_id = conversation.model_id
                    flow.workingMemory.put("model_id", model_id)
                    flow.workingMemory.put("LastRevision",
                                modelDelegateService.getLatestRevision(model_id, false))
                    /* Maintain reference to the previous revision in session
                       memory to ensure it is not overwritten. Do it with a
                       random variable name to allow updating of multiple
                       models simultaneously by the same user
                    */
                    String alphabet=(('A'..'Z')+('a'..'z')).join()
                    String variableName=new Random().with {
                        (1..10).collect { alphabet[ nextInt( alphabet.length() ) ] }.join()
                    }
                    flow.workingMemory.put("SafeReferenceVariable", variableName)
                    session."${variableName}" = flow.workingMemory.get("LastRevision")
                }
                submissionService.initialise(flow.workingMemory)
                if (flow.isUpdate) {
                    skipDisclaimer()
                }
                else {
                    goToDisclaimer()
                }
            }
            on("skipDisclaimer").to "uploadFiles"
            on("goToDisclaimer").to "displayDisclaimer"
            on(Exception).to "handleException"
        }
        displayDisclaimer {
            on("Continue").to "uploadFiles"
            on("Cancel").to "cleanUpAndTerminate"
        }
        uploadFiles {
            on("Upload") {
                // get the main and the additional files just added and their descriptions
                // these operations are based on params
                // the principal purpose of this step is to store an UploadCommand object to workingMemory
                // this object persists the latest changes on the upload file page
                try {
                    def mainMultipartList = request.getMultiFileMap().mainFile
                    def extraFileField = request.getMultiFileMap().extraFiles
                    List<MultipartFile> extraMultipartList = []
                    if (extraFileField instanceof MultipartFile) {
                        extraMultipartList = [extraFileField]
                    }
                    else {
                        extraMultipartList = extraFileField
                    }
                    def descriptionFields = params?.description ?: [""]
                    if (descriptionFields instanceof String) {
                        descriptionFields = [descriptionFields]
                    }
                    def noMains = params.deletedMain
                    List<String> mainsToBeDeleted = []
                    if (noMains) {
                        if (!(noMains instanceof CharSequence)) {
                            mainsToBeDeleted.addAll(Arrays.asList(noMains))
                        } else {
                            mainsToBeDeleted.add(noMains)
                        }
                    }
                    def noAdditionals = params.deletedAdditional
                    List<String> additionalsToBeDeleted = []
                    if (noAdditionals) {
                        if (noAdditionals.getClass().isArray()) {
                            additionalsToBeDeleted.addAll(Arrays.asList(noAdditionals))
                        } else {
                            additionalsToBeDeleted.add(noAdditionals)
                        }
                    }
                    // Because of using an input element to store deleted files,
                    // we need to reprocess the list of them for getting the list of file name
                    // before sending the result to Submission Service
                    List<String> additionalFilesToBeDeleted = new LinkedList<String>()
                    additionalsToBeDeleted.each {String filename ->
                        int posLessThan = filename.indexOf("<")
                        if (posLessThan > 0) {
                            additionalFilesToBeDeleted.add(filename.substring(0, posLessThan))
                        }
                    }
                    if (IS_DEBUG_ENABLED) {
                        if (mainMultipartList?.size() == 1) {
                            log.debug("""\
    New submission started. The main file supplied is ${mainMultipartList.properties}.""")
                        } else {
                            log.debug("""\
    New submission started. Main files: ${mainMultipartList.inspect()}.""")
                        }
                        log.debug("Additional files supplied: ${extraMultipartList.inspect()}.\n")
                    }

                    def cmd = new UploadFilesCommand()
                    cmd.mainFile = mainMultipartList
                    cmd.extraFiles = extraMultipartList
                    cmd.mainDeletes = mainsToBeDeleted
                    cmd.extraDeletes = additionalFilesToBeDeleted
                    cmd.description = descriptionFields
                    if (IS_DEBUG_ENABLED) {
                        log.debug "Data binding done :${cmd.properties}"
                    }
                    flow.workingMemory.put("UploadCommand", cmd)

                    // store additional files existing on UI, i.e. the files are in updated process
                    // data stored are a map of file names and corresponding descriptions.
                    // For instance, manual.pdf: guidelines and help, readme.txt: introduction and preface, ...
                    Map<String, String> additionalFiles = new HashMap<String, String>()
                    String additionalFilesInWorking = params.additionalFilesInWorking
                    if (additionalFilesInWorking.trim() != "empty") {
                        String[] workingFiles = additionalFilesInWorking.split(', ')
                        workingFiles.each {
                            def (fileName, fileDescription) = it.split(':')
                            additionalFiles.put(fileName.trim(), fileDescription.trim())
                        }
                        flow.workingMemory.put("additionals_in_working", additionalFiles)
                    }
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }.to "transferFilesToService"
            on("ProceedWithoutValidation"){

            }.to "inferModelInfo"
            on("ProceedAsUnknown"){
                flow.workingMemory.get("model_type").identifier = "UNKNOWN"
            }.to "inferModelInfo"
            on("Cancel").to "cleanUpAndTerminate"
            on("Back"){}.to "displayDisclaimer"
        }
        transferFilesToService {
            action {
                UploadFilesCommand cmd =
                            flow.workingMemory.remove("UploadCommand") as UploadFilesCommand
                boolean fileValidationError = false
                boolean furtherProcessingRequired = true
                def deletedMains = cmd.mainDeletes
                List mainFiles = flow.workingMemory.get("repository_files").findAll { it.mainFile }
                boolean mainFileDeleted = mainFileDeleted(mainFiles, cmd.mainFile, deletedMains)
                if (mainFileDeleted) {
                    return MainFileMissingError()
                }
                if (!cmd.validate()) {
                    // No main file! This must be an error!
                    fileValidationError = true
                    // Unless there was one all along

                    if (cmd.errors["mainFile"].codes.find{ it.contains("mainFile.blank") ||
                                    it.contains("mainFile.nullable") }) {
                        if (flow.workingMemory.containsKey("repository_files")) {
                            if (mainFiles && !mainFiles.isEmpty()) {
                                if (!mainFileOverwritten(mainFiles, cmd.extraFiles)) {
                                    fileValidationError = false
                                    furtherProcessingRequired = true
                                } else {
                                    return AdditionalReplacingMainError()
                                }
                            } else {
                                return MainFileMissingError()
                            }
                        } else {
                            return MainFileMissingError()
                        }
                    } else {
                        throw new Exception("""\
Error in uploading files. Cmd did not validate: ${cmd.getProperties()}""")
                    }
                } else {
                    if (IS_DEBUG_ENABLED) {
                        log.debug("The files are valid.")
                    }
                }
                if (!fileValidationError && furtherProcessingRequired) {
                    //should this be in a separate action state?
                    def uuid = UUID.randomUUID().toString()
                    if (IS_DEBUG_ENABLED) {
                        log.debug "Generated submission UUID: ${uuid}"
                    }
                    //pray that exchangeDirectory has been defined
                    File submission_folder = null
                    def sep = File.separator
                    if (!flow.workingMemory.containsKey("repository_files")) {
                        def exchangeDir = grailsApplication.config.jummp.vcs.exchangeDirectory
                        submission_folder = new File(exchangeDir, uuid)
                        submission_folder.mkdirs()
                    }
                    else {
                        RFTC existing = flow.workingMemory.get("repository_files").get(0) as RFTC
                        submission_folder = (new File(existing.path)).getParentFile()
                    }
                    def parent = submission_folder.canonicalPath + sep
                    // transfer uploaded files to File objects:
                    // For Main file
                    List<File> mainFileList
                    if (cmd.mainFile) {
                        mainFileList = transferFiles(parent, cmd.mainFile)
                    }
                    else {
                        mainFileList = new LinkedList<File>()
                    }
                    // For the extra files, i.e. the files have just added, not the existing
                    // additional files. This does not allow us modifying the descriptions of
                    // the existing ones.
                    List<File> extraFileList = transferFiles(parent, cmd.extraFiles)
                    List<String> descriptionList = cmd.description
                    def additionalsMap = [:]
                    extraFileList.eachWithIndex{ file, i ->
                        additionalsMap[file] = descriptionList[i]
                    }
                    if (IS_DEBUG_ENABLED) {
                        log.debug """\
About to submit ${mainFileList.inspect()} and ${additionalsMap.inspect()}."""
                    }
                    flow.workingMemory["submitted_mains"] = mainFileList
                    flow.workingMemory["submitted_additionals"] = additionalsMap

                    // store the deleted file names into working memory
                    List<String> deletedFileNames = []
                    deletedFileNames.addAll(deletedMains)
                    deletedFileNames.addAll(cmd.extraDeletes)
                    // ensure there are no lists within this list
                    flow.workingMemory["deleted_filenames"] = deletedFileNames.flatten()

                    // transfer HashMap<String, String> to HashMap<File, String>
                    // Why? we can get the submission folder at this step and this operation
                    // repairs the data for handling file upload afterward.
                    if (flow.workingMemory.containsKey("additionals_in_working")) {
                        Map<File, String> additionalFiles = new HashMap<File, String>()
                        def additionals_in_working =
                            flow.workingMemory.get("additionals_in_working") as HashMap<String, String>
                        additionals_in_working.each {String keyAsFilename, String valueAsDescription ->
                            additionalFiles.put(new File(parent+keyAsFilename), valueAsDescription)
                        }
                        flow.workingMemory.put("additional_files_in_working", additionalFiles)
                    }
                    submissionService.handleFileUpload(flow.workingMemory)
                }

            }
            on("MainFileMissingError") {
                flash.flashMessage = "submission.upload.error.fileerror"
            }.to "uploadFiles"
            on("AdditionalReplacingMainError") {
                flash.flashMessage = "submission.upload.error.additional_replacing_main"
            }.to "uploadFiles"
            on("success").to "performValidation"
            on(Exception).to "handleException"
        }

        performValidation {
            action {
                final boolean SHOULD_DETECT_FORMAT = flow.workingMemory["changedMainFiles"] ||
                    !flow.workingMemory.containsKey("model_type")
                if (SHOULD_DETECT_FORMAT) {
                    submissionService.inferModelFormatType(flow.workingMemory)
                }
                // clear changedMainFiles in case the user clicks back from displayModelInfo
                flow.workingMemory.remove("changedMainFiles")
                submissionService.performValidation(flow.workingMemory)
                MFTC format = flow.workingMemory.get("model_type")
                if (format && format.identifier !="UNKNOWN" && format.formatVersion == "*") {
                    UnknownFormatVersion()
                }
                else if (!flow.workingMemory.containsKey("validation_error")) {
                    Valid()
                }
                else
                {
                    String errorAsString = flow.workingMemory.remove("validation_error") as String
                    if (errorAsString.contains("ModelValidationError")) {
                        ModelNotValid()
                    }
                    else {
                        FilesNotValid()
                    }
                }
            }
            on("Valid"){
                flow.workingMemory.put("Valid", true)
            }.to "inferModelInfo"
            on("ModelNotValid") {
                flow.workingMemory.put("Valid", false)
                // read this parameter to display option to upload without
                // validation in upload files view
                flash.showProceedWithoutValidationDialog = true
            }.to "uploadFiles"
            on("UnknownFormatVersion") {
                flow.workingMemory.put("FormatVersionUnsupported", true)
                // read this parameter to display option to upload without
                // validation in upload files view
                flash.showProceedAsUnknownFormat = true
                flash.modelFormatDetectedAs = flow.workingMemory.get("model_type").identifier
            }.to "uploadFiles"
            on("FilesNotValid") {
                flash.error = "submission.upload.error.fileerror"
            }.to "uploadFiles"
            on(Exception).to "handleException"
        }
        inferModelInfo {
            action {
                submissionService.inferModelInfo(flow.workingMemory)
            }
            on("success").to "displayModelInfo"
            on(Exception).to "handleException"
        }
        displayModelInfo {
            on("Continue") {
                //populate modifications object with form data
                Map<String,Object> modifications = new HashMap<String,Object>()
                final String NAME = params.name
                final String DESC = params.description
                final String changeStatus = params.changed
                if (NAME && NAME.trim()) {
                    modifications.put("new_name", NAME.trim())
                }
                if (DESC && DESC.trim()) {
                    modifications.put("new_description", DESC.trim())
                }
                modifications.put("changeStatus", changeStatus);
                submissionService.refineModelInfo(flow.workingMemory, modifications)
                ModelTransportCommand model = flow.workingMemory.get('ModelTC') as ModelTransportCommand
                RevisionTransportCommand revision = flow.workingMemory.get("RevisionTC") as RevisionTransportCommand
            }.to "enterPublicationLink"
            on("Cancel").to "cleanUpAndTerminate"
            on("Back"){}.to "uploadFiles"
        }
        enterPublicationLink {
            on("Continue") {
                if (!params.PubLinkProvider && params.PublicationLink) {
                    flash.flashMessage = "Please select a publication link type."
                    return error()
                }
                Map<String,String> modifications = new HashMap<String,String>()
                    if (params.PubLinkProvider) {
                        if (!pubMedService.verifyLink(params.PubLinkProvider, params.PublicationLink)) {
                            flash.flashMessage = "The link is not a valid ${params.PubLinkProvider}"
                            return error()
                        }
                        ModelTransportCommand model = flow.workingMemory.get("ModelTC") as ModelTransportCommand
                        boolean providerHasChanged = params.PubLinkProvider !=
                                model.publication?.linkProvider?.linkType
                        boolean linkHasChanged = params.PublicationLink != model.publication?.link
                        if (providerHasChanged || linkHasChanged) {
                            modifications.put("PubLinkProvider", params.PubLinkProvider)
                            modifications.put("PubLink", params.PublicationLink)
                            submissionService.updatePublicationLink(flow.workingMemory,
                                        modifications)
                        } else {
                            flow.workingMemory.put("RetrievePubDetails", false)
                        }
                    } else {
                        ModelTransportCommand model = flow.workingMemory.get('ModelTC') as ModelTransportCommand
                        RevisionTransportCommand revision = flow.workingMemory.get("RevisionTC") as RevisionTransportCommand
                        def publication = revision.model.publication
                        if (publication) {
                            revision.model.publication = null
                            if (flow.workingMemory.containsKey("Authors")) {
                                flow.workingMemory.remove("Authors")
                            }
                        }
                        flow.workingMemory.put("RetrievePubDetails", false)
                    }
            }.to "getPublicationDataIfPossible"
            on("Cancel").to "cleanUpAndTerminate"
            on("Back").to "displayModelInfo"
        }
        getPublicationDataIfPossible {
            action {
                if (flow.workingMemory.remove("RetrievePubDetails") as Boolean) {
                    ModelTransportCommand model = flow.workingMemory.get("ModelTC") as ModelTransportCommand
                    if (model.publication.link && model.publication.linkProvider) {
                        def retrieved
                        try {
                            retrieved = pubMedService.getPublication(model.publication)
                        }
                        catch(Exception e) {
                            log.error(e.message, e)
                        }
                        if (retrieved) {
                            model.publication = retrieved
                            flow.workingMemory.put("Authors", model.publication.authors)
                        }
                    }
                    // use authors of the existing publication if available
                    if (model.publication) {
                        flow.workingMemory.put("Authors", model.publication.authors)
                    }
                    flow.workingMemory.put("Authors", model.publication.authors)
                    conversation.changesMade.add("Amended publication details")
                    publicationInfoPage()
                }
                else {
                    displaySummaryOfChanges()
                }
            }
            on("publicationInfoPage").to "publicationInfoPage"
            on("displaySummaryOfChanges").to "displaySummaryOfChanges"
            on(Exception).to "handleException"
        }
        publicationInfoPage {
            on("Continue"){
                ModelTransportCommand model =
                            flow.workingMemory.get("ModelTC") as ModelTransportCommand
                bindData(model.publication, params, [exclude: ['authors']])
                String[] authorList = params.authorFieldTotal.split(",")
                List<PersonTransportCommand> validatedAuthors = new LinkedList<PersonTransportCommand>()
                authorList.each {
                    if (it) {
                        String[] parts = it.split("<init>")
                        String name = parts[0]
                        String orcid = parts[1]
                        String institution = parts[2]
                        def authorListSrc = model.publication.authors
                        if (!authorListSrc) {
                            authorListSrc = new LinkedList<PersonTransportCommand>()
                        }
                        def author = authorListSrc.find { auth ->
                            if (orcid != "no_orcid") {
                                return orcid == auth.orcid
                            } else {
                                return name == auth.userRealName
                            }
                            return false
                        }
                        if (!author) {
                            author = new PersonTransportCommand(userRealName: parts[0],
                                        orcid: orcid != "no_orcid" ? orcid : null,
                                        institution: institution != "no_institution_provided" ? institution : null)
                            if (!model.publication.authors) {
                                model.publication.authors=new LinkedList<PersonTransportCommand>()
                            }
                            model.publication.authors.add(author)
                        }
                        if (author.validate()) {
                            validatedAuthors.add(author)
                        } else {
                            log.error """\
Submission did not validate: ${author.properties}.
Errors: ${author.errors.allErrors.inspect()}."""
                            flash.validationErrorOn = author
                            return error()
                        }
                    }
                }
                model.publication.authors = validatedAuthors
                if (!model.publication.validate()) {
                    log.error """\
Submission did not validate: ${model.publication.properties}.
Errors: ${model.publication.errors.allErrors.inspect()}."""
                    flash.validationErrorOn = model.publication
                    return error()
                }
            }.to "displaySummaryOfChanges"
            on("Cancel").to "cleanUpAndTerminate"
            on("Back").to "enterPublicationLink"
        }
        displaySummaryOfChanges {
            on("Continue") {
                Map<String,String> modifications = new HashMap<String,String>()
                if (params.RevisionComments) {
                    modifications.put("RevisionComments", params.RevisionComments)
                } else {
                    modifications.put("RevisionComments", "Model revised without commit message")
                }
                submissionService.updateFromSummary(flow.workingMemory, modifications)
            }.to "saveModel"
            on("Cancel").to "cleanUpAndTerminate"
            on("Back"){}.to "enterPublicationLink"
        }
        saveModel {
            action {
                def changes = submissionService.handleSubmission(flow.workingMemory)
                changes.each {
                    conversation.changesMade.add(it.toString())
                }
                session.result_submission = flow.workingMemory.get("model_id")
                if (flow.isUpdate) {
                    flash.sendMessage = "Model has been updated."
                    session.removeAttribute(flow.workingMemory.get("SafeReferenceVariable") as String)
                    return redirectWithMessage()
                }
            }
            on("success").to "displayConfirmationPage"
            on("redirectWithMessage").to "redirectWithMessage"
            on(Exception).to "handleException"
        }
        cleanUpAndTerminate {
            action {
                submissionService.cleanup(flow.workingMemory)
                if (flow.isUpdate) {
                    flash.sendMessage = "Model update was cancelled."
                    session.removeAttribute(flow.workingMemory.get("SafeReferenceVariable") as String)
                    return redirectWithMessage()
                }
            }
            on("success").to "abort"
            on("redirectWithMessage").to "redirectWithMessage"
        }
        redirectWithMessage {
            action {
                redirect(controller: 'model', action: 'showWithMessage',
                        id: conversation.model_id, params: [flashMessage: flash.sendMessage])
            }
            on("success").to "displayConfirmationPage"
            on(Exception).to "displayConfirmationPage"
        }
        handleException {
            action {
                Throwable t = flash.flowExecutionException
                log.error("Exception thrown during the submission process: ${t.message}", t)
                String ticket = UUID.randomUUID().toString()
                if (flow.isUpdate) {
                    session.removeAttribute(flow.workingMemory.get("SafeReferenceVariable") as String)
                }
                if (flow.workingMemory.containsKey("repository_files")) {
                    def repFile = flow.workingMemory.get("repository_files").first()
                    if (repFile) {
                        File aSingleFile = new File(repFile.path)
                        final String EXCHG = grailsApplication.config.jummp.vcs.exchangeDirectory
                        final File PARENT = new File(EXCHG)
                        File buggyFiles = new File(PARENT, "buggy")
                        File temporaryStorage = new File(buggyFiles, ticket)
                        temporaryStorage.mkdirs()
                        FileUtils.copyDirectory(new File(aSingleFile.getParent()), temporaryStorage)
                    }
                }
                submissionService.cleanup(flow.workingMemory)
                mailService.sendMail {
                    to grailsApplication.config.jummp.security.registration.email.adminAddress
                    from grailsApplication.config.jummp.security.registration.email.sender
                    subject "Bug in submission: ${ticket}"
                    body "MESSAGE: ${ExceptionUtils.getStackTrace(t)}"
                }
                session.messageForError = ticket
            }
            on("success").to "displayErrorPage"
        }
        abort()
        displayConfirmationPage()
        displayErrorPage()
    }

    private void serveModelAsZip(List<RFTC> files, def resp) {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream()
        ZipOutputStream zipFile = new ZipOutputStream(byteBuffer)
        files.each {
            File file = new File(it.path)
            zipFile.putNextEntry(new ZipEntry(file.getName()))
            byte[] fileData = file.getBytes()
            zipFile.write(fileData, 0, fileData.length)
            zipFile.closeEntry()
        }
        zipFile.close()
        resp.setContentType("application/zip")
        // TODO: set a proper name for the model
        resp.setHeader("Content-disposition", "attachment;filename=\"model.zip\"")
        resp.outputStream << new ByteArrayInputStream(byteBuffer.toByteArray())
    }

    private void serveModelAsFile(RFTC rf, def resp, boolean inline, boolean preview = false) {
        File file = new File(rf.path)
        resp.setContentType(rf.mimeType)
        final String INLINE = inline ? "inline" : "attachment"
        final String F_NAME = file.name
        resp.setHeader("Content-disposition", "${INLINE};filename=\"${F_NAME}\"")
        byte[] fileData = file.readBytes()
        int previewSize = grailsApplication.config.jummp.web.file.preview as Integer
        if (!preview || previewSize > fileData.length) {
            resp.outputStream << new ByteArrayInputStream(fileData)
        }
        else {
            resp.outputStream << new ByteArrayInputStream(Arrays.copyOf(fileData, previewSize))
        }
    }

    /**
     * File download of the model file for a model by id
     */
    def download = {
        if (!params.filename) {
            final List<RFTC> FILES = modelDelegateService.retrieveModelFiles(
                            modelDelegateService.getRevisionFromParams(params.id, params.revisionId))
            List<RFTC> mainFiles = FILES.findAll { it.mainFile }
            if (FILES.size() == 1) {
                serveModelAsFile(FILES.first(), response, false)
            } else if (mainFiles.size() == 1) {
                serveModelAsFile(mainFiles.first(), response, false)
            } else {
                serveModelAsZip(FILES, response)
            }
        } else {
            final List<RFTC> FILES = modelDelegateService.retrieveModelFiles(
                            modelDelegateService.getRevisionFromParams(params.id, params.revisionId))
            RFTC requested = FILES.find {
                if (it.hidden) {
                    return false
                }
                File file = new File(it.path)
                file.getName() == params.filename
            }
            boolean inline = params.inline == "true"
            boolean preview  = params.preview == "true"
            if (requested) {
                serveModelAsFile(requested, response, inline, preview)
            }
        }
    }

    /**
     * Display basic information about the model
     */
    def summary = {
        RevisionTransportCommand rev = modelDelegateService.getRevisionFromParams(params.id)
        [
            publication: modelDelegateService.getPublication(params.id),
            revision: rev,
            notes: sbmlService.getNotes(rev),
            annotations: sbmlService.getAnnotations(rev)
        ]
    }

    def overview = {
        RevisionTransportCommand rev = modelDelegateService.getRevisionFromParams(params.id)
        [
            reactions: sbmlService.getReactions(rev),
            rules: sbmlService.getRules(rev),
            parameters: sbmlService.getParameters(rev),
            compartments: sbmlService.getCompartments(rev)
        ]
    }

    /**
     * Renders html snippet with Publication information for the current Model identified by the id.
     */
    def publication = {
        PublicationTransportCommand publication = modelDelegateService.getPublication(params.id)
        [publication: publication]
    }

    def notes = {
        RevisionTransportCommand rev = modelDelegateService.getRevisionFromParams(params.id)
        [notes: sbmlService.getNotes(rev)]
    }

    /**
     * Retrieve annotations and hand them over to the view
     */
    def annotations = {
        RevisionTransportCommand rev = modelDelegateService.getRevisionFromParams(params.id)
        [annotations: sbmlService.getAnnotations(rev)]
    }

    /**
     * File download of the model file for a model by id
     */
    def downloadModelRevision = {
        RevisionTransportCommand rev = modelDelegateService.getRevisionFromParams(params.id)
        byte[] bytes = modelDelegateService.retrieveModelFiles(rev)
        response.setContentType("application/xml")
        // TODO: set a proper name for the model
        response.setHeader("Content-disposition", "attachment;filename=\"model.xml\"")
        response.outputStream << new ByteArrayInputStream(bytes)
    }

    private List<File> transferFiles(String parent, List multipartFiles) {
        List<File> outcome = []
        multipartFiles.each { MultipartFile f ->
            final String originalFilename = f.getOriginalFilename()
            if (!originalFilename.isEmpty()) {
                final def transferredFile = new File(parent + originalFilename)
                if (IS_DEBUG_ENABLED) {
                    log.debug "Transferring file ${transferredFile}"
                }
                f.transferTo(transferredFile)
                outcome << transferredFile
            }
        }
        outcome
    }

    private List getMainFiles(Map<String,Object> workingMemory) {
        List<RFTC> uploaded = workingMemory.get("repository_files") as List<RFTC>
        return uploaded.findAll { it.mainFile }
    }

    private boolean mainFileOverwritten(List mainFiles, List multipartFiles) {
        boolean returnVal = false
        mainFiles.each { RFTC mainFile ->
            String name = new File(mainFile.path).name
            multipartFiles.each { MultipartFile uploaded ->
                if (uploaded.getOriginalFilename() == name) {
                    returnVal = true
                }
            }
        }
        return returnVal
    }

    private boolean mainFileDeleted(List mainFiles, List cmdMains, List<String> mainsToBeDeleted) {
        def nonEmptyCmdMains = cmdMains?.find{!it.isEmpty()}
        if (nonEmptyCmdMains) {
            return false
        }
        def mainFileNames = mainFiles.collect { RFTC rf -> new File(rf.path).name }
        def remainingFiles = mainFileNames - mainsToBeDeleted
        return remainingFiles.isEmpty()
    }

    private boolean isPositiveNumber(String value) {
        for (char c in value.toCharArray()) {
            if (!Character.isDigit(c)) {
                return false
            }
        }
        return true
    }
}
