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
import net.biomodels.jummp.core.certification.QcInfoCategory
import net.biomodels.jummp.core.certification.QcInfoTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.model.Revision
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * @short Adapter class for the Revision domain class
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
public class RevisionAdapter {
    private static final Log log = LogFactory.getLog(RevisionAdapter.class)

    Revision revision

    def grailsApplication = Holders.getGrailsApplication()

    RevisionTransportCommand toCommandObject() {
        def msg = """\
Converting Revision #${revision.id} (attached: ${revision.isAttached()}),
(format attached: ${revision.format.isAttached()}) to cmd object
isSynchronisationActive: ${TransactionSynchronizationManager.isSynchronizationActive()}
sessionClosed: ${grailsApplication.mainContext.sessionFactory.currentSession.isClosed()}""".toString()
        log.info(msg)
        def formatAdapter = new ModelFormatAdapter(format: revision.format)
        def formatCmd = formatAdapter.toCommandObject()
        String submitterName = revision.owner.person.userRealName
        def modelAdapter = new ModelAdapter(model: revision.model)
        def modelCmd = modelAdapter.toCommandObject(false)
        QcInfoTransportCommand qcInfoCmd
        use(QcInfoCategory) {
            qcInfoCmd = revision.qcInfo?.toCommandObject()
        }
        RevisionTransportCommand rev = new RevisionTransportCommand(
                id: revision.id,
                state: revision.state,
                curationState: revision.curationState,
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
                validationLevel: revision.validationLevel,
                validationReport: revision.validationReport,
                qcInfo: qcInfoCmd,
                readmeSubmission: revision.readmeSubmission
        )
        return rev
    }
}
