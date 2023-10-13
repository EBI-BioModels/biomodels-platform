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
import grails.converters.XML
import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional
import grails.util.Environment
import net.biomodels.jummp.CommonController
import net.biomodels.jummp.core.IFileSystemService
import net.biomodels.jummp.core.ModelException
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.core.annotation.StatementTransportCommand as STC
import net.biomodels.jummp.core.constants.BioModels
import net.biomodels.jummp.core.model.*
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.util.ReactomeEnvironment
import net.biomodels.jummp.deployment.biomodels.CurationNotesTransportCommand as CNTC
import net.biomodels.jummp.deployment.biomodels.TagTransportCommand as TagTC
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.Team
import net.biomodels.jummp.utils.redis.KeyCollection
import net.biomodels.jummp.utils.redis.Operations
import net.biomodels.jummp.webapp.rest.errors.Error
import net.biomodels.jummp.webapp.rest.model.show.Model as RestfulModel
import net.biomodels.jummp.webapp.rest.model.show.ModelFiles
import org.apache.http.HttpEntity
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.codehaus.groovy.grails.web.json.JSONObject
import org.json.JSONArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.commons.CommonsMultipartFile

import javax.servlet.http.HttpServletResponse
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Secured(['IS_AUTHENTICATED_FULLY'])
class ModelController extends CommonController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelController.class)
    private final boolean IS_DEBUG_ENABLED = LOGGER.isDebugEnabled()
    IFileSystemService fileSystemService
    def springSecurityService
    def modelDelegateService
    def modelFileFormatService
    def teamService
    def sbmlService
    def submissionService
    def grailsApplication
    def publicationService
    def metadataDelegateService
    def omexService
    def modelConversionService
    def userService
    def publishClientService

    /**
     * The list of actions for which we should not automatically create an audit item.
     */
    final List<String> AUDIT_EXCEPTIONS = ['showWithMessage',
                                           'getFileDetails', 'submitForPublication', 'updateCurationState',
                                           'searchModellingApproach', 'submit', 'terms', 'uploadFile',
                                           'identifiers', 'createCombineArchive']

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
                int historyItem = modelDelegateService.updateHistory(model, username, accessType, formatType,
                    changesMade)
                request.lastHistory = historyItem
                return true
            } else {
                LOGGER.error "Ignoring invalid request for $actionUri with params $params."
                forward(controller: "errors", action: "error404")
                return false
            }
        } catch(Exception e) {
            LOGGER.error(e.message, e)
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
            LOGGER.error e.message, e
        }
    }

    @Transactional
    def showWithMessage() {
        flash["giveMessage"] = params.flashMessage
        StringBuilder modelId = new StringBuilder(params.id as String)
        if (params.revisionId) {
            modelId.append('.').append(params.revisionId as String)
        }
        redirect(action: "show", id: modelId.toString())
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    @Transactional
    def show() {
        RevisionTransportCommand rev
        boolean isPrivateModel = false
        try {
            rev = modelDelegateService.getRevisionFromParams(params.id, params.revisionId)
        } catch (AccessDeniedException e) {
            Model model = Model.findByPublicationIdOrSubmissionId(params.id as String, params.id as String)
            LOGGER.warn("""An anonymous or restricted access user is trying to retrieve this model: ${model.submissionId}""")
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
                    String vcsId = modelDelegateService.getVcsIdentifier(PERENNIAL_ID)
                    String modelParentFolder = ""
                    if (vcsId) {
                        modelParentFolder = vcsId.take(3)
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
                    long totalSize = repoFiles.collect { it.size }.sum() as long
                    boolean canCreateOmex = totalSize <= BioModels.MAX_FILE_SIZE // 500MB
                    repoFiles = modelDelegateService.sortModelFilesByName(repoFiles)
                    List<RevisionTransportCommand> revs =
                        modelDelegateService.getAllRevisions(PERENNIAL_ID)
                    List<String> reactomeIds = metadataDelegateService.getPathwaysForModelId(PERENNIAL_ID)
                    CNTC curationNotes =
                        metadataDelegateService.fetchCurationNotes(rev)
                    String curationState = rev.curationState.name()
                    List<String> possibleCurationStates = CurationState.values()*.name()
                    List<STC> modelLevelAnnotations = metadataDelegateService.getModelLevelAnnotations(rev)
                    List<String> originalModels = metadataDelegateService.fetchOriginalModels(modelLevelAnnotations)
                    Map<String, String> modellingApproaches =
                        metadataDelegateService.fetchModellingApproaches(rev)
                    boolean hasCuratorRole = userService.isLoggedInUserACurator()
                    boolean supportedForConversion = modelConversionService.isSupportedForConversion(rev)
                    List<RFTC> convertedFilesTC = modelConversionService.getConvertedFiles(rev)
                    Set<TagTC> tags = metadataDelegateService.findTagsByModel(rev.model)
                    String reactomeUrl = ReactomeEnvironment.getUrlForThisEnvironment()
                    String hrefLinkToNewtEditor = makeLinkToNewtEditor(revision, repoFiles)
                    def currentUser = springSecurityService.currentUser
                    boolean canAskReviewerAccount = true
                    if (!currentUser) {
                        canAskReviewerAccount = false
                    } else {
                        canAskReviewerAccount = modelDelegateService.canAskReviewerAccount(revision, hasCuratorRole)
                    }
                    def contributors = modelDelegateService.collectContributors(revision.model.contributors)
                    boolean canSeeCurationTab = modelDelegateService.canSeeCurationTab(revision, hasCuratorRole, currentUser)
                    Map model = [
                         revision               : rev,
                         reactomeIds            : reactomeIds,
                         reactomeUrl            : reactomeUrl,
                         hrefLinkToNewtEditor   : hrefLinkToNewtEditor,
                         authors                : rev.model.creators,
                         contributors           : contributors,
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
                         modelLevelAnnotations  : modelLevelAnnotations,
                         originalModels         : originalModels,
                         hasCuratorRole         : hasCuratorRole,
                         supportedForConversion : supportedForConversion,
                         convertedFilesTC       : convertedFilesTC,
                         bmTags                 : tags,
                         canAskReviewerAccount  : canAskReviewerAccount,
                         canSeeCurationTab      : canSeeCurationTab,
                         modelParentFolder      : modelParentFolder,
                         canCreateOmex          : canCreateOmex
                    ]
                    Map cmmProps = COMMON_PROPERTIES
                    model.putAll(cmmProps)
                    if (rev.id == revision.id) {
                        flash.genericModel = model
                        ModelFormatTransportCommand format = revision.format
                        String formatController = modelFileFormatService.getPluginForFormat(format)
                        if (formatController) {
                            forward controller: formatController, action: "show", id: PERENNIAL_ID
                        } else {
                            final String fmtId = format.identifier
                            LOGGER.error "Could not find a controller for format $fmtId of $PERENNIAL_ID"
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
            LOGGER.error err.message, err
            forward controller: 'errors', action: 'error404'
        }
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def revisionsState() {
        // PageFragmentCachingFilter throws a NPE for unsupported format parameter values
        if (!(response.format in ['json', 'xml'])) {
            render view: '/errors/error415', status: 415
            return
        }
        try {
            Map resultMap = modelDelegateService.getRevisionsState(params.id)
            withFormat {
                json { respond resultMap }
                xml { respond resultMap }
                '*' { render status: 415, view: "/errors/error415" }
            }
        } catch(Exception err) {
            LOGGER.error err.message, err
            forward controller: 'errors', action: 'error404'
        }
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def identifiers() {
        if (!(response.format in ['json', 'xml'])) {
            render view: '/errors/error415', status: 415
            return
        }
        try {
            List<String> listAllIdentifiers = modelDelegateService.getAllModelIdentifiers()
            Map models = ["hits": listAllIdentifiers?.size() ?: 0, "models": listAllIdentifiers]
            withFormat {
                json { render models as JSON }
                xml { render models as XML }
                '*' { render status: 415, view: "/errors/error415" }
            }
        } catch(Exception err) {
            println(err.printStackTrace())
            LOGGER.error(err.message, err)
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
            LOGGER.error(e.message, e)
            forward(controller: "errors", action: "error403")
        } catch(IllegalArgumentException e) {
            LOGGER.error(e.message)
            redirect(action: "showWithMessage",
                    id: rev.identifier(),
                    params: [flashMessage: "Model has not been published because there is a " +
                            "problem with this version of the model. Sorry!"])
        } catch(Exception e) {
            LOGGER.error("General exception thrown while publishing ${rev.identifier()} (${published?.identifier()})", e)
            redirect(action: "showWithMessage", id: rev.identifier(), params: [flashMessage: "An internal error prevented this model from being published"])
        }
    }

    def submitForPublication() {
        def rev = modelDelegateService.getRevisionFromParams(params.id)
        boolean allowed2Request = modelDelegateService.canSubmitForPublication(rev)
        if (!allowed2Request) {
            redirect(action: "showWithMessage",
                id: rev.identifier(),
                params: [flashMessage: "Sorry! You are not allowed to perform this operation."])
            return
        }
        try {
            modelDelegateService.submitModelRevisionForPublication(rev)
            def perms = modelDelegateService.getPermissionsMap(rev.model.submissionId)
            def currentUser = springSecurityService.currentUser
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
            LOGGER.error(e.message, e)
            String message = "Sorry!!! There has been a problem. Please try it later or contact us for further help."
            redirect(action: "showWithMessage",
                id: modelDelegateService.getRevisionFromParams(params.id).identifier(),
                params: [flashMessage: message])
        }
    }

    private Map initialiseSubmission(final boolean isUpdate) {
        Map<String, Object> initials = new HashMap<String, Object>()
        // TODO: reconcile two variables
        initials.put("isUpdate", isUpdate)
        initials.put("isUpdateOnExistingModel", isUpdate)
        initials.put("shouldCreateNewRevision", true) // TODO: where is this used
        if (isUpdate) {
            initials.put("modelId", params.id)
        }
        submissionService.initialise(initials)
        buildModelInfo(initials)
        initials
    }

    private Map buildModelInfo(Map initials) {
        Map modelInfo = new HashMap()
        boolean isUpdate = initials.get("isUpdate")
        RevisionTransportCommand revisionTC = initials.get("RevisionTC")
        modelInfo.put("detectedName", isUpdate ? revisionTC?.name : "")
        modelInfo.put("detectedDescription", isUpdate ? revisionTC?.description : "")

        Map detectedModelFormat = [:]
        detectedModelFormat.put("id", revisionTC?.format?.id.toString())
        detectedModelFormat.put("name", revisionTC?.format?.name)
        detectedModelFormat.put("readme", revisionTC?.readmeSubmission)
        modelInfo.put("detectedModelFormat", detectedModelFormat)

        Map detectedModelling = [:]
        detectedModelling.put("approach", initials.get("modellingApproach"))
        detectedModelling.put("otherInfo", initials.get("otherInfo"))
        modelInfo.put("detectedModelling", detectedModelling)

        initials.put("modelInfo", modelInfo as JSON)
        initials.put("readmeSubmission", detectedModelFormat.get("readme"))

        if (isUpdate) {
            def pub = publicationService.findPublicationOfModel(revisionTC.modelIdentifier())
            initials.put("publication", pub)
        }

        initials
    }

    def submit() {
        Map initials = initialiseSubmission(false)
        initials.put("controller", "model")
        initials.put("operation", "submit")
        initials.put("titlePage", "Submit a new model | BioModels")
        initials.put("uploadingFilesHeading", g.message(code: "submission.upload.header"))
        render(view: "submit", model: initials)
    }

    def update() {
        Map initials = initialiseSubmission(true)
        String modelId = params.id
        String titlePage = "Update model ${modelId} | BioModels"
        initials.put("controller", "model")
        initials.put("operation", "update")
        initials.put("modelId", modelId)
        initials.put("titlePage", titlePage)
        initials.put("uploadingFilesHeading", g.message(code: "submission.upload.review.titlePage"))

        String submissionFolder = initials.get("submissionFolder")
        RevisionTransportCommand revisionTC = initials.get("RevisionTC")
        Map submissionDataMap = ["latestModelDescription": revisionTC.description ?: ""]
        Operations.doRedisHSet(submissionFolder, submissionDataMap)

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
        LOGGER.debug("Submission folder: ${submissionFolder}")
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

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def createCombineArchive() {
        RevisionTransportCommand revisionTC
        String filePath = ""
        String modelId = ""
        Integer revisionNumber = 0
        try {
            revisionTC = modelDelegateService.getRevisionFromParams(params.id, params.revisionId)
            modelId = revisionTC.model.submissionId
            revisionNumber = revisionTC.revisionNumber
            final List<RFTC> FILES = modelDelegateService.retrieveModelFiles(revisionTC)
            JSONArray array = modelDelegateService.buildJsonArray(modelId, revisionNumber, FILES)

            CloseableHttpClient httpClient = HttpClientBuilder.create().build()
            final String FS_SVR_URL = System.getenv().getOrDefault("FS_SVR_URL", "http://localhost:8090/api/v1.0")
            try {
                HttpPost request = new HttpPost("${FS_SVR_URL}/file-format/create-omex")
                StringEntity params = new StringEntity(array.toString(), "UTF-8")
                request.addHeader("content-type", "application/json")
                request.setEntity(params)

                CloseableHttpResponse response = httpClient.execute(request)
                try {
                    HttpEntity entity = response.getEntity()
                    if (entity != null) {
                        filePath = EntityUtils.toString(entity)
                    }

                } finally {
                    response.close()
                }
            } catch (Exception ignored) {
                // handle exception here
                ignored.printStackTrace()
            } finally {
                httpClient.close()
            }
        } catch (ModelException ignored) {
            ignored.printStackTrace()
        } finally {
            LOGGER.info("File Path: $filePath")
        }
        // asynchronous jobs have to be called here
        // TODO: email or notify the requester the location of the OMEX file so that they can download it later.
        // TODO: send the map of parameters below to the server to copy this file to FTP public (for the public ones)
        // and the location that will be expired within 1 hour (for the private ones)
        render([modelId: modelId, revisionNumber: revisionNumber, location: filePath] as JSON)
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
            LOGGER.error e.message, e
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
                LOGGER.debug("Permissions for ${REVISION.identifier()}: ${retval as JSON}")
            }
            render retval as JSON
        } catch(Exception e) {
            LOGGER.error e.message, e
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
            LOGGER.error error.message, error
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
                LOGGER.error e.message, e
                valid = false
            }
        }
        if (!valid) {
            render (['success': false, 'message': "Could not update permissions"] as JSON)
        }
    }

    private void serveModelAsCombineArchive(RevisionTransportCommand revision, List<RFTC> files, def resp) {
        String EBI_BM_FTP = "${BioModels.EBI_BM_PUBLIC_FTP}/repository"
        String omexName = "${revision.model.submissionId}.${revision.revisionNumber}.omex"
        String filePath = "${revision.model.submissionId}/${revision.revisionNumber}/${omexName}"
        String modelParentFolder = modelDelegateService.getRevisionsState(revision.modelIdentifier()).vcsId
        // Use case 2 and 4: Revision is public regardless of its size
        if (revision.state == ModelState.PUBLISHED) {
            println "use case 2 and 4: public"
            String url = "${EBI_BM_FTP}/${modelParentFolder}/$filePath"
            redirect(url: url)
            return
        }
        // Revision is private, then considering the size of the request
        long totalSize = files.collect { it.size }.sum()
        boolean isLargeSubmission = totalSize >= BioModels.MAX_FILE_SIZE
        // Use case 1: large and private
        if (isLargeSubmission) {
            println "use case 1: large and private"
            Map result = createCombineArchive()
            filePath = result.get("location")
            if (filePath) {
                forward(filePath)
            } else {
                forward(controller: "errors", action: "error413")
            }
            return
        }
        // Use case 3: small and private, then generate/create CombineArchive on the spot
        println "use case 3: small and private"
        createInstantCombineArchive(files, resp)
    }

    private void createInstantCombineArchive(List<RFTC> files, def resp) {
        long time = System.nanoTime()
        String omexFileName = omexService.createCombineArchive(files, params.id)
        File omexFile = new File(omexFileName)
        String name = omexFile.name
        resp.setContentType("application/zip")
        resp.setHeader("Content-disposition", "attachment;filename=\"${name}\"")
        ByteArrayInputStream  stream = null
        try {
            stream = new ByteArrayInputStream(omexFile.readBytes())
            resp.outputStream << stream
        } catch (IOException ioE) {
            LOGGER.error("The client might have aborted their download request.", ioE)
        } finally {
            if (omexFile.delete()) {
                LOGGER.info("The temporary file was deleted successfully.")
            } else {
                LOGGER.info("Cannot delete the temporary file.")
            }
            if (stream) {
                stream.close()
            }
        }
        time = (System.nanoTime() - time) / 1_000_000.0
        long seconds = time / 1_000;
        long HH = seconds / 3600;
        long MM = (seconds % 3600) / 60;
        long SS = seconds % 60;
        String timeInHHMMSS = String.format("%02d:%02d:%02d", HH, MM, SS);
        LOGGER.info("It took ${time}ms ~ ${timeInHHMMSS} to generate the OMEX file $omexFileName.")
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
        ByteArrayInputStream  stream = null
        try {
            stream = new ByteArrayInputStream(byteBuffer.toByteArray())
            resp.outputStream << stream
        } catch (IOException ioE) {
            LOGGER.error("The client might have aborted their download request.", ioE)
        } finally {
            if (zipFile) {
                LOGGER.debug("ZipFile ${zipFile.comment} has been flushed and closed.")
                zipFile.flush()
                zipFile.close()
            }
            if (byteBuffer) {
                LOGGER.debug("byteBuffer (built from ${zipFile.comment}) has been flushed and closed.")
                byteBuffer.flush()
                byteBuffer.close()
            }
            if (stream) {
                LOGGER.debug("OutputStream of the file has been flushed and closed.")
                stream.close()
            }
        }
    }

    private void serveModelAsFile(RFTC rf, def resp, boolean inline, boolean preview = false) {
        File file = new File(rf.path)
        resp.setContentType(rf.mimeType)
        final String INLINE = inline ? "inline" : "attachment"
        final String F_NAME = URLEncoder.encode(file.name, "UTF-8")
        resp.setCharacterEncoding("UTF-8")
        resp.setHeader( "Content-Disposition", "${INLINE};filename=\"${F_NAME}\"")
        byte[] fileData = null
        int previewSize = grailsApplication.config.jummp.web.file.preview as Integer
        ByteArrayInputStream  stream = null
        try {
            if (file.length() > BioModels.MAX_FILE_SIZE) {
                String warnMsg = "File ${file.name} is too large to be served now."
                LOGGER.debug(warnMsg)
                println(warnMsg)
                stream = new ByteArrayInputStream("BIG_FILE".bytes)
            } else {
                fileData = file.readBytes()

                if (!preview || previewSize > fileData.length) {
                    stream = new ByteArrayInputStream(fileData)
                } else {
                    fileData = file.readBytes()
                    stream = new ByteArrayInputStream(Arrays.copyOf(fileData, previewSize))
                }
            }
            resp.outputStream << stream
        } catch (IOException ioE) {
            LOGGER.error("The client might have cancelled downloading the file ${file.name}.", ioE)
        } finally {
            if (stream != null) {
                LOGGER.debug("InputStream of the file ${file.name} has been flushed and closed.")
                stream.close()
            }
            Arrays.fill(fileData, (byte)0)
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
                String fileName = params.filename.decodeHTML()
                if (Environment.isWarDeployed() && fileName != null) {
                    fileName = handleDlSpecialCharacters(fileName)
                }
                RevisionTransportCommand revision = modelDelegateService.getRevisionFromParams(modelId, revisionId)
                final List<RFTC> FILES = modelDelegateService.retrieveModelFiles(revision)
                if (!fileName) {
                    serveModelAsCombineArchive(revision, FILES, response)
                } else {
                    RFTC requested = FILES.find {
                        !it.hidden && new File(it.path).getName() == fileName
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
                forward(controller: "errors", action: "error400")
            }
        } catch (Exception e ) {
            String errDesc = handleDlException(e)
            render(status: 400, view: "/errors/error400", model: [errorDescription: errDesc])
        } finally {
            // TODO: How to clean up the recently used resources to free the heap memory
            // In fact, the cleaning process is performed in the sub processes of this action,
            // e.g. {@see serveModelAsCombineArchive()} and {@see serveModelAsFile()} method
        }
    }

    /**
     * Updates the curation status of the model
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
        ByteArrayInputStream stream = new ByteArrayInputStream(bytes)
        try {
            response.outputStream << stream
        } catch (IOException ioE) {
            LOGGER.error("The client might have cancelled their download request.", ioE)
        } finally {
            if (stream) {
                stream.close()
            }
        }
    }

    private String handleDlException(Exception e) {
        if (e instanceof AccessDeniedException) {
            forward(controller: "errors", action: "error403")
        }
        String errDesc = ""
        if (e instanceof IOException) {
            errDesc = "The client has probably aborted the download request."
        } else {
            errDesc = "The model identifier parameter must be provided."
        }
        LOGGER.error(errDesc, e)
        errDesc
    }

    private String handleDlSpecialCharacters(String fileName) {
        // This block temporarily solves this problem with special characters in the file name
        String resCharacterEncoding = response.characterEncoding
        boolean IS_ISO_8859_1 = resCharacterEncoding.equalsIgnoreCase("iso-8859-1")
        boolean FILENAME_REQUESTED = request.parameterMap.containsKey("filename")
        if (IS_ISO_8859_1 && FILENAME_REQUESTED) {
            fileName = request.getParameter("filename")
            fileName = new String(fileName.getBytes("iso-8859-1"))
        }
        return fileName
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

    private String makeLinkToNewtEditor(final RevisionTransportCommand revision, final List<RFTC> repoFiles) {
        boolean published = ModelState.PUBLISHED == revision.state
        boolean isSBMLModel = "SBML" == revision.format.name
        String href = ""
        if (published && isSBMLModel) {
            String modelMainFileName = repoFiles.find { it?.mainFile }?.filename
            if (modelMainFileName) {
                String downloadLink = createLink(controller: 'model',
                    action: 'download', params: [id: revision.identifier(), filename: modelMainFileName], absolute: true)
                String otherParams = "inferNestingOnLoad=true&applyLayoutOnURL=true"
                href = "https://web.newteditor.org/"
                href = "$href?URL=${downloadLink}&${otherParams}"
            }
        }
        return href
    }
}
