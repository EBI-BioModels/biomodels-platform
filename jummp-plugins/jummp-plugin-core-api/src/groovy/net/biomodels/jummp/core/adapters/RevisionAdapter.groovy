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
 **/

package net.biomodels.jummp.core.adapters

import grails.util.Holders
import net.biomodels.jummp.core.annotation.ElementAnnotationCategory
import net.biomodels.jummp.core.annotation.ElementAnnotationTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Revision

/**
 * @short Adapter class for the Revision domain class
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
public class RevisionAdapter extends DomainAdapter {
    Revision revision

    def modelService = Holders.getGrailsApplication().mainContext.modelService

    List<RFTC> getRepositoryFilesForRevision() {
        List<RFTC> repFiles = new LinkedList<RFTC>()
        List<File> files = modelService.retrieveModelRepFiles(revision)
        revision.repoFiles.each { rf ->
            File tmpFile = files.find { it.getName() == (new File(rf.path)).getName() }
            if (tmpFile != null) {
                RFTC rftc = new RFTC(
                    id: rf.id,
                    path: tmpFile.getCanonicalPath(),
                    description: rf.description,
                    hidden: rf.hidden,
                    mainFile: rf.mainFile,
                    userSubmitted: rf.userSubmitted,
                    mimeType: rf.mimeType)
                repFiles.add(rftc)
            }
        }
        return repFiles
    }

    RevisionTransportCommand toCommandObject() {
        List<ElementAnnotationTransportCommand> annotations
        use(ElementAnnotationCategory) {
            annotations = revision.annotations.collect { it.toCommandObject() }
        }
        def formatAdapter = getAdapter(revision.format)
        def formatCmd = formatAdapter.toCommandObject()
        String submitterName = revision.owner.person.userRealName
        def modelAdapter = new ModelAdapter(model: revision.model)
        def modelCmd = modelAdapter.toCommandObject()
        RevisionTransportCommand rev = new RevisionTransportCommand(
                id: revision.id,
                state: revision.state,
                revisionNumber: revision.revisionNumber,
                owner: submitterName,
                minorRevision: revision.minorRevision,
                validated: revision.validated,
                name: revision.name,
                description: revision.description,
                comment: revision.comment,
                uploadDate: revision.uploadDate,
                format: formatCmd,
                model: modelCmd,
                annotations: annotations,
                validationLevel: revision.validationLevel,
                validationReport: revision.validationReport
        )
        return rev
    }
}
