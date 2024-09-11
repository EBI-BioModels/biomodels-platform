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
* Perf4j (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Perf4j used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.core.model.ModelElementTypeCategory as METC
import net.biomodels.jummp.core.model.ModelElementTypeTransportCommand as METTC
import net.biomodels.jummp.model.ModelElementType as MET
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.core.model.FileFormatService
import net.biomodels.jummp.core.model.ModelFormatTransportCommand as MFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.model.Revision
import org.perf4j.aop.Profiled
import net.biomodels.jummp.core.adapters.ModelFormatAdapter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.jdbc.BadSqlGrammarException

/**
 * @short Service to handle Model files.
 *
 * This service provides methods to extract information from and about a Model file.
 * It does not provide own methods but delegates the calls to the concrete service for
 * the specific ModelFormat.
 *
 * It is essential to note that this service plays the role of the factory which methods are used
 * to return a concrete object of the specific ModelFormat. This is determined by using the first
 * factory method, named 'serviceFormat'.
 *
 * Additionally the service provides methods to allow a plugin to register a new ModelFormat
 * and to tell the application which service is responsible for a format.
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * Last modified date: 14/04/2016
 */
class ModelFileFormatService implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelFileFormatService.class)

    static transactional = true
    /**
     * Dependency Injection of grails application
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication

    /**
     * <p>Extracts the representative format of the supplied @p modelFiles.</p>
     *
     * <p>Scans mime types of the supplied files to guess the typical format identifier of these files. Then,
     * looks up this identifier in the database so as to form a corresponding ModelFormatTransportCommand object.</p>
     *
     * <p><strong>Notes</strong>: the default ModelFormat representation with an empty formatVersion (i.e. the
     * formatVersion denotes as asterisk) is first looked up, since this is expected to exist for every format that is handled.</p>
     * <p><strong>Parameters:</strong></p>
     * <ul>
     *   <li>modelFiles: the list of RepositoryFileTransportCommand objects corresponding to a model</li>
     * </ul>
     * <p><strong>Returns</strong>: the corresponding model format transport command object or unknown if this cannot
     * be inferred. This command object must be converted from an specific domain object of model format so that it
     * makes sure that the inferred model format is existing one associated with an auto identifier. </p>
     */
    @Profiled(tag = "modelFileFormatService.inferModelFormat")
    MFTC inferModelFormat(List<RFTC> modelFiles) {
        if (!modelFiles) {
            return null
        }
        List<File> fileList = new LinkedList<File>()
        modelFiles.each {
            if (it.mainFile) {
                File file = new File(it.path)
                if (file.length() <= 100*1024*1024) {
                    fileList.add(file)
                } else {
                    LOGGER.debug("The model main file ${it.path} exceeds 100MB to able to detect a model format.")
                }
            }
        }
        Map<String, String> services = getServices()

        String match = services.keySet().find {
            if (it == "UNKNOWN") return false
            String serviceName = services[it]
            def ffs = grailsApplication.mainContext.getBean(serviceName, FileFormatService)
            boolean isThisFormat = ffs.areFilesThisFormat(fileList)
            isThisFormat
        }
        if (!match) {
            return new ModelFormatAdapter(format:
                ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*")).toCommandObject()
        } else {
            ModelFormat format = ModelFormat.findByIdentifierAndFormatVersion(match, "*")
            MFTC unknownVersionFormat = new ModelFormatAdapter(format: format).toCommandObject()
            RTC rev = new RTC(files: modelFiles, format: unknownVersionFormat)
            String formatVersion = getFormatVersion(rev)
            ModelFormat knownVersionFormat = ModelFormat.findByIdentifierAndFormatVersion(match, formatVersion)
            if (knownVersionFormat) {
                return new ModelFormatAdapter(format: knownVersionFormat).toCommandObject()
            }
            return unknownVersionFormat
        }
    }

    /**
     * Registers a new ModelFormat in the application if it does not yet exist.
     * If the application already knows the ModelFormat identified by @p identifier and @version
     * the existing ModelFormat is returned, otherwise a new ModelFormat is created
     * and stored in the database.
     * @param identifier The machine readable name of the ModelFormat, e.g. SBML
     * @param name A human readable name of the ModelFormat to be used in UIs.
     * @param version The version of the @p format in which the model is encoded.
     * @return Existing or new ModelFormat represented in a ModelFormatTransportCommand
     */
    @Profiled(tag = "modelFileFormatService.registerModelFormat")
    MFTC registerModelFormat(final String identifier, final String name, String version) {
        ModelFormat modelFormat = ModelFormat.findByIdentifierAndFormatVersion(identifier, version)
        if (!modelFormat) {
            modelFormat = new ModelFormat(identifier: identifier, name: name, formatVersion: version)
            modelFormat.save(flush: true)
        }
        return new ModelFormatAdapter(format:modelFormat).toCommandObject()
    }

    MFTC registerModelFormat(final String identifier, final String name) {
        return registerModelFormat(identifier, name, "*")
    }

    METTC registerModelElementType(final MFTC modelFormatTC, final String name) {
        ModelFormat modelFormat = ModelFormat.findByIdentifierAndFormatVersion(modelFormatTC.identifier, modelFormatTC.formatVersion)
        try {
            MET modelElementType = MET.findByModelFormatAndName(modelFormat, name)
            if (modelElementType) {
                use(METC) {
                    return modelElementType.toCommandObject()
                }
            } else {
                modelElementType = new MET(modelFormat: modelFormat, name: name)
                if (!modelElementType.save(flush: true)) {
                    def err = modelElementType.errors.allErrors
                    String msg = "Illegal element type $name for fmt ${modelFormat.id}: ${err}"
                    throw new IllegalArgumentException(msg)
                }
                use(METC) {
                    return modelElementType.toCommandObject()
                }
            }
        } catch (BadSqlGrammarException exception) {
            throw new IllegalStateException("Model element type table does not exist. Caused ${exception.message}")
        }
    }

    /**
     * Registers @p service to be responsible for ModelFormat identified by @p format.
     * This method can be used by a Plugin to register its service to be responsible for a
     * file format. The convention is to place templates used to display a model of
     * @p format in views/model/@p plugin
     * @param format The ModelFormat to be registered as a ModelFormatTransportCommand
     * @param service The name of the service which handles the ModelFormat.
     * @param plugin The name of the plugin (determines how the templates for the model display are loaded)
     * @throws IllegalArgumentException if the @p format has not been registered yet
     */
    @Profiled(tag = "modelFileFormatService.handleModelFormat")
    void handleModelFormat(MFTC format, String service, String controller) {
        String formatVersion = format.formatVersion ?: "*"
        ModelFormat modelFormat = ModelFormat.findByIdentifierAndFormatVersion(format.identifier, formatVersion)
        if (!modelFormat) {
            throw new IllegalArgumentException("ModelFormat ${format.properties} not registered in database")
        }
        getServices().put(format.identifier, service)
        getControllers().put(format.identifier, controller)
    }

    boolean validate(final List<File> model, String formatId, final List<String> errors) {
        return validate(model, ModelFormat.findByIdentifier(formatId), errors)
    }

    /**
     * Validates the Model file in specified @p format.
     * @param model The Model file to validate.
     * @param format The format of the Model file
     * @return @c true, if the @p model is valid, @c false otherwise
     */
    boolean validate(final List<File> model, final ModelFormat format, final List<String> errors) {
        FileFormatService service = serviceForFormat(format)
        if (service != null) {
            return service.validate(model, errors)
        } else {
            return false
        }
    }

    /**
     * Extracts the name of the Model from the @p model in specified @p format.
     * @param model The Model file to use as a source
     * @param format The format of the Model file
     * @return The name of the Model, if possible, an empty String if not possible
     */
    String extractName(final List<File> model, final ModelFormat format) {
        FileFormatService service = serviceForFormat(format)
        if (service != null) {
            return service.extractName(model)
        } else {
            return ""
        }
    }

    /**
     * Attempts to set the model name of @p revision to @p name.
     *
     * @param revision The revision that should be updated.
     * @param name The new name that the model should have.
     * @return true if the operation was successful, false otherwise.
     */
    boolean updateName(RTC revision, final String name) {
        try {
            FileFormatService service = serviceForFormat(revision.format)
            assert service
            service.updateName(revision, name)
        } catch (Exception exception) {
            LOGGER.error("Failed to update the model name. Caused ${exception.message}")
            return false
        }
    }

    /**
     * Extracts the description of the Model from the @p model in specified @p format.
     * @param model The Model file to use as a source
     * @param format The format of the Model file
     * @return The description of the Model, if possible, an empty String if not possible
     */
    String extractDescription(final List<File> model, final ModelFormat format) {
        FileFormatService service = serviceForFormat(format)
        if (service != null) {
            return service.extractDescription(model)
        } else {
            return ""
        }
    }

    /**
     * Attempts to set the model description of @p revision to @p description.
     *
     * @param revision The revision that should be updated.
     * @param name The new description that the model should have.
     * @return true if the operation was successful, false otherwise.
     */
    boolean updateDescription(RTC revision, final String description) {
        FileFormatService service = serviceForFormat(revision.format)
        assert service
        service.updateDescription(revision, description)
    }

    /**
     * Retrieves the version of the format in which {@link RTC} is encoded.
     * @param revision the RevisionTransportCommand/Revision for which to extract the format version.
     * @return The format version, or '*' if this cannot be extracted.
     */
    String getFormatVersion(def revision) {
        FileFormatService service = serviceForFormat(revision?.format)
        RTC rtc = new RevisionAdapter(revision: revision).toCommandObject()
        return service ? service.getFormatVersion(rtc) : "*"
    }

    /**
     * Retrieves all annotation URNs through the service responsible for the format used
     * by the @p revision.
     * @param rev The Revision for which all URNs should be retrieved
     * @return List of all URNs used in the Revision
     */
    List<String> getAllAnnotationURNs(Revision rev) {
        FileFormatService service = serviceForFormat(rev.format)
        if (service) {
            RTC rtc = new RevisionAdapter(revision: rev).toCommandObject()
            return service.getAllAnnotationURNs(rtc)
        } else {
            return []
        }
    }

    /**
     * Retrieves all pubmed annotations through the service responsible for the format used
     * by the @p revision.
     * @param revision The Revision for which all pubmed annotations should be retrieved
     * @return List of all pubmeds used in the Revision
     */
    List<String> getPubMedAnnotation(Revision rev) {
        FileFormatService service = serviceForFormat(rev.format)
        if (service) {
            return service.getPubMedAnnotation(new RevisionAdapter(revision: rev).toCommandObject())
        } else {
            return []
        }
    }

    List<String> getPubMedAnnotation(final RTC rev) {
        FileFormatService service = serviceForFormat(rev.format.identifier)
        if (service) {
            return service.getPubMedAnnotation(rev)
        } else {
            return []
        }
    }

    List<String> getPublicationAnnotations(final RTC rev) {
        FileFormatService service = serviceForFormat(rev.format.identifier)
        if (service) {
            return service.getPublicationAnnotations(rev)
        } else {
            return []
        }
    }

    ModellingApproach extractModellingApproachFromFiles(final RTC revision) {
        FileFormatService service = serviceForFormat(revision.format)
        if (service) {
            return service.getModellingApproach(revision)
        } else {
            return null
        }
    }

    ModellingApproach guessModellingApproachFromFiles(final File modelFile, final String formatIdentifier) {
        FileFormatService service = serviceForFormat(formatIdentifier)
        if (service) {
            return service.guessModellingApproach(modelFile)
        } else {
            return null
        }
    }

    /**
     * Used to select the templates used to display the model of the @p format provided.
     * The convention is to place the templates inside views/model/"uniquelabel". The uniquelabel
     * is specified by the format plugin during registration.
     * @param rev The Revision for which all pubmed annotations should be retrieved
     * @return The folder where template for the model display can be found
     */
    String getPluginForFormat(final MFTC format) {
        return getControllers().get(format.identifier)
    }

    /**
     * Used to select the appropriate method to do postprocessing annotations before saving them into database.
     * This selection is performed dynamically at run time thank to using Factory Method Pattern 'serviceForFormat'
     */
    @Profiled(tag = "modelFileFormatService.doBeforeSavingAnnotations")
    boolean doBeforeSavingAnnotations(File annoFile, RTC newRevision) {
        FileFormatService service = serviceForFormat(newRevision.format)
        assert service
        if (service) {
            return service.doBeforeSavingAnnotations(annoFile, newRevision)
        } else {
            return false
        }
    }

    /**
     * Helper function to get the proper service for @p format.
     *
     * @param format The ModelFormatTransportCommand/ModelFormat identifier for which the service should be returned.
     * @return The service which handles the format.
     */
    private FileFormatService serviceForFormat(final def format) {
        if (format) {
            String formatIdentifier
            if (format instanceof String) {
                formatIdentifier = format
            } else {
                formatIdentifier = format.identifier
            }
            Map<String,String> services = getServices()
            if (services.containsKey(formatIdentifier)) {
                return grailsApplication.mainContext.getBean((String) services[formatIdentifier])
            } else {
                return null
            }
        } else {
            return null
        }
    }

    private Map<String,String> getServices() {
        grailsApplication.mainContext.getBean("modelFileFormatConfig").getServices()
    }

    private Map<String,String> getControllers() {
        grailsApplication.mainContext.getBean("modelFileFormatConfig").getControllers()
    }

    @Override
    void afterPropertiesSet() throws Exception {
        log.info("Finished the bean initialisation")
    }
}
