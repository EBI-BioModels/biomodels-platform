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
 */

package net.biomodels.jummp.annotation

import eu.ddmore.metadata.service.MetadataWriterImpl
import net.biomodels.jummp.core.MetadataSavingStrategy
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand
import net.biomodels.jummp.core.annotation.StatementTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.jena.riot.RDFFormat

/**
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk> on 12/04/2016.
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk> on 12/04/2016.
 */
class RDFWriter implements MetadataSavingStrategy {
    private static final Log log = LogFactory.getLog(this)
    /**
     * Dependency injection for Grails Application.
     */
    def grailsApplication
    /**
     * Dependency injection for Model Format Service.
     */
    def modelFileFormatService

    @Override
    List<RepositoryFileTransportCommand> marshallAnnotations(RevisionTransportCommand revisionTC,
                                List<StatementTransportCommand> statementTransportCommands,
                                boolean isUpdate = false) {
        def metadataWriter = createMetadataWriter(revisionTC, statementTransportCommands)
        List<RepositoryFileTransportCommand> files = revisionTC.files
        String annoFilePath
        File annoFile
        RepositoryFileTransportCommand rf
        if (!isUpdate) {
            String fileBase = System.properties['java.io.tmpdir']
            String fileName = "${revisionTC.model.submissionId}.rdf"
            annoFile = new File(fileBase, fileName)
            annoFilePath = annoFile.absolutePath
            rf = new RepositoryFileTransportCommand(path: annoFilePath, description: "annotation file")
            files.add rf
        } else {
            rf = files.find {
                it.path?.endsWith(".rdf")
            }
            annoFilePath = rf.path
            annoFile = new File(annoFilePath)
        }
        metadataWriter.writeRDFModel(annoFilePath, RDFFormat.RDFXML)
        boolean preProcessingOK = modelFileFormatService.doBeforeSavingAnnotations(annoFile, revisionTC)
        if (!preProcessingOK) {
            log.error """\
            Metadata ${metadataWriter.dump()} based on revision ${revisionTC.id} will not save cleanly."""
            return null
        } else {
            return files
        }
    }

    public MetadataWriterImpl createMetadataWriter(RevisionTransportCommand revisionTC, List<StatementTransportCommand> statements){
        def subject = "${grailsApplication.config.grails.serverURL}/model/${revisionTC.model.submissionId}"
        def rdfTypeProperty = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type"
        def modelOntologyTerm = "http://www.pharmml.org/ontology/PHARMMLO_0000001"

        def metadataWriter = new MetadataWriterImpl()
        statements.each { StatementTransportCommand statement ->
            String predicate = statement.predicate.uri
            ResourceReferenceTransportCommand xref = statement.object
            String object = xref.uri ?: xref.name
            boolean isLiteralTriple = xref.uri ? false : true
            if (isLiteralTriple) {
                metadataWriter.generateLiteralTriple(subject, predicate, object)
            } else {
                metadataWriter.generateTriple(subject, predicate, object)
            }
        }
        metadataWriter.generateTriple(subject, rdfTypeProperty, modelOntologyTerm)

        return metadataWriter
    }
}
