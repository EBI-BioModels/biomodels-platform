/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.converters.JSON
import grails.plugin.springsecurity.annotation.Secured
import net.biomodels.jummp.core.IFileSystemService
import net.biomodels.jummp.core.InvalidPublicationAuthorsException
import net.biomodels.jummp.core.adapters.ModelFormatAdapter
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.core.model.*
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.PublicationDetailExtractionContext as PDEC
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.audit.AccessFormat
import net.biomodels.jummp.core.model.audit.AccessType
import net.biomodels.jummp.core.util.ReactomeEnvironment
import net.biomodels.jummp.deployment.biomodels.CurationNotesTransportCommand
import net.biomodels.jummp.deployment.biomodels.TagTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.Team
import net.biomodels.jummp.utils.redis.KeyCollection
import net.biomodels.jummp.webapp.rest.errors.Error
import net.biomodels.jummp.webapp.rest.model.show.Model as RestfulModel
import net.biomodels.jummp.webapp.rest.model.show.ModelFiles
import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.codehaus.groovy.grails.web.json.JSONObject
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartFile

import javax.servlet.http.HttpServletResponse
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Secured(['IS_AUTHENTICATED_FULLY'])
class ModelController {
    private final Logger log = LoggerFactory.getLogger(this.getClass())
    /**
     * Flag that checks whether the dynamically-inserted logger is set to DEBUG or higher.
     */
    private final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    IFileSystemService fileSystemService
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
     * Dependency injection of PublicationService
     */
    def publicationService
    /*
    * Dependency injection of mailService
    */
    def mailService
    /**
     * Dependency injection of MetadataDelegateService
     */
    def metadataDelegateService
    /**
     * Dependency injection of OmexService
     */
    def omexService

    def modelConversionService

    def userService

    def messageSource

    def publishClientService

    /**
     * The list of actions for which we should not automatically create an audit item.
     */
    final List<String> AUDIT_EXCEPTIONS = ['updateFlow', 'createFlow', 'uploadFlow',
                'showWithMessage', 'share', 'getFileDetails', 'submitForPublication', 'updateCurationState', 'searchModellingApproach', 'submit', 'newUpdate', 'terms', 'uploadFile']

    def beforeInterceptor = [action: this.&auditBefore, except: AUDIT_EXCEPTIONS]

    def afterInterceptor = [action: this.&auditAfter, except: AUDIT_EXCEPTIONS]

    // if this method returns false, the controller method is no longer called.
    private boolean auditBefore() {
        try {
            // XSS guard for the actions from this controller (excluding submission)
            params.id = params.id
            params.revisionId = params.revisionId
            String modelIdParam = params.id
            String revisionIdParam = params.revisionId
            String modelId = null
            String username = userService.getUsername()
            String accessType = actionUri
            String formatType = response.format
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
                        modelId = rev.modelIdentifier()
                    }
                }
            }
            ModelTransportCommand model
            if (!modelId) {
                model = modelDelegateService.findByPerennialIdentifier(modelIdParam)
            }
            if (model) {
                modelId = (model.publicationId) ?: model.submissionId
                int historyItem = modelDelegateService.updateHistory(modelId, username, accessType, formatType, changesMade)
                request.lastHistory = historyItem
                return true
            } else {
                log.error "Ignoring invalid request for $actionUri with params $params."
                forward(controller: "errors", action: "error404")
                return false
            }
        } catch(Exception e) {
            log.error(e.message, e)
            String actionError = params?.action == "download" ? "error400" : "error403"
            forward(controller: "errors", action: actionError)
            return false
        }
    }

    private void auditAfter(def model) {
        try {
            if (request.lastHistory) {
                modelDelegateService.updateAuditSuccess(request.lastHistory, true)
                request.removeAttribute("lastHistory")
            }
        } catch(Exception e) {
            log.error e.message, e
        }
    }

    @grails.transaction.Transactional
    def showWithMessage() {
        flash["giveMessage"] = params.flashMessage
        StringBuilder modelId = new StringBuilder(params.id)
        if (params.revisionId) {
            modelId.append('.').append(params.revisionId)
        }
        redirect(action: "show", id: modelId.toString())
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    @grails.transaction.Transactional
    def show() {
        RevisionTransportCommand rev
        boolean isPrivateModel = false
        try {
            rev = modelDelegateService.getRevisionFromParams(params.id, params.revisionId)
        } catch (AccessDeniedException e) {
            Model model = Model.findByPublicationIdOrSubmissionId(params.id, params.id)
            log.warn("""\
An anonymous or restricted access user is trying to retrieve this model: ${model.submissionId}""")
            int revisionNumber = -1
            if (params.revisionId) {
                revisionNumber = params.int("revisionId")
            }
            // TODO need to establish if the requested model revision exists in a way that bypasses
            // ACLs and that doesn't rely on accessing domain objects from the controller
            Revision revision = revisionNumber >= 0 ?
                model.revisions[revisionNumber - 1] : model.revisions.last()
            if (!revision) {
                forward(controller: 'errors', action: 'error404')
                return
            }
            rev = new RevisionAdapter(revision: revision).toCommandObject()
            rev.name = rev.model.submissionId
            model.publication = null
            rev.format = new ModelFormatTransportCommand()
            rev.files = new ArrayList<>()
            rev.description = g.message(code: "net.biomodels.jummp.core.model.show.MessageForPrivateModel")
            isPrivateModel = true
        }
        withFormat {
            html {
                if (!rev) {
                    forward(controller: 'errors', action: 'error404')
                    return
                }
                publishClientService.publish(KeyCollection.REDIS_CHANNEL_MODEL_VIEW,
                    "Accessing the model: ${rev.identifier()}")
                if (isPrivateModel) {
                    render(view: "showBasicView", model: [id: rev.model.submissionId, description: rev.description])
                    return
                } else {
                    final String PERENNIAL_ID = (rev.model.publicationId) ?: (rev.model.submissionId)
                    def components = [:]
                    try {
                        components = sbmlService.extractComponentsFromBP(PERENNIAL_ID)
                    } catch (RuntimeException re){
                        log.error("Error while extracting components from BP")
                        log.error(re.inspect())
                    }
                    RevisionTransportCommand revision = modelDelegateService.getLatestRevision(PERENNIAL_ID)
                    boolean showPublishOption = modelDelegateService.canPublish(revision)
                    boolean canSubmitForPublication = modelDelegateService.canSubmitForPublication(revision)
                    boolean canCertify = modelDelegateService.canCertify(revision)
                    boolean canUpdate = modelDelegateService.canAddRevision(PERENNIAL_ID)
                    boolean canDelete = modelDelegateService.canDelete(PERENNIAL_ID)
                    boolean canShare = modelDelegateService.canShare(PERENNIAL_ID)
                    List<FlagTransportCommand> flags = modelDelegateService.getFlags(PERENNIAL_ID)
                    String flashMessage = ""
                    if (flash.now["giveMessage"]) {
                        flashMessage = flash.now["giveMessage"]
                    }
                    List<RFTC> repoFiles = modelDelegateService.retrieveModelFiles(rev)
                    List<RevisionTransportCommand> revs =
                        modelDelegateService.getAllRevisions(PERENNIAL_ID)
                    List<String> reactomeIds = metadataDelegateService.getPathwaysForModelId(PERENNIAL_ID)
                    CurationNotesTransportCommand curationNotes =
                        metadataDelegateService.fetchCurationNotes(rev)
                    String curationState = rev.curationState.name()
                    List<String> possibleCurationStates = CurationState.values()*.name()
                    List<String> originalModels = metadataDelegateService.fetchOriginalModels(rev)
                    Map<String, String> modellingApproaches =
                        metadataDelegateService.fetchModellingApproaches(rev)
                    boolean hasCuratorRole = userService.isLoggedInUserACurator()
                    boolean supportedForConversion = modelConversionService.isSupportedForConversion(rev)
                    List<RFTC> convertedFilesTC = modelConversionService.getConvertedFiles(rev)
                    Set<TagTransportCommand> tags = metadataDelegateService.findTagsByModel(rev.model)
                    String reactomeUrl = ReactomeEnvironment.getUrlForThisEnvironment()

                    def model = [revision               : rev,
                                 reactomeIds            : reactomeIds,
                                 reactomeUrl            : reactomeUrl,
                                 authors                : rev.model.creators,
                                 allRevs                : revs,
                                 flashMessage           : flashMessage,
                                 canUpdate              : canUpdate,
                                 canDelete              : canDelete,
                                 canShare               : canShare,
                                 showPublishOption      : showPublishOption,
                                 canSubmitForPublication: canSubmitForPublication,
                                 canCertify             : canCertify,
                                 repoFiles              : repoFiles,
                                 validationLevel        : rev.getValidationLevelMessage(),
                                 certComment            : rev.getCertificationMessage(),
                                 flags                  : flags,
                                 curationState          : curationState,
                                 possibleCurationStates : possibleCurationStates,
                                 modellingApproaches    : modellingApproaches,
                                 curationNotes          : curationNotes,
                                 originalModels         : originalModels,
                                 hasCuratorRole         : hasCuratorRole,
                                 supportedForConversion : supportedForConversion,
                                 convertedFilesTC       : convertedFilesTC,
                                 bmTags                 : tags,
                                 components             : components
                    ]
                    if (rev.id == revision.id) {
                        flash.genericModel = model
                        ModelFormatTransportCommand format = revision.format
                        String formatController = modelFileFormatService.getPluginForFormat(format)
                        if (formatController) {
                            forward controller: formatController, action: "show", id: PERENNIAL_ID
                        } else {
                            final String fmtId = format.identifier
                            log.error "Could not find a controller for format $fmtId of $PERENNIAL_ID"
                        }
                    } else { //showing an old version, with the default page. Do not allow updates.
                        model["canUpdate"] = false
                        model["showPublishOption"] = false
                        model["oldVersion"] = true
                        model["canDelete"] = false
                        model["canShare"] = false
                        model["canCertify"] = false
                        model["flags"] = flags
                        return model
                    }
                }
            }
            json {
                if (!rev) {
                    respond net.biomodels.jummp.webapp.rest.errors.Error("Invalid Id",
                        "An invalid model id was specified")
                } else {
                    RestfulModel model = new RestfulModel(rev, isPrivateModel)
                    String contentType = "application/json"
                    String jsonModel = model.outputModelAsString(contentType)
                    render(text: jsonModel, contentType: contentType)
                }
            }
            xml {
                if (!rev) {
                    respond net.biomodels.jummp.webapp.rest.errors.Error("Invalid Id",
                        "An invalid model id was specified")
                } else {
                    RestfulModel model = new RestfulModel(rev, isPrivateModel)
                    String contentType = "application/xml"
                    String xmlModel = model.outputModelAsString(contentType)
                    render(text: xmlModel, contentType: contentType)
                }
            }
            '*' {
                render view: '/errors/error415', status: 415 }
        }
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def files() {
        // PageFragmentCachingFilter throws a NPE for unsupported format parameter values
        if (!(response.format in ['json', 'xml'])) {
            render view: '/errors/error415', status: 415
            return
        }
        try {
            def revisionFiles = modelDelegateService.getRevisionFromParams(params.id, params.revisionId).files
            def responseFiles = revisionFiles.findAll { !it.hidden }
            def modelFiles = new ModelFiles(responseFiles)
            withFormat {
                json { respond modelFiles }
                xml { respond modelFiles }
                '*' { render status: 415, view: "/errors/error415" }
            }
        } catch(Exception err) {
            log.error err.message, err
            forward controller: 'errors', action: 'error404'
        }
    }

    def publish() {
        RevisionTransportCommand rev
        RevisionTransportCommand published
        try {
            rev = modelDelegateService.getRevisionFromParams(params.id, params.revisionId)
            published = modelDelegateService.publishModelRevision(rev)
            def currentUser = springSecurityService.currentUser
            if (currentUser) {
                def notification = [
                    revision: rev,
                    user: currentUser,
                    perms: modelDelegateService.getPermissionsMap(rev.model.submissionId)]
                sendMessage("seda:model.publish", notification)
            }
            boolean havePublicationId = published.model.publicationId != null
            String extraMsg = havePublicationId ?
                " with the publication identifier ${published.modelIdentifier()}." : "."
            redirect(action: "showWithMessage", id: published.identifier(),
                        params: [flashMessage: "Model has been published${extraMsg}"])
        } catch(AccessDeniedException e) {
            log.error(e.message, e)
            forward(controller: "errors", action: "error403")
        } catch(IllegalArgumentException e) {
            log.error(e.message)
            redirect(action: "showWithMessage",
                    id: rev.identifier(),
                    params: [flashMessage: "Model has not been published because there is a " +
                            "problem with this version of the model. Sorry!"])
        } catch(Exception e) {
            log.error("General exception thrown while publishing ${rev.identifier()} (${published?.identifier()})", e)
            redirect(action: "showWithMessage", id: rev.identifier(), params: [flashMessage: "An internal error prevented this model from being published"])
        }
    }

    def submitForPublication() {
        try {
            def rev = modelDelegateService.getRevisionFromParams(params.id)
            modelDelegateService.submitModelRevisionForPublication(rev)
            def currentUser = springSecurityService.currentUser
            def perms = modelDelegateService.getPermissionsMap(rev.model.submissionId)
            if (currentUser) {
                def notification = [revision: rev,
                                    user    : currentUser,
                                    perms   : perms]
                sendMessage("seda:model.sub4pub", notification)
            }
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

    Map initialiseSubmission(final boolean isUpdate) {
        Map<String, Object> initials = new HashMap<String, Object>()
        // TODO: reconcile two variables
        initials.put("isUpdate", isUpdate)
        initials.put("isUpdateOnExistingModel", isUpdate)
        initials.put("shouldCreateNewRevision", true) // TODO: get the checkbox
        if (isUpdate) {
            initials.put("modelId", params.id)
        }
        submissionService.initialise(initials)
        initials
    }

    def submit() {
        Map initials = initialiseSubmission(false)
        initials.put("titlePage", "Submit a new model | BioModels")
        initials.put("uploadingFilesHeading", g.message(code: "submission.upload.header"))
        render(view: "submit", model: initials)
    }

    def newUpdate() {
        Map initials = initialiseSubmission(true)
        String modelId = params.id
        String titlePage = "Update model ${modelId} | BioModels"

        initials.put("modelId", modelId)
        initials.put("titlePage", titlePage)
        initials.put("uploadingFilesHeading", g.message(code: "submission.upload.review.titlePage"))

        // TODO: store the initial values (from initials) on Redis
        render(view: "submit", model: initials)
    }

    /**
     * This action acts as the backend of the uploading file from dmuploader plugin.
     *
     * <p>Please see the documentation at <a href="https://github.com/ITersDesktop/uploader">jQuery Ajax File Uploader
     * Widget</a>
     * @return a JSON object to the client/caller
     */
    def uploadFile() {
        String submissionFolder = params.get("submissionFolder")
        log.debug("Submission folder: ${submissionFolder}")
        CommonsMultipartFile uploadFile = request.getMultiFileMap().file?.first()
        String originalFilename = uploadFile?.originalFilename
        File file = fileSystemService.transferFile(submissionFolder, uploadFile)
        long length = file?.length()
        Map returned = [path: originalFilename, length: length]
        if (length) {
            returned.putAll([message: "Uploaded files successfully", status: "OK"])
        } else {
            returned.putAll([message: "Uploaded files unsuccessfully", status: "Failed"])
        }
        render(returned as JSON)
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def terms() {
        [serverURL: grailsApplication.config.grails.serverURL]
    }
    def delete() {
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
    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def getFileDetails() {
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

    def share() {
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

    def shareUpdate() {
        boolean valid = params.collabMap
        if (valid) {
            try {
                def collabData = params.collabMap.decodeHTML()
                def map = JSON.parse(collabData)
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
                    modelDelegateService.updateHistory(MODEL_ID, "something wrong", "update", "html", null, false)
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
                modelDelegateService.updateHistory(session.result_submission, username, "update", "html", update, true)
                if (currentUser && update != "") {
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

    def createFlow = {
        uploadPipeline {
            subflow(controller: "model", action: "upload", input: [isUpdate:false])
            on("abort").to "abort"
            on("displayConfirmationPage") {
                final String USERNAME = userService.getUsername()
                final String AUDIT_ID = session.result_submission
                modelDelegateService.updateHistory(AUDIT_ID, USERNAME, "create", "html", null, true)
                final String biomodelsCuraMailingList = grailsApplication.config.jummp.model.curators.mailinglist
                final String submitterEmail = userService.getEmailAddress()
                if (submitterEmail && USERNAME) {
                    String model = session.result_submission
                    def notification = [
                            model: modelDelegateService.getModel(model),
                            user: springSecurityService.currentUser,
                            emails: [biomodelsCuraMailingList, submitterEmail]]
                    sendMessage("seda:model.create", notification)
                }
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
    def uploadFlow = {
        input {
            isUpdate(required: true)
        }
        start {
            action {
                Map<String, Object> workingMemory = new HashMap<String,Object>()
                flow.workingMemory = workingMemory
                flow.workingMemory.put("isUpdateOnExistingModel", flow.isUpdate)
                conversation.changesMade = new TreeSet<String>()
                if (flow.isUpdate) {
                    String model_id = conversation.model_id
                    flow.workingMemory.put("model_id", model_id)
                    RevisionTransportCommand latest = modelDelegateService.getLatestRevision(model_id, false)
                    flow.workingMemory.put("LastRevision", latest)
                    ModellingApproach approach = latest.model.modellingApproach
                    String modellingApproach = approach ? approach.name : ""
                    flow.workingMemory.put("modelling_approach", modellingApproach)
                    flow.workingMemory.put("other_info", latest.model.otherInfo ?: "")
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
                } else {
                    flow.workingMemory.put("modelling_approach", "")
                    flow.workingMemory.put("other_info", "")
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
                    List<String> mainFileDescription = params.list("mainFileDescription")
                    def extraFileField = request.getMultiFileMap().extraFiles
                    List<MultipartFile> extraMultipartList = []
                    if (extraFileField instanceof MultipartFile) {
                        extraMultipartList = [extraFileField]
                    } else {
                        extraMultipartList = extraFileField
                    }
                    List<String> descriptionFields = params.list("description")
                    List<String> mainsToBeDeleted = params.list("deletedMain")
                    List<String> additionalsToBeDeleted = params.list("deletedAdditional")
                    // Because of using an input element to store deleted files,
                    // we need to reprocess the list of them for getting the list of file name
                    // before sending the result to Submission Service
                    List<String> additionalFilesToBeDeleted = new LinkedList<String>()
                    additionalsToBeDeleted.each {String filename ->
                        int posLessThan = filename.indexOf("<")
                        if (posLessThan > 0) {
                            additionalFilesToBeDeleted.add(filename.substring(0, posLessThan))
                        } else {
                            additionalFilesToBeDeleted.add(filename)
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
                    cmd.mainFileDescription = mainFileDescription
                    cmd.extraFiles = extraMultipartList
                    cmd.mainDeletes = mainsToBeDeleted
                    cmd.extraDeletes = additionalFilesToBeDeleted
                    cmd.description = descriptionFields.toList()
                    if (IS_DEBUG_ENABLED) {
                        log.debug "Data binding done: ${cmd.properties}"
                    }
                    flow.workingMemory.put("UploadCommand", cmd)
                    // store the main files uploaded from the upload flow
                    // into the working memory variable named mains_in_working
                    Map<String, String> mainFiles = new HashMap<String, String>()
                    // submit new models, add new main file
                    if (cmd.mainFile?.first()?.size > 0) {
                        // add the main files when
                        // - the submission flow has just started
                        // - the main files have been removed and added again
                        cmd.mainFile.eachWithIndex{ MultipartFile entry, int index ->
                            String originalFilename = entry.originalFilename
                            String description = cmd.mainFileDescription[index]
//                            println description
                            mainFiles.put(originalFilename, description)
                        }
                    } else if (params.mainFileUpload && params.mainFileDescription) {
                        // the main file has not been updated any more when
                        // the user goes back and forth between the file upload screen
                        // and the model information during the upload flow
                        List fileNames = params.list("mainFileUpload")
                        List descriptions = params.list("mainFileDescription")
                        fileNames.eachWithIndex { String fileName, int index ->
                            mainFiles.put(fileName, descriptions[index] as String)
                        }
                    }
                    flow.workingMemory.put("mains_in_working", mainFiles)

                    // retrieve the additional files from the upload process.
                    // The result is the map of file names and corresponding descriptions.
                    // For instance, manual.pdf: guidelines and help, readme.txt: introduction and preface, ...
                    Map<String, String> additionalFiles = new HashMap<String, String>()
                    // add the existing files that were already uploaded
                    if (params.existedExtraFiles && params.existedExtraFileDescriptions) {
                        List fileNames = params.list("existedExtraFiles")
                        List descriptions = params.list("existedExtraFileDescriptions")
                        fileNames.eachWithIndex { String fileName, int index ->
                            additionalFiles.put(fileName, descriptions[index] as String)
                        }
                    }
                    // add the recently uploaded files
                    if (cmd.extraFiles && cmd.description) {
                        cmd.extraFiles.eachWithIndex { MultipartFile f, int index ->
                            additionalFiles.put(f.originalFilename, cmd.description[index] as String)
                        }
                    }
                    flow.workingMemory.put("additionals_in_working", additionalFiles)
                }
                catch(Exception e) {
                    e.printStackTrace();
                }
            }.to "transferFilesToService"
            on("ProceedWithoutValidation"){
                // do nothing except for logging
                log.debug("The submitter decided to proceed the submission without validation")
            }.to "inferModelInfo"
            on("ProceedAsUnknownFormatVersion"){
                flow.workingMemory.get("model_type").identifier = "UNKNOWN"
            }.to "inferModelInfo"
            on("ProceedAsUnknownFormat"){
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
                List mainFiles = getMainFiles(flow.workingMemory)
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
                    // should this be in a separate action state?
                    def uuid = UUID.randomUUID().toString()
                    if (IS_DEBUG_ENABLED) {
                        log.debug "Generated submission UUID: ${uuid}"
                    }
                    // pray that exchangeDirectory has been defined
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

                    /**
                     Transfer HashMap<String, String> to HashMap<File, String>
                     We know that a map of file names with their descriptions
                     is built at the end of uploadFiles step. This map maintains the newly-updated files
                     which do not contain the deleted ones. Why do we need to transfer?
                     we can get the submission folder at this step and
                     this operation repairs the data for handling file upload afterward.
                    */

                    // FOR THE MAIN FILES
                    // Copy the recently uploaded files to the exchanged folder if they are available
                    List<File> mainFileList
                    if (cmd.mainFile?.first()?.size > 0) {
                        // the main files might be just uploaded
                        mainFileList = fileSystemService.transferMultipartFiles(parent, cmd.mainFile)
                        if (mainFileList.size() == 0) {
                            log.error("""\
There is an error while attempting to copy the main files
wrapped in ${cmd.mainFile.inspect()} to the exchanged folder""")
                        }
                    }
                    // Build a map of the main file objects from the working main files
                    Map<File, String> mainFilesMap = new LinkedHashMap<File, String>()
                    if (flow.workingMemory.containsKey("mains_in_working")) {
                        def mains_in_working = flow.workingMemory.get("mains_in_working") as HashMap<String, String>
                        mains_in_working.each {String keyAsFilename, String valueAsDescription ->
                            mainFilesMap.put(new File(parent, keyAsFilename), valueAsDescription)
                        }
                        flow.workingMemory["submitted_mains"] = mainFilesMap
                    }

                    // FOR THE ADDITIONAL FILES
                    // Copy the recently uploaded files to the exchanged folder if they are available
                    List<File> extraFileList
                    if (cmd.extraFiles) {
                        extraFileList = submissionService.transferMultipartFiles(parent, cmd.extraFiles)
                        if (extraFileList.size() == 0) {
                            log.error("""\
There is an error while attempting to copy the supplemental files
wrapped in ${cmd.extraFiles.inspect()} to the exchanged folder""")
                        }
                    }
                    // Build a map of the additional file objects from the working additional files
                    Map<File, String> additionalFilesMap = new LinkedHashMap<File, String>()
                    if (flow.workingMemory.containsKey("additionals_in_working")) {
                        def additionals_in_working =
                            flow.workingMemory.get("additionals_in_working") as HashMap<String, String>
                        additionals_in_working.each {String keyAsFilename, String valueAsDescription ->
                            additionalFilesMap.put(new File(parent, keyAsFilename), valueAsDescription)
                        }
                        flow.workingMemory["submitted_additionals"] = additionalFilesMap
                    }

                    if (IS_DEBUG_ENABLED) {
                        log.debug """\
About to submit ${mainFilesMap.inspect()} and ${additionalFilesMap.inspect()}."""
                    }

                    // FOR THE DELETED FILES
                    // store the deleted file names into working memory
                    List<String> deletedFileNames = []
                    deletedFileNames.addAll(deletedMains)
                    deletedFileNames.addAll(cmd.extraDeletes)
                    // ensure there are no lists within this list
                    flow.workingMemory["deleted_filenames"] = deletedFileNames.flatten()
                    submissionService.handleFileUpload(flow.workingMemory)
                }

            }
            on("MainFileMissingError") {
                flash.flashMessage = "submission.upload.error.file.missing"
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
                boolean ignoreCheckingVersion = ModelFormatAdapter.ignoreCheckingVersion(format)
                if (format && format.identifier == "UNKNOWN") {
                    UnknownFormat()
                } else if (format && format.identifier !="UNKNOWN" &&
                    format.formatVersion == "*" && !ignoreCheckingVersion) {
                    UnknownFormatVersion()
                } else if (!flow.workingMemory.containsKey("validation_error") ||
                    (format && format.identifier !="UNKNOWN" &&
                    format.formatVersion == "*" && ignoreCheckingVersion)) {
                    Valid()
                } else {
                    String errorAsString = flow.workingMemory.remove("validation_error") as String
                    if (errorAsString.contains("ModelValidationError")) {
                        ModelNotValid()
                    } else {
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
            on("UnknownFormat") {
                flow.workingMemory.put("FormatVersionUnsupported", true)
                flash.showProceedAsUnknownFormat = true
                flash.modelFormatDetectedAs = "UNKNOWN"
            }.to("uploadFiles")
            on("UnknownFormatVersion") {
                flow.workingMemory.put("FormatVersionUnsupported", true)
                // read this parameter to display option to upload without
                // validation in upload files view
                flash.showProceedAsUnknownFormatVersion = true
                flash.modelFormatDetectedAs = flow.workingMemory.get("model_type").identifier
            }.to "uploadFiles"
            on("FilesNotValid") {
                String actualErrorMessage = flow.workingMemory.remove("validation_error") as String
                String[] args = [actualErrorMessage]
                flash.flashMessage = messageSource.getMessage("submission.upload.error.file.invalid",
                    args, Locale.getDefault())
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
                final String modellingApproach = params.modelling_approach
                final String readmeSubmission = params.readme_submission
                final String otherInfo = params.other_info
                final long modelFormat = params.getLong("model_format")
                if (NAME && NAME.trim()) {
                    modifications.put("new_name", NAME.trim())
                }
                if (DESC && DESC.trim()) {
                    modifications.put("new_description", DESC.trim())
                }
                modifications.put("changeStatus", changeStatus)
                submissionService.refineModelInfo(flow.workingMemory, modifications)
                flow.workingMemory.put("readme_submission", readmeSubmission)
                flow.workingMemory.put("modelling_approach", modellingApproach)
                flow.workingMemory.put("other_info", otherInfo)
                flow.workingMemory.put("model_format", modelFormat)
            }.to "enterPublicationLink"
            on("Cancel").to "cleanUpAndTerminate"
            on("Back"){}.to "uploadFiles"
        }
        enterPublicationLink {
            on("Continue") {
                if (!params.PubLinkProvider && params.PublicationLink) {
                    flash.flashMessage = "Please select a publication link type."
                    return error()
                } else {
                    String pubLinkProvider = params.list("PubLinkProvider")[0]
                    String pubLink = params.list("PublicationLink")[0]
                    /**
                     * When 'Publication without link' is chosen, the 'pubLink' is an empty string. Its according value
                     * in the database is null. To make a correct comparison below, we need to transform an empty string
                     * to null.
                     */
                    pubLink = pubLink ?: null
                    if (pubLinkProvider) { // one of the publication link providers has been selected
                        if (!publicationService.verifyLink(pubLinkProvider, pubLink)) {
                            flash.flashMessage = "The link is not a valid ${params.PubLinkProvider}"
                            return error()
                        }
                        ModelTransportCommand model = flow.workingMemory.get("ModelTC") as ModelTransportCommand
                        boolean providerHasChanged = params.PubLinkProvider !=
                            model.publication?.linkProvider?.linkType
                        boolean linkHasChanged = pubLink != model.publication?.link
                        if (providerHasChanged || linkHasChanged) {
                            Map<String,String> modifications = new HashMap<String,String>()
                            modifications.put("PubLinkProvider", params.PubLinkProvider)
                            modifications.put("PubLink", params.PublicationLink)
                            submissionService.updatePublicationLink(flow.workingMemory, modifications)
                        } else {
                            // go through publication editor in any case
                            flow.workingMemory.put("previousPubLinkProvider", model.publication?.linkProvider)
                            flow.workingMemory.put("previousPubLink", model.publication?.link)
                            flow.workingMemory.put("RetrievePubDetails", true)
                        }
                        flow.workingMemory.put("SelectedPubLinkProvider", params.PubLinkProvider)
                    } else { // 'No publication available' has been chosen
                        RevisionTransportCommand revision = flow.workingMemory.get("RevisionTC") as RevisionTransportCommand
                        PublicationTransportCommand publication = revision.model.publication
                        if (publication) {
                            revision.model.publication = null
                            if (flow.workingMemory.containsKey("Authors")) {
                                flow.workingMemory.remove("Authors")
                            }
                        }
                        flow.workingMemory.put("RetrievePubDetails", false)
                    }
                }
            }.to "getPublicationDataIfPossible"
            on("Cancel").to "cleanUpAndTerminate"
            on("Back").to "displayModelInfo"
        }
        getPublicationDataIfPossible {
            action {
                if (flow.workingMemory.remove("RetrievePubDetails") as Boolean) {
                    ModelTransportCommand model = flow.workingMemory.get("ModelTC") as ModelTransportCommand
                    if (model.publication.linkProvider) {
                        PublicationTransportCommand retrieved
                        PDEC publicationContext
                        // Case: PUBMED, DOI, CUSTOM, MANUAL_ENTRY
                        if (flow.workingMemory.containsKey("publication_objects_in_working")) {
                            def publicationMap = flow.workingMemory.get("publication_objects_in_working") as Map<Object, PDEC>
                            publicationContext = publicationMap.get(params.PubLinkProvider)
                            if (publicationContext.publication) {
                                PLPTC previousPubLinkProvider = flow.workingMemory.get("previousPubLinkProvider") as PLPTC
                                String previousPubLink = flow.workingMemory.get("previousPubLink")
                                PLPTC updatedPubLinkProvider = publicationContext.publication?.linkProvider
                                boolean changedPubLinkProvider = previousPubLinkProvider?.linkType != updatedPubLinkProvider?.linkType
                                boolean changedPubLink = previousPubLink != publicationContext?.publication?.link
                                boolean changed =  changedPubLinkProvider || changedPubLink
                                // reload the publication from cache
                                retrieved = publicationContext?.publication
                                if (!changed) {
                                    if (publicationContext.comesFromDatabase) {
                                        flash.flashMessage = g.message(code: "publication.editor.duplicateEntry.message")
                                    }
                                } else { // load from database, external call or create a default PTC
                                    if (updatedPubLinkProvider.linkType == PublicationLinkProvider.LinkType.MANUAL_LABEL) {
                                        if (publicationContext.comesFromDatabase) {
                                            flash.flashMessage = g.message(code: "publication.editor.duplicateEntry.message")
                                        }
                                    } else {
                                        publicationContext = loadOrFetchOrCreatePublication(model)
                                        retrieved = publicationContext?.publication
                                    }
                                }
                            } else { // load from database, external call or create a default PTC
                                publicationContext = loadOrFetchOrCreatePublication(model)
                                retrieved = publicationContext?.publication
                            }
                        } else {
                            log.error("Expected publication objects initialised in workingMemory.")
                        }

                        if (retrieved) {
                            model.publication = retrieved
                            flow.workingMemory.put("Authors", model.publication.authors)

                            // Update the publication objects in working
                            def publicationMap = flow.workingMemory.get("publication_objects_in_working") as Map<Object, PDEC>
                            publicationContext.publication = retrieved
                            publicationMap.put(params.PubLinkProvider, publicationContext)
                        }
                    }
                    // use authors of the existing publication if available
                    if (model.publication) {
                        flow.workingMemory.put("Authors", model.publication.authors)
                    }
                    conversation.changesMade.add("Amended publication details")
                    publicationInfoPage()
                } else {
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
                def publicationMap = flow.workingMemory.get("publication_objects_in_working") as Map<Object, PDEC>
                PDEC pubContext = publicationMap.get(flow.workingMemory.get("SelectedPubLinkProvider"))
                PublicationTransportCommand tempPTC = pubContext.publication
                bindData(tempPTC, params, [exclude: ['authors']])
                try  {
                    publicationService.assembleAuthors(tempPTC, params.authorListContainer.decodeHTML())
                } catch (InvalidPublicationAuthorsException e) {
                    String errMsg = e.getI18nErrorMessage4InvalidAuthor()
                    flash.flashMessage = "There have been errors while parsing authors of the publication:<br/>${errMsg}"
                    return error()
                }
                if (tempPTC.hasErrors()) {
                    flash.validationErrorOn = model.publication
                    return error()
                }
                // Update the publication objects in working
                model.publication = tempPTC
                pubContext.publication = tempPTC
                publicationMap.put(flow.workingMemory.get("SelectedPubLinkProvider"), pubContext)
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

    private PDEC loadOrFetchOrCreatePublication(ModelTransportCommand modelTC) {
        try {
            PDEC publicationContext = publicationService.getPublicationExtractionContext(modelTC.publication)
            if (publicationContext.publication) {
                if (publicationContext.comesFromDatabase) {
                    flash.flashMessage = g.message(code: "publication.editor.duplicateEntry.message")
                }
            } else {
                PublicationTransportCommand retrieved
                retrieved = publicationService.createPTCWithMinimalInformation(params.PubLinkProvider, params.PublicationLink, [])
                publicationContext.publication = retrieved
                publicationContext.comesFromDatabase = false
            }
            return publicationContext
        } catch (Exception e) {
            log.error(e.message, e)
            return null
        }
    }

    private void serveModelAsCombineArchive(List<RFTC> files, def resp) {
        String omexFileName = omexService.createCombineArchive(files, params.id)
        File omexFile = new File(omexFileName)
        String name = omexFile.name
        resp.setContentType("application/zip")
        resp.setHeader("Content-disposition", "attachment;filename=\"${name}\"")
        resp.outputStream << new ByteArrayInputStream(omexFile.readBytes())
        if (omexFile.delete()) {
            log.info("The temporary file was deleted successfully.")
        } else {
            log.info("Cannot delete the temporary file.")
        }
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
        resp.setHeader("Content-disposition", "attachment;filename=\"${params.id}.zip\"")
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
    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def download() {
        try {
            if (params.containsKey("id")) {
                def modelId = params.id
                def revisionId = params.revisionId
                String fileName = params.filename
                if (!fileName) {
                    final List<RFTC> FILES = modelDelegateService.retrieveModelFiles(
                        modelDelegateService.getRevisionFromParams(modelId, revisionId))
                    serveModelAsCombineArchive(FILES, response)
                } else {
                    final List<RFTC> FILES = modelDelegateService.retrieveModelFiles(
                        modelDelegateService.getRevisionFromParams(modelId, revisionId))
                    RFTC requested = FILES.find {
                        if (it.hidden) {
                            return false
                        }
                        File file = new File(it.path)
                        file.getName() == fileName
                    }
                    boolean inline = params.inline == "true"
                    boolean preview = params.preview == "true"
                    if (requested) {
                        serveModelAsFile(requested, response, inline, preview)
                    } else {
                        response.status = HttpServletResponse.SC_BAD_REQUEST
                        def err = new Error("Invalid file name",
                            "Cannot find file ${fileName} belonging to model $modelId")
                        withFormat {
                            json { respond err }
                            xml { respond err }
                            // for all else we send a 404
                        }
                    }
                }
            } else {
                response.status = HttpServletResponse.SC_BAD_REQUEST
                forward(controller: "errors", action: "error400")
            }
        } catch (Exception e) {
            log.error(e.message, e)
            render(status: 400,
                view: "/errors/error400",
                model: [errorDescription: "The model identifier parameter must be provided."])
            return
        }
    }

    /**
     * Update the curation status of the model
     */
    def updateCurationState() {
        def requestObject = request.JSON
        if (!requestObject['revisionNumber'] || !requestObject['modelId'] || !requestObject['curationState']) {
            response.status = 400
            render([message: 'Bad request'] as JSON)
            return
        }

        int revision = Integer.parseInt(requestObject['revisionNumber'] as String)
        String modelId = requestObject['modelId']
        boolean canUpdate = modelDelegateService.canAddRevision(modelId as String)
        boolean hasCuratorRole = userService.isLoggedInUserACurator()
        if (canUpdate && hasCuratorRole) {
            CurationState curationState = CurationState.valueOf(requestObject['curationState'] as String)
            RevisionTransportCommand revisionTC = modelDelegateService.updateCurationStateRevision(modelId, revision, curationState)
            Map result = [:]
            result["message"] = "Curation status has been updated successfully"
            result["publicationId"] = revisionTC.model.publicationId
            render(result as JSON)
            return
        }
        response.status = 401
        render([message: "You do not have right permissions to change the curation status"] as JSON)
    }

    /**
     * Search modelling approaches based what users are typing. The data populate the source of
     * Autocomplete widgets. The data can be customised but they have to include two mandatory
     * fields as label and value. These two fields are formed from the other ones. For example:
     * label = MAMO accession: the friendly name
     * Example: MAMO_0000009: constraint-based model
     */
    @Secured(['IS_AUTHENTICATED_FULLY'])
    @grails.transaction.Transactional
    def searchModellingApproach() {
        Integer request = params.getInt("request")
        String searchTerm = params.get("search")
        if (request == RequestType.SEARCH_TERMS.value) {
            List modellingApproaches = metadataDelegateService.searchModellingApproach(searchTerm)
            List approaches = []
            modellingApproaches.each { approach ->
                long id = approach[0]
                String accession = approach[1]
                String name = approach[2]
                String resource = approach[3]
                String label = name
                approaches << [id: id, name: name, resource: resource, value: id, label: label]
            }
            render(approaches as JSON)
        } else if (request == RequestType.SELECT_VALUE.value) {
            String approach = params.get("name")
            ModellingApproach modellingApproach = metadataDelegateService.getModellingApproach(approach)
            render([modellingApproach] as JSON)
        } else {
            String message = """\
Please type a few first characters of your thinking words or select a modelling
approach from the list of suggested values. Otherwise, type 'Other'"""
            render([message: message] as JSON)
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
