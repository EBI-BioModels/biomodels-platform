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
 **/





package net.biomodels.jummp.plugins.omicsdi

import grails.util.Holders
import grails.util.Metadata
import groovy.xml.MarkupBuilder
import groovy.xml.XmlUtil
import net.biomodels.jummp.core.adapters.DomainAdapter
import net.biomodels.jummp.core.annotation.QualifierTransportCommand
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.Person
import net.biomodels.jummp.qcinfo.QcInfo
import org.perf4j.aop.Profiled

import javax.xml.ws.Holder

/**
 * @short Omicsdi class for managing OmicsDI's settings
 *
 * This class provides means of handling functionalities for searching models based on OmicsDI's API.
 * It accesses the database and get essential data that are included into XML files.
 * Otherwise, we implement the methods configuring OmicsDI API.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */

class OmicsdiService {
    private final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Dependency Injection of Metadata Delegate Service
     **/
    def metadataDelegateService
    /**
     * Dependency Injection of Spring Security Service
     **/
    def springSecurityService
    /**
     * Dependency Injection of Grails Application
     **/
    def grailsApplication

    static final String GRAILS_CONF_LOCATION = "grails-app/conf"
    static final String OMICSDI_CONFIG_LOCATION = "omicsdi"

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
            // compulsory fields
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

            // cross references fields
            def revTC = DomainAdapter.getAdapter(r).toCommandObject()
            Map<QualifierTransportCommand, List<ResourceReferenceTransportCommand>> genericAnno =
                metadataDelegateService.fetchGenericAnnotations revTC
            if (genericAnno.size() > 0) {
                println("Annotations of the revision $revTC.model.submissionId: ${genericAnno.size()}")
                genericAnno.each {anno ->
                    QualifierTransportCommand qualifier = anno.key
                    List<ResourceReferenceTransportCommand> references = anno.value
                    //println "$anno.key, has value: $anno.value"
                    //println "$qualifier.type and $qualifier.namespace and ${references.size()}"
                    references.each { ref ->
                        String dbkey = "Unknown"
                        String dbname = "Somewhere"
                        println ref.uri
                        if (ref.uri != null) {
                            int accessionDelim = ref.uri?.lastIndexOf("/")
                            String accession =  ref.uri?.substring(accessionDelim + 1)
                            dbkey = accession
                            dbname = ref.datatype
                        }
                        println "dbkey: $dbkey and dbname: $dbname" // and ${ref.name ?: ref.accession ?: ref.uri ?: ref.collectionName}"
                    }

                }
            }

            entries.add(e)
        }
        return entries
    }

    @Profiled(tag="omicsdiService.buildStringOmicsdiSchemaXml")
    String buildStringOmicsdiSchemaXml() {
        def stringWriter = new StringWriter()
        def markupBuilder = new MarkupBuilder(stringWriter)
        markupBuilder.setDoubleQuotes(true)
        markupBuilder.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")

        List<OmicsdiDataSetEntry> modelEntries = generateOmicsdiDataSetEntry()
        String _name = grailsApplication.config.jummp.metadata.officialDatabaseName
        String _description = grailsApplication.config.jummp.metadata.officialDatabaseDescription
        byte _releaseVersion = 1
        Date _releaseDate = new Date()
        int _entryCount = modelEntries.size() ?: 0
        markupBuilder.database {
            // add the principal information of database into MarkupBuilder object
            name(_name)
            description(_description)
            release(_releaseVersion)
            release_date(_releaseDate)
            entry_count(_entryCount)
            // add each model into MarkupBuilder object
            entries {
                setOmitEmptyAttributes(true)
                setOmitNullAttributes(true)
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
        def content = stringWriter.toString()
        return content
    }

    @Profiled(tag="omicsdiService.buildFileOmicsdiSchemaXml")
    void buildFileOmicsdiSchemaXml(String filePath, String fileName) {
        if (filePath.charAt(filePath.length() - 1) != File.separatorChar) {
            filePath += File.separatorChar
        }
        def fileWriter = new FileWriter("${filePath}${fileName}")
        def content = buildStringOmicsdiSchemaXml()
        fileWriter.write(content)
        fileWriter.close()
    }

    boolean saveAsOmicsdiSchemaXML() {
        String folderPath = "/homes/tnguyen/Documents/Synchronisation/"
        String fileName = "OmicsDISchemaDemo.xml"
        def file = buildFileOmicsdiSchemaXml(folderPath, fileName)
        return file != null
        /*def writer = new FileWriter("${folderPath}OmicsdiTest.xml")
        def text = buildStringOmicsdiSchemaXml()
        def xml = new XmlSlurper().parseText(text)
        XmlUtil.serialize(xml, writer)*/
    }


}
