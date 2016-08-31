/**
 * Created by tnguyen on 24/08/16.
 */

package net.biomodels.jummp.plugins.omicsdi

class OmicsdiDataSetEntry {
    String id
    String name
    String description
    Date dateSubmitted
    Date datePublished
    Date dateLastModified
    String submitterName
    String submitterMail
    String submitterAffiliation
    String fullDataSetLink
    String publication
    String repositoryName
    String diseaseName
    String omicsType
    String dataProtocol
    String sampleProtocol
    String technologyType

    String modelFormat
    String submissionId
    String publicationId
    String levelVersion
    String validationStatus
    String certificationComment
    String elementId
    String elementName
    String elementDescription
    boolean isPublic
    boolean deleted
    boolean certified
    String sbmlSBOTerm
    String curators
    String authors
    String derivations

    /*  PharmML-specific fields */
    String pharmmlTherapeuticArea
    String pharmmlModellingContextDescription
    String pharmmlLongTechnicalDescription
    String pharmmlShortDescription
    String pharmmlPublicationSource
    String pharmmlImplementationConformsToLiterature
    String pharmmlImplementationDiscrepancies
    String pharmmlModelDevelopmentContext
    String pharmmlCodeFromLiterature
    String pharmmlResearchStage
    String pharmmlTasks
    String pharmmlTypeOfData
}
