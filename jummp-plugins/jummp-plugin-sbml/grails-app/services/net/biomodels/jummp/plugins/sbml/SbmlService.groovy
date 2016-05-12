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
import java.util.regex.Pattern
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import net.biomodels.jummp.core.IMetadataService
import net.biomodels.jummp.core.ISbmlService
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand
import net.biomodels.jummp.core.annotation.StatementTransportCommand
import net.biomodels.jummp.core.model.FileFormatService
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
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
import org.sbfc.converter.models.BioPaxModel
import org.sbfc.converter.models.OctaveModel
import org.sbfc.converter.models.SBMLModel
import org.sbfc.converter.sbml2biopax.SBML2BioPAX_l3
import org.sbfc.converter.sbml2dot.SBML2Dot
import org.sbfc.converter.sbml2octave.SBML2Octave
import org.sbml.jsbml.AlgebraicRule
import org.sbml.jsbml.Annotation
import org.sbml.jsbml.AssignmentRule
import org.sbml.jsbml.CVTerm
import org.sbml.jsbml.Compartment
import org.sbml.jsbml.Event
import org.sbml.jsbml.EventAssignment
import org.sbml.jsbml.ExplicitRule
import org.sbml.jsbml.FunctionDefinition
import org.sbml.jsbml.ListOf
import org.sbml.jsbml.Model
import org.sbml.jsbml.Parameter
import org.sbml.jsbml.QuantityWithUnit
import org.sbml.jsbml.RateRule
import org.sbml.jsbml.Reaction
import org.sbml.jsbml.Rule
import org.sbml.jsbml.SBMLDocument
import org.sbml.jsbml.SBMLError
import org.sbml.jsbml.SBMLReader
import org.sbml.jsbml.SBMLWriter
import org.sbml.jsbml.SBO
import org.sbml.jsbml.SBase
import org.sbml.jsbml.SimpleSpeciesReference
import org.sbml.jsbml.Species
import org.sbml.jsbml.SpeciesReference
import org.sbml.jsbml.Symbol
import org.sbml.jsbml.Variable
import org.springframework.beans.factory.InitializingBean

/**
 * Service class for handling Model files in the SBML format.
 * @author  Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author  Raza Ali <raza.ali@ebi.ac.uk>
 * @author  Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class SbmlService implements FileFormatService, ISbmlService, InitializingBean {
    static transactional = true
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * Dependency Injection of MiriamService
     */
    def miriamService
    /**
     * Dependency Injection of metadata delegate service.
     */
    IMetadataService metadataDelegateService
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
    SbmlCache<RevisionTransportCommand, SBMLDocument> cache = new SbmlCache(100)

    public void afterPropertiesSet() {
        if (Environment.current == Environment.PRODUCTION) {
            // only initialize the SBML2* Converters during startup in production mode
            // FIXME: fails the startup of Tomcat server
//             sbml2dotConverter()
            sbml2OctaveConverter()
            // FIXME: fails the startup of Tomcat server
            //sbml2BioPaxConverter()
        }
    }

    private SBMLDocument getFileAsValidatedSBMLDocument(final File model, final List<String> errors) {
        // TODO: we should insert the parsed model into the cache
        SBMLDocument doc
        SBMLReader reader = new SBMLReader()
        try {
            doc = reader.readSBML(model)
        } catch (XMLStreamException e) {
            e.printStackTrace()
            String error = "SBMLDocument could not be read from ${model.name}"
            log.error(error)
            errors.add(error)
            return null
        }
        if (doc == null) {
            // although the API documentation states that an Exception is thrown for incorrect files, it seems that null is returned
            String error = "SBMLDocument is not valid for file ${model.name}"
            log.error(error)
            errors.add(error)
            return null
        }
        // TODO: WARNING: checkConsistency uses an online validator. This might render timeouts during model upload
        try {
            final int CONSISTENCY_ERRORS = doc.checkConsistency()
            if (CONSISTENCY_ERRORS == -1) {
                log.debug("Internal error in online SBML Validator while validating ${doc.inspect()}\t${doc.properties}")
                return null
            }
            if (CONSISTENCY_ERRORS > 0) {
                // search for an error
                for (SBMLError error in doc.getListOfErrors().validationErrors) {
                    if (error.isFatal() || error.isInternal() || error.isSystem() || error.isXML() || error.isError()) {
                        String errorMsg = error.getMessage()
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

    /**
     * Checks whether the files passed comprise a model of this format.
     *
     * @param files The files comprising a potential model of this format
     */
    public boolean areFilesThisFormat(final List<File> files) {
        if (files == null || files.size() == 0) {
            return false
        }

        // let us assume that they are, and change our minds as soon as we discover the contrary
        boolean areAllSbml = true
        int iFiles = 0
        final fileCount = files.size()
        //should work with any value above 2, but sometimes there are comments at the start of the file
        final int DEPTH_LIMIT = 15
        BufferedReader reader = null
        String currentLine
        final def p = Pattern.compile(".*<sbml xmlns=\"http://www\\.sbml\\.org/sbml/level.*\".*")

        while (areAllSbml && iFiles < fileCount) {
            try {
                reader = new BufferedReader(new FileReader(files[iFiles]))
                boolean foundSbmlDeclarationLine = false
                int iLine = 0
                while (!foundSbmlDeclarationLine && iLine < DEPTH_LIMIT) {
                    currentLine = reader.readLine()
                    if (currentLine == null) {
                        areAllSbml = false
                        break
                    }
                    if (Pattern.matches(p.toString(), currentLine)) {
                        foundSbmlDeclarationLine = true
                    }
                    else {
                        iLine++
                    }
                }
                areAllSbml &= foundSbmlDeclarationLine
            } catch(IOException ex) {
                ex.printStackTrace();
            	def msg = new StringBuffer("Could not check if files ${files.inspect()} are valid SBML.")
                msg.append(" Encountered ${ex.message} while reading line $currentLine of file ${files[iFiles]}")
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
    public boolean validate(final List<File> model, final List<String> errors) {
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
    public String extractName(List<File> model) {
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
    public boolean updateName(RevisionTransportCommand revision, final String name) {
        //TODO update file contents
        if (revision && name.trim()) {
            revision.name = name.trim()
            return true
        }
        return false
    }

    /**
     * Extracts the SBML model notes.
     *
     * Note that all files with the exception of the first one are ignored.
     * @param model A list of files in SBML format
     * @return the description of the model or an empty string if no file is supplied.
     */
    @Profiled(tag="SbmlService.extractDescription")
    public String extractDescription(final List<File> model) {
        if (!model) {
            def errMsg = new StringBuffer("Cannot extract the description from undefined file ${model.properties}")
            log.warn errMsg.toString()
            return ""
        }
        def description = new StringBuffer()
        def nsList = [ "http://www.sbml.org/sbml/level3/version1/core",
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
            def errMsg = new StringBuffer("Exception encountered while extracting description from ${model.inspect()}")
            errMsg.append(": ${e.message}")
            log.error(errMsg.toString(), e)
            return ""
        } catch (IOException e) {
            def errMsg = new StringBuffer("IOException encountered while extracting description from ${model.inspect()}")
            errMsg.append(": ${e.message}")
            log.error(errMsg.toString(), e)
            return ""
        }
        return description.toString()
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Profiled(tag="sbmlService.updateDescription")
    public boolean updateDescription(RevisionTransportCommand revision, final String DESC) {
        //TODO update file contents
        if (revision && DESC.trim()) {
            revision.description = DESC.trim()
            return true
        }
        return false
    }

    @Profiled(tag="SbmlService.getMetaId")
    public String getMetaId(RevisionTransportCommand revision) {
        return getFromCache(revision)?.model?.metaId
    }

    @Profiled(tag="SbmlService.getVersion")
    public long getVersion(RevisionTransportCommand revision) {
        return fetchModelAttributeFromRevision(revision, "version")
    }

    @Profiled(tag="SbmlService.getLevel")
    public long getLevel(RevisionTransportCommand revision) {
        return fetchModelAttributeFromRevision(revision, "level")
    }

    @Profiled(tag="SbmlService.getFormatVersion")
    public String getFormatVersion(RevisionTransportCommand revision) {
        final long LEVEL = getLevel(revision)
        final long VERSION = getVersion(revision)
        return "L${LEVEL}V${VERSION}"
    }

    @Profiled(tag="SbmlService.getNotes")
    public String getNotes(RevisionTransportCommand revision) {
        String notesString = getFromCache(revision)?.model?.notesString ?: ""
        return notesString
    }

    @Profiled(tag="SbmlService.getAnnotations")
    public List<Map> getAnnotations(RevisionTransportCommand revision) {
        Model model = getFromCache(revision).model
        return convertCVTerms(model.annotation)
    }

    @Profiled(tag="SbmlService.getParameters")
    public List<Map> getParameters(RevisionTransportCommand revision) {
        Model model = getFromCache(revision).model
        ListOf<Parameter> parameters = model.getListOfParameters()
        List<Map> list = []
        parameters.each { parameter ->
            list << parameterToMap(parameter)
        }
        return list
    }

    @Profiled(tag="SbmlService.getParameter")
    public Map getParameter(RevisionTransportCommand revision, String id) {
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
    public List<Map> getLocalParameters(RevisionTransportCommand revision) {
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
    public List<Map> getReactions(RevisionTransportCommand revision) {
        Model model = getFromCache(revision).model
        List<Map> reactions = []
        model.listOfReactions.each { reaction ->
            reactions << reactionToMap(reaction)
        }
        return reactions
    }

    @Profiled(tag="SbmlService.getReaction")
    public Map getReaction(RevisionTransportCommand revision, String id) {
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
    public List<Map> getEvents(RevisionTransportCommand revision) {
        Model model = getFromCache(revision).model
        List<Map> events = []
        model.listOfEvents.each { event ->
            events << eventToMap(event)
        }
        return events
    }

    @Profiled(tag="SbmlService.getEvent")
    public Map getEvent(RevisionTransportCommand revision, String id) {
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
    public List<Map> getRules(RevisionTransportCommand revision) {
        Model model = getFromCache(revision).model
        List<Map> rules = []
        model.listOfRules.each { rule ->
            rules << ruleToMap(rule)
        }
        return rules
    }

    @Profiled(tag="SbmlService.getRule")
    public Map getRule(RevisionTransportCommand revision, String variable) {
        Model model = getFromCache(revision).model
        ExplicitRule rule = model.getRule(variable)
        if (!rule) {
            return [:]
        }
        Map ruleMap = ruleToMap(rule)
        ruleMap.put("annotation", convertCVTerms(rule.annotation))
        ruleMap.put("notes", rule.notesString)
        return ruleMap
    }

    public List<Map> getFunctionDefinitions(RevisionTransportCommand revision) {
        Model model = getFromCache(revision).model
        List<Map> functions = []
        model.listOfFunctionDefinitions.each { function ->
            functions << functionDefinitionToMap(function)
        }
        return functions
    }

    public Map getFunctionDefinition(RevisionTransportCommand revision, String id) {
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
    public List<Map> getCompartments(RevisionTransportCommand revision) {
        Model model = getFromCache(revision).model
        List<Map> compartments = []
        model.listOfCompartments.each { compartment ->
             compartments << compartmentToMap(compartment)
        }
        return compartments
    }

    @Profiled(tag="SbmlService.getCompartment")
    public Map getCompartment(RevisionTransportCommand revision, String id) {
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
    public List<Map> getAllSpecies(RevisionTransportCommand revision) {
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
     public Map getSpecies(RevisionTransportCommand revision, String id) {
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
    public byte[] generateSvg(RevisionTransportCommand revision) {
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
    public String generateOctave(RevisionTransportCommand revision) {
        SBMLModel sbmlModel = resolveSbmlModel(revision)
        OctaveModel octaveModel = sbml2OctaveConverter().octaveExport(sbmlModel)
        return octaveModel.modelToString()
    }

    @Profiled(tag="SbmlService.generateBioPax")
    public String generateBioPax(RevisionTransportCommand revision) {
        SBMLModel sbmlModel = resolveSbmlModel(revision)
        BioPaxModel bioPaxModel = sbml2BioPaxConverter().biopaxexport(sbmlModel)
        return bioPaxModel.modelToString()
    }

    @Profiled(tag="SbmlService.getAllAnnotationURNs")
    public List<String> getAllAnnotationURNs(RevisionTransportCommand revision) {
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
    public List<List<String>> getPubMedAnnotation(RevisionTransportCommand revision) {
        Model model = getFromCache(revision)?.model
        Annotation annotation = model?.annotation
        if(!annotation) {
            return null
        }
        List<CVTerm> filters = annotation.filterCVTerms(CVTerm.Qualifier.BQM_IS_DESCRIBED_BY)
        List<List<String>> pubMedAnnotation = []
        filters.each { filter ->
            CVTerm cvTerm = new CVTerm(filter)
            pubMedAnnotation.add(cvTerm.filterResources("pubmed"))
        }
        return pubMedAnnotation
    }

    /**
     * Returns the SBMLDocument for the @p revision from the cache.
     * If the cache does not contain the SBMLDocument, the model file is
     * retrieved, parsed and inserted into the Cache.
     * @param revision The revision for which the SBMLDocument needs to be retrieved
     * @return The parsed SBMLDocument
     */
    private SBMLDocument getFromCache(RevisionTransportCommand revision) throws XMLStreamException {
        SBMLDocument document = cache.get(revision)
        if (document) {
            return document
        }
        //SBMLDocument document=null;
        // we do not have a document, so retrieve first the file
        //List<RepositoryFileTransportCommand> files = grailsApplication.mainContext.getBean("modelDelegateService").retrieveModelFiles(revision)
        List<RepositoryFileTransportCommand> files=revision.files
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
                  cache.put(revision, document)
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
            type = "rate"
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

    private SBML2Dot sbml2dotConverter() {
        if (!dotConverter) {
            dotConverter = new SBML2Dot()
        }
        return dotConverter
    }

    private SBML2Octave sbml2OctaveConverter() {
        if (!octaveConverter) {
            octaveConverter = new SBML2Octave()
        }
        return octaveConverter
    }

    private SBML2BioPAX_l3 sbml2BioPaxConverter() {
        if (!biopaxConverter) {
            biopaxConverter = new SBML2BioPAX_l3()
        }
        return biopaxConverter
    }

    private long fetchModelAttributeFromRevision(RevisionTransportCommand revision, String attributeName) {
        File mainFile = fetchMainFileFromRevision(revision)
        if (!mainFile) {
            //we have already logged this error, just return
            return 0
        }
        String attributeValue = findModelAttribute(mainFile, "sbml", attributeName)
        return longFromString(attributeValue, attributeName, mainFile)
    }

    private File fetchMainFileFromRevision(RevisionTransportCommand revision) {
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

    /*
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
     * @param revision The RevisionTransportCommand from which to extract the SBMLModel.
     * @return The SBMLModel to be found or an empty array if the model could not be found.
     */
    private SBMLModel resolveSbmlModel(RevisionTransportCommand revision) {
        try {
        Model model = getFromCache(revision).model
        SBMLWriter sbmlWriter = new SBMLWriter()
        String sbmlString = sbmlWriter.writeSBMLToString(model.getSBMLDocument())
        SBMLModel sbmlModel = new SBMLModel()
        sbmlModel.setModelFromString(sbmlString)
        return sbmlModel
        } catch (Exception e) {
            e.printStackTrace()
            return [:]
        }
    }

    /**
     * Triggers the generation of a sub model for an existing model encoded in SBML.
     *
     * @return A String representation of the new model encoded in SBML.
     */
    public String triggerSubmodelGeneration(
            RevisionTransportCommand revision, String subModelId, String metaId, 
            List<String> compartmentIds, List<String> speciesIds, List<String> reactionIds,
            List<String> ruleIds, List<String> eventIds) {
        Model model = getFromCache(revision).model
        return new SubmodelGenerator().generateSubModel(
                model, subModelId, metaId, compartmentIds, speciesIds, reactionIds, ruleIds, eventIds)
    }

    boolean doBeforeSavingAnnotations(File annoFile, RevisionTransportCommand rev) {
        return true
    }
}
