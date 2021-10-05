/**
 * Copyright (C) 2010-2021 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 */


import grails.plugin.springsecurity.SpringSecurityUtils
import grails.plugin.springsecurity.acl.AclUtilService
import groovy.io.FileType
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.biomodels.jummp.core.JummpException
import net.biomodels.jummp.core.ModelException
import net.biomodels.jummp.core.model.*
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.model.*
import net.biomodels.jummp.core.*
import net.biomodels.jummp.plugins.security.User
import net.biomodels.jummp.utils.ModelSubmissionHelper as MSH
import net.biomodels.jummp.utils.RunScriptHelper
import net.biomodels.jummp.utils.redis.Operations
import org.apache.camel.CamelContext
import org.springframework.orm.hibernate4.SessionHolder
import org.springframework.security.acls.domain.BasePermission
import org.springframework.security.core.Authentication
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.regex.Pattern

/**
 * Simple holder for variables defined in this script that are accessed by different methods.
 */
@CompileStatic
class CuratedUpdateSupport {
    static final String MODEL_ID_LIKE_QUERY_PATTERN = "BIOMD000000%"

    static final String adminUsername = System.getenv("ADMIN_USER")
    // the current session
    static final SessionHolder session = null
    static final String constraintBasedModel = "http://identifiers.org/mamo/MAMO_0000009"
    static final ModellingApproach modellingApproach = lookupModellingApproach()

    /**
     * Returns the User account owning the models to be updated.
     *
     * All models were submitted by the same account, and we guard this assumption with a runtime (fatal) exception.
     *
     * @return the User account that should be used to update these models.
     */
    @CompileDynamic
    static User getSubmitterAccount(String publicationId) {
        // we deliberately call get() because we expect a single result; getting > 1 results would be an error which
        // would be propagated upstream to the callees of this method. 
        // The get() method doesn't sure to return a single result. However, we pretty sure we only need the submitter's
        // username of the latest revision. So, ordering the list of Revision objects then withrawing the first element 
        // ensure to have a single user and makes sense of getting the newest submitter's info.
        def owners = Revision.createCriteria().list {
            projections {
                distinct "owner"
            }
            model {
                like "publicationId", publicationId
            }
            order("revisionNumber", "desc")
        }
        owners?.first()
    }

    @CompileDynamic
    private static ModellingApproach lookupModellingApproach() {
        ModellingApproach.findByResource(constraintBasedModel)
    }
}

/**
 * Model-centric log holder.
 *
 * Printing to System.out in a multi-threaded environment introduces unnecessary blocking and also
 * makes the order in which the statements are printed unpredictable.
 *
 * This class addresses both issues: all log messages pertaining to a particular model are queued, so
 * the order is preserved. Also, because we process each model in a dedicated thread, there's no need
 * to block.
 * See also the printModelLog() method.
 */
@CompileStatic
class ModelLogger {
    /**
     * Log for model-related messages.
     *
     * Keys represent model identifiers. Values represent pairs of ordered sets of messages
     * corresponding to the error log and the info log respectively.
     *
     * Since all messages relating to a model will be inserted by the same thread, there is no
     * need to use locks.
     */
    static ConcurrentHashMap<String, ModelLogger> messageLog = new ConcurrentHashMap<>()
    /**
     * The error log for this model
     */
    Set err
    /**
     * The message log for this model
     */
    Set out

    void logMsg(def msg) {
        out << msg
    }

    void errMsg(def msg) {
        err << msg
    }

    String toString() {
        "out: $out, err: $err"
    }
}

/**
 * Submits multiple models concurrently to test model identifier generators
 * in the context of multiple instance deployment
 *
 * @author <a href="mailto:nvntung@gmail.com">Tung Nguyen</a> on 04/06/20.
 */
class BatchSubmissionMainClass {
    /**
     * Expected contents of a typical folder for literature-based models
     */
    def expectedFiles = [
        "[A-Z0-9]*_urn\\.xml": "Auto-generated SBML file with URNs",
        "[A-Z0-9]*-biopax2\\.owl": "Auto-generated BioPAX (Level 2)",
        "[A-Z0-9]*-biopax3\\.owl": "Auto-generated BioPAX (Level 3)",
        "[A-Z0-9]*\\.cellml": "Auto-generated CellML",
        "[A-Z0-9]*\\.m" : "Auto-generated Octave file",
        "[A-Z0-9]*\\.pdf" : "Auto-generated PDF file",
        "[A-Z0-9]*\\_manual.png" : "Manually generated Reaction graph (PNG)",
        "[A-Z0-9]*\\_manual.svg" : "Manually generated Reaction graph (SVG)",
        "[A-Z0-9]*\\.png" : "Auto-generated Reaction graph (PNG)",
        "[A-Z0-9]*\\.svg" : "Auto-generated Reaction graph (SVG)",
        "[A-Z0-9]*\\.sci" : "Auto-generated Scilab file",
        "[A-Z0-9]*\\.vcml" : "Auto-generated VCML file",
        "[A-Z0-9]*\\.xpp" : "Auto-generated XPP file" ]
    def ctx
    CamelContext camelContext
    MSH mshelper
    static final String adminUsername = "administrator"// System.getenv("ADMIN_USER")
    static final String MODELS_DIR = System.getenv("MODELS_DIR")
    // auth token for admin account; used by worker threads to publish models
    static final Authentication adminAuth = RunScriptHelper.createTokenForUser(adminUsername)

    static final AtomicInteger processedCount = new AtomicInteger()
    static final AtomicInteger failureCount = new AtomicInteger()

    // initiate the map of model main files from the database
    Map<String, RFTC> modelMainFileMap = new LinkedHashMap<String, RFTC>()
    // initiate the map of the file name descriptions from the database
    Map<String, List> fileNameDescriptionMap = new LinkedHashMap<String, List<RFTC>>()
    // the set of the main files renamed due to containing special characters or having typos
    final Map<String, String> MAIN_FILES_RENAMED = [
        "BIOMD0000000923": "Lio2012_Modelling osteomyelitis_Control Model.xml",
        "BIOMD0000000928": "Baker2017_Fig14.xml"
    ] as Map

    void init() {
        camelContext = ctx.getBean('camelContext', CamelContext)
        mshelper = new MSH(ctx: ctx, camelContext: camelContext)
    }

    static void addModelMsg(String model, def msg) {
        def logger = getOrCreateModelLogger model
        logger.logMsg msg
    }

    static void addModelError(String model, def msg) {
        failureCount.incrementAndGet()
        def logger = getOrCreateModelLogger model
        logger.errMsg msg
    }

    static ModelLogger getOrCreateModelLogger(String model) {
        ModelLogger.messageLog.putIfAbsent(model,
            new ModelLogger(err: new LinkedHashSet(), out: new LinkedHashSet()))
        ModelLogger.messageLog[model]
    }

    static void printModelLog() {
        ModelLogger.messageLog.keySet().sort().each { String modelId ->
            ModelLogger logger = ModelLogger.messageLog[modelId]
            Set infoMessages = logger.out
            for (m in infoMessages) println("$modelId: $m")
        }
        if (failureCount.get() > 0) {
            println("Failed to import the following models:")
            ModelLogger.messageLog.keySet().sort().each { String modelId ->
                ModelLogger logger = ModelLogger.messageLog[modelId]
                Set errorMessages = logger.err
                for (m in errorMessages) System.err.println("$modelId: $m")
            }
        }
    }

    /**
     * Atomically deletes a revision, its repository files and associated ACL permissions.
     *
     * <p>Since
     * {@link net.biomodels.jummp.core.ModelService#addRevision(java.util.List, java.util.List,
     * net.biomodels.jummp.core.model.RevisionTransportCommand)}
     * inserts the new revision in a dedicated transaction which gets committed and flushed before the method returns, we
     * cannot use transaction rollback to undo that insertion &ndash; we need to use another dedicated transaction which
     * undoes that work.</p>
     *
     * <p>We also run <tt>git revert</tt> so that the working directory of the model is cleaned.</p>
     *
     * <p>This method will throw an {@code IllegalStateException} if any of the entities could not be deleted.</p>
     * @param toDelete the revision that should be deleted
     * @see BatchSubmissionMainClass#rollBackSessionAndRevision(net.biomodels.jummp.model.Revision, org.springframework.orm.hibernate4.SessionHolder)
     */
    void undoRevisionInsertion(Revision toDelete) {
        assert toDelete
        long rId = toDelete.id
        AclUtilService aclUtilService = ctx.getBean "aclUtilService", AclUtilService
        // a revision and its ACLs are inserted in a dedicated transaction, so we cannot simply
        def txSettings = [propagationBehavior: TransactionDefinition.PROPAGATION_NESTED,
                          isolationLevel     : TransactionDefinition.ISOLATION_READ_COMMITTED]
        Revision.withTransaction(txSettings) { TransactionStatus status ->
            try {
                aclUtilService.deleteAcl(toDelete)
                String query = "delete RepositoryFile rf where rf.revision = :revision"

                RepositoryFile.executeUpdate(query, [revision: toDelete])
                toDelete.delete(flush: true)
            } catch (Exception e) {
                // rollback everything in this transaction and notify the callee
                status.setRollbackOnly()
                String msg = "Could not delete Revision $rId: ${e.message}"
                throw new IllegalStateException(msg)
            }
            try {
                // database rollback ok, now try the reverting VCS changes for this revision
                revertGitRevision(toDelete)
            } catch (Exception e) {
                // VCS rollback failed, don't commit the database rollback to preserve consistency
                status.setRollbackOnly()
                String msg = "Could not revert VCS changes introduced by Revision $rId: $e"
                throw new IllegalStateException(msg, e)
            }
        }
    }

    static boolean markSessionAsRollbackOnly(SessionHolder session) {
        assert session: "Hibernate Session not available. Was persistenceInterceptor.init() called?"
        session.setRollbackOnly()
        session.isRollbackOnly()
    }

    /**
     * </p>Marks the current Hibernate session as rollback only and atomically deletes all entities associated with a
     * revision.</p>
     *
     * <p>To be used as the preferred rollback mechanism in code invoked after the new model revision has been created and
     * persisted into the database via {@code modelService.addRevision()}, assuming the insertion has been successful.</p>
     *
     * @param revision      the revision which should be deleted
     * @param session       the session in which the revision should be deleted
     *
     * @see BatchSubmissionMainClass#undoRevisionInsertion
     * @see BatchSubmissionMainClass#markSessionAsRollbackOnly
     */
    void rollBackSessionAndRevision(Revision revision, SessionHolder session) {
        markSessionAsRollbackOnly(session)
        undoRevisionInsertion(revision)
    }

    /**
     * Supports to partition the model main and additional files
     *
     * @param folder    A File object representing the model folder
     * @return          A map of the main and additional files
     */
    static Map<String, Object> partitionMainAndAdditionalFiles(File folder) {
        File modelFile = null
        // the main file is the SBML (.xml extension)
        List<File> additionals = []
        String perennialId = folder.name
        RFTC mainFileRFTCFromDB = modelMainFileMap.get(perennialId)
        String mainFileName = ""

        if (MAIN_FILES_RENAMED.contains(perennialId)) {
            mainFileName = MAIN_FILES_RENAMED.get(perennialId)
        } else if (mainFileRFTCFromDB) {
            mainFileName = mainFileRFTCFromDB.filename
        } else {
            addModelError(perennialId, """Cannot find the model main file of this model from database""")
            return null
        }
        for (File child : folder.listFiles()) {
            if (child.name == mainFileName) {
                // the model main file
                modelFile = child
            } else if (!child.name.startsWith(".")) {
                // ignore the directories beginning with a dot such as .git or .DS_Store
                additionals << child
            }
        }
        [main: modelFile, additionals: additionals]
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    @CompileDynamic
    ModelFormat fetchModelFormat(MFTC cmd) {
        ModelFormat.findByIdentifierAndFormatVersion(cmd.identifier, cmd.formatVersion)
    }

    Map<String, Object> createRepoFiles(File folder, Model model) {
        List<RFTC> repoFilesTC = null
        Map<String, String> repoFilesMap = new HashMap<String, String>()
        Revision revision = null
        if (model) {
            // it means we must reuse the description of their existing files
            //1. Get the latest revision
            ModelService modelService = ctx.getBean("modelService")
            revision = modelService.getLatestRevision(model, false)

            RepositoryFileService rfService = ctx.getBean("repositoryFileService")
            repoFilesTC = rfService.getRepositoryFilesForRevision(revision)
            //2. Get the list of repository files of that revision
            for (RFTC o : repoFilesTC) {
                repoFilesMap.put(o.filename, o.description)
            }
        }
        def partitionedFiles = partitionMainAndAdditionalFiles(folder)
        File modelFile = partitionedFiles.main as File
        List<File> additionals = partitionedFiles.additionals as List<File>
        final String modelId = folder.name
        assert modelFile: "No main file found in submission folder $modelId"

        String description = "Model main file"
        if (repoFilesMap.containsKey(modelFile.name)) {
            description = repoFilesMap.get(modelFile.name)
        }
        RFTC mainFileRFTC = mshelper.createRepoFile(modelFile, true, description)
        List<RFTC> otherRepoFiles = additionals.collect { f ->
            description = "Additional files"
            if (repoFilesMap.containsKey(f.name)) {
                description = repoFilesMap.get(f.name)
            }
            mshelper.createRepoFile(f, false, description)
        }

        [main: mainFileRFTC, additionals: otherRepoFiles, latestRevision: revision]
    }

    Map<String, Object> createRevisionTCForModelFolder(String id, File folder, Model model) {
        Map<String, Object> repoFileMap = createRepoFiles(folder, model)
        Revision latest = repoFileMap.latestRevision as Revision
        RFTC modelFile = repoFileMap.main as RFTC
        List<RFTC> additionals = repoFileMap.additionals as List<RFTC>
        def mainFiles = [new File(modelFile.path as String)]

        ModelFileFormatService modelFileFormatService = ctx.getBean("modelFileFormatService")
        MFTC fmtCmd = modelFileFormatService.inferModelFormat([modelFile])
        ModelFormat fmt = fetchModelFormat(fmtCmd)
        List errors = []
        boolean isValid = modelFileFormatService.validate(mainFiles, fmt.identifier, errors)
        if (!isValid) {
            addModelError(id, "Model failed validation: $errors")
            return null
        }

        String modelName = modelFileFormatService.extractName(mainFiles, fmt)
        String modelDesc = modelFileFormatService.extractDescription(mainFiles, fmt)

        ModelTransportCommand modelCmd = new ModelTransportCommand(deleted: false)
        if (model) {
            modelCmd.submissionId = model.submissionId
        }
        addModelMsg id, "fake mtc created"
        // avoid calling new ModelAdapter(model: model).toCommandObject(false) because, for
        // reasons not entirely understood yet, modelService.getSpringDatabaseRoles() returns an empty set
        // which causes the query in getLatestRevision() to throw a BadSqlGrammarException. This is because
        // we cannot use empty collections with the 'in' operator in HQL.
        // new ModelAdapter(model: model).toCommandObject(false)
        // addModelMsg id, "real mtc created"

        def repoFileCommands = [modelFile] + additionals
        // GitManager needs filesToDelete and repoFileCommands to not have any overlapping files.
        def filesToDelete = []
        String commitMessage = "Enhance the quality of '$modelName' by resubmitting the updated files."
        def revisionCmd = new RTC(files: repoFileCommands, format: fmtCmd, validated: isValid,
            name: modelName, description: modelDesc, validationLevel: ValidationState.APPROVED,
            curationState: CurationState.NON_CURATED, minorRevision: false, context: ctx,
            comment: commitMessage, model: modelCmd)
        if (latest) {
            revisionCmd.id = latest.id
        }
        [revision: revisionCmd, toAdd: repoFileCommands, toDelete: filesToDelete]
    }

    /**
     * Makes a given revision readable to anyone.
     */
    void publish(Revision r, String owner) {
        SpringSecurityUtils.doWithAuth(owner) {
            AclUtilService aclUtilService = ctx.getBean "aclUtilService", AclUtilService
            aclUtilService.addPermission(r, "ROLE_USER", BasePermission.READ)
            aclUtilService.addPermission(r, "ROLE_ANONYMOUS", BasePermission.READ)
            r.state = ModelState.PUBLISHED
            if (!r.save()) {
                def err = r.errors.allErrors
                assert markSessionAsRollbackOnly(MSH.getCurrentSession()): """Publishing $r failed ($err), but we \
could not roll back the session" throw new IllegalStateException("Cannot publish revision ${r.id}: $err"""
            }
        }
    }

    Revision doInsertNewRevision(String modelId, File modelFolder, Model model) {
        println "doInsertNewRevision"
        Map<String, Object> revisionData = createRevisionTCForModelFolder(modelId, modelFolder, model)
        if (!revisionData) { // something went wrong, the error has already been logged
            return null
        }
        RTC revisionCmd = revisionData.revision as RTC
        List<RFTC> filesToAdd = revisionData.toAdd as List<RFTC>
        List<RFTC> filesToDelete = revisionData.toDelete as List<RFTC>
        Revision newRevision = null
        ModelService modelService = ctx.getBean "modelService", ModelService
        try {
            //modelService.addModellingApproachAsAnnotation(revisionCmd, UhlenScriptSupport.modellingApproach)
            //addModelMsg(modelId, "Assigned modelling approach")
            if (model) {
                newRevision = modelService.addRevision(filesToAdd, filesToDelete, revisionCmd)
            } else {
                model = modelService.uploadValidatedModel(filesToAdd, revisionCmd)
                newRevision = model.revisions.first()
            }
        } catch (ModelException e) {
            assert markSessionAsRollbackOnly(MHS.getCurrentSession()): "Adding revision ${revisionCmd.properties} " +
                "failed but could not roll back"
            addModelError(modelId, "ModelException thrown when inserting the new revision: $e.message")
        }
        newRevision
    }

    /**
     * Support method for handleModelFolder()
     */
    private void doHandleModelFolder(SessionHolder session, File submissionFolder, Model model, String owner) {
        assert submissionFolder?.isDirectory(): "'$submissionFolder' is not a model folder that exists"
//        processedCount.incrementAndGet()
        String id = submissionFolder.name
        println "Model '$id' should have already been imported but isn't."
        Revision newRevision = doInsertNewRevision(id, submissionFolder, model)
        addModelMsg id, "inserted revision $newRevision"
        if (!newRevision || newRevision?.hasErrors()) {
            addModelError(id, "Model update failed: ${newRevision?.errors?.allErrors}")
            println("Model $id update failed: ${newRevision?.errors?.allErrors}")
            assert markSessionAsRollbackOnly(session): """No new revision could be inserted and we failed to roll back the current session"""
        } else {
            println "trying to get the first revision"
            Revision.withTransaction { status ->
                try {
                    // the session of this transaction is empty; attach the new revision to it
                    newRevision = Revision.get(newRevision.id)
                    publish newRevision, owner
                } catch (JummpException e) {
                    addModelError(id, e.message)
                    rollBackSessionAndRevision newRevision, session
                }
            }
        }
    }

    /**
     * Updates the model, adds the modelling approach, publishes its latest version and
     * deletes the non-representative models for the model's category.
     */
    void handleModelFolder(File modelFolder, Model model, String owner) {
        mshelper.doInSession { SessionHolder s ->
            doHandleModelFolder(s, modelFolder, model, owner)
        }
    }

    private void doSubmissionDetected(File root) {
        final String rootName = root.name
        String ownerUsername = CuratedUpdateSupport.getSubmitterAccount(rootName)
        if (!ownerUsername) {
            println("Cannot find the owner of the model $rootName. Using the administrator account instead")
            ownerUsername = "administrator"
        }
        ModelService modelService = ctx.getBean "modelService", ModelService
        Model model = modelService.findByPerennialIdentifier(rootName)
        boolean isUpdated = true
        if (!model) {
            println("""Model '${rootName}' does not exist. The model is about depositing in BioModels under the \
account '${ownerUsername}'.""")
            isUpdated = false
        }

        SpringSecurityUtils.doWithAuth(ownerUsername) {
            try {

                handleModelFolder(root, model, ownerUsername)
                println("handled the model $rootName")
            } catch (IllegalStateException ise) {
                addModelError(rootName, ise.message)
            } catch (AssertionError e) {
                addModelError rootName, e.toString()
            } catch (Exception e) {
                addModelError(rootName, "oops: $rootName -- $e")
                e.printStackTrace()
            }
        }
    }

    @CompileDynamic
    void initiateModelMainFileMap(File root, Pattern modelFolderPattern) {
        RepositoryFileService rfService = ctx.getBean("repositoryFileService")
        RFTC mainFileRFTC = null
        root.eachFileRecurse(FileType.DIRECTORIES) { File dir ->
            final String dirName = dir.name
            if (dirName ==~ modelFolderPattern) {
                mainFileRFTC = rfService.getMainFile(dirName)
                modelMainFileMap.put(dirName, mainFileRFTC)
            }
        }
        Set<String> keys = modelMainFileMap.keySet()
        for (String k: keys) {
            RFTC file = modelMainFileMap.get(k)
            println("${file.filename}: ${file.description}")
        }
    }

    @CompileDynamic
    void initiateFileNameDescriptionMap(File root, Pattern modelFolderPattern) {
        RepositoryFileService rfService = ctx.getBean("repositoryFileService")
        List<RFTC> rftcList = null
        root.eachFileRecurse(FileType.DIRECTORIES) { File dir ->
            final String dirName = dir.name
            if (dirName ==~ modelFolderPattern) {
                rftcList = rfService.getRepositoryFilesForRevision(dirName)
                if (rftcList) {
                    fileNameDescriptionMap.put(dirName, rftcList)
                }
            }
        }
        Set<String> keys = fileNameDescriptionMap.keySet()
        for (String k: keys) {
            RFTC mainFile = modelMainFileMap.get(k)
            println("Main file: ${mainFile?.filename}: ${mainFile?.description}")
            List files = fileNameDescriptionMap.get(k)
            for (RFTC rftc: files) {
                println("${rftc.filename}: ${rftc.description}")
            }
            println("----")
        }
    }

    @CompileDynamic
    void processFolderOfSubmissions(File root, Pattern modelFolderPattern) {
        root.eachFileRecurse(FileType.DIRECTORIES) { File dir ->
            final String dirName = dir.name
            if (dirName ==~ modelFolderPattern) {
                doSubmissionDetected dir
            }
        }
    }

    void runBatchSubmission() {
        println "Started the job: ${new Date().format("dd/MM/yyyy HH:mm:ss")}"

        init()
        String duration
        Instant startTime = Instant.now()
        ctx.persistenceInterceptor?.init()

        try {
            def modelFolderPattern = ~/^BIOMD\d{10}$/
            File base = new File(MODELS_DIR)
            initiateModelMainFileMap(base, modelFolderPattern)
            initiateFileNameDescriptionMap(base, modelFolderPattern)
            processFolderOfSubmissions(base, modelFolderPattern)
            mshelper.awaitCompletionOfIndexingJobs()
        } catch (Exception e) {
            System.err.println("Generic exception encountered while importing the models: $e")
        } finally {
            ctx.persistenceInterceptor?.destroy()
            duration = Duration.between(startTime, Instant.now())
            String formattedDuration = duration.toString()
            println "Submission lasting in $formattedDuration"
            printModelLog()
        }

        /*try {
            Map revisions = buildDataSubmission()
            final int POOL_SIZE = 8
            GParsPool.withPool(POOL_SIZE) {
                revisions.eachParallel { RTC cmd, List files ->
                    RunScriptHelper.simpleRunAs(adminAuth, {
                        ctx.persistenceInterceptor?.init()
                        try {
                            submitModel(files, cmd)
                        } catch (Exception e) {
                            String message = """Cannot submit the models due to ${e.message}"""
                            println(message)
                            e.printStackTrace()
                        } finally {
                            ctx.persistenceInterceptor?.destroy()
                        }
                    })
                }
            }
        } finally {
            ctx.persistenceInterceptor?.destroy()
            duration = Duration.between(startTime, Instant.now())
            String formattedDuration = duration.toString()
            println "Submission lasting in $formattedDuration"
        }*/
        println "Completed the job: ${new Date().format("dd/MM/yyyy HH:mm:ss")}"
    }

    Map buildDataSubmission() {
        Map data = [:]
        String tmpDir = System.getProperty("java.io.tmpdir")
        File location = new File(tmpDir)

        for (int i = 1; i <= 10; i++) {
            File mainFile = MSH.createSimpleMatlabModel("MODEL$i", location)
            String desc = "This is a sample Matlab model $i"
            RFTC mainRFTC = mshelper.createRepoFile(mainFile, true, desc)
            ModelFormat fmt = ModelFormat.findByIdentifierAndName("matlab", "MATLAB (Octave)")
            MFTC fmtCmd = new MFTC(identifier: "matlab", name: "MATLAB (Octave)", formatVersion: fmt.formatVersion)
            boolean isValid = true
            String modelName = "This is a sample Matlab model $i"
            String modelDesc = "This model simulates how to submit multiple models to BioModels concurrently"
            ModelTransportCommand modelCmd = new ModelTransportCommand(deleted: false)
            RTC command = new RTC(files: [mainRFTC], format: fmtCmd, context: ctx,
                validated: isValid, name: modelName, description: modelDesc, uploadDate: new Date(),
                validationLevel: ValidationState.APPROVED,
                curationState: CurationState.NON_CURATED, minorRevision: false,
                comment: "Push the first commit of '$modelName'.", model: modelCmd)
            data.put(command, [mainRFTC] as List)
        }
        return data
    }

    void submitModel(final List files, final RTC revision) {
        println """Submitting the model revision ${revision.dump()} with the repository files ${files.dump()} with the submission id: ${revision.model.submissionId}"""
        //ctx.modelService.uploadValidatedModel(files, revision)
        //helper.awaitCompletionOfIndexingJobs()
        /*String submissionId = ""
        synchronized (this) {
            submissionId = ctx.modelService.submissionIdGenerator.generate()
        }
        println "The new model identifier is $submissionId"*/
    }

    void startBatchSubmissionWithTrigger() {
        Boolean running = false
        while (!running) {
            // sleep for 1 second
            println("Waiting 1 second...")
            Thread.sleep(1000)
            running = Operations.doRedisGet("run-batch-submission").toBoolean()
        }
        // ready to go
        runBatchSubmission()
//        Operations.doRedisSet("run-batch-submission", "false")
    }
}
/**
 * Steps to enable the trigger
 * 1. Run this script on different terminals to simulate BioModels' multiple concurrent running instances
 * ./grailsw run-script scripts/BatchSubmission.groovy >> logs/run-script-batch-submission`date +%F`.log
 *
 * 2. When the lines 'Waiting 1 second...' begin appearing in the log file, it's time to make a bang by running the
 * following from Redis CLI
 * SET run-batch-submission true
 *
 * 3. Take your coffee and watch the log file
 */
new BatchSubmissionMainClass(ctx: ctx).startBatchSubmissionWithTrigger()
