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

/**
 * @short OmicsdiDataSetEntry class for handling OmicsDI specification's entry fields
 *
 * This class provides means of managing the fields for each entry as described in OmicsDI specification.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */

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
