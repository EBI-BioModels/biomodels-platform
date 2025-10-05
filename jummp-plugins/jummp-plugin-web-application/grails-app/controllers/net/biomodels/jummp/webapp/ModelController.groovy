/**
 * Copyright (C) 2010-2024 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.async.Promise
import grails.converters.JSON
import grails.converters.XML
import grails.plugin.springsecurity.annotation.Secured
import grails.transaction.Transactional
import grails.util.Environment
import net.biomodels.jummp.CommonController
import net.biomodels.jummp.core.IFileSystemService
import net.biomodels.jummp.core.ModelException
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.core.annotation.StatementTransportCommand as STC
import net.biomodels.jummp.core.constants.BioModels
import net.biomodels.jummp.core.events.ModelOperationEvent
import net.biomodels.jummp.core.events.ModelPublishedEvent
import net.biomodels.jummp.core.model.*
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.core.util.JummpHttpService
import net.biomodels.jummp.core.util.ReactomeEnvironment
import net.biomodels.jummp.deployment.biomodels.CurationNotesTransportCommand as CNTC
import net.biomodels.jummp.deployment.biomodels.TagTransportCommand as TagTC
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.Team
import net.biomodels.jummp.utils.MathUtils
import net.biomodels.jummp.utils.WebServiceFetcher as WSF
import net.biomodels.jummp.utils.redis.KeyCollection
import net.biomodels.jummp.webapp.rest.errors.Error
import net.biomodels.jummp.webapp.rest.model.Model as RestfulModel
import net.biomodels.jummp.webapp.rest.model.ModelFiles
import org.codehaus.groovy.grails.web.json.JSONObject
import org.json.JSONArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.multipart.commons.CommonsMultipartFile

import javax.servlet.http.HttpServletResponse
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

import static grails.async.Promises.task

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
    def redisService

    /**
     * The list of actions for which we should not automatically create an audit item.
     */
    final List<String> AUDIT_EXCEPTIONS = ['showWithMessage', 'getFileDetails',
       'submitForPublication', 'updateCurationState',
       'searchModellingApproach', 'submit', 'terms', 'uploadFile',
       'identifiers', 'createCombineArchive', 'doAddOrRemoveGalaxyLink',
       'create', 'about', 'revisionsState', 'generateOmex', 'metadatardf', 'retrieveRevisionsState',
       'retrieveModelLevelMetadata', 'cacheAnnotationsAndOrganismOnRedis', 'loadAllAnnotations', 'formats'
    ]

    def beforeInterceptor = [action: this.&auditBefore, except: AUDIT_EXCEPTIONS]
    def afterInterceptor = [action: this.&auditAfter, except: AUDIT_EXCEPTIONS]

    // if this method returns false, the controller method is no longer called.
    private boolean auditBefore() {
        try {
            // XSS guard for the actions from this controller (excluding submission)
            params.id = params.id.decodeHTML()
            params.revisionId = params.revisionId.decodeHTML()
            String modelIdParam = params.id
            String revisionIdParam = params.revisionId
            String modelId = null
            String username = userService.getUsername()
            String accessType = actionUri
            String formatType = response.format
            String changesMade = null
            // not call isPositiveNumber if modelIdParam is null
            final boolean HAS_ONLY_DIGITS = modelIdParam ? MathUtils.isPositiveNumber(modelIdParam) : false
            // perennial model identifiers include literals
            final boolean IS_REVISION_ID = !revisionIdParam && HAS_ONLY_DIGITS
            if (IS_REVISION_ID) {
                // publish uses revision ids, annoyingly enough.
                if (accessType.contains("publish")) {
                    def rev = modelDelegateService.getRevisionDetails(
                        new RTC(id: modelIdParam.toInteger()))
                    if (rev) {
                        modelId = rev.modelIdentifier()
                    }
                }
            }
            ModelTransportCommand model = null
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
        } catch (Exception e) {
            LOGGER.error(e.message, e)
            String actionError = params?.action == "download" ? "error400" : "error403"
            forward(controller: "errors", action: actionError)
            return false
        }
    }

    private void auditAfter(def model) {
        try {
            if (request.lastHistory) {
                modelDelegateService.updateAuditSuccess(request.lastHistory as Long, true)
                request.removeAttribute("lastHistory")
                //LOGGER.info("Model in auditAfter: ${model?.dump()}")
            }
        } catch (Exception e) {
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
        RTC rev = null
        List<RTC> myList = new ArrayList()
        boolean isPrivateModel = false
        try {
            /**
             * When a privileged user such as model owner or admin accesses the private revision, the isPrivateModel
             * flag should be assigned true. However, to keep the logic simply, we handle the private revision as
             * the public one with the super users.
             */
            LOGGER.info("""${params.id}${params.revisionId ? ".".concat(params.revisionId as String) : ""} \
has been accessed!""")
            rev = modelDelegateService.getRevisionFromParams(params.id as String, params.revisionId as String)
        } catch (AccessDeniedException e) {
            // then access the model by bypassing ACLs
            Model model = Model.findByPublicationIdOrSubmissionId(params.id as String, params.id as String)
            if (!model) {
                LOGGER.debug("${params.id} doesn't not exist!")
                forward(controller: 'errors', action: 'error404')
                return
            }
            LOGGER.warn("You are trying to retrieve a private model: " + e.message)
            isPrivateModel = true
            doShowPreparePrivateRevision(model, rev, myList)
            rev = myList.first() as RTC
        }
        // allowAccessHTMLViaBrowser(request.getHeader("User-Agent") as String, params?.format as String)
        String userAgent = request.getHeader("User-Agent")
        if (params?.format && params?.format?.toLowerCase() == "html" &&
            !WSF.isUserAgentSupported(userAgent)) {
            LOGGER.warn("$userAgent has tried to access HTML format of ${rev.identifier()}.")
            render("Apologise! Your operation is not supported.")
            return
        }
        withFormat {
            html {
                if (!rev) {
                    forward(controller: 'errors', action: 'error404')
                    return
                }
                publishClientService.publish(KeyCollection.REDIS_CHANNEL_MODEL_VIEW, "Accessing the model: ${rev.identifier()}")
                if (isPrivateModel) {
                    render(view: "showBasicView", model: [id: rev.model.submissionId, description: rev.description])
                } else {
                    final String PERENNIAL_ID = (rev.model.publicationId) ?: (rev.model.submissionId)
                    RTC revision = modelDelegateService.getLatestRevision(PERENNIAL_ID)
                    List<RFTC> repoFiles = modelDelegateService.retrieveModelFiles(rev)
                    repoFiles = modelDelegateService.sortModelFilesByName(repoFiles)

                    Map model = doShowGetInitialValues(PERENNIAL_ID, rev, revision, repoFiles)
                    Map cmmProps = COMMON_PROPERTIES
                    model.putAll(cmmProps)
                    model.putAll(doShowGetCheckConditions(PERENNIAL_ID, rev, repoFiles))
                    model.putAll(doShowGetCurationData(rev))
                    model.putAll(doShowGetExternalLinkedData(PERENNIAL_ID, rev, repoFiles))
                    model.putAll(doShowGetAnnotationsBasedData(PERENNIAL_ID, rev))
                    if (rev.id == revision.id) {
                        if (redisService.doRedisGet(KeyCollection.DEBUGGING_MODE)) {
                            if (redisService.doRedisGet(KeyCollection.DEBUGGING_MODE).toBoolean()) {
                                // For testing and debugging this method with a simple view
                                LOGGER.debug("Debugging mode is ON")
                                // will more the next call out of the here later
                                model.putAll(doShowInferSpecificTabs(PERENNIAL_ID, rev))
                                model.putAll(["newLook": true, announcement: "Back to the old interface"])
                                render(view: "display", model: model)
                                return true
                            }
                        }
                        model.putAll(["newLook": false, announcement: "Try with a new look and feel of this page"])
                        doShowRenderLatestRevision(model, revision, PERENNIAL_ID)
                    } else { // showing an old version, with the default page. Do not allow updates.
                        model.putAll(["newLook": false, announcement: "Try with a new look and feel of this page"])
                        doShowPrepareOldRevision(model)
                        render(view: "show", model: model)
                    }
                }
            }
            json { doShowRenderWithFormat(rev, isPrivateModel, "json") }
            xml { doShowRenderWithFormat(rev, isPrivateModel, "xml") }
            '*' { render(view: '/errors/error415', model: [code: 415]) }
        }
    }

    private void allowAccessHTMLViaBrowser(final String userAgent, final String format = null) {
        if (format && format?.toLowerCase() == "html" && !WSF.isUserAgentSupported(userAgent)) {
            render(view: '/errors/error415', model: [code: 415])
        }
    }

    private void doShowPreparePrivateRevision(final Model model, RTC rtc, List<RTC> list) {
        int revisionNumber = -1
        if (params.revisionId) {
            revisionNumber = params.int("revisionId")
        }
        // TODO need to establish if the requested model revision exists in a way that bypasses
        // ACLs and that doesn't rely on accessing domain objects from the controller
        Revision revision = revisionNumber >= 0 ?
            model.revisions[revisionNumber - 1] : model.revisions.last()
        if (!revision) {
            LOGGER.debug("${params.id}.${revisionNumber} doesn't not exist!")
            forward(controller: 'errors', action: 'error404')
            return
        }
        rtc = new RevisionAdapter(revision: revision).toCommandObject()
        rtc.model = new ModelAdapter(model: model).toCommandObject()
        rtc.name = revision.model.submissionId
        model.publication = null
        rtc.format = new ModelFormatTransportCommand()
        rtc.files = new ArrayList<>()
        rtc.description = g.message(code: "net.biomodels.jummp.core.model.show.MessageForPrivateModel")
        list.add(rtc)
    }

    private Map doShowGetCheckConditions(final String PERENNIAL_ID, final RTC revision,
                                         final List<RFTC> repoFiles) {
        boolean showPublishOption = modelDelegateService.canPublish(revision)
        boolean showUnpublishOption = modelDelegateService.canUnpublish(revision)
        boolean canSubmitForPublication = modelDelegateService.canSubmitForPublication(revision)
        boolean canCertify = modelDelegateService.canCertify(revision)
        boolean canUpdate = modelDelegateService.canAddRevision(PERENNIAL_ID)
        boolean canDelete = modelDelegateService.canDelete(PERENNIAL_ID)
        boolean canShare = modelDelegateService.canShare(PERENNIAL_ID)
        boolean hasCuratorRole = userService.isLoggedInUserACurator()
        boolean hasAdminRole = userService.isLoggedInUserAAdmin()
        boolean supportedForConversion = modelConversionService.isSupportedForConversion(revision)

        def currentUser = springSecurityService.currentUser
        boolean canAskReviewerAccount
        if (!currentUser) {
            canAskReviewerAccount = false
        } else {
            canAskReviewerAccount = modelDelegateService.canAskReviewerAccount(revision, hasCuratorRole)
        }
        boolean canSeeCurationTab = modelDelegateService.canSeeCurationTab(revision, hasCuratorRole, currentUser)
        boolean canManageContributors = hasAdminRole || canAskReviewerAccount

        boolean canCreateOmex = false
        if (repoFiles) {
            long totalSize = repoFiles.collect { it.size }.sum() as long
            canCreateOmex = totalSize <= BioModels.MAX_FILE_SIZE // 500MB
        }

        [
            canUpdate               : canUpdate,
            canDelete               : canDelete,
            canShare                : canShare,
            showPublishOption       : showPublishOption,
            showUnpublishOption     : showUnpublishOption,
            canSubmitForPublication : canSubmitForPublication,
            canCertify              : canCertify,
            hasAdminRole            : hasAdminRole,
            hasCuratorRole          : hasCuratorRole,
            supportedForConversion  : supportedForConversion,
            canAddGalaxyLink        : hasCuratorRole || hasAdminRole,
            canAskReviewerAccount   : canAskReviewerAccount,
            canSeeCurationTab       : canSeeCurationTab,
            canCreateOmex           : canCreateOmex,
            canManageContributors   : canManageContributors
        ]
    }

    private Map doShowGetAnnotationsBasedData(final String PERENNIAL_ID, final RTC revision) {
        List<STC> modelLevelAnnotations = metadataDelegateService.getModelLevelAnnotations(revision)
        Map genericAnnotations = metadataDelegateService.fetchGenericAnnotations(modelLevelAnnotations)
        List<String> originalModels = metadataDelegateService.fetchOriginalModels(modelLevelAnnotations)
        List<FlagTransportCommand> flags = modelDelegateService.getFlags(PERENNIAL_ID)
        Map<String, String[]> modellingApproaches = metadataDelegateService.fetchModellingApproaches(revision)
        Set<TagTC> tags = metadataDelegateService.findTagsByModel(revision.model)
        [
            modelLevelAnnotations   : modelLevelAnnotations,
            genericAnnotations      : genericAnnotations,
            originalModels          : originalModels,
            flags                   : flags,
            modellingApproaches     : modellingApproaches,
            bmTags                  : tags,
        ]
    }

    private Map doShowGetInitialValues(final String PERENNIAL_ID, final RTC revFromParams,
                                       final RTC lastRev, final List<RFTC> repoFiles) {
        String vcsId = modelDelegateService.getVcsIdentifier(PERENNIAL_ID)
        String modelParentFolder = vcsId ? vcsId.take(3) : ""
        String flashMessage = flash.now["giveMessage"] ?: ""
        List<RTC> revs = modelDelegateService.getAllRevisions(PERENNIAL_ID)
        List<RFTC> convertedFilesTC = null //modelConversionService.getConvertedFiles(revFromParams)
        def contributors = modelDelegateService.convertContributors(lastRev.contributors)

        Map model = [
            perennialId            : PERENNIAL_ID,
            revision               : revFromParams,
            authors                : revFromParams.model.creators,
            contributors           : contributors,
            allRevs                : revs,
            flashMessage           : flashMessage,
            repoFiles              : repoFiles,
            modelParentFolder      : modelParentFolder,
            convertedFilesTC       : convertedFilesTC,
        ]

        model
    }

    private Map doShowGetCurationData(final RTC revision) {
        String curationState = revision.curationState.name()
        List<String> possibleCurationStates = CurationState.values()*.name()
        CNTC curationNotes = metadataDelegateService.fetchCurationNotes(revision)
        [
            shouldDisplayDisclaimer : modelDelegateService.shouldDisplayDisclaimer(revision),
            curationState           : curationState,
            curationNotes           : curationNotes,
            possibleCurationStates  : possibleCurationStates,
            validationLevel         : revision.getValidationLevelMessage(),
            certComment             : revision.getCertificationMessage(),
        ]
    }

    private Map doShowGetExternalLinkedData(final String PERENNIAL_ID, final RTC revision,
                                            final List<RFTC> repoFiles) {
        List<String> reactomeIds = metadataDelegateService.getPathwaysForModelId(PERENNIAL_ID)
        String reactomeUrl = ReactomeEnvironment.getUrlForThisEnvironment()
        String hrefLinkToNewtEditor = makeLinkToNewtEditor(revision, repoFiles)
        [
            reactomeIds             : reactomeIds,
            reactomeUrl             : reactomeUrl,
            hrefLinkToNewtEditor    : hrefLinkToNewtEditor,
            hasRosetteLink          : modelDelegateService.retrieveRosetteLink(PERENNIAL_ID),
            hasGalaxyLink           : modelDelegateService.retrieveGalaxyLink(PERENNIAL_ID),
            hasMenelmacarLink       : modelDelegateService.retrieveMenelmacarLink(PERENNIAL_ID)
        ]
    }

    private void doShowRenderLatestRevision(final Map model, final RTC revision, final String PERENNIAL_ID) {
        flash.genericModel = model
        ModelFormatTransportCommand format = revision.format
        String formatController = modelFileFormatService.getPluginForFormat(format)
        if (formatController) {
            forward controller: formatController, action: "show", id: PERENNIAL_ID
        } else {
            final String fmtId = format.identifier
            LOGGER.error("Could not find any controller for format $fmtId of $PERENNIAL_ID.")
            forward(controller: "errors", action: "error400")
        }
    }

    private Map doShowInferSpecificTabs(final String PERENNIAL_ID, final RTC rev) {
        ModelFormatTransportCommand format = rev.format
        String formatController = modelFileFormatService.getPluginForFormat(format)
        if (formatController) {
            Map specificTabs = modelFileFormatService.getContentsOfSpecificTabs(rev)
            return [specificTabs: specificTabs]
        }
        [:]
    }

    private static void doShowPrepareOldRevision(Map model) {
        model["canUpdate"] = false
        model["showPublishOption"] = false
        model["showUnpublishOption"] = false
        model["oldVersion"] = true
        model["canDelete"] = false
        model["canShare"] = false
        model["canCertify"] = false
    }

    private void doShowRenderWithFormat(final RTC rev, final boolean isPrivateModel,
                                        final String format) {

        if (!rev) {
            respond net.biomodels.jummp.webapp.rest.errors.Error("Invalid Id",
                "An invalid model id was specified")
        } else {
            RestfulModel model = new RestfulModel(rev, isPrivateModel)
            String contentType = format.toLowerCase() == "json" ? "application/json" : "application/xml"
            String output = model.outputModelAsString(contentType)
            render(text: output, contentType: contentType)
        }
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def formats() {
        Map mapResult = modelFileFormatService.getAllFormats()
        handleRestApi(mapResult)
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def files() {
        // PageFragmentCachingFilter throws a NPE for unsupported format parameter values
        if (!(response.format in ['json', 'xml'])) {
            render view: '/errors/error415', status: 415
            return
        }
        try {
            def revision = modelDelegateService.getRevisionFromParams(params.id as String, params.revisionId as String)
            def revisionFiles = revision.files
            def responseFiles = revisionFiles.findAll { !it.hidden }
            def modelFiles = new ModelFiles(responseFiles)
            withFormat {
                json { respond modelFiles }
                xml { respond modelFiles }
                '*' { render status: 415, view: "/errors/error415" }
            }
        } catch (Exception err) {
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
            Map resultMap = modelDelegateService.getRevisionsState(params.id as String)
            withFormat {
                json { render resultMap as JSON }
                xml { render resultMap as XML }
                '*' { render status: 415, view: "/errors/error415" }
            }
        } catch (Exception err) {
            LOGGER.error err.message, err
            forward controller: 'errors', action: 'error404'
        }
    }

    @Secured(['IS_AUTHENTICATED_FULLY'])
    def retrieveRevisionsState() {
        revisionsState()
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def identifiers() {
        if (!(response.format in ['json', 'xml'])) {
            render view: '/errors/error415', status: 415
            return
        }
        try {
            List<String> listAllIdentifiers
            if (params.containsKey("isprivate")) {
                Boolean isPrivate = params.containsKey("isprivate") ? params.getBoolean("isprivate") : false
                listAllIdentifiers = modelDelegateService.getPublicOrPrivateIdentifiers(isPrivate)
            } else if (params.containsKey("autogen")) {
                Boolean isAutogenerated = params.containsKey("autogen") ? params.getBoolean("autogen") : false
                listAllIdentifiers = modelDelegateService.getAllModelIdentifiers(isAutogenerated)
            } else {
                listAllIdentifiers = modelDelegateService.getAllModelIdentifiers()
            }
            Map models = ["hits": listAllIdentifiers?.size() ?: 0, "models": listAllIdentifiers]
            withFormat {
                json { render models as JSON }
                xml { render models as XML }
                '*' { render status: 415, view: "/errors/error415" }
            }
        } catch (Exception err) {
            println(err.printStackTrace())
            LOGGER.error(err.message, err)
            forward controller: 'errors', action: 'error404'
        }
    }
    // TODO: merge with createCombineArchive above? should we keep both?
    // Using this action to generate OMEX files for the older versions to submit to BioStudies
    @Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
    def generateOmex() {
        // if the params.metadata is unavailable, it means false.
        boolean metadata = params.getBoolean("metadata")
        boolean noLargeFiles = params.get("nolargefiles") ? params.getBoolean("nolargefiles") : false
        try {
            Map models = doGenerateOmex(params.id as String, params.revisionId as Integer, metadata, noLargeFiles)
            handleRestApi(models)
        } catch (Exception err) {
            LOGGER.error(err.message, err)
            forward controller: 'errors', action: 'error404'
        }
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def metadatardf() {
        String result = doGenerateOmexMetadataRDF(params.id as String, params.revisionId as Integer)
        render(contentType: 'application/xml', text: result)
    }

    @Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
    def loadAllAnnotations() {
        Boolean fromRedis = true
        if (params.containsKey("fromRedis")) {
            fromRedis = params.getBoolean("fromRedis")
        }
        RTC revision = modelDelegateService.getRevisionFromParams(params.id as String, params.revisionId as String)
        Map mapResult
        if (fromRedis) {
            mapResult = metadataDelegateService.getAnnotationsAndOrganismFromRedis(revision.model.submissionId)
        } else {
            mapResult = metadataDelegateService.stringifyAnnotations(revision)
        }
        handleRestApi(mapResult)
    }

    @Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
    def cacheAnnotationsAndOrganismOnRedis() {
        RTC revision = modelDelegateService.getRevisionFromParams(params.id as String, params.revisionId as String)
        Map mapResult = metadataDelegateService.cacheAnnotationsAndOrganismOnRedis(revision)
        handleRestApi(mapResult)
    }

    @Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
    def retrieveModelLevelMetadata() {
        RTC revision = modelDelegateService.getRevisionFromParams(params.id as String, params.revisionId as String)
        List<STC> modelLevelAnnotations = metadataDelegateService.getModelLevelAnnotations(revision)
        Map genericAnnotations = metadataDelegateService.fetchGenericAnnotations(modelLevelAnnotations)
        Map mapResult = [:] as HashMap
        mapResult.putAll(genericAnnotations)
        handleRestApi(mapResult)
    }

    @Secured(['ROLE_ADMIN', 'ROLE_CURATOR'])
    def publish() {
        RTC rev = null
        RTC published = null
        try {
            rev = modelDelegateService.getRevisionFromParams(params.id as String, params.revisionId as String)
            published = modelDelegateService.publishModelRevision(rev)
            def currentUser = springSecurityService.currentUser
            if (currentUser) {
                def notification = [
                    revision: rev,
                    user    : currentUser,
                    perms   : modelDelegateService.getPermissionsMap(rev.model.submissionId)]
                sendMessage("seda:model.publish", notification)
            }
            boolean havePublicationId = published.model.publicationId != null
            String extraMsg = havePublicationId ?
                " with the publication identifier ${published.modelIdentifier()}." : "."
            redirect(action: "showWithMessage", id: published.identifier(),
                params: [flashMessage: "Model has been published${extraMsg}"])

            // doCopyFilesToEBIFTP(published)

            // fire a published event up so that listeners/subscribers can see it
            ModelOperationEvent event = new ModelPublishedEvent("${userService.username};published", published.model)
            applicationContext.publishEvent(event)
        } catch (AccessDeniedException e) {
            LOGGER.error(e.message, e)
            forward(controller: "errors", action: "error403")
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.message)
            redirect(action: "showWithMessage",
                id: rev.identifier(),
                params: [flashMessage: """Model has not been published because due to an internal problem. \
Please contact the developers team for support!"""])
        } catch (Exception e) {
            LOGGER.error("General exception thrown while publishing ${rev?.identifier()} (${published?.identifier()})", e)
            redirect(action: "showWithMessage", id: rev?.identifier(),
                params: [flashMessage: """An internal error prevented this model from being published. \
Please contact the developers team for support!"""])
        }
    }

    @Secured(['ROLE_ADMIN'])
    def unpublish() {
        LOGGER.info("Unpublishing ${params.id}.${params.revisionId}...")
        RTC rev = null
        try {
            rev = modelDelegateService.getRevisionFromParams(params.id as String, params.revisionId as String)
            modelDelegateService.unpublishModelRevision(rev)
            redirect(action: "showWithMessage",
                params: [id: "${params.id}.${params.revisionId}", flashMessage: "Model has been moved back to the private zone!"])

            // fire a published event up so that listeners/subscribers can see it
            ModelPublishedEvent event = new ModelPublishedEvent("${userService.username};unpublished", rev.model)
            grailsApplication.mainContext.publishEvent(event)
        } catch (AccessDeniedException e) {
            LOGGER.error(e.message, e)
            forward(controller: "errors", action: "error403")
        } catch (IllegalArgumentException e) {
            LOGGER.error(e.message)
            redirect(action: "showWithMessage",
                id: rev.identifier(),
                params: [flashMessage: """Model has not been unpublished due to a problem. \
Please contact the developers team for support!"""])
        } catch (Exception e) {
            LOGGER.error("General exception thrown while unpublishing ${rev?.identifier()} (${rev?.identifier()})", e)
            redirect(action: "showWithMessage", id: rev?.identifier(),
                params: [flashMessage: """An internal error prevented this model from being unpublished. \
Please contact the developers team for support!"""])
        }
    }

    def submitForPublication() {
        def rev = modelDelegateService.getRevisionFromParams(params.id as String)
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
                id: modelDelegateService.getRevisionFromParams(params.id as String).identifier(),
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
        RTC revisionTC = initials.get("RevisionTC") as RTC
        modelInfo.put("detectedName", isUpdate ? revisionTC?.name : "")
        modelInfo.put("detectedDescription", isUpdate ? revisionTC?.description : "")

        Map detectedModelFormat = [:]
        detectedModelFormat.put("id", revisionTC?.format?.id?.toString() ?: "")
        detectedModelFormat.put("name", revisionTC?.format?.name ?: "")
        detectedModelFormat.put("readme", revisionTC?.readmeSubmission ?: "")
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
        RTC revisionTC = initials.get("RevisionTC") as RTC
        Map submissionDataMap = ["latestModelDescription": revisionTC.description ?: ""]
        redisService.doRedisHSet(submissionFolder, submissionDataMap)

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

    def doAddOrRemoveGalaxyLink() {
        String modelId = params.get("modelId")
        String flag = params.get("flag")

        if (flag == "Yes") {
            String href = "https://usegalaxy.eu/root?tool_id=biomodels_${modelId?.toLowerCase()}"
            render(template: "/templates/biomodels/modelDisplay/linkGalaxyEU_RenderLink",
                model: [
                    externalLink        : href,
                    linkTitle           : "Click here to run the model in the European Galaxy server",
                    externalResourceIcon: "${serverURL}/images/biomodels/galaxy-eu-logo.png",
                    shortDescription    : "Run the model in the European Galaxy server",
                    canRemoveGalaxyLink : true
                ] as Map)
        } else {
            render(template: "/templates/biomodels/modelDisplay/linkGalaxyEU_AddButton")
        }
        modelDelegateService.doAddOrRemoveGalaxyLink(modelId, flag)
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def terms() {
        [serverURL: grailsApplication.config.grails.serverURL]
    }

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def createCombineArchive() {
        String modelId = params.id
        if (!modelId) {
            forward(controller: "errors", action: "error500")
            return
        }
        Integer revisionId = params.getInt("revisionId")
        Map m = doGenerateOmex(modelId, revisionId)

        withFormat {
            html {
                render(model: m)
            }
            json {
                render m as JSON
            }
            xml {
                render m as XML
            }
            '*' {
                render(controller: "errors", action: "error404", view: '/errors/error404', status: 404)
            }
        }
    }

    private Map doGenerateOmex(String modelId, Integer revisionNumber,
                               final boolean biomodelsMetadataAdded = false,
                               final boolean noLargeFiles = false) {
        RTC revisionTC
        String filePath = ""
        try {
            revisionTC = modelDelegateService.getRevisionFromParams(modelId, revisionNumber as String)
            modelId = revisionTC.model.submissionId
            revisionNumber = revisionTC.revisionNumber
            final List<RFTC> FILES = modelDelegateService.retrieveModelFiles(revisionTC)
            String parentDir = modelDelegateService.getVcsIdentifier(modelId)?.take(3)
            String modelExportsDir = grailsApplication.config.jummp.model.exportFolder
            final String MODEL_CACHE = grailsApplication.config.jummp.model.cache.dir
            JSONArray array = modelDelegateService.buildJsonArray(deployTarget,
                parentDir, modelId, revisionNumber, FILES, modelExportsDir, MODEL_CACHE, revisionTC.state.name())

            final String DEFAULT_FS_SVR = "http://localhost:8090/biomodels/services/file-format/api/v1.0"
            final String FS_SVR_URL = System.getenv().getOrDefault("FS_SVR_URL", DEFAULT_FS_SVR)
            String userParams = "metadata=${biomodelsMetadataAdded.toString()}"
            userParams += "&nolargefiles=${noLargeFiles}"
            final String serviceURI = "$FS_SVR_URL/create-omex?${userParams}"
            filePath = WSF.executePostRequest(serviceURI, array.toString())
        } catch (ModelException ignored) {
            ignored.printStackTrace()
        } finally {
            LOGGER.info("File Path of the omex file: $filePath")
        }
        // asynchronous jobs have to be called here
        // TODO: email or notify the requester the location of the OMEX file so that they can download it later.
        // TODO: send the map of parameters below to the server to copy this file to FTP public (for the public ones)
        // and the location that will be expired within 1 hour (for the private ones)
        [modelId: modelId, revisionNumber: revisionNumber, location: filePath]
    }

    private String doGenerateOmexMetadataRDF(final String modelId, final Integer revisionId) {
        final String DEFAULT_FS_SVR = "http://localhost:8090/biomodels/services/file-format/api/v1.0"
        final String FS_SVR_URL = System.getenv().getOrDefault("FS_SVR_URL", DEFAULT_FS_SVR)
        String identifier = modelId + (revisionId != null ? ".${revisionId}" : "")
        String rdfContent = "Cannot generate metadata.rdf file for this model revision $identifier"
        try {
            JSONObject object = new JSONObject()
            object.put("revisionId", identifier)
            JSONArray array = new JSONArray()
            array.put(object)
            String uri = "${FS_SVR_URL}/create-omex-metadata-rdf/${identifier}"
            rdfContent = WSF.executePostRequest(uri, array.toString())
        } catch (Exception ignored) {
            // handle exception here
            ignored.printStackTrace()
        } finally {
        }
        return rdfContent
    }

    def delete() {
        try {
            boolean deleted = modelDelegateService.deleteModel(params.id as String)
            def currentUser = springSecurityService.currentUser
            if (currentUser) {
                def notification = [
                    model: modelDelegateService.getModel(params.id as String),
                    user : currentUser,
                    perms: modelDelegateService.getPermissionsMap(params.id as String)]
                sendMessage("seda:model.delete", notification)
            }
            redirect(action: "showWithMessage", id: params.id,
                params: [flashMessage: deleted ?
                    "Model has been deleted, and moved into archives." :
                    "Model could not be deleted"])
        } catch (Exception e) {
            LOGGER.error e.message, e
            forward(controller: "errors", action: "error403")
        }
    }

    // uses revision id and filename
    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def getFileDetails() {
        try {
            final RTC REVISION =
                modelDelegateService.getRevisionFromParams(params.id as String, params.revisionId as String)
            def retval = modelDelegateService.getFileDetails(REVISION.id, params.filename as String)
            if (IS_DEBUG_ENABLED) {
                LOGGER.debug("Permissions for ${REVISION.identifier()}: ${retval as JSON}")
            }
            render retval as JSON
        } catch (Exception e) {
            LOGGER.error e.message, e
            return "INVALID ID"
        }
    }

    def share() {
        try {
            def rev = modelDelegateService.getRevisionFromParams(params.id as String)
            def perms = modelDelegateService.getPermissionsMap(rev.model.submissionId)
            def teams = getTeamsForCurrentUser()
            return [revision: rev, permissions: perms as JSON, teams: teams]
        } catch (Exception error) {
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
                modelDelegateService.setPermissions(params.id as String, collabsNew)
                JSON result = ['success': true, 'permissions':
                    modelDelegateService.getPermissionsMap(params.id as String)]
                render result
            } catch (Exception e) {
                LOGGER.error e.message, e
                valid = false
            }
        }
        if (!valid) {
            render(['success': false, 'message': "Could not update permissions"] as JSON)
        }
    }

    private void serveModelAsCombineArchive(RTC revision, List<RFTC> files, def resp) {
        if (revision.state == ModelState.PUBLISHED) {
            LOGGER.info "${revision.modelIdentifier()}: download omex => use case 2 and 4: public - regardless of its size"
            serveModelAsCombineArchiveForPublished(revision)
        } else {
            LOGGER.info "${revision.modelIdentifier()}: download omex => use case 1 and 3: private - considering its size to serve instantly or later"
            serveModelAsCombineArchiveForPrivate(revision, files, resp)
        }
    }

    private void serveModelAsCombineArchiveForPrivate(RTC revision, List<RFTC> files, resp) {
        // Revision is private, then considering the size of the request
        long totalSize = files.collect { it.size }.sum() as long
        boolean isLargeSubmission = totalSize >= BioModels.MAX_FILE_SIZE
        if (isLargeSubmission) {
            // Use case 1: large and private
            LOGGER.info "${revision.modelIdentifier()}: download omex => use case 1: large and private"
            String[] parts = defineMrPathAndFileNameForOmex(revision)
            String filePath = parts[1]
            // the OMEX file could be created using the external FileService
            serveModelAsCombineArchiveWithCheckingAndFileService(revision, filePath)
        } else {
            // Use case 3: small and private, then generate/create CombineArchive on the spot
            LOGGER.info "${revision.modelIdentifier()}: download omex => use case 3: small and private"
            serveModelAsCombineArchiveInstantly(revision, files, resp)
        }
    }

    private void serveModelAsCombineArchiveForPublished(RTC revision) {
        String[] parts = defineMrPathAndFileNameForOmex(revision)
        String filePath = parts[1]
        // Use case 2 and 4: Revision is public regardless of its size
        if (deployTarget == "local") {
            serveModelAsCombineArchiveWithCheckingAndFileService(revision, filePath)
        } else {
            String modelParentFolder = modelDelegateService.getRevisionsState(revision.modelIdentifier()).vcsId
            String url = "${EBI_BM_FTP_REPO}/${modelParentFolder}/$filePath"
            if (JummpHttpService.isReachable(url)) {
                LOGGER.info("Downloading COMBINE Archive (OMEX) file from EBI FTP: ${url}")
                redirect(url: url)
            } else {
                // fallback
                serveModelAsCombineArchiveWithCheckingAndFileService(revision, filePath)
            }
        }
    }

    private void serveModelAsCombineArchiveInstantly(RTC revision, List<RFTC> files, def resp) {
        long time = System.nanoTime()
        final String modelId = revision.model.submissionId
        final Integer revisionId = revision.revisionNumber
        String omexFileName = omexService.createCombineArchive(files, modelId, revisionId, false)
        File omexFile = new File(omexFileName)
        String name = omexFile.name
        resp.setContentType("application/zip")
        resp.setHeader("Content-disposition", "attachment;filename=\"${name}\"")
        ByteArrayInputStream stream = null
        try {
            stream = new ByteArrayInputStream(omexFile.readBytes())
            resp.outputStream << stream
        } catch (IOException ioE) {
            LOGGER.error("The client might have aborted their download request.", ioE)
        } finally {
            LOGGER.info("Downloading COMBINEArchive instantly: $omexFile")
            if (omexFile.delete()) {
                LOGGER.info("The temporary file was deleted successfully.")
            } else {
                LOGGER.info("Cannot delete the temporary file.")
            }
            if (stream) {
                stream.close()
            }
        }
        time = (long) ((System.nanoTime() - time) / 1_000_000.0)
        long seconds = (long) (time / 1_000)
        long HH = (long) (seconds / 3600)
        long MM = (long) ((seconds % 3600) / 60)
        long SS = seconds % 60
        String timeInHHMMSS = String.format("%02d:%02d:%02d", HH, MM, SS)
        LOGGER.info("It took ${time}ms ~ ${timeInHHMMSS} to generate the OMEX file $omexFileName.")
    }

    private void serveModelAsCombineArchiveWithCheckingAndFileService(final RTC revision, final String filePath) {
        final String MODEL_CACHE = grailsApplication.config.jummp.model.cache.dir
        File omexFile = new File(MODEL_CACHE, filePath)
        String DOWNLOAD_SERVICE_URL = grailsApplication.config.jummp.model.download.server
        String url = "${DOWNLOAD_SERVICE_URL}/get-files/$filePath"
        if (!omexFile.exists()) {
            Map result = doGenerateOmex(revision.model.submissionId, revision.revisionNumber) as Map
            String omexLocation = result.get("location")
            if (omexLocation && revision.state != ModelState.PUBLISHED) {
                String msg = """Your file might be big. It is being generated. Please be patient and check the download \
link <a href='${url}' target='_blank'>${url}</a> after a few seconds. If you have any trouble in downloading the file after \
about a quarter of an hour, please feel free to <a href='mailto:${grailsApplication.config.jummp.model.curators.mailinglist}'>contact us</a>.\
<br/><br/>Thank you for your understanding!"""
                LOGGER.info(msg)
                render(view: "download/inform", model: [message: msg])
            } else if (omexLocation && revision.state == ModelState.PUBLISHED) {
                String modelParentFolder = modelDelegateService.getRevisionsState(revision.modelIdentifier()).vcsId
                String ftpDownloadUrl = "${EBI_BM_FTP_REPO}/${modelParentFolder}/$filePath"
                String msg = """Your file might be large and is being generated. It will be available shortly on \
the public FTP at <a href='${ftpDownloadUrl}' target='_blank'>${ftpDownloadUrl}</a>. Please be patient and check it \
after a few seconds. If you have any trouble in downloading the file after about a quarter of an hour, please feel free to \
<a href='mailto:${grailsApplication.config.jummp.model.curators.mailinglist}'>contact us</a>.\
<br/><br/>Thank you for your understanding!"""
                LOGGER.info(msg)
                render(view: "download/inform", model: [message: msg])
            } else {
                forward(controller: "errors", action: "error413")
            }
        } else {
            LOGGER.info("Downloading COMBINE Archive (OMEX) file from the model cache directory: ${filePath}")
            redirect(url: url)
        }
    }

    private void serveModelAsFile(RTC revision, RFTC rf, String fileName,
                                  def resp, boolean inline, boolean preview = false) {
        if (preview) {
            serveModelAsFileInstantly(rf, resp, inline, preview)
            return
        }

        if (revision.state == ModelState.PUBLISHED) {
            // Use case 2 and 4: Revision is public regardless of its size
            println "use case 2 and 4: public - regardless of its size"
            serveModelAsFileForPublished(revision, rf, fileName, resp, inline, preview)
        } else {
            // Use case 1 and 3: Revision is private and large/small
            println "use case 1 and 3: private - considering its size to serve instantly or later"
            serveModelAsFileForPrivate(rf, resp, inline, preview)
        }
    }

    private void serveModelAsFileForPublished(RTC revision, RFTC rf, String fileName, def resp,
                                              boolean inline, boolean preview = false) {
        String modelParentFolder = modelDelegateService.getRevisionsState(revision.modelIdentifier()).vcsId
        String[] parts = defineMrPathAndFileNameForOmex(revision)
        String mrPath = parts[2]
        String url = "${EBI_BM_FTP_REPO}/${modelParentFolder}/${mrPath}/${fileName}"
        if (JummpHttpService.isReachable(url)) {
            LOGGER.info("Downloading from FTP: ${url}")
            redirect(url: url)
        } else {
            // fallback
            serveModelAsFileInstantly(rf, resp, inline, preview)
        }
    }

    private void serveModelAsFileForPrivate(RFTC rf, def resp, boolean inline, boolean preview = false) {
        boolean isLargeFile = rf.size >= BioModels.MAX_FILE_SIZE
        if (isLargeFile) {
            // send the FTP location of the requested file and expire it after an hour
            // using downloader server to serve the file
            LOGGER.info("We will implement this feature soon")
            forward(controller: "errors", action: "error413")
        } else {
            serveModelAsFileInstantly(rf, resp, inline, preview)
        }
    }

    private void serveModelAsFileInstantly(RFTC rf, def resp, boolean inline, boolean preview = false) {
        File file = new File(rf.path)
        resp.setContentType(rf.mimeType)
        final String INLINE = inline ? "inline" : "attachment"
        final String F_NAME = URLEncoder.encode(file.name, "UTF-8")
        resp.setCharacterEncoding("UTF-8")
        resp.setHeader("Content-Disposition", "${INLINE};filename=\"${F_NAME}\"")
        byte[] fileData = null
        int previewSize = grailsApplication.config.jummp.web.file.preview as Integer
        ByteArrayInputStream stream = null
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
            LOGGER.info("Downloading file instantly: ${file?.absolutePath}")
            if (stream != null) {
                LOGGER.debug("InputStream of the file ${file.name} has been flushed and closed.")
                stream.close()
            }
            Arrays.fill(fileData, (byte) 0)
        }
    }

    /**
     * After OMEX created, it should be copied to EBI FTP for serving
     * @param source
     * @param target
     * @return
     */
    private static Promise doCopyOmexFileToFTP(final File source, final File target) {
        Promise p = task {
            LOGGER.info("Moved the OMEX file: ${source}")
            println("Moved the OMEX file: ${source}")
            Files.copy(Paths.get(source.absolutePath),
                Paths.get(target.absolutePath), StandardCopyOption.REPLACE_EXISTING)

            //Thread.sleep 5000
        }
        p.onError { Throwable err ->
            String msg = "When moving ${source.name} to ${target.name},  an error occured ${err.message}"
            LOGGER.debug(msg)
            println msg
        }
        p.onComplete { res ->
            println "Promise returned $res"
        }
        // block until the result is called to prevent async execution error
        p.get()
        return p
    }

    private static String[] defineMrPathAndFileNameForOmex(final RTC revision) {
        final String modelId = revision.model.submissionId
        final String revisionId = revision.revisionNumber.toString()
        String omexFileName = "${modelId}.${revisionId}.omex"
        // Model Revision File Path (mrFilePath): the path is made up model submission and revision number
        String mrPath = "${modelId}${File.separator}${revisionId}"
        String omexMrFilePath = "${modelId}${File.separator}${revisionId}${File.separator}${omexFileName}"
        [omexFileName, omexMrFilePath, mrPath].toArray()
    }

    /**
     * File download of the model file for a model by id
     */
    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def download() {
        try {
            if (params.containsKey("id")) {
                String modelId = params.id
                String revisionId = params.revisionId
                String fileName = params.filename.decodeHTML()
                if (Environment.isWarDeployed() && fileName != null) {
                    fileName = handleDlSpecialCharacters(fileName)
                }
                RTC revision = modelDelegateService.getRevisionFromParams(modelId, revisionId)
                final List<RFTC> FILES = modelDelegateService.retrieveModelFiles(revision)
                if (!fileName) {
                    serveModelAsCombineArchive(revision, FILES, response)
                } else {
                    RFTC requested = FILES.find {
                        !it.hidden && new File(it.path).getName() == fileName
                    } as RFTC
                    boolean inline = params.getBoolean("inline", false)
                    boolean preview = params.getBoolean("preview", false)
                    if (requested.id != null) {
                        serveModelAsFile(revision, requested, fileName, response, inline, preview)
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
        } catch (Exception e) {
            String errDesc = handleDlException(e)
            render(status: 400, view: "/errors/error400", model: [code: 400, errorDescription: errDesc])
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
            RTC revisionTC = modelDelegateService.updateCurationStateRevision(modelId, revision, curationState)
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
    @Transactional
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
                approaches << [id: id, name: name, resource: resource, value: id, label: label, accession: accession]
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
        RTC rev = modelDelegateService.getRevisionFromParams(params.id as String)
        [
            publication: modelDelegateService.getPublication(params.id as String),
            revision   : rev,
            notes      : sbmlService.getNotes(rev),
            annotations: sbmlService.getAnnotations(rev)
        ]
    }

    def overview = {
        RTC rev = modelDelegateService.getRevisionFromParams(params.id as String)
        [
            reactions   : sbmlService.getReactions(rev),
            rules       : sbmlService.getRules(rev),
            parameters  : sbmlService.getParameters(rev),
            compartments: sbmlService.getCompartments(rev)
        ]
    }

    /**
     * Renders html snippet with Publication information for the current Model identified by the id.
     */
    def publication = {
        PublicationTransportCommand publication = modelDelegateService.getPublication(params.id as String)
        [publication: publication]
    }

    def notes = {
        RTC rev = modelDelegateService.getRevisionFromParams(params.id as String)
        [notes: sbmlService.getNotes(rev)]
    }

    /**
     * Retrieve annotations and hand them over to the view
     */
    def annotations = {
        RTC rev = modelDelegateService.getRevisionFromParams(params.id as String)
        [annotations: sbmlService.getAnnotations(rev)]
    }

    /**
     * File download of the model file for a model by id
     */
    def downloadModelRevision = {
        RTC rev = modelDelegateService.getRevisionFromParams(params.id as String)
        byte[] bytes = modelDelegateService.retrieveModelFiles(rev) as byte[]
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
        String errDesc
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

    private String makeLinkToNewtEditor(final RTC revision, final List<RFTC> repoFiles) {
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

    @Secured(['IS_AUTHENTICATED_ANONYMOUSLY'])
    def about() {
        Map map = ["description": "Model Controller"]
        withFormat {
            json { render map as JSON }
            xml { render map as XML }
            '*' { render status: 415, view: "/errors/error415" }
        }
    }

    @Secured(['IS_AUTHENTICATED_FULLY'])
    def create() {
        Map map = ["description": "Create a new model"]
        withFormat {
            json { render map as JSON }
            xml { render map as XML }
            '*' { render status: 415, view: "/errors/error415" }
        }
    }

    /**
     * When a model revision is made publicly accessible, its files should be copied to EBI's FTP for serving
     * @param revision {@link Revision}
     */
    private void doCopyFilesToEBIFTP(RTC revision) {
        if (deployTarget == "local") {
            LOGGER.info("No need to copy model files to EBI FTP because this is a local server.")
            return
        }
        LOGGER.info("Publishing (i.e., copying) model files to EBI FTP.")
        final String SVC_URL = "http://ebi-mol-sys-dev.ebi.ac.uk:8000/model/publish"
        List info = modelDelegateService.getRevisionsState(revision.model.submissionId) as List
        final String PARENT = info["vcsId"] as String
        final String SUB_ID = revision.model.submissionId
        final int REV_NUM = revision.revisionNumber
        final String DEP = deployTarget
        Promise p = task {
            String URL = "${SVC_URL}/${DEP}/${PARENT}/${SUB_ID}/${REV_NUM}"
            URL url = new URL(URL)
            LOGGER.info("Copying job info: " + url.text)
        }
        p.onError { Throwable err ->
            String msg = "Error when copying the files of ${revision.identifier()} to EBI's FTP."
            LOGGER.debug(msg)
            println msg
        }
        p.onComplete { res ->
            println "Promise returned $res"
        }
        // block until the result is called to prevent async execution error
        p.get()
    }
}
