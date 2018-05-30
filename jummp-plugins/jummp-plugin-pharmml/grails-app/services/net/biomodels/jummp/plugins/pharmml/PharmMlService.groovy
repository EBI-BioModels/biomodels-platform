/**
* Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
* Apache Tika, Apache Commons, LibPharmml, Perf4j (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Tika, Apache Commons, LibPharmml, Perf4j used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.plugins.pharmml

import eu.ddmore.libpharmml.IPharmMLResource
import eu.ddmore.libpharmml.IValidationError
import eu.ddmore.libpharmml.IValidationReport
import eu.ddmore.libpharmml.dom.PharmML
import eu.ddmore.libpharmml.dom.commontypes.AnnotationType
import eu.ddmore.libpharmml.dom.modeldefn.ModelDefinition
import eu.ddmore.libpharmml.dom.modellingsteps.ModellingSteps
import eu.ddmore.libpharmml.dom.modellingsteps.StepDependency
import eu.ddmore.libpharmml.dom.trialdesign.Population
import eu.ddmore.libpharmml.dom.trialdesign.TrialDesign
import eu.ddmore.libpharmml.dom.trialdesign.TrialStructure
import groovy.xml.QName
import groovy.xml.XmlUtil
import net.biomodels.jummp.core.IPharmMlService
import net.biomodels.jummp.core.model.FileFormatService
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.util.JummpXmlUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.tika.detect.DefaultDetector
import org.apache.tika.metadata.Metadata
import org.perf4j.aop.Profiled
import org.xml.sax.SAXException

import javax.xml.parsers.ParserConfigurationException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service class containing the logic to handle models encoded in PharmML.
 * @see net.biomodels.jummp.core.model.FileFormatService
 * @see net.biomodels.jummp.core.IPharmMlService
 * @see net.biomodels.jummp.plugins.pharmml.AbstractPharmMlHandler
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class PharmMlService implements FileFormatService {
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    static transactional = false
    private static final Map<String, String> PHARMML_COMMON_TYPES_NAMESPACES =
        ['0.2.1': 'http://www.pharmml.org/2013/03/CommonTypes',
         '0.3'  : 'http://www.pharmml.org/2013/03/CommonTypes',
         '0.3.1': 'http://www.pharmml.org/2013/03/CommonTypes',
         '0.4'  : 'http://www.pharmml.org/2013/03/CommonTypes',
         '0.4.1': 'http://www.pharmml.org/2013/03/CommonTypes',
         '0.5'  : 'http://www.pharmml.org/2013/03/CommonTypes',
         '0.5.1': 'http://www.pharmml.org/2013/03/CommonTypes',
         '0.6'  : 'http://www.pharmml.org/pharmml/0.6/CommonTypes',
         '0.6.1': 'http://www.pharmml.org/pharmml/0.6/CommonTypes',
         '0.6.2': 'http://www.pharmml.org/pharmml/0.6/CommonTypes',
         '0.7.3': 'http://www.pharmml.org/pharmml/0.7/CommonTypes',
         '0.8'  : 'http://www.pharmml.org/pharmml/0.8/CommonTypes',
         '0.8.1': 'http://www.pharmml.org/pharmml/0.8/CommonTypes'
        ]

    @Profiled(tag = "pharmMlService.validate")
    public boolean validate(List<File> model, final List<String> errors) {
        long start = System.nanoTime()
        if (!model) {
            if (IS_INFO_ENABLED) {
                String error = "Refusing to validate an undefined list of files as a PharmML submission."
                log.info error
                errors.add(error)
            }
            return false
        }
        model = model.findAll { it && it.canRead() }
        if (IS_INFO_ENABLED) {
            log.info "Validating ${model.inspect()} as a PharmML submission."
        }
        File pharmMlFile = AbstractPharmMlHandler.findPharmML(model)
        if (!pharmMlFile) {
            if (IS_INFO_ENABLED) {
                String error = "No PharmML file found in ${model.inspect()}, hence nothing to validate."
                log.info(error)
                errors.add(error)
            }
            return false
        }
        if (IS_INFO_ENABLED) {
            log.info "Asking libPharmML to validate ${pharmMlFile}."
        }
        assert pharmMlFile != null && pharmMlFile.exists() && pharmMlFile.canRead()
        long step1 = System.nanoTime()
        if (IS_INFO_ENABLED) {
            log.info "Pre-validation checks completed in ${(step1 - start) / 1000000.0}ms."
        }
        IPharmMLResource resource
        try {
            resource = AbstractPharmMlHandler.getResourceFromPharmML(pharmMlFile)
            if (!resource) {
                return false
            }
            long step2 = System.nanoTime()
            if (IS_INFO_ENABLED) {
                log.info("libPharmML validation took ${(step2 - step1) / 1000000.0}ms.")
            }
            IValidationReport report = resource.getCreationReport()
            if (IS_INFO_ENABLED) {
                final int ERR_COUNT = report.numErrors()
                if (ERR_COUNT) {
                    def err = []
                    Iterator<IValidationError> iErr = report.errorIterator()
                    while (iErr.hasNext()) {
                        IValidationError e = iErr.next()
                        String error = new StringBuffer(e.getRuleId()).append(':')
                            .append(e.getErrorMsg()).append("\n").toString();
                        err << error
                        errors.add(error)
                    }
                    log.info(err.inspect())
                }
            }
            if (IS_INFO_ENABLED) {
                log.info "Validation report check took ${(System.nanoTime() - step2) / 1000000.0}ms."
            }
            return report.isValid()
        } catch (Exception e) {
            def sb = new StringBuilder("PharmML model ${pharmMlFile} does not validate: ")
            sb.append(e.message)
            String error = sb.toString();
            log.error(error, e)
            errors.add(error)
            return false
        }
    }

    @Profiled(tag = "pharmMlService.extractName")
    public String extractName(List<File> model) {
        if (!model) {
            return ""
        }
        def sherlock = new DefaultDetector()
        def noMetadata = new Metadata()
        model = model.findAll { f ->
            if (!f.canRead()) {
                log.error "Cannot read file ${f.inspect()} while extracting PharmML model name."
                return false
            }
            String mime = sherlock.detect(new BufferedInputStream(new FileInputStream(f)), noMetadata).toString()
            return "application/xml".equals(mime)
        }
        String theName = model.inject(new StringBuilder()) { name, f ->
            final String thisName = JummpXmlUtils.findModelElement(f, "Name")
            if (thisName) {
                name.append(thisName).append(" ")
            }
        }.toString().trim()
        if (IS_INFO_ENABLED) {
            log.info("PharmML model ${model.inspect()} is entitled ${theName}")
        }
        return theName
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Profiled(tag = "pharmMlService.updateName")
    public boolean updateName(RevisionTransportCommand revision, final String NAME) {
        return updatePharmMlElement(revision, "Name",
            PHARMML_COMMON_TYPES_NAMESPACES[revision.format.formatVersion], NAME)
    }

    /**
     * Retrieves the description element from a file encoded PharmML.
     *
     * @see net.biomodels.jummp.core.model.FileFormatService#extractDescription(List)
     */
    @Profiled(tag = "pharmMlService.extractDescription")
    public String extractDescription(final List<File> model) {
        if (!model) {
            return ""
        }
        final String SEP = System.properties["line.separator"]
        def merged = model.inject(new StringBuilder()) { desc, m ->
            def dom = AbstractPharmMlHandler.getDomFromPharmML(m)
            String d = dom?.description?.value
            if (d) {
                desc.append(d).append(SEP)
            }
        }
        String theDescription = merged?.toString()?.trim() ?: ""
        if (IS_INFO_ENABLED) {
            log.info("PharmML model ${model.inspect()} has description ${theDescription}.")
        }
        return theDescription
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Profiled(tag = "pharmMlService.updateDescription")
    public boolean updateDescription(RevisionTransportCommand revision, final String DESCRIPTION) {
        List<File> revisionFiles = AbstractPharmMlHandler.fetchMainFilesFromRevision(revision)
        final File pharmML = AbstractPharmMlHandler.findPharmML(revisionFiles)
        def res = AbstractPharmMlHandler.getResourceFromPharmML(pharmML)
        def dom = res.dom
        AnnotationType current = dom.description
        if (current) {
            current.value = DESCRIPTION
            println "dom ${dom.description?.value} vs variable ${current.value}"
        } else {
            def newDesc = new AnnotationType()
            newDesc.value = DESCRIPTION
            dom.description = newDesc
        }
        AbstractPharmMlHandler.savePharmML(pharmML, res)
    }

    @Profiled(tag = "pharmMlService.getAllAnnotationURNs")
    public List<String> getAllAnnotationURNs(RevisionTransportCommand revision) {
        return []
    }

    @Profiled(tag = "pharmMlService.getPubMedAnnotation")
    public List<String> getPubMedAnnotation(RevisionTransportCommand revision) {
        return []
    }

    /**
     * Detects whether the supplied files are in the format supported by this Service
     * @param files a list of files that should be checked
     * @return true if all files are supported, false otherwise.
     * @see net.biomodels.jummp.core.model.FileFormatService#areFilesThisFormat(List files)
     */
    @Profiled(tag="pharmMlService.areFilesThisFormat")
    public boolean areFilesThisFormat(List<File> files) {
        if (!files) {
            return false
        }
        files = files.findAll {it && it.canRead()}
        def fileQueue = new ConcurrentLinkedQueue(files)
        def outcomes = new AtomicBoolean(false)
        def iFiles = fileQueue.iterator()
        def threadFactory = Executors.defaultThreadFactory()
        while (iFiles.hasNext()) {
            final File theFile = iFiles.next()
            final PharmMlDetector detector = new PharmMlDetector(theFile)
            def modelDetectorThread = threadFactory.newThread(detector)
            if (modelDetectorThread) {
                if (IS_INFO_ENABLED) {
                    log.info("Testing ${theFile.name} for pharmML.")
                }
                modelDetectorThread.setName("pharmML detector for ${theFile.name}")
                modelDetectorThread.start()
                modelDetectorThread.join()
                if (detector.isRecognisedFormat(theFile)) {
                    if (IS_INFO_ENABLED) {
                        log.info("Found PharmML namespace in ${theFile.name}")
                    }
                    return true
                }
            } else {
                //todo retry
                log.error("Cannot start a PharmML detector thread for ${theFile.properties}")
            }
        }
        return false
    }

    @Profiled(tag="pharmMlService.getFormatVersion")
    public String getFormatVersion(RevisionTransportCommand revision) {
        if (!revision) {
            log.error "Cannot get PharmML model format version from undefined revision."
            return "*"
        }
        assert revision.format.identifier == "PharmML"
        List<File> revisionFiles = AbstractPharmMlHandler.fetchMainFilesFromRevision(revision)
        final File pharmML = AbstractPharmMlHandler.findPharmML(revisionFiles)
        String version = JummpXmlUtils.findModelAttribute(pharmML, "PharmML", "writtenVersion")
        return version
    }

    @Profiled(tag="pharmMlService.getIndependentVariable")
    String getIndependentVariable(PharmML dom, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getIndependentVariable(dom)
    }

    @Profiled(tag="pharmMlService.getFunctionDefinitions")
    List getFunctionDefinitions(PharmML dom, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getFunctionDefinitions(dom)
    }

    @Profiled(tag="pharmMlService.getModelDefinition")
    ModelDefinition getModelDefinition(PharmML dom, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getModelDefinition(dom)
    }

    @Profiled(tag="pharmMlService.getCovariateModel")
    List getCovariateModel(ModelDefinition modelDefinition, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getCovariateModel(modelDefinition)
    }

    @Profiled(tag="pharmMlService.getVariabilityLevel")
    List getVariabilityModel(ModelDefinition modelDefinition, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getVariabilityModel(modelDefinition)
    }

    @Profiled(tag="pharmMlService.getParameterModel")
    List getParameterModel(ModelDefinition modelDefinition, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getParameterModel(modelDefinition)
    }

    @Profiled(tag="pharmMlService.getStructuralModel")
    List getStructuralModel(ModelDefinition modelDefinition, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getStructuralModel(modelDefinition)
    }

    @Profiled(tag="pharmMlService.getObservationModel")
    List getObservationModel(ModelDefinition modelDefinition, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getObservationModel(modelDefinition)
    }

    @Profiled(tag="pharmMlService.getTrialDesign")
    TrialDesign getTrialDesign(PharmML dom, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getTrialDesign(dom)
    }

    @Profiled(tag="pharmMlService.getTrialDesignStructure")
    TrialStructure getTrialDesignStructure(TrialDesign design, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getTrialDesignStructure(design)
    }

    @Profiled(tag="pharmMlService.getIndividualDosing")
    List getIndividualDosing(TrialDesign design, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getIndividualDosing(design)
    }

    @Profiled(tag="pharmMlService.getPopulation")
    Population getPopulation(TrialDesign design, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getPopulation(design)
    }

    @Profiled(tag="pharmMlService.getModellingSteps")
    ModellingSteps getModellingSteps(PharmML dom, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getModellingSteps(dom)
    }

    @Profiled(tag="pharmMlService.getCommonModellingSteps")
    List getCommonModellingSteps(ModellingSteps steps, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getCommonModellingSteps(steps)
    }

    @Profiled(tag="pharmMlService.getSimulationSteps")
    List getSimulationSteps(ModellingSteps steps, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getSimulationSteps(steps)
    }

    @Profiled(tag="pharmMlService.getEstimationSteps")
    List getEstimationSteps(ModellingSteps steps, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getEstimationSteps(steps)
    }

    @Profiled(tag="pharmMlService.getStepDependencies")
    StepDependency getStepDependencies(ModellingSteps steps, final String VERSION) {
        IPharmMlService handler = PharmMlVersionAwareHandlerFactory.getHandler(VERSION)
        return handler.getStepDependencies(steps)
    }

    boolean doBeforeSavingAnnotations(File annoFile, RevisionTransportCommand rev) {
        List<File> revisionFiles = AbstractPharmMlHandler.fetchMainFilesFromRevision(rev)
        final File pharmML = AbstractPharmMlHandler.findPharmML(revisionFiles)
        def res = AbstractPharmMlHandler.getResourceFromPharmML(pharmML)
        def dom = res.dom
        dom.metadataFile = annoFile.name
        AbstractPharmMlHandler.savePharmML(pharmML, res)
    }

    /*
     * Helper function that updates a given element of model encoded in PharmML.
     *
     * Specifically, it updates the XML element, as well as the corresponding attribute in
     * the {@link net.biomodels.jummp.core.model.RevisionTransportCommand}.
     *
     * @param rev The revision which is being updated. A PharmML file must be present.
     * @param elem The element which is being updated. If the element does not exist,
     *      it will be added as the first child of its immediate parent node.
     * @param elemNs The namespace of the element being updated.
     * @param value The new value of the element in question
     * @return true if the update was successful, false if gibberish was provided.
     */
    private boolean updatePharmMlElement(RevisionTransportCommand rev, String elem,
                                         String elemNs, String value) {
        if (rev && value?.trim()) {
            if (!(rev.files)) {
                log.error "Cannot update name for ${rev.properties} - it has no files."
                return false
            }
            final String VALUE = value.trim()
            List<File> revisionFiles = AbstractPharmMlHandler.fetchMainFilesFromRevision(rev)
            final File pharmML = AbstractPharmMlHandler.findPharmML(revisionFiles)
            XmlParser parser = new XmlParser(false, true)
            def root
            try {
                root = parser.parse(pharmML)
                Node item = root.children().find { it.name().getLocalPart() == elem }
                if (item) {
                    item.setValue(VALUE)
                    if (item.name().getNamespaceURI() != elemNs) {
                        item.name = new QName(elemNs, item.name().getLocalPart(),item.name().getPrefix())
                    }
                } else {
                    def elemXml = "<ct:$elem xmlns:ct=\"$elemNs\">$VALUE</ct:$elem>"
                    def parsedElem = new XmlParser(false, true).parseText(elemXml)
                    root.children().add(0, parsedElem)
                }
                XmlUtil.serialize(root, new BufferedWriter(new FileWriter(pharmML)))
                rev."${elem.toLowerCase()}" = VALUE
                return true
            } catch (SAXException | ParserConfigurationException e) {
                log.error ("""\
Cannot parse $pharmML from ${rev.properties} while setting $elem to $value:
""", e)
                return false
            }
        } else {
            log.warn("Revision ${rev.properties} is null, or $elem '$value' is empty.")
        }
        return false
    }
}
