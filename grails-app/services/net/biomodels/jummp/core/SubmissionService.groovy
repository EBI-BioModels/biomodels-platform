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
 * Apache Commons, Perf4j (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 *{Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Apache Commons, Perf4j used as well as
 * that of the covered work.}
 **/


package net.biomodels.jummp.core

import grails.plugin.cache.Cacheable
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import net.biomodels.jummp.core.adapters.ModelFormatAdapter
import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC //rude?
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.core.model.PublicationDetailExtractionContext
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import org.hibernate.SessionFactory
import org.perf4j.aop.Profiled
import org.apache.commons.io.FilenameUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Service that provides model building functionality to a wizard-style model
 * import or update implemented in the web app. It is currently kept in core as
 * we may wish to reuse some of it when we build the curation pipeline. If it is
 * found to be unsuitable for reuse, please move to the web-app plugin.
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @date 20160216
 */
@CompileStatic
class SubmissionService {
    private final Logger log = LoggerFactory.getLogger(this.getClass())

    // concrete strategies for the submission state machine
    private final NewModelStateMachine newModel = new NewModelStateMachine()
    private final NewRevisionStateMachine newRevision = new NewRevisionStateMachine()
    private final InPlaceStateMachine inPlaceMachine = new InPlaceStateMachine()
    /**
     * Disable transactional behaviour for this service.
     */
    static transactional = false
    /**
     * Dependency Injection of ModelFileFormatService
     */
    ModelFileFormatService modelFileFormatService
    /**
     * Dependency Injection of ModelService
     */
    ModelService modelService

    ModelDelegateService modelDelegateService

    /**
     * Dependency Injection of session factory to prevent serialisation of revision
     * domain object.
     */
    transient SessionFactory sessionFactory

    def redisService

    def decorationService

    FileSystemService fileSystemService

    /**
     * Abstract state machine strategy, to be extended by the two concrete
     * strategy implementations
     */

    @CompileStatic
    abstract class StateMachineStrategy {
        /**
         * Load existing objects associated with the model in working into the application cache,
         * called workingMemory before the upload (i.e. new submission and update) process is into gear.
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @CompileStatic(TypeCheckingMode.SKIP)
        @Cacheable('sortedModelFormats')
        //@Cacheable('definedModellingApproaches') // should split it into two methods so as to apply cacheable
        void initialise(Map<String, Object> workingMemory) {
            List<ModelFormat> sortedModelFormats = net.biomodels.jummp.model.ModelFormat.list().sort { it.name }
            workingMemory.put("sorted_model_formats", sortedModelFormats)
            List<ModellingApproach> definedModellingApproaches = ModellingApproach.list()
            workingMemory.put("defined_modelling_approaches", definedModellingApproaches)
            ModelFormat unknownFormat = ModelFormat.findByIdentifier("UNKNOWN")
            MFTC unknownFormatTC = new ModelFormatAdapter(format: unknownFormat).toCommandObject()
            workingMemory.put("unknown_format_command", unknownFormatTC)
        }

        /**
         * Initialise required objects
         */
        @CompileStatic(TypeCheckingMode.SKIP)
//        @Cacheable('sortedModelFormats')
        Map init(final String springSessionId) {
            long unixTime = System.currentTimeMillis() / 1000L
            String submissionSessionId = "submission-$unixTime-$springSessionId"
            println "Initialise the submission process with the session id: $submissionSessionId"

            String uuid = UUID.randomUUID().toString()
            log.debug("Generated submission UUID: ${uuid}")

            ModelFormat unknownFormat = ModelFormat.findByIdentifier("UNKNOWN")
            MFTC unknownFormatTC = new ModelFormatAdapter(format: unknownFormat).toCommandObject()
            Map submissionMap =  ["submission-session-id": submissionSessionId,
                                  "submission-folder": uuid]
            return submissionMap
            //workingMemory.put("unknown_format_command", unknownFormatTC)
            // init the unknown format

//            decorationService
            /*List<ModelFormat> sortedModelFormats = net.biomodels.jummp.model.ModelFormat.list().sort { it.name }
            workingMemory.put("sorted_model_formats", sortedModelFormats)
            List<ModellingApproach> definedModellingApproaches = ModellingApproach.list()
            workingMemory.put("defined_modelling_approaches", definedModellingApproaches)
            ModelFormat unknownFormat = ModelFormat.findByIdentifier("UNKNOWN")
            MFTC unknownFormatTC = new ModelFormatAdapter(format: unknownFormat).toCommandObject()
            workingMemory.put("unknown_format_command", unknownFormatTC)*/
        }
        /**
         * The method allows filtering out the files being added and the ones will be deleted.
         * At the same time, the cache system, i.e. workingMemory, is also made up-to-date.
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.handleFileUpload")
        void handleFileUpload(Map<String, Object> workingMemory) {
            List<RFTC> filesToBeAdded
            List<String> filesToDelete
            Map<File, String> mainFiles
            Map<File, String> additionalFiles
            if (workingMemory.containsKey("submitted_mains")) {
                mainFiles = workingMemory.remove("submitted_mains") as HashMap<File, String>
                workingMemory.put("reprocess_files", true)
                List<RFTC> mainRFTCs = new LinkedList<RFTC>()
                mainFiles.each {File key, String value ->
                    mainRFTCs.add(createRFTC(key, true, value))
                }
                workingMemory.put("main_repository_files_in_working", mainRFTCs)
            } else {
                mainFiles = new HashMap<File, String>()
            }
            if (workingMemory.containsKey("submitted_additionals")) {
                additionalFiles = workingMemory.remove("submitted_additionals") as HashMap<File, String>
                List<RFTC> allExtraFilesWorking = new LinkedList<RFTC>()
                additionalFiles.each { File key, String value ->
                    allExtraFilesWorking.add(createRFTC(key, false, value))
                }
                workingMemory.put("additional_repository_files_in_working", allExtraFilesWorking)
            } else {
                additionalFiles = new HashMap<File, String>()
            }
            filesToBeAdded = createRFTCList(mainFiles, additionalFiles)
            if (workingMemory.containsKey("removeFromVCS")) {
                def removeFromVcs = workingMemory.get("removeFromVCS") as List<RFTC>
                removeFromVcs.removeAll(filesToBeAdded) // update after delete -> update
            }
            if (workingMemory.containsKey("deleted_filenames")) {
                filesToDelete = workingMemory.remove("deleted_filenames") as List<String>
                workingMemory.put("reprocess_files", true)
                // check for replacement
                def overlapping = filesToDelete.findAll {
                    filesToBeAdded.find { RFTC testFile -> new File(testFile.path).getName() == it }
                }
                if (overlapping) {
                    filesToDelete = filesToDelete - overlapping
                }
            }
            // update the list of RFTC and the list of files that would be deleted
            // this update is really done on workingMemory
            storeRFTC(workingMemory, filesToBeAdded, filesToDelete)
        }

        /**
         * Removes deleted files from memory
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.NewModelStateMachine.handleDeletes")
        protected void handleDeletes(Map<String, Object> workingMemory, List<RFTC> filesToDelete) {
            if (workingMemory.containsKey("repository_files")) {
                List<RFTC> existing = (workingMemory.get("repository_files") as List<RFTC>)
                existing.removeAll(filesToDelete)
            }
            removeFromVCS(workingMemory, filesToDelete)
        }

        abstract void removeFromVCS(Map<String, Object> workingMemory, List<RFTC> filesToDelete);

        /**
         * Purpose: append supplied RFTC list to those in workingMemory (if any, otherwise create)
         * The method updates the list of main files and the list of additional files which are used for
         * the next phase. For example:
         * a) Once pressing update button, the method gets the list of main files and additional files existing
         *    into the database which are displayed on the upload file page
         * b) Once adding or discarding files and then hitting Upload button, the method will update the list of
         *    main and additional files which are gone alongside the new revision.
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         * @param modifications a Map containing the existing files in the model, to be modified
         */
        @Profiled(tag = "submissionService.storeRFTC")
        protected void storeRFTC(Map<String, Object> workingMemory,
                                 List<RFTC> tobeAdded,
                                 List<String> filesToDelete) {
            Collection<RFTC> mains
            Collection<RFTC> additionals
            if (workingMemory.containsKey("repository_files")) {
                /* case: the repository files are being updated and maintained in memory */
                Collection<RFTC> existing = workingMemory.get("repository_files") as List<RFTC>
                if (!tobeAdded && !filesToDelete &&
                        !workingMemory['isUpdateOnExistingModel']) {
                    workingMemory.put("changedMainFiles", false)
                    return
                }
                Collection<RFTC> currentMains = existing.findAll { RFTC it -> it.mainFile }
                List<RFTC> toDelete = new LinkedList<RFTC>()
                Set<RFTC> willBeReplaced = new HashSet<RFTC>()
                existing.each { RFTC oldfile ->
                    String oldname = (new File(oldfile.path)).getName()
                    tobeAdded.each { RFTC newfile ->
                        String newname = (new File(newfile.path)).getName()
                        if (newname == oldname) {
                            willBeReplaced.add(oldfile)
                        }
                    }
                    if (filesToDelete) {
                        filesToDelete.each { deleteFile ->
                            if (oldname == deleteFile) {
                                toDelete.add(oldfile)
                            }
                        }
                    }
                }
                if (willBeReplaced) {
                    existing.removeAll(willBeReplaced)
                }
                if (filesToDelete) {
                    handleDeletes(workingMemory, toDelete)
                }
                if (tobeAdded) {
                    existing.addAll(tobeAdded)
                }
                mains = existing.findAll { RFTC it -> it.mainFile }
                if (currentMains != mains) {
                    workingMemory.put("changedMainFiles", true)
                } else {
                    workingMemory.put("changedMainFiles", false)
                }
                mains = workingMemory.remove("main_repository_files_in_working") as List<RFTC>
                additionals = workingMemory.remove("additional_repository_files_in_working") as List<RFTC>
            } else {
                /* case: at the beginning of the updating process, i.e. at the time when hitting Update button first */
                workingMemory.put("repository_files", tobeAdded)
                // DON'T CHANGE IF IS UPDATE ON EXISTING MODEL
                workingMemory.put("changedMainFiles", true)
                mains = tobeAdded.findAll { RFTC it -> it.mainFile }
                additionals = tobeAdded - mains
            }
            workingMemory.put("main_files", mains)
            workingMemory.put("additional_files", additionals)
        }

        abstract boolean processingRequired(Map<String, Object> workingMemory);

        /**
         * Detects the format of the model and stores this information in the working memory
         * using the key <tt>model_type</tt>
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.inferModelFormatType")
        void inferModelFormatType(Map<String, Object> workingMemory) {
            if (workingMemory['changedMainFiles'] || !workingMemory['model_type']) {
                MFTC format = modelFileFormatService.inferModelFormat(getRepFiles(workingMemory))
                if (format) {
                    workingMemory.put("model_type", format)
                    // revision.format will be updated in updateRevisionFromFiles
                }
            }
        }

        /**
         * Perform validation on the model
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        void performValidation(Map<String, Object> workingMemory) {
            if (processingRequired(workingMemory)) {
                workingMemory.remove("validationErrorList")
                List<File> modelFiles = getFilesFromMemory(workingMemory, false)
                modelFiles.each { File it ->
                    if (!it) {
                        workingMemory.put("validation_error", "Null file passed!")
                    }
                    if (!it.exists()) {
                        workingMemory.put("validation_error", "File does not exist")
                    }
                    if (it.isDirectory()) {
                        workingMemory.put("validation_error", "Directory passed as input")
                    }
                }
                final List<String> errors = new LinkedList<String>()
                List<File> mainFiles = getFilesFromMemory(workingMemory, true)
                final String fmt = ((MFTC) workingMemory.get("model_type")).identifier as String
                boolean modelsAreValid = modelFileFormatService.validate(mainFiles, fmt, errors)
                workingMemory.put("model_validation_result", modelsAreValid)
                if (!workingMemory.containsKey("model_type")) {   //TODO IS THIS NEEDED?
                    workingMemory.put("validation_error",
                        "Missing Format Error: Validation could not be performed, format unknown")
                } else if (!modelsAreValid || errors?.size() > 0) {
                    //TODO be more specific to the user about what went wrong.
                    workingMemory.put("validation_error", "ModelValidationError")
                    workingMemory.put("validationErrorList", errors)
                }
            }
        }

        /**
         * Convenience function to store the supplied DOMs in working memory
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.storeTCs")
        protected void storeTCs(Map<String, Object> workingMemory, MTC model, RTC revision) {
            workingMemory.put("ModelTC", model)
            workingMemory.put("RevisionTC", revision)
        }

        /**
         * Purpose
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        protected abstract void createTransportObjects(Map<String, Object> workingMemory);

        @TypeChecked(TypeCheckingMode.SKIP)
        protected boolean updatePubs(MTC model, String publinkType, String publink) {
            boolean refreshPublication = false
            PublicationLinkProvider.LinkType linkType = PublicationLinkProvider.LinkType.findLinkTypeByLabel(publinkType)
            if (publinkType) {
                if (!model.publication) {
                    model.publication = new PublicationTransportCommand()
                }
                refreshPublication = true
            }
            model.publication.link = publink
            PublicationLinkProvider publSrc = PublicationLinkProvider.withCriteria(uniqueResult: true) {
                eq("linkType", linkType)
            }
            model.publication.linkProvider = new PublicationLinkProviderAdapter(linkProvider:
                    publSrc).toCommandObject()
            return refreshPublication
        }

        /**
         * This tries to capture readme information about unknown format and other modelling approach.
         *
         * Retrieving all these information from the working memory and update the revision in question.
         *
         * @param revision  Revision
         * @param workingMemory a map of temporary variables
         */
        @Profiled(tag = "submissionService.storeReadmeInfo")
        @TypeChecked(TypeCheckingMode.SKIP)
        protected void storeReadmeInfo(RTC revision, Map<String, Object> workingMemory) {
            // update modelling approach
            String modellingApproach = workingMemory.get("modelling_approach")
            ModellingApproach approach = ModellingApproach.findByName(modellingApproach)
            revision.model.modellingApproach = approach

            if (workingMemory.get("other_info")) {
                revision.model.otherInfo = workingMemory.get("other_info")
            }

            // add MAMO term representing the modelling approach into the SBML file if the curators or the submitter
            // has not added it to model level annotations yet
            if (revision.format.identifier == "SBML") {
                modelService.addModellingApproachAsAnnotation(revision, approach)
            }

            // update model format
            final long fmtId = workingMemory.get("model_format") as Long
            if (fmtId != revision.format.id) {
                // the model format has been changed by the user
                MFTC formatTC = new ModelFormatAdapter(format: ModelFormat.get(fmtId)).toCommandObject()
                revision.format = formatTC
                revision.model.format = revision.model.format
            }
            // TODO: check that 'Original code *' is the currently chosen value. If not, don't do the statement below
            if (workingMemory.get("readme_submission")) {
                revision.readmeSubmission = workingMemory.get("readme_submission")
            }
        }

        /**
         * Get the model name from uploading files
         *
         * @param mainFiles All of the uploading files
         * @return  a string representing the file name as model name
         */
        @Profiled(tag = "submissionService.getModelNameFromFiles")
        @TypeChecked(TypeCheckingMode.SKIP)
        protected String getModelNameFromFiles(List<File> mainFiles) {
            StringBuilder name = new StringBuilder()
            boolean first = true
            mainFiles.each { File it ->
                if (!first) {
                    name.append(", ")
                }
                name.append(FilenameUtils.getBaseName(it.name))
                first = false
            }
            return name.toString()
        }

        /**
         * Get the model description from files uploading
         *
         * @param allFiles
         * @return
         */
        @Profiled(tag = "submissionService.getModelDescriptionFromFiles")
        @TypeChecked(TypeCheckingMode.SKIP)
        protected String getModelDescriptionFromFiles(List<File> allFiles) {
            StringBuilder desc = new StringBuilder("Model comprised of files: ")
            String fileNames = allFiles.collect { File it -> it.name }.join(', ')
            return desc.append(fileNames).toString()
        }

        /**
         * Purpose Convenience function to update the revision dom from the files
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.updateRevisionFromFiles")
        @TypeChecked(TypeCheckingMode.SKIP)
        protected void updateRevisionFromFiles(Map<String, Object> workingMemory) {
            RTC revision = workingMemory.get("RevisionTC") as RTC
            MFTC fmt = workingMemory["model_type"] as MFTC
            if (revision.format != fmt) {
                revision.format = fmt
            }
            List<File> files = getFilesFromMemory(workingMemory, true)
            final String fmtVersion = revision.format.formatVersion ?: "*"
            final String fmtId = revision.format.identifier
            def modelFormat = ModelFormat.findByIdentifierAndFormatVersion(fmtId, fmtVersion)
            revision.name = modelFileFormatService.extractName(files, modelFormat)
            if (!revision.name) {
                revision.name = getModelNameFromFiles(files)
            }
            String newDescription = modelFileFormatService.extractDescription(files, modelFormat)
            if (newDescription) {
                revision.description = newDescription
            }
            revision.validated = workingMemory.get("model_validation_result") as Boolean
        }

        /**
         * Purpose Extract information about the model.
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.inferModelInfo")
        void inferModelInfo(Map<String, Object> workingMemory) {
            if (!workingMemory.containsKey("RevisionTC")) {
                createTransportObjects(workingMemory)
            }
            RTC revision = workingMemory.get("RevisionTC") as RTC
            workingMemory.put("readme_submission", revision.readmeSubmission ?: "")
            workingMemory.put("other_info", revision.model.otherInfo)
            // At this stage, the modelling approach has been loaded from the database if it was persisted.
            // Otherwise, it will be extracted from the model file.
            if (!workingMemory.get("modelling_approach")) {
                ModellingApproach approach = modelFileFormatService.extractModellingApproachFromFiles(revision)
                String modellingApproach = approach ? approach.name : ""
                workingMemory.put("modelling_approach", modellingApproach)
            }
            updateRevisionFromFiles(workingMemory)
        }

        /**
         * Purpose Update the name/description in the model data structures and files
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         * @param modifications a Map containing the user's modifications to the model information we extracted.
         */
        @Profiled(tag = "submissionService.refineModelInfo")
        void refineModelInfo(Map<String, Object> workingMemory, Map<String, Object> modifications) {
            RTC revision = workingMemory.get("RevisionTC") as RTC
            final String NAME = revision.name
            final String DESC = revision.description
            String NEW_NAME = ((String) modifications["new_name"])?.trim()
            String NEW_DESC = ((String) modifications["new_description"])?.trim()
            if (modifications["changeStatus"] == "true") {
                if (!NEW_NAME && !NEW_DESC) {
                    return
                }
                if ((NAME == NEW_NAME) && (DESC == NEW_DESC)) {
                    return
                }
                if ((!NAME && NEW_NAME) || (!DESC && NEW_DESC)) {
                    handleModificationsToSubmissionInfo(workingMemory, modifications)
                }
                if ((NEW_NAME != NAME) || (NEW_DESC != DESC)) {
                    handleModificationsToSubmissionInfo(workingMemory, modifications)
                }
            }
        }

        /**
         * Purpose Handle changes made at the submission summary.
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         * @param modifications a Map containing the user's modifications to the model information we extracted.
         */
        protected void handleModificationsToSubmissionInfo(Map<String, Object> workingMemory,
                Map<String, Object> modifications) {
            final RTC REVISION = workingMemory["RevisionTC"] as RTC
            final String R_NAME = REVISION.name
            final String R_DESCRIPTION = REVISION.description
            final String NEW_NAME = modifications["new_name"]
            final String NEW_DESC = modifications["new_description"]
            if (R_NAME != NEW_NAME) {
                workingMemory["new_name"] = NEW_NAME
            }
            if (R_DESCRIPTION != NEW_DESC) {
                workingMemory["new_description"] = NEW_DESC
            }
        }

        void updatePublicationLink(Map<String, Object> workingMemory, Map<String, String> modifications) {
            if (modifications.containsKey("PubLinkProvider")) {
                MTC mtc = workingMemory.get("ModelTC") as MTC
                workingMemory.put("previousPubLinkProvider", mtc.publication?.linkProvider)
                workingMemory.put("previousPubLink", mtc.publication?.link)
                workingMemory.put("RetrievePubDetails",
                    updatePubs(mtc,
                        modifications.get("PubLinkProvider"),
                        modifications.get("PubLink")))
                if (workingMemory.containsKey("Authors")) {
                    workingMemory.remove("Authors")
                }
            }
        }

        /**
         * Purpose
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         * @param modifications a Map containing the user's modifications to the model information we extracted.
         */
        void updateFromSummary(Map<String, Object> workingMemory, Map<String, String> modifications) {
            //does nothing in the base class.
        }

        /**
         * Purpose submit files, remove intermediate files from disk
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.handleSubmission")
        @TypeChecked(TypeCheckingMode.SKIP)
        HashSet<String> handleSubmission(Map<String, Object> workingMemory) {
            HashSet<String> retval
            try {
                retval = completeSubmission(workingMemory)
                cleanup(workingMemory)
            }
            catch (Exception e) {
                log.error "Cannot process submission $workingMemory: ${e.message}", e
                throw e // need this to enter error subflow
            }
            retval
        }

        /**
         * Concrete implementations perform the actual submission
         */
        protected abstract HashSet<String> completeSubmission(Map<String, Object> workingMemory)

        /**
         * Purpose Remove intermediate files from disk
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.cleanup")
        void cleanup(Map<String, Object> workingMemory) {
            try {
                List<RFTC> repoFiles = getRepFiles(workingMemory)
                File parent = null
                repoFiles?.each { Object it ->
                    RFTC rfTC = it as RFTC
                    File deleteMe = new File(rfTC.path as String)
                    if (!parent) {
                        parent = deleteMe.getParentFile()
                    }
                    deleteMe.delete()
                }
                if (parent) {
                    parent.delete()
                }
            }
            catch (Exception e) {
                e.printStackTrace()
            }
        }

        /**
         * Purpose
         *
         * @param mainFiles       a Map of all the main files alongside their descriptions associated with the model.
         * @param additionalFiles a Map comprising any supplementary files and corresponding descriptions
         *                          that are also part of the model that is submitted.
         */
        @Profiled(tag = "submissionService.createRFTCList")
        protected List<RFTC> createRFTCList(Map<File, String> mainFiles,
                Map<File, String> additionalFiles) {
            List<RFTC> returnMe = new LinkedList<RFTC>()
            mainFiles.keySet().each { File it ->
                returnMe.add(createRFTC(it, true, mainFiles.get(it)))
            }
            additionalFiles.keySet().each { File it ->
                returnMe.add(createRFTC(it, false, additionalFiles.get(it)))
            }
            returnMe
        }

        /*
         * Convenience method for creating
         * @link{net.biomodels.jummp.core.model.RepositoryFileTransportCommand} objects
         */
        public RFTC createRFTC(File file, boolean isMain, String description) {
            new RFTC(path: file.getCanonicalPath(), mainFile: isMain, userSubmitted: true,
                    hidden: false, description: description)
        }


        /**
         * Purpose: Utility method to generate the publication map of
         * publication link providers could be used during submission process. It enables to keep
         * track of information submitter has entered in publication editor form so avoid losing it.
         */
        protected Map<Object, PublicationDetailExtractionContext> initialisePublicationMap() {
            List<String> linkSourceTypes = PublicationLinkProvider.LinkType.values().collect {
                PublicationLinkProvider.LinkType it -> it.label
            }
            Map<Object, PublicationDetailExtractionContext> publication_objects_in_working =
                new HashMap<Object,PublicationDetailExtractionContext>()
            linkSourceTypes.each {
                PublicationDetailExtractionContext context = new PublicationDetailExtractionContext()
                context.comesFromDatabase = false
                context.publication = null
                publication_objects_in_working.put(it, context)
            }
            publication_objects_in_working
        }
    }

    @CompileStatic
    class InPlaceStateMachine extends StateMachineStrategy {
        void initialise(Map<String, Object> workingMemory) {
            super.initialise(workingMemory)
        }

        Map init(final String springSessionId) {
            super.init(springSessionId)
        }

        void removeFromVCS(Map<String, Object> workingMemory, List<RFTC> filesToDelete) {
            //nothing in VCS, need to do nothing
        }

        //Always process files in create mode. Possibly needs optimisation.
        boolean processingRequired(Map<String, Object> workingMemory) {
            return true;
        }

        @Profiled(tag = "submissionService.InPlaceStateMachine.createTransportObjects")
        protected void createTransportObjects(Map<String,Object> workingMemory) {

        }
        @TypeChecked(TypeCheckingMode.SKIP)
        @Profiled(tag = "submissionService.InPlaceStateMachine.completeSubmission")
        HashSet<String> completeSubmission(Map<String, Object> workingMemory) {

        }
    }

    /**
     * Provides a concrete implementation of the @link{StateMachineStrategy} that is responsible
     * for handling the submission of new models to JUMMP.
     */
    @CompileStatic
    class NewModelStateMachine extends StateMachineStrategy {

        /**
         * Initialises the publication objects of publication link provider that could be used
         * during submission process currently.
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        void initialise(Map<String, Object> workingMemory) {
            super.initialise(workingMemory)
            def publication_objects_in_working = initialisePublicationMap()
            workingMemory.put("publication_objects_in_working", publication_objects_in_working)
        }

        Map init(final String springSessionId) {
            super.init(springSessionId)
        }

        void removeFromVCS(Map<String, Object> workingMemory, List<RFTC> filesToDelete) {
            //nothing in VCS, need to do nothing
        }

        //Always process files in create mode. Possibly needs optimisation.
        boolean processingRequired(Map<String, Object> workingMemory) {
            return true
        }

        /**
         * Purpose Create new model and revision transport objects, store in working memory
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.NewModelStateMachine.createTransportObjects")
        protected void createTransportObjects(Map<String,Object> workingMemory) {
            MTC model = new MTC()
            MFTC format = workingMemory.get("model_type") as MFTC
            List<RFTC> repoFiles = getRepFiles(workingMemory) as List<RFTC>
            RTC revision = new RTC(files: repoFiles, model: model, format: format)
            storeTCs(workingMemory, model, revision)
        }

        /**
         * Purpose Handles changes made on the summary screen.
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         * @param modifications a Map containing the user's modifications to the model information we extracted.
         */
        void handleModificationsToSubmissionInfo(Map<String, Object> workingMemory,
                Map<String, Object> modifications) {
            super.handleModificationsToSubmissionInfo(workingMemory, modifications)
        }

        /**
         * Purpose Saves the model in the database and repository
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @TypeChecked(TypeCheckingMode.SKIP)
        @Profiled(tag = "submissionService.NewModelStateMachine.completeSubmission")
        HashSet<String> completeSubmission(Map<String, Object> workingMemory) {
            List<RFTC> repoFiles = getRepFiles(workingMemory)
            RTC revision = workingMemory.get("RevisionTC") as RTC
            MTC model = revision.model
            model.format = revision.format
            // update model format, modelling approach and readme info if they're provided
            storeReadmeInfo(revision, workingMemory)
            revision.comment = "Import of ${revision.name}".toString()
            Model newModel = modelService.uploadValidatedModel(repoFiles, revision)
            Revision latest = modelService.getLatestRevision(newModel, false)
            RTC latestRTC = new RevisionAdapter(revision: latest).toCommandObject()

            final String NEW_NAME = workingMemory["new_name"]
            final String NEW_DESCRIPTION = workingMemory["new_description"]
            final boolean SHOULD_UPDATE = NEW_NAME || NEW_DESCRIPTION
            if (NEW_NAME) {
                latestRTC.name = NEW_NAME
                modelFileFormatService.updateName(latestRTC, NEW_NAME)
            }
            if (NEW_DESCRIPTION) {
                latestRTC.description = NEW_DESCRIPTION
                modelFileFormatService.updateDescription(latestRTC, NEW_DESCRIPTION)
            }
            if (SHOULD_UPDATE) {
                latestRTC.comment = "Edited model metadata online."
                modelService.addRevision(latestRTC.files, [], latestRTC)
            }
            String modelId = newModel.submissionId
            workingMemory.put("model_id", modelId)
            HashSet<String> result = [modelId] as HashSet
            return result
        }
    }

    /**
     * Provides a concrete implementation of the @link{StateMachineStrategy} that is responsible
     * for handling the submission of updated versions of existing models.
     */
    @CompileStatic
    class NewRevisionStateMachine extends StateMachineStrategy {

        /**
         * Initialises the revision transport command object, the currently
         * existing files associated with the revision in working memory, and the publication
         * objects that could be used during submission process.
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.NewRevisionStateMachine.initialise")
        void initialise(Map<String, Object> workingMemory) {
            super.initialise(workingMemory)
            // fetch files from repository, make RFTCs out of them
            RTC rev = workingMemory.get("LastRevision") as RTC
            List<RFTC> repFiles = rev.getFiles()
            storeRFTC(workingMemory, repFiles, null)
            workingMemory.put("existing_files", new ArrayList<RFTC>(repFiles))
            // initialise the map of publication type objects would be added to the model
            def publication_objects_in_working = initialisePublicationMap()
            if (rev.model.publication) {
                PublicationDetailExtractionContext context = new PublicationDetailExtractionContext()
                context.comesFromDatabase = true
                context.publication = rev.model.publication
                publication_objects_in_working.put(rev.model.publication.linkProvider.linkType, context)
            }
            workingMemory.put("publication_objects_in_working", publication_objects_in_working)
            sessionFactory.currentSession.clear()
        }

        Map init(final String springSessionId) {
            super.init(springSessionId)
        }

        void removeFromVCS(Map<String, Object> workingMemory, List<RFTC> filesToDelete) {
            if (!workingMemory.containsKey("removeFromVCS")) {
                workingMemory.put("removeFromVCS", new LinkedList<RFTC>())
            }
            def removeFromVcs = (Collection<RFTC>) workingMemory.get("removeFromVCS")
            def existing = workingMemory.get("existing_files") as List<RFTC>
            filesToDelete.each { RFTC candidate ->
                if (existing.find { RFTC r ->
                    new File(r.path).getName() == new File(candidate.path).getName()
                }) {
                    removeFromVcs.add(candidate)
                }
            }
        }

        //Reprocess files if a new main file has been added or files have been deleted
        boolean processingRequired(Map<String, Object> workingMemory) {
            return workingMemory.containsKey("reprocess_files")
        }

        /**
         * Initialises the Revision object based on the object stored
         * for the last revision and the <tt>model_type</tt> from working memory
         *
         * @param workingMemory a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.NewRevisionStateMachine.createTransportObjects")
        protected void createTransportObjects(Map<String, Object> workingMemory) {
            RTC revision = workingMemory.get("LastRevision") as RTC
            if (workingMemory.containsKey("reprocess_files")) {
                MFTC format = workingMemory["model_type"] as MFTC
                if (format != revision.format) {
                    revision.format = format
                }
            } else {
                workingMemory.put("model_type", revision.format)
                workingMemory.put("model_validation_result", revision.validated)
            }
            storeTCs(workingMemory, revision.model, revision)
            //ensure that a new revision tc is used for submission, use
            //this one for copying info!
        }

        /* Handles removal of files from the model. Is called from
         * handleFileUploads. The revision implementation should remove files from
         * both the working memory and the repository
         * @param workingMemory     a Map containing all objects exchanged throughout the flow.
         * @param modifications     the files to be modified/removed
         */
        void handleModificationsToSubmissionInfo(Map<String, Object> workingMemory,
                Map<String, Object> modifications) {
            super.handleModificationsToSubmissionInfo(workingMemory, modifications)
        }

        /* Updates the revision's comments. New comment is passed through the
         * modifications map. Kept as map to allow passing other info
         * @param workingMemory     a Map containing all objects exchanged throughout the flow.
         * @param modifications     the revision comments (and any other info to be updated)
         */
        @Profiled(tag = "submissionService.NewRevisionStateMachine.updateRevisionComments")
        void updateFromSummary(Map<String, Object> workingMemory, Map<String, String> modifications) {
            RTC revision = workingMemory.get("RevisionTC") as RTC
            revision.comment = modifications.get("RevisionComments")
        }

        /* Submits the revision to modelService
         * @param workingMemory     a Map containing all objects exchanged throughout the flow.
         */
        @Profiled(tag = "submissionService.NewRevisionStateMachine.completeSubmission")
        @TypeChecked(TypeCheckingMode.SKIP)
        HashSet<String> completeSubmission(Map<String, Object> workingMemory) {
            HashSet<String> changes = new HashSet<String>()
            RTC revision = workingMemory.get("RevisionTC") as RTC
            List<RFTC> repoFiles = getRepFiles(workingMemory)
            List<RFTC> deleteFiles = getRepFiles(workingMemory, "removeFromVCS")
            deleteFiles.each { RFTC rf ->
                File file = new File(rf.path)
                changes.add("Deleted file: ${file.getName()}")
            }
            def existing = workingMemory.get("existing_files") as List<RFTC>
            repoFiles.each { RFTC it ->
                String fileAdded = new File(it.path).getName()
                def exists = existing.find { RFTC fileExisting ->
                    fileAdded == new File(fileExisting.path).getName()
                }
                if (!exists) {
                    changes.add("Added file: ${fileAdded}")
                }
            }

            // update model format, modelling approach and readme info if they're provided and changed
            storeReadmeInfo(revision, workingMemory)

            Revision newlyCreated = modelService.addRevision(repoFiles, deleteFiles, revision)
            RTC newlyCreatedRTC = new RevisionAdapter(revision: newlyCreated).toCommandObject()
            final String NEW_NAME = workingMemory["new_name"]
            final String NEW_DESCRIPTION = workingMemory["new_description"]
            final boolean SHOULD_UPDATE = NEW_NAME || NEW_DESCRIPTION
            if (NEW_NAME) {
                newlyCreatedRTC.name = NEW_NAME
                modelFileFormatService.updateName(newlyCreatedRTC, NEW_NAME)
                changes.add("Edited model name")
            }
            if (NEW_DESCRIPTION) {
                newlyCreatedRTC.description = NEW_DESCRIPTION
                modelFileFormatService.updateDescription(newlyCreatedRTC, NEW_DESCRIPTION)
                changes.add("Edited model description")
            }
            if (SHOULD_UPDATE) {
                newlyCreatedRTC.comment = "Edited model metadata online."
                def updated = modelService.addRevision(newlyCreatedRTC.files, [], newlyCreatedRTC)
                workingMemory.put("model_id", updated.model.submissionId)
            } else {
                workingMemory.put("model_id", newlyCreated.model.submissionId)
            }

            return changes
        }
    }

    /**
     * Called by ModelController to initialise working memory
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     * @param modifications
     */
    @Profiled(tag = "submissionService.initialise")
    void initialise(Map<String, Object> workingMemory) {
        getStrategyFromContext(workingMemory).initialise(workingMemory)
    }

    @Profiled(tag = "submissionService.init")
    Map init(final String springSessionId) {
        // TODO; use getStrategyFromContext
        newModel.init(springSessionId)
    }

    void writeUploadingFileToRedis(final File file) {
        // TODO: implement me
        decorationService
    }
    /**
     * Called by ModelController for adding or removing files from the working memory
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    @Profiled(tag = "submissionService.handleFileUpload")
    void handleFileUpload(Map<String, Object> workingMemory) {
        getStrategyFromContext(workingMemory).handleFileUpload(workingMemory)
    }

    /**
     * Detects the format of the model and stores this information in the working memory
     * using the key <tt>model_type</tt>
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    @Profiled(tag = "submissionService.inferModelFormatType")
    void inferModelFormatType(Map<String, Object> workingMemory) {
        getStrategyFromContext(workingMemory).inferModelFormatType(workingMemory)
    }

    /**
     * Performs validation on the supplied model and stores the result in
     * <tt>model_validation_result</tt>
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    @Profiled(tag = "submissionService.performValidation")
    void performValidation(Map<String, Object> workingMemory) throws Exception {
        /*
         * Throws an exception if files are not valid, or do not comprise a valid model
         */
        getStrategyFromContext(workingMemory).performValidation(workingMemory)
    }

    /**
     * Extracts the model's information and creates transport command
     * objects for Revision, stored in <tt>RevisionTC</tt>
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    @Profiled(tag = "submissionService.inferModelInfo")
    void inferModelInfo(Map<String, Object> workingMemory) {
        /* create RevisionTC, ModelTC, populate fields */
        getStrategyFromContext(workingMemory).inferModelInfo(workingMemory)
    }

    /**
     * update the working memory with user specified modifications
     * creating separate objects where necessary to ensure that
     * the modifications are performed as separate commits or revisions
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    @Profiled(tag = "submissionService.refineModelInfo")
    void refineModelInfo(Map<String, Object> workingMemory, Map<String, Object> modifications) {
        /*
         */
        getStrategyFromContext(workingMemory).refineModelInfo(workingMemory, modifications)
    }

    /**
     * update the working memory with revision specific comments
     * parameter left as a map<string,string> for forward-compatibility
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    @Profiled(tag = "submissionService.updateFromSummary")
    void updateFromSummary(Map<String, Object> workingMemory, Map<String, String> modifications) {
        getStrategyFromContext(workingMemory).updateFromSummary(workingMemory, modifications)
    }

    /**
     * update the working memory with publication data.
     * parameter left as a map<string,string> for forward-compatibility
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    @Profiled(tag = "submissionService.updateFromSummary")
    void updatePublicationLink(Map<String, Object> workingMemory, Map<String, String> modifications) {
        getStrategyFromContext(workingMemory).updatePublicationLink(workingMemory, modifications)
    }

    /**
     * Purpose Create or update DOM objects as necessary
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    @Profiled(tag = "submissionService.handleSubmission")
    HashSet<String> handleSubmission(Map<String, Object> workingMemory) {
        def strategy = getStrategyFromContext(workingMemory)
        strategy.handleSubmission(workingMemory)
    }

    /**
     * Purpose: Remove the intermediate files from the disk
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    @Profiled(tag = "submissionService.cleanup")
    void cleanup(Map<String, Object> workingMemory) {
        getStrategyFromContext(workingMemory).cleanup(workingMemory)
    }

    /**
     * Purpose: Get the appropriate strategy for the flow (update or create)
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    private StateMachineStrategy getStrategyFromContext(Map<String, Object> workingMemory) {
        Boolean isUpdateOnExistingModel = (Boolean) workingMemory.get("isUpdateOnExistingModel")
        Boolean shouldCreateNewRevision = (Boolean) workingMemory.get("shouldCreateNewRevision")
        if (isUpdateOnExistingModel) {
            shouldCreateNewRevision = Boolean.TRUE
            if (!shouldCreateNewRevision) {
                inPlaceMachine
            }
            return newRevision
        }
        return newModel
    }

    /**
     * Purpose: Convenience function to extract files from memory.
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     * @param filterMain a boolean parameter specifying whether or not to exclude additional files
     */
    /* generics + @CompileStatic ==> https://issues.apache.org/jira/browse/GROOVY-7477 */
    protected List getFilesFromMemory(Map workingMemory, boolean filterMain) {
        List<RFTC> repFiles = getRepFiles(workingMemory)
        if (!repFiles) {
            repFiles = new LinkedList<RFTC>();
            //only for testing, remove and throw exception perhaps!
        }
        if (filterMain) {
            /* filter out non-main files */
            repFiles = repFiles.findAll { it ->
                RFTC rftc = it as RFTC
                rftc.mainFile
            } as List<RFTC>
        }
        return getFilesFromRepFiles(repFiles)
    }

    /**
     * Purpose: Convenience function to convert a list of RFTC to a list of files
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    /* generics + @CompileStatic ==> https://issues.apache.org/jira/browse/GROOVY-7477 */
    protected List getFilesFromRepFiles(List<RFTC> repFiles) {
        return repFiles?.collect { RFTC it -> new File(it.path) }
    }

    /**
     * Purpose: Convenience function to extract repository files from working memory
     *
     * @param workingMemory a Map containing all objects exchanged throughout the flow.
     */
    /* generics + @CompileStatic ==> https://issues.apache.org/jira/browse/GROOVY-7477 */
    protected List getRepFiles(Map workingMemory,
            String mapName = "repository_files") {
        return (List) workingMemory.get(mapName)
    }
}
