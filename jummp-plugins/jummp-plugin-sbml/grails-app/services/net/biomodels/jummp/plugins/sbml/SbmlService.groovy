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
* JSBML, groovy, Apache Commons, JDOM, XStream, Spring Framework, Perf4j, Grails,
* SBFC Converter (or a modified version of that library), containing parts
* covered by the terms of GNU GPL v2.0, BSD license, Apache License v2.0,
* JDOM license, GNU LGPL v2.1, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of JSBML, groovy, Apache Commons,
* JDOM, XStream, Spring Framework, Perf4j, Grails, SBFC Converter used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.plugins.sbml

import com.thoughtworks.xstream.converters.ConversionException
import grails.util.Environment
import net.biomodels.jummp.core.ISbmlService
import net.biomodels.jummp.core.ModelException
import net.biomodels.jummp.core.model.FileFormatServiceAdapter
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RepoFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand as RevisionTC
import net.biomodels.jummp.model.ModellingApproach
import net.biomodels.jummp.model.PublicationLinkProvider as PubLP
import org.apache.commons.io.FileUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.plugins.codecs.URLCodec
import org.jdom.Document
import org.jdom.Element
import org.jdom.JDOMException
import org.jdom.Namespace
import org.jdom.input.SAXBuilder
import org.jdom.output.XMLOutputter
import org.jdom.xpath.XPath
import org.perf4j.aop.Profiled
import org.sbml.jsbml.*
import org.sbml.jsbml.CVTerm.Qualifier
import org.springframework.beans.factory.InitializingBean

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import java.util.regex.Pattern

//import org.sbfc.converter.models.BioPaxModel
//import org.sbfc.converter.models.OctaveModel
//import org.sbfc.converter.models.SBMLModel
//import org.sbfc.converter.sbml2biopax.SBML2BioPAX_l3
//import org.sbfc.converter.sbml2dot.SBML2Dot
//import org.sbfc.converter.sbml2octave.SBML2Octave

/**
 * Service class for handling Model files in the SBML format.
 * @author  Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author  Raza Ali <raza.ali@ebi.ac.uk>
 * @author  Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author  Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
class SbmlService extends FileFormatServiceAdapter implements ISbmlService, InitializingBean {
    static transactional = true
    def bpToModelDisplayService
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * Dependency Injection of MiriamService
     */
    def miriamService
    /**
     * Dependency injection of grails application.
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication

    /**
     * Keep one of each SBML2* converters around as it takes quite some time to load the converters.
     * These are defs because we don't want to call the static initialization code immediately.
     */
    @SuppressWarnings("GrailsStatelessService")
    private def dotConverter = null
    @SuppressWarnings("GrailsStatelessService")
    private def octaveConverter = null
    @SuppressWarnings("GrailsStatelessService")
    private def biopaxConverter = null

    // TODO: move initialization into afterPropertiesSet and make it configuration dependent
    @SuppressWarnings("GrailsStatelessService")
    /** keys are {@link net.biomodels.jummp.core.model.RevisionTC}s */
    SbmlCache cache = new SbmlCache(100)

    void afterPropertiesSet() {
        if (Environment.current == Environment.PRODUCTION) {
            // only initialize the SBML2* Converters during startup in production mode
            // FIXME: fails the startup of Tomcat server
//             sbml2dotConverter()
            //sbml2OctaveConverter()
            // FIXME: fails the startup of Tomcat server
            //sbml2BioPaxConverter()
        }
    }

    void checkConsistency(RevisionTC revision, final List<String> errors) {
        RepoFTC mainFileTC = revision.files.find {
            it.mainFile
        }
        if (!mainFileTC) {
            String modelSubmissionId = revision.model.submissionId
            String error = "SBMLModel with the identifier ${modelSubmissionId} has no a main file"
            log.debug(error)
            errors.add(error)
        } else {
            File sbmlFile = new File(mainFileTC.path)
            getFileAsValidatedSBMLDocument(sbmlFile, errors)
        }
    }

    @Override
    boolean addModelIdentifiersAsAnnotation(RevisionTC revision, String... identifiers)
            throws ModelException {
        Qualifier qualifier = Qualifier.BQM_IS
        String accessionPattern = "biomodels.db[/:](BIOMD|MODEL)[0-9]{10}"
        addAnnotations2Model(revision, qualifier, accessionPattern, identifiers)
    }

    @Override
    boolean addModellingApproachAsAnnotation(RevisionTC revision,
            ModellingApproach approach) throws ModelException {
        final Qualifier bqbHasProperty = Qualifier.BQB_HAS_PROPERTY
        String[] identifiers = ["http://identifiers.org/mamo/${approach?.accession}"] as String[]
        String accessionPattern = "mamo[/:]MAMO_[0-9]{7}"
        addAnnotations2Model(revision, bqbHasProperty, accessionPattern, identifiers)
    }

    @Override
    boolean addPublicationAsAnnotation(RevisionTC revision, PubTC publication) throws ModelException {
        final Qualifier bqmIsDescribedBy = Qualifier.BQM_IS_DESCRIBED_BY
        String pubmedPattern = "^\\d+\$"
        String doiPattern = "^(doi\\:)?10\\.\\d+/.*\$"
        String accessionPattern
        String namespace
        if (publication.linkProvider.linkType == PubLP.LinkType.PUBMED_LABEL) {
            accessionPattern = pubmedPattern
            namespace = "pubmed"
        } else if (publication.linkProvider.linkType == PubLP.LinkType.DOI_LABEL) {
            accessionPattern = doiPattern
            namespace = "doi"
        } else {
            throw new UnsupportedOperationException("BioModels only supports to add an annotation to SBML file for PubMed and DOI.")
            return false
        }
        String[] identifiers = ["http://identifiers.org/$namespace:$publication.link"] as String[]
        addAnnotations2Model(revision, bqmIsDescribedBy, accessionPattern, identifiers)
    }


    private boolean addAnnotationsIfNeeded(RevisionTC revision,
                                           SBMLDocument document,
                                           Qualifier qualifier,
                                           String accessionPattern,
                                           String... identifiers) throws ModelException {
        String rID = Objects.requireNonNull(revision).identifier()
        Model model = Objects.requireNonNull(document).model

        // resources with this pattern should be removed
        def targetAccessionPattern = Pattern.compile(accessionPattern)

        List<CVTerm> cVTerms = model.filterCVTerms(qualifier)

        if (cVTerms.isEmpty()) {
            def cvTerm = new CVTerm(qualifier, identifiers)
            if (!model.addCVTerm(cvTerm)) {
                throw new ModelException(revision.model, "Could not add a CVTerm for $identifiers")
            }
            return true
        }

        // find the set of resources that should be kept
        def filteredAnnotations = cVTerms.collect { CVTerm t ->
            t.getResources().findAll { String xref ->
                if (!targetAccessionPattern.matcher(xref).find()) {
                    return true
                }
                return false
            }
        }.flatten() as List<String>

        for (CVTerm t: cVTerms) {
            if (!model.removeCVTerm(t)) {
                throw new ModelException(revision.model, "Could not remove CVTerm $t for revision $rID")
            }
        }

        List<String> idList = Arrays.asList(identifiers)
        filteredAnnotations.addAll(idList)
        String[] replacementAnnotations = filteredAnnotations as String[]
        def replacementCVTerm = new CVTerm(qualifier, replacementAnnotations)

        boolean haveAddedNewQualifier = model.addCVTerm(replacementCVTerm)
        if (!haveAddedNewQualifier) {
            throw new ModelException(revision.model, "Could not insert qualifier for resources ${identifiers} to revision $rID")
        }
        true
    }

    private SBMLDocument getFileAsValidatedSBMLDocument(final File model, final List<String> errors) {
        // TODO: we should insert the parsed model into the cache
        String errorMsg = ""
        SBMLDocument doc
        SBMLReader reader = new SBMLReader()
        try {
            doc = reader.readSBML(model)
        } catch (XMLStreamException | NullPointerException e) {
            e.printStackTrace()
            errorMsg = "SBMLDocument could not be read from ${model.name} caused by\n${e.message}"
            log.error(errorMsg)
            errors.add(errorMsg)
            return null
        }
        if (doc == null) {
            // although the API documentation states that an Exception is thrown for incorrect files, it seems that null is returned
            errorMsg = "SBMLDocument is not valid for file ${model.name}"
            log.error(errorMsg)
            errors.add(errorMsg)
            return null
        }
        // TODO: WARNING: checkConsistency uses an online validator. This might render timeouts during model upload
        // we only check consistency as long as the model file size is less than the maximum upload file limit
        // TODO: externalise this value by defined the property,
        //  e.g. grailsApplication.config.jummp.plugins.sbml.validation.maxFileSize
        final long MAX_SIZE = 10*1024*1024 // 10MB
        long actualSize = model.length()
        if (0 >= actualSize || actualSize > MAX_SIZE) {
            errorMsg = """Your file exceeds the maximum upload size limit that our system currently supports. \
The consistency check for your model is being ignored."""
            errors.add(errorMsg)
            log.debug(errorMsg)
            return doc
        }

        if (grailsApplication.config.jummp.plugins.sbml.validation) {
            try {
                int CONSISTENCY_ERRORS = doc.checkConsistency() // by default, this method calls the online validator
                if (CONSISTENCY_ERRORS == -1) { // -1 means the online validator is unreachable
                    errorMsg = """An internal error happening while trying to reach the SBML online validator \
to validate the file ${doc.inspect()}\t${doc.properties}. \
The system has tried to call the fallback to the SBML offline validator..."""
                    println(errorMsg)
                    log.error(errorMsg)
                    CONSISTENCY_ERRORS = doc.checkConsistencyOffline()
                }
                if (CONSISTENCY_ERRORS > 0) {
                    // search for an error
                    for (SBMLError error in doc.getListOfErrors().validationErrors) {
                        if (error.isFatal() || error.isInternal() || error.isSystem() || error.isXML() || error.isError()) {
                            errorMsg = error.getMessage()
                            println(errorMsg)
                            log.debug(errorMsg)
                            errors.add(errorMsg)
                            doc = null
                            break
                        }
                    }
                }
                return doc
            } catch (ConversionException e) {
                log.error(e.getMessage(), e)
                return null
            }
        }
    }

    /**
     * Checks whether the files passed comprise a model of this format.
     *
     * @param files The files comprising a potential model of this format
     */
    boolean areFilesThisFormat(final List<File> files) {
        if (files == null || files.size() == 0) {
            return false
        }

        // let us assume that they are, and change our minds as soon as we discover the contrary
        boolean areAllSbml = true
        int iFiles = 0
        final fileCount = files.size()
        // should work with any value above 2, but sometimes there are comments at the start of the file
        final int DEPTH_LIMIT = 20
        BufferedReader reader = null
        String currentLine
        final def p = Pattern.compile(".*<sbml.(\\s*).*xmlns=\"http://www\\.sbml\\.org/sbml/level.*\".*")

        while (areAllSbml && iFiles < fileCount) {
            try {
                reader = new BufferedReader(new FileReader(files[iFiles]))
                boolean foundSbmlDeclarationLine = false
                StringBuilder sbmlHeader = new StringBuilder()
                int iLine = 0
                while (!foundSbmlDeclarationLine && iLine < DEPTH_LIMIT) {
                    currentLine = reader.readLine()
                    sbmlHeader.append(currentLine)
                    if (p.matcher(sbmlHeader.toString()).matches()) {
                        foundSbmlDeclarationLine = true
                        break
                    } else {
                        iLine++
                    }
                }
                areAllSbml &= foundSbmlDeclarationLine
            } catch(IOException ex) {
                def msg = new StringBuffer("""\
Could not check if SBML files ${files.inspect()} are valid or not.""")
                msg.append(" Encountered $ex while reading line $currentLine of file ${files[iFiles]}")
                log.error(msg.toString())
                return false
            } finally {
                reader?.close()
                iFiles++
            }
        }
        return areAllSbml
    }

    private SBMLDocument getDocumentFromFiles(final List<File> model, final List<String> errors  = []){
        SBMLDocument retval = null
        model.each {
            try {
                SBMLDocument doc  = getFileAsValidatedSBMLDocument(it, errors)
                if (doc) {
                    retval = doc
                }
            } catch(Exception e) {
                log.error(e.message, e)
            }
        }
        return retval
    }

    @Profiled(tag="SbmlService.validate")
    boolean validate(final List<File> model, final List<String> errors) {
        if (!grailsApplication.config.jummp.plugins.sbml.validation) {
            log.info("Validation for ${model.inspect()} skipped due to configuration option")
            return true
        }
        if (getDocumentFromFiles(model, errors)) {
            return true
        }
        return false
    }

    /**
     * Extracts the name of a model from a list of SBML files.
     *
     * Note that all files with the exception of the first one are ignored.
     * @param model A list of files in SBML format
     * @return the name of the model or an empty string if no file is supplied.
     */
    @Profiled(tag="SbmlService.extractName")
    String extractName(List<File> model) {
        model = model.findAll{it && it.exists() && it.canRead()}
        if (!model) {
            return ""
        }
        String name = findModelAttribute(model.first(), "model", "name")
        return name ? name : ""
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Profiled(tag="sbmlService.updateName")
    boolean updateName(RevisionTC revision, final String name) {
        if (revision && name.trim()) {
            // update the name of the revision
            revision.name = name.trim()

            // update the name of SBML model file of the revision
            SBMLDocument sbmlDocument = getFromCache(revision)
            Model sbmlModel = sbmlDocument.getModel()
            if (!sbmlModel) {
                log.error("Cannot update the model name for the main model file of the revision: ${revision.dump()}")
                return false
            }
            sbmlModel.setName(name)
            File sbmlFile = fetchMainFileFromRevision(revision)
            SBMLWriter sbmlWriter = new SBMLWriter()
            sbmlWriter.writeSBML(sbmlDocument, sbmlFile)
            return true
        } else {
            log.warn("""\
Revision ${revision.id} of the model ${revision.model.submissionId} is null or
the user has attempted to update an blank value for the name attribute.""")
            return false
        }
    }

    Map extractComponentsFromBP(String modelId) throws IOException {
        return bpToModelDisplayService.getComponentsFromBP(modelId)
    }
    /**
     * Extracts the SBML model notes.
     *
     * Note that all files with the exception of the first one are ignored.
     * @param model A list of files in SBML format
     * @return the description of the model or an empty string if no file is supplied.
     */
    @Profiled(tag="SbmlService.extractDescription")
    String extractDescription(final List<File> model) {
        if (!model) {
            String errMsg = "Cannot extract the description from undefined file ${model.properties}"
            log.warn(errMsg)
            return ""
        }
        def description = new StringBuffer()
        def nsList = ["http://www.sbml.org/sbml/level2/version5",
                      "http://www.sbml.org/sbml/level2/version4",
                      "http://www.sbml.org/sbml/level2/version3",
                      "http://www.sbml.org/sbml/level2/version2",
                      "http://www.sbml.org/sbml/level2",
                      "http://www.sbml.org/sbml/level1"
        ]
        try {
            // find the namespace without loading the file
            def builder = new SAXBuilder()
            Document doc = builder.build(model.first())

            for (String namespace: nsList) {
                Namespace ns = Namespace.getNamespace("ns", namespace)
                XPath xpath = XPath.newInstance("/ns:sbml/ns:model/ns:notes")
                xpath.addNamespace(ns)
                List<Element> noteElements = xpath.selectNodes(doc)
                if (! noteElements.isEmpty()) {
                    for (Element elt: noteElements) {
                        // required step as 'notes' contains further XML tags
                        XMLOutputter xmlOut = new XMLOutputter()
                        description.append(xmlOut.outputString(elt))
                    }
                    // no need to try other namespaces
                    break
                }
            }
        } catch (JDOMException e) {
            String errMsg ="Exception encountered while extracting description from ${model.inspect()}: ${e.message}"
            log.error(errMsg, e)
            return ""
        } catch (IOException e) {
            String errMsg = "IOException encountered while extracting description from ${model.inspect()}: ${e.message}"
            log.error(errMsg, e)
            return ""
        }
        return description.toString()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Profiled(tag="sbmlService.updateDescription")
    boolean updateDescription(RevisionTC revision, final String DESC) {
        if (revision && DESC.trim()) {
            // update the description of SBML model file of the revision
            revision.description = DESC.trim()

            // update the description of SBML model file of the revision
            SBMLDocument sbmlDocument = getFromCache(revision)
            Model sbmlModel = sbmlDocument.getModel()
            sbmlModel.setNotes(DESC)
            File sbmlFile = fetchMainFileFromRevision(revision)
            SBMLWriter sbmlWriter = new SBMLWriter()
            sbmlWriter.writeSBML(sbmlDocument, sbmlFile)
            return true
        }
        return false
    }

    @Profiled(tag="SbmlService.getMetaId")
    String getMetaId(RevisionTC revision) {
        return getFromCache(revision)?.model?.metaId
    }

    @Profiled(tag="SbmlService.getVersion")
    long getVersion(RevisionTC revision) {
        return fetchModelAttributeFromRevision(revision, "version")
    }

    @Profiled(tag="SbmlService.getLevel")
    long getLevel(RevisionTC revision) {
        return fetchModelAttributeFromRevision(revision, "level")
    }

    @Profiled(tag="SbmlService.getFormatVersion")
    String getFormatVersion(RevisionTC revision) {
        final long LEVEL = getLevel(revision)
        final long VERSION = getVersion(revision)
        return "L${LEVEL}V${VERSION}"
    }

    @Profiled(tag="SbmlService.getNotes")
    String getNotes(RevisionTC revision) {
        String notesString = getFromCache(revision)?.model?.notesString ?: ""
        return notesString
    }

    @Profiled(tag="SbmlService.getAnnotations")
    List<Map> getAnnotations(RevisionTC revision) {
        Model model = getFromCache(revision).model
        return convertCVTerms(model.annotation)
    }

    @Profiled(tag="SbmlService.getParameters")
    List<Map> getParameters(RevisionTC revision) {
        Model model = getFromCache(revision).model
        ListOf<Parameter> parameters = model.getListOfParameters()
        List<Map> list = []
        parameters.each { parameter ->
            list << parameterToMap(parameter)
        }
        return list
    }

    @Profiled(tag="SbmlService.getParameter")
    Map getParameter(RevisionTC revision, String id) {
        Model model = getFromCache(revision).model
        QuantityWithUnit param = model.getParameter(id)
        if (!param) {
            param = (QuantityWithUnit)model.findLocalParameters(id).find { it.id == id }
        }
        if (!param) {
            return [:]
        }
        Map map = parameterToMap(param)
        map.put("notes", param.getNotesString())
        map.put("annotation", convertCVTerms(param.annotation))
        return map
    }

    @Profiled(tag="SbmlService.getLocalParameters")
    List<Map> getLocalParameters(RevisionTC revision) {
        Model model = getFromCache(revision).model
        List<Map> reactions = []
        model.listOfReactions.each { reaction ->
            List<Map> localParameters = []
            reaction.kineticLaw?.getListOfLocalParameters()?.each { parameter ->
                Map map = parameterToMap(parameter)
                localParameters << map
            }
            reactions << [id: reaction.id, name: reaction.name, parameters: localParameters]
        }
        return reactions
    }

    @Profiled(tag="SbmlService.getReactions")
    List<Map> getReactions(RevisionTC revision) {
        Model model = getFromCache(revision).model
        List<Map> reactions = []
        model.listOfReactions.each { reaction ->
            reactions << reactionToMap(reaction)
        }
        return reactions
    }

    @Profiled(tag="SbmlService.getReaction")
    Map getReaction(RevisionTC revision, String id) {
        Model model = getFromCache(revision).model
        Reaction reaction = model.getReaction(id)
        if (!reaction) {
            return [:]
        }
        Map reactionMap = reactionToMap(reaction)
        reactionMap.put("annotation", convertCVTerms(reaction.annotation))
        reactionMap.put("math", reaction.kineticLaw ? reaction.kineticLaw.mathMLString : "")
        reactionMap.put("notes", reaction.notesString)
        return reactionMap
    }

    @Profiled(tag="SbmlService.getEvents")
    List<Map> getEvents(RevisionTC revision) {
        Model model = getFromCache(revision).model
        List<Map> events = []
        model.listOfEvents.each { event ->
            events << eventToMap(event)
        }
        return events
    }

    @Profiled(tag="SbmlService.getEvent")
    Map getEvent(RevisionTC revision, String id) {
        Model model = getFromCache(revision).model
        Event event = model.getEvent(id)
        Map eventMap = eventToMap(event)
        eventMap.put("annotation", convertCVTerms(event.annotation))
        eventMap.put("notes", event.notesString)
        eventMap.put("sbo", sboName(event))
        eventMap.put("trigger", event.trigger ? event.trigger.mathMLString : "")
        eventMap.put("delay", event.delay ? event.delay.mathMLString : "")
        return eventMap
    }

    @Profiled(tag="SbmlService.getRules")
    List<Map> getRules(RevisionTC revision) {
        Model model = getFromCache(revision).model
        List<Map> rules = []
        model.listOfRules.each { rule ->
            rules << ruleToMap(rule)
        }
        return rules
    }

    @Profiled(tag="SbmlService.getRule")
    Map getRule(RevisionTC revision, String variable) {
        Model model = getFromCache(revision).model
        ExplicitRule rule = model.getRuleByVariable(variable)
        if (!rule) {
            return [:]
        }
        Map ruleMap = ruleToMap(rule)
        ruleMap.put("annotation", convertCVTerms(rule.annotation))
        ruleMap.put("notes", rule.notesString)
        return ruleMap
    }

    List<Map> getFunctionDefinitions(RevisionTC revision) {
        Model model = getFromCache(revision).model
        List<Map> functions = []
        model.listOfFunctionDefinitions.each { function ->
            functions << functionDefinitionToMap(function)
        }
        return functions
    }

    Map getFunctionDefinition(RevisionTC revision, String id) {
        Model model = getFromCache(revision).model
        FunctionDefinition function = model.getFunctionDefinition(id)
        if (!function) {
            return [:]
        }
        Map functionMap = functionDefinitionToMap(function)
        functionMap.put("annotation", convertCVTerms(function.annotation))
        functionMap.put("notes", function.notesString)
        functionMap.put("sbo", sboName(function))
        return functionMap
    }

    @Profiled(tag="SbmlService.getCompartments")
    List<Map> getCompartments(RevisionTC revision) {
        Model model = getFromCache(revision).model
        List<Map> compartments = []
        model.listOfCompartments.each { compartment ->
             compartments << compartmentToMap(compartment)
        }
        return compartments
    }

    @Profiled(tag="SbmlService.getCompartment")
    Map getCompartment(RevisionTC revision, String id) {
        Model model = getFromCache(revision).model
        Compartment compartment = model.getCompartment(id)
        if(!compartment) {
            return [:]
        }
        Map compartmentMap = compartmentToMap(compartment)
        compartmentMap.put("annotation", convertCVTerms(compartment.annotation))
        compartmentMap.put("notes", compartment.notesString)
        return compartmentMap
    }

    @Profiled(tag="SbmlService.getAllSpecies")
    List<Map> getAllSpecies(RevisionTC revision) {
        Model model = getFromCache(revision).model
        List<Map> allSpecies = []
        model.listOfSpecies.each { species ->
            allSpecies << speciesToMap(species)
        }
        return allSpecies
    }

    @Profiled(tag="SbmlService.getAllCompartmentSpecies")
    private List<Map> getAllCompartmentSpecies(Compartment compartment) {
        Model model = compartment.model
        List<Map> allSpecies = []
        model.listOfSpecies.each { species ->
            if (species.compartmentInstance == compartment) {
                allSpecies << speciesToMap(species)
            }
        }
        return allSpecies
    }

     @Profiled(tag="SbmlService.getSpecies")
     Map getSpecies(RevisionTC revision, String id) {
         Model model =getFromCache(revision).model
         Species species = model.getSpecies(id)
         if(!species) {
             return [:]
         }
         Map speciesMap = speciesToMap(species)
         speciesMap.put("annotation", convertCVTerms(species.annotation))
         speciesMap.put("notes", species.notesString)
         return speciesMap
     }

    @Profiled(tag="SbmlService.generateSvg")
    byte[] generateSvg(RevisionTC revision) {
        File dotFile = File.createTempFile("jummp", "dot")
        PrintWriter writer = new PrintWriter(dotFile)
        sbml2dotConverter().dotExport(getFromCache(revision), writer)
        File svgFile = File.createTempFile("jummp", "svg")
        def process = "dot -Tsvg -o ${svgFile.absolutePath} ${dotFile.absolutePath}".execute()
        process.waitFor()
        FileUtils.deleteQuietly(dotFile)
        if (process.exitValue()) {
            FileUtils.deleteQuietly(svgFile)
            return new byte[0]
        }
        byte[] bytes = svgFile.readBytes()
        FileUtils.deleteQuietly(svgFile)
        return bytes
    }

    @Profiled(tag="SbmlService.generateOctave")
    String generateOctave(RevisionTC revision) {
//        SBMLModel sbmlModel = resolveSbmlModel(revision)
//        OctaveModel octaveModel = sbml2OctaveConverter().octaveExport(sbmlModel)
//        return octaveModel.modelToString()
        return ""
    }

    @Profiled(tag="SbmlService.generateBioPax")
    String generateBioPax(RevisionTC revision) {
//        SBMLModel sbmlModel = resolveSbmlModel(revision)
//        BioPaxModel bioPaxModel = sbml2BioPaxConverter().biopaxexport(sbmlModel)
//        return bioPaxModel.modelToString()
        return ""
    }

    @Profiled(tag="SbmlService.getAllAnnotationURNs")
    List<String> getAllAnnotationURNs(RevisionTC revision) {
        SBMLDocument document = getFromCache(revision)
        List<String> urns = []
        List<SBase> sbases = []
        sbases.addAll(document.model.listOfCompartments)
        sbases.addAll(document.model.listOfConstraints)
        sbases.addAll(document.model.listOfEvents)
        sbases.addAll(document.model.listOfFunctionDefinitions)
        sbases.addAll(document.model.listOfInitialAssignments)
        sbases.addAll(document.model.listOfParameters)
        sbases.addAll(document.model.listOfReactions)
        sbases.addAll(document.model.listOfRules)
        sbases.addAll(document.model.listOfSpecies)
        sbases.addAll(document.model.listOfUnitDefinitions)
        sbases << document.model
        sbases.each { sbase ->
            sbase.annotation.listOfCVTerms.each { cvTerm ->
                cvTerm.resources.each {
                    urns << it
                }
            }
        }
        return urns
    }

    @Profiled(tag="SbmlService.getPubMedAnnotation")
    List<String> getPubMedAnnotation(RevisionTC revision) {
        List<CVTerm> filters = getIsDescribedByAnnotations(revision)
        List<String> pubMedAnnotation = []
        filters.each { filter ->
            CVTerm cvTerm = new CVTerm(filter)
            pubMedAnnotation.addAll(cvTerm.filterResources("pubmed"))
        }
        return pubMedAnnotation
    }

    @Profiled(tag="SbmlService.getPublicationAnnotations")
    List<String> getPublicationAnnotations(RevisionTC revision) {
        List<String> annotations = []
        List<CVTerm> filters = getIsDescribedByAnnotations(revision)
        filters.each { filter ->
            CVTerm cvTerm = new CVTerm(filter)
            annotations.addAll(cvTerm.filterResources("pubmed", "PubMed", "doi", "DOI"))
        }
        return annotations
    }

    private List<CVTerm> getIsDescribedByAnnotations(RevisionTC revision) {
        Model model = getFromCache(revision)?.model
        Annotation annotation = model?.annotation
        if(!annotation) {
            return null
        }
        List<CVTerm> bqbiolIsDescribedByTerms = annotation.filterCVTerms(Qualifier.BQB_IS_DESCRIBED_BY)
        List<CVTerm> bqmodelIsDescribedByTerms = annotation.filterCVTerms(Qualifier.BQM_IS_DESCRIBED_BY)
        List<CVTerm> filters = bqbiolIsDescribedByTerms + bqmodelIsDescribedByTerms
        return filters
    }

    /**
     * Returns the SBMLDocument for the @p revision from the cache.
     * If the cache does not contain the SBMLDocument, the model file is
     * retrieved, parsed and inserted into the Cache.
     * @param revision The revision for which the SBMLDocument needs to be retrieved
     * @return The parsed SBMLDocument
     */
    private SBMLDocument getFromCache(RevisionTC revision) throws XMLStreamException {
        SBMLDocument document = cache.get(revision.id)
        if (document) {
            return document
        }
        //SBMLDocument document=null;
        // we do not have a document, so retrieve first the file
        //List<RepoFTC> files = grailsApplication.mainContext.getBean("modelDelegateService").retrieveModelFiles(revision)
        List<RepoFTC> files = revision.files
        files = files.findAll { it.mainFile }

        files.each {
            def bis
            try {
                File file=new File(it.path)
                byte[] fileBytes = file.getBytes()
                bis = new ByteArrayInputStream(fileBytes)
                def reader = new SBMLReader()
                document = reader.readSBML(file)
                if (document) {
                  cache.put(revision.id, document)
                  //break
                }
            } catch(Exception ignore) {
                ignore.printStackTrace();
            } finally {
                bis?.close()
            }
        }
        return document
    }

    private Map parameterToMap(QuantityWithUnit parameter) {
        return [
                id: parameter.id,
                name: parameter.name,
                metaId: parameter.metaId,
                constant: (parameter instanceof Parameter) ? parameter.constant : true,
                value: parameter.isSetValue() ? parameter.value : null,
                sbo: sboName(parameter),
                unit: parameter.units
        ]
    }

    private List<Map> convertCVTerms(Annotation annotation) {
        List<Map> list = []
        annotation.listOfCVTerms.each { cvTerm ->
            list << [
                    qualifier: cvTerm.biologicalQualifier ? cvTerm.biologicalQualifierType.toString() : (cvTerm.modelQualifier ? cvTerm.modelQualifierType.toString() : ""),
                    biologicalQualifier: cvTerm.biologicalQualifier,
                    modelQualifier: cvTerm.modelQualifier,
                    resources: cvTerm.resources.collect {
                        Map data = miriamService.miriamData(it)
                        data.put("urn", it)
                        data
                    }
            ]
        }
        return list
    }

    private List<Map> convertSpeciesReferences(List<SimpleSpeciesReference> list) {
        List<Map> species = []
        list.each {
            if (it instanceof SpeciesReference) {
                species << speciesReferenceToMap(it)
            } else {
                species << [species: it.species, speciesName: it.model.getSpecies(it.species).name]
            }
        }
        return species
    }

    private Map speciesReferenceToMap(SpeciesReference reference) {
        return [
                species: reference.species,
                speciesName: reference.model.getSpecies(reference.species).name,
                constant: reference.constant,
                stoichiometry: reference.stoichiometry
        ]
    }

    private Map reactionToMap(Reaction reaction) {

        return [
                id: reaction.id,
                metaId: reaction.metaId,
                name: reaction.name,
                reversible: reaction.reversible,
                sbo: sboName(reaction),
                reactants: convertSpeciesReferences(reaction.listOfReactants),
                products: convertSpeciesReferences(reaction.listOfProducts),
                modifiers: convertSpeciesReferences(reaction.listOfModifiers)
        ]
    }

    private Map eventToMap(Event event) {
        return [
                id: event.id,
                metaId: event.metaId,
                name: event.name,
                assignments: eventAssignmentsToList(event.listOfEventAssignments)
        ]
    }

    private List<Map> eventAssignmentsToList(List<EventAssignment> assignments) {
        List<Map> eventAssignments = []
        assignments.each { assignment ->
            Symbol symbol = assignment.model.findSymbol(assignment.variable)
            eventAssignments << [
                    meataId: assignment.metaId,
                    math: assignment.mathMLString,
                    variableId: assignment.variable,
                    variableName: symbol ? symbol.name : "",
                    variableType: symbol ? symbol.elementName : ""
            ]
        }
        return eventAssignments
    }

    private Map ruleToMap(Rule rule) {
        String type = null
        Variable symbol = null
        if (rule instanceof RateRule) {
            type = "rateShow"
            symbol = rule.model.findSymbol(rule.variable)
        } else if (rule instanceof AssignmentRule) {
            type = "assignment"
            symbol = rule.model.findSymbol(rule.variable)
        } else if (rule instanceof AlgebraicRule) {
            type = "algebraic"
            symbol = rule.derivedVariable
        }

        return [
                metaId: rule.metaId,
                math: rule.getMathMLString(),
                variableId : symbol ? symbol.id : null,
                variableName: symbol ? symbol.name : null,
                variableType: symbol ? symbol.elementName : null,
                type: type
        ]
    }

    private Map functionDefinitionToMap(FunctionDefinition function) {
        return [
                id: function.id,
                name: function.name,
                metaId: function.metaId,
                math: function.mathMLString
        ]
    }

    private Map compartmentToMap(Compartment compartment) {
        return [
                metaId: compartment.metaId,
                id: compartment.id,
                name: compartment.name,
                size: compartment.size,
                spatialDimensions: compartment.getSpatialDimensions(),
                units: compartment.units,
                sbo: sboName(compartment),
                allSpecies: getAllCompartmentSpecies(compartment)
        ]
    }

    private Map speciesToMap(Species species) {
        def initialAmount
        def initialConcentration
        if (species.isSetInitialAmount()) {
            initialAmount = species.initialAmount
        } else {
            initialAmount = null
        }
        if (species.isSetInitialConcentration()) {
            initialConcentration = species.initialConcentration
        } else {
            initialConcentration = null
        }
        return [
                metaid: species.metaId,
                id: species.id,
                compartment: species.compartment,
                initialAmount: initialAmount,
                initialConcentration: initialConcentration,
                substanceUnits: species.substanceUnits,
                sbo: sboName(species)
        ]
    }

    /**
     * Retrieves the name of the SBOTerm in the given @p sbase.
     * @param sbase The sbase from which to extract the SBO Term name.
     * @return The name or an empty String if there is no name
     */
    private Map sboName(SBase sbase) {
        try {
            String name = SBO.getTerm(sbase.getSBOTerm()).name
            Map map = miriamService.miriamData("urn:miriam:obo.sbo:${URLCodec.encode(sbase.getSBOTermID())}")
            map.put("name", name)
            return map
        } catch (NoSuchElementException e) {
            return [:]
        }
    }

//    private SBML2Dot sbml2dotConverter() {
//        if (!dotConverter) {
//            dotConverter = new SBML2Dot()
//        }
//        return dotConverter
//    }
//
//    private SBML2Octave sbml2OctaveConverter() {
//        if (!octaveConverter) {
//            octaveConverter = new SBML2Octave()
//        }
//        return octaveConverter
//    }
//
//    private SBML2BioPAX_l3 sbml2BioPaxConverter() {
//        if (!biopaxConverter) {
//            biopaxConverter = new SBML2BioPAX_l3()
//        }
//        return biopaxConverter
//    }

    private long fetchModelAttributeFromRevision(RevisionTC revision, String attributeName) {
        File mainFile = fetchMainFileFromRevision(revision)
        if (!mainFile) {
            //we have already logged this error, just return
            return 0
        }
        String attributeValue = findModelAttribute(mainFile, "sbml", attributeName)
        return longFromString(attributeValue, attributeName, mainFile)
    }

    private File fetchMainFileFromRevision(RevisionTC revision) {
        final String mainFileLocation = revision?.files?.find {it.mainFile}?.path
        if (!mainFileLocation) {
            log.error "The main file of revision ${revision.properties} is undefined."
            return null
        }
        File mainFile = new File(mainFileLocation)
        if (!mainFile || !mainFile.canRead()) {
            def errMsg = new StringBuilder("None of the files ").append(revision?.files?.inspect()).
                        append(" of revision ").append(revision.properties).append("is a main file.")
            log.error errMsg.toString()
            return null
        }
        return mainFile
    }

    private String findModelAttribute(final File model, String elementName, String attributeName) {
        if (null == elementName) {
            elementName = ""
        }
        if (null == attributeName) {
            attributeName = ""
        }
        if (IS_INFO_ENABLED) {
            def info = new StringBuilder("Extracting attribute ").append(attributeName).
                        append(" of element ").append(elementName).append(" from ").append(model.properties)
            log.info(info.toString())
        }
        String theResult
        def fileReader = new FileReader(model)
        XMLInputFactory factory = XMLInputFactory.newInstance()
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE)
        XMLStreamReader xmlReader
        try {
            xmlReader = factory.createXMLStreamReader(fileReader)
            boolean found = false
            while (xmlReader.hasNext() && !found) {
                xmlReader.next()
                if (xmlReader.startElement) {
                    String result = processXmlElement(xmlReader, elementName, attributeName)
                    if (result) {
                        theResult = result
                        found = true
                    } else {
                        xmlReader.next()
                    }
                }
            }
        } catch (XMLStreamException e) {
            def errorMsg = new StringBuilder("Error while extracting property ").append(elementName).
                        append(".").append(attributeName).append(" from ").append(model.properties)
            errorMsg.append(". The offending file caused ${e.message}.\n")
            log.error (errorMsg.toString(), e)
        } finally {
            xmlReader?.close()
            fileReader?.close()
            return theResult
        }
    }

    private String processXmlElement(XMLStreamReader reader, String element, String attribute) {
        if (element.equals(reader.getLocalName())) {
            return reader.getAttributeValue(null, attribute)
        }
        return null
    }

    /**
     * Convenience method for handling conversions from String to long.
     * @param value the value that should be converted to long
     * @param name the name of attribute whose value is being converted. Used in the log messages.
     * @param mainFile the name of the file from which the attribute was extracted. Used in the log messages.
     * @return the value of the attribute as a long, or zero if the conversion fails.
     */
    private long longFromString(String value, String name, File mainFile) {
        long level = 0
        try {
            level = value as long
        } catch (NumberFormatException e) {
            def errMsg = new StringBuilder("Error extracting model attribute from ").append(mainFile.properties).
                        append(". ").append(name). append(" ").append(value).append(" is not a number.")
            log.error errMsg.toString(), e
        } finally {
            return level
        }
    }

    /**
     * Resolves the SBMLModel from the given @p revision.
     * @param revision The RevisionTC from which to extract the SBMLModel.
     * @return The SBMLModel to be found or an empty array if the model could not be found.
     */
//    private SBMLModel resolveSbmlModel(RevisionTC revision) {
//        try {
//        Model model = getFromCache(revision).model
//        SBMLWriter sbmlWriter = new SBMLWriter()
//        String sbmlString = sbmlWriter.writeSBMLToString(model.getSBMLDocument())
//        SBMLModel sbmlModel = new SBMLModel()
//        sbmlModel.setModelFromString(sbmlString)
//        return sbmlModel
//        } catch (Exception e) {
//            e.printStackTrace()
//            return [:]
//        }
//    }

    /**
     * Triggers the generation of a sub model for an existing model encoded in SBML.
     *
     * @return A String representation of the new model encoded in SBML.
     */
    String triggerSubmodelGeneration(
            RevisionTC revision, String subModelId, String metaId,
            List<String> compartmentIds, List<String> speciesIds, List<String> reactionIds,
            List<String> ruleIds, List<String> eventIds) {
        Model model = getFromCache(revision).model
        return new SubmodelGenerator().generateSubModel(
                model, subModelId, metaId, compartmentIds, speciesIds, reactionIds, ruleIds, eventIds)
    }

    boolean doBeforeSavingAnnotations(File annoFile, RevisionTC rev) {
        return true
    }

    @Override
    ModellingApproach getModellingApproach(final RevisionTC revision) {
        SBMLDocument document = getFromCache(revision)
        def rID = revision.identifier() ? "revision ${revision.identifier()}" : "the provisional revision in the new submission"
        guessModellingApproachFromSBMLDocument(document, rID)
    }

    ModellingApproach guessModellingApproach(final File modelFile) {
        List<String> errors = new ArrayList<>()
        SBMLDocument document = getFileAsValidatedSBMLDocument(modelFile, errors)
        String rID = modelFile.name
        guessModellingApproachFromSBMLDocument(document, rID)
    }

    private ModellingApproach guessModellingApproachFromSBMLDocument(final SBMLDocument document, final String rID) {
        if (null == document) {
            log.error("Cannot extract modelling approach from $rID as we could not parse its main files")
            return null
        } else {
            Model model = document.model
            Annotation annotation = model?.annotation
            if(!annotation) {
                return null
            }
            List<CVTerm> filters = annotation.filterCVTerms(Qualifier.BQB_HAS_PROPERTY)
            List<String> mamoTerms = []
            filters.each { filter ->
                CVTerm cvTerm = new CVTerm(filter)
                List<String> resources = cvTerm.filterResources("MAMO", "mamo")
                if (!resources.isEmpty()) {
                    mamoTerms.addAll(resources)
                }
            }
            if (mamoTerms.isEmpty()) {
                log.info("No modelling approach MAMO terms found in $rID")
                return null
            }
            def first = mamoTerms.find { it != null && !it?.isEmpty() }
            if (!first) {
                log.warn("Expected to have MAMO terms. Bug in JSBML filterCVTerms")
                return null
            }
            String[] parts = first.split("/mamo/")
            if (!parts[0] || parts.length != 2) {
                log.warn("Revision $rID has invalid modelling approach '$first'")
                return null
            }
            ModellingApproach approach = ModellingApproach.findByResourceOrAccession(first, parts[1])
            log.info("Revision $rID declares modelling approach ${approach?.name}")
            return approach
        }
    }
    /**
     * Add one or multiple annotations having the same biological qualifier to a given model.
     *
     * This utility is used to add annotations in a batch mode. The requirement is that these annotations
     * have to go with the identical biological qualifier.
     *
     * @param revision  The Revision instance denoting the given model
     * @param qualifier The Qualifier instance denoting the biological qualifier
     * @param identifiers   The list of identifiers.org based URLs denoting the input annotations
     * @return a boolean value indicating whether the service finished successfully or failed.
     */
    private boolean addAnnotations2Model(RevisionTC revision,
                                         Qualifier qualifier,
                                         String accessionPattern,
                                         String... identifiers) {
        boolean validRevision = revision && "SBML".equals(revision.format.identifier)
        boolean validIdentifiers = null != identifiers && 0 != identifiers.length
        if (!validIdentifiers || !validRevision) {
            String msg = """A revision whose main files are encoded in SBML and at least one model \
identifier are required"""
            throw new IllegalArgumentException(msg)
            return false
        }
        SBMLDocument document = getFromCache(revision)
        def rID = revision.identifier() ? "revision ${revision.identifier()}" : "the provisional revision in the new submission"
        if (null == document) {
            log.error("Cannot add $identifiers to the main files of the revision $rID as we could not parse its main files")
            return false
        }

        boolean needsUpdating = addAnnotationsIfNeeded(revision, document, qualifier, accessionPattern, identifiers)
        if (!needsUpdating) {
            return false
        }
        File sbmlFile = fetchMainFileFromRevision(revision)
        SBMLWriter sbmlWriter = new SBMLWriter()
        try {
            sbmlWriter.writeSBML(document, sbmlFile)
            return true
        } catch (SBMLException | IOException | XMLStreamException e) {
            def fn = sbmlFile.name
            def msg = """Failed to add model annotations $identifiers to file $fn of revision $rID \
due to an issue with JSBML"""
            log.error "$msg: $e"
            throw new ModelException(revision.model, msg)
        }
    }
}
