package net.biomodels.jummp.plugins.omicsdi

import grails.util.Holders
import grails.util.Metadata
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.qcinfo.QcInfo
import org.perf4j.aop.Profiled

import javax.xml.ws.Holder
import java.text.DateFormat

class OmicsdiService {
    private final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
    * Dependency Injection of Spring Security Service
    */
    def springSecurityService
    /**
     * Dependency Injection of SearchService
     */
    def searchService

    def grailsApplication

    static final String GRAILS_CONF_LOCATION = "grails-app/conf"
    static final String OMICSDI_CONFIG_LOCATION = "omicsdi"

    void init() {

    }
    @Profiled(tag="omicsdiService.loadSchemaXml")
    String loadSchemaXml() {
        return GRAILS_CONF_LOCATION.concat("/").concat(OMICSDI_CONFIG_LOCATION)
    }

    List<OmicsdiDataSetEntry> generateOmicsdiDataSetEntry() {
        List<Model> models = Model.getAll()
        List<OmicsdiDataSetEntry> entries = new ArrayList<OmicsdiDataSetEntry>()

        models.each {Model m ->
            // get the latest revision
            Revision r = m.revisions.getAt(m.revisions.size()-1)
            OmicsdiDataSetEntry e = new OmicsdiDataSetEntry()
            e.id = m.submissionId
            e.name = r.name
            e.description = "" //r.description // deal with it later because of html tags
            e.dateSubmitted = m.revisions.first().uploadDate
            e.datePublished = m.firstPublished
            e.dateLastModified = r.uploadDate
            // additional fields
            e.submitterName = Person.findById(r.owner.id).userRealName
            e.submitterMail = r.owner.email
            e.submitterAffiliation = Person.findById(r.owner.id).institution
            e.repositoryName = grailsApplication.config.jummp.metadata.officialDatabaseName //Metadata.current.'app.name'
            e.fullDataSetLink = "${Holders.grailsApplication.config.grails.serverURL}/model/${e.id}"
            Publication publication = Publication.findById(m.publicationId)
            String pub = ""
            if (publication) {
                pub = """
                    ${publication.synopsis}. ${publication.issue}, ${publication.volume}.
                    ${publication.affiliation}
                """
            }
            e.publication = pub
            e.diseaseName = "Unknown"
            e.omicsType = "Models"
            QcInfo qcInfo = QcInfo.findById(r.qcInfoId)
            String curationComment = qcInfo?.comment ?: ""
            e.dataProtocol = curationComment
            e.sampleProtocol = curationComment
            e.technologyType = curationComment

            e.modelFormat = r.format?.name ?: "Unknown"
            if (publication?.id) {
                e.publicationId = publication.id
            }
            e.submissionId = m.submissionId
            ModelFormat modelFormat = ModelFormat.findById(r.formatId)
            e.levelVersion = modelFormat?.formatVersion ?: ""

            e.validationStatus = r.validationReport ?: ""
            e.certificationComment = curationComment

            e.deleted = m.deleted
            e.isPublic = m.firstPublished != null
            e.deleted = m.deleted
            e.certified = qcInfo  != null


            entries.add(e)
        }
        return entries
    }

    String buildOmicsdiSchemaXml() {
        List<OmicsdiDataSetEntry> entries = generateOmicsdiDataSetEntry()
        String name = "BioModels Database"
        String description  = """
BioModels Database is a repository of computational models of biological processes. Models described
        from literature are manually curated and enriched with cross-references.
"""
        int entryCount = entries.size() ?: 0
        OmicsdiDataSet bmDb = new OmicsdiDataSet(name: name, description: description,
            releaseVersion: 1, dateReleased: new Date(), entryCount: entryCount)

        StringBuilder content = new StringBuilder()
        content.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        content.append("<database>")
        content.append("<name>${bmDb.name}</name>")
        content.append("<description>${bmDb.description}</description>")
        content.append("<release>${bmDb.releaseVersion}</release>")
        content.append("<release_date>${bmDb.dateReleased}</release_date>")
        content.append("<entry_count>${bmDb.entryCount}</entry_count>")

        content.append("<entries>")
        entries.each {e ->
            content.append("<entry id='${e.id}'>")
            content.append("<name>${e.name}</name>")
            content.append("<description>${e.description}</description>")

            content.append("<dates>")
            content.append("<date type=\"submission\" value=\"${e.dateSubmitted}\"/>")
            content.append("<date type=\"publication\" value=\"${e.datePublished}\"/>")
            content.append("<date type=\"last_modification\" value=\"${e.dateLastModified}\"/>")
            content.append("</dates>")

            content.append("<additional_fields>")
            content.append("<field name=\"submitter\">${e.submitterName}</field>")
            content.append("<field name=\"submitter_mail\">${e.submitterMail}</field>")
            content.append("<field name=\"submitter_affiliation\">${e.submitterAffiliation}</field>")
            content.append("<field name=\"repository\">${e.repositoryName}</field>")
            content.append("<field name=\"full_dataset_link\">${e.fullDataSetLink}</field>")
            content.append("<field name=\"publication\">${e.publication}</field>")
            content.append("<field name=\"disease\">${e.diseaseName}</field>")
            content.append("<field name=\"omics_type\">${e.omicsType}</field>")
            content.append("<field name=\"data_protocol\">${e.dataProtocol}</field>")
            content.append("<field name=\"sample_protocol\">${e.sampleProtocol}</field>")
            content.append("<field name=\"technology_type\">${e.technologyType}</field>")

            content.append("<field name=\"modelFormat\">${e.modelFormat}</field>")
            content.append("<field name=\"submissionId\">${e.submissionId}</field>")
            content.append("<field name=\"publicationId\">${e.publicationId}</field>")
            content.append("<field name=\"levelVersion\">${e.levelVersion}</field>")
            content.append("<field name=\"validationStatus\">${e.validationStatus}</field>")
            content.append("<field name=\"certificationComment\">${e.certificationComment}</field>")
            content.append("<field name=\"elementName\">${e.elementName}</field>")
            content.append("<field name=\"elementId\">${e.elementId}</field>")
            content.append("<field name=\"elementDescription\">${e.elementDescription}</field>")
            content.append("<field name=\"public\">${e.isPublic}</field>")
            content.append("<field name=\"deleted\">${e.deleted}</field>")
            content.append("<field name=\"certified\">${e.certified}</field>")
            content.append("<field name=\"sbmlSBOTerm\">${e.sbmlSBOTerm}</field>")
            content.append("<field name=\"curators\">${e.curators}</field>")
            content.append("<field name=\"authors\">${e.authors}</field>")
            content.append("<field name=\"derivations\">${e.derivations}</field>")
            /*  PharmML-specific fields */
            content.append("<field name=\"pharmmlTherapeuticArea\">${e.pharmmlTherapeuticArea}</field>")
            content.append("<field name=\"pharmmlModellingContextDescription\">${e.pharmmlModellingContextDescription}</field>")
            content.append("<field name=\"pharmmlLongTechnicalDescription\">${e.pharmmlLongTechnicalDescription}</field>")
            content.append("<field name=\"pharmmlShortDescription\">${e.pharmmlShortDescription}</field>")
            content.append("<field name=\"pharmmlPublicationSource\">${e.pharmmlPublicationSource}</field>")
            content.append("<field name=\"pharmmlImplementationConformsToLiterature\">${e.pharmmlImplementationConformsToLiterature}</field>")
            content.append("<field name=\"pharmmlImplementationDiscrepancies\">${e.pharmmlImplementationDiscrepancies}</field>")
            content.append("<field name=\"pharmmlModelDevelopmentContext\">${e.pharmmlModelDevelopmentContext}</field>")
            content.append("<field name=\"pharmmlCodeFromLiterature\">${e.pharmmlCodeFromLiterature}</field>")
            content.append("<field name=\"pharmmlResearchStage\">${e.pharmmlResearchStage}</field>")
            content.append("<field name=\"pharmmlTasks\">${e.pharmmlTasks}</field>")
            content.append("<field name=\"pharmmlTypeOfData\">${e.pharmmlTypeOfData}</field>")

            content.append("</additional_fields>")

            content.append("</entry>")
        }
        content.append("</entries>")
        content.append("</database>")
        return content.toString()
    }
    void saveAsOmicsdiSchemaXML() {
        String folder = "/automount/vnas-homes_vol-vol_homes-homes/tnguyen/Documents/Synchronisation/"
        String fileName = "OmicsDISchematest.xml"
        def fileWriter = new FileWriter("${folder}${fileName}")
        def markupBuilder = new MarkupBuilder(fileWriter)
        markupBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")

        List<OmicsdiDataSetEntry> modelEntries = generateOmicsdiDataSetEntry()
        String _name = grailsApplication.config.jummp.metadata.officialDatabaseName
        String _description  = grailsApplication.config.jummp.metadata.officialDatabaseDescription
        byte _releaseVersion = 1
        Date _releaseDate = new Date()
        int _entryCount = modelEntries.size() ?: 0
//        OmicsdiDataSet bmDb = new OmicsdiDataSet(name: name, description: description,
//            releaseVersion: 1, dateReleased: new Date(), entryCount: entryCount)
        markupBuilder.database {
            // add the principal information of database
            name(_name)
            description(_description)
            release(_releaseVersion)
            release_date(_releaseDate)
            entry_count(_entryCount)
            // add the models
            entries {
                modelEntries.each { e ->
                    entry(id: "$e.id") {
                        name("$e.name")
                        if (e.description) {
                            description("$e.description")
                        }
                        dates() {
                            if (e.dateSubmitted) {
                                date(type: "submission", value: "$e.dateSubmitted")
                            }
                            if (e.datePublished) {
                                date(type: "publication", value: "$e.datePublished")
                            }
                            if (e.dateLastModified) {
                                date(type: "last_modification", value: "$e.dateLastModified")
                            }
                        }
                        additional_fields() {
                            if (e.submitterName) {
                                field(name: "submitter", "$e.submitterName")
                            }
                            if (e.submitterMail) {
                                field(name: "submitter_mail", "$e.submitterMail")
                            }
                            if (e.submitterAffiliation) {
                                field(name: "submitter_affiliation", "$e.submitterAffiliation")
                            }
                            if (e.repositoryName) {
                                field(name: "repository", "$e.repositoryName")
                            }
                            if (e.fullDataSetLink) {
                                field(name: "full_dataset_link", "$e.fullDataSetLink")
                            }
                            if (e.publication) {
                                field(name: "publication", "$e.publication")
                            }
                            if (e.diseaseName) {
                                field(name: "disease", "$e.diseaseName")
                            }
                            if (e.omicsType) {
                                field(name: "omics_type", "$e.omicsType")
                            }
                            if (e.dataProtocol) {
                                field(name: "data_protocol", "$e.dataProtocol")
                            }
                            if (e.sampleProtocol) {
                                field(name: "sample_protocol", "$e.sampleProtocol")
                            }
                            if (e.technologyType) {
                                field(name: "technology_type", "$e.technologyType")
                            }
                            if (e.modelFormat) {
                                field(name: "modelFormat", "$e.modelFormat")
                            }
                            if (e.submissionId) {
                                field(name: "submissionId", "$e.submissionId")
                            }
                            if (e.publicationId) {
                                field(name: "publicationId", "$e.publicationId")
                            }
                            if (e.levelVersion) {
                                field(name: "levelVersion", "$e.levelVersion")
                            }
                            if (e.validationStatus) {
                                field(name: "validationStatus", "$e.validationStatus")
                            }
                            if (e.certificationComment) {
                                field(name: "certificationComment", "$e.certificationComment")
                            }
                            if (e.elementName) {
                                field(name: "elementName", "$e.elementName")
                            }
                            if (e.elementId) {
                                field(name: "elementId", "$e.elementId")
                            }
                            if (e.elementDescription) {
                                field(name: "elementDescription", "$e.elementDescription")
                            }
                            field(name: "public", "$e.isPublic")
                            field(name: "deleted", "$e.deleted")
                            field(name: "certified", "$e.certified")
                            if (e.sbmlSBOTerm) {
                                field(name: "sbmlSBOTerm", "$e.sbmlSBOTerm")
                            }
                            if (e.curators) {
                                field(name: "curators", "$e.curators")
                            }
                            if (e.authors) {
                                field(name: "authors", "$e.authors")
                            }
                            if (e.derivations) {
                                field(name: "derivations", "$e.derivations")
                            }
                            if (e.pharmmlTherapeuticArea) {
                                field(name: "pharmmlTherapeuticArea", "$e.pharmmlTherapeuticArea")
                            }
                            if (e.pharmmlModellingContextDescription) {
                                field(name: "pharmmlModellingContextDescription", "$e.pharmmlModellingContextDescription")
                            }
                            if (e.pharmmlLongTechnicalDescription) {
                                field(name: "pharmmlLongTechnicalDescription", "$e.pharmmlLongTechnicalDescription")
                            }
                            if (e.pharmmlShortDescription) {
                                field(name: "pharmmlShortDescription", "$e.pharmmlShortDescription")
                            }
                            if (e.pharmmlPublicationSource) {
                                field(name: "pharmmlPublicationSource", "$e.pharmmlPublicationSource")
                            }
                            if (e.pharmmlImplementationConformsToLiterature) {
                                field(name: "pharmmlImplementationConformsToLiterature", "$e.pharmmlImplementationConformsToLiterature")
                            }
                            if (e.pharmmlImplementationDiscrepancies) {
                                field(name: "pharmmlImplementationDiscrepancies", "$e.pharmmlImplementationDiscrepancies")
                            }
                            if (e.pharmmlModelDevelopmentContext) {
                                field(name: "pharmmlModelDevelopmentContext", "$e.pharmmlModelDevelopmentContext")
                            }
                            if (e.pharmmlCodeFromLiterature) {
                                field(name: "pharmmlCodeFromLiterature", "$e.pharmmlCodeFromLiterature")
                            }
                            if (e.pharmmlResearchStage) {
                                field(name: "pharmmlResearchStage", "$e.pharmmlResearchStage")
                            }
                            if (e.pharmmlTasks) {
                                field(name: "pharmmlTasks", "$e.pharmmlTasks")
                            }
                            if (e.pharmmlTypeOfData) {
                                field(name: "pharmmlTypeOfData", "$e.pharmmlTypeOfData")
                            }
                        }
                    }
                }
            }
        }
        fileWriter.close()

        def writer = new FileWriter('/homes/tnguyen/tmp/file.xml')
        def text = buildOmicsdiSchemaXml()
        def xml = new XmlSlurper().parseText(text)
        XmlUtil.serialize(xml, writer)
    }
}
