/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import groovyx.gpars.GParsPool
import net.biomodels.jummp.core.annotation.ElementAnnotationTransportCommand as EATC
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.model.PublicationLinkProvider as PLP
import net.biomodels.jummp.model.Revision
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.time.Duration
import java.time.Instant

/**
 * @author Tung Nguyen <nvntung@gmail.com> on 11/02/2022.
 */
class BioModels2ExternalResourcesMapper {
    def ctx

    private static final Logger logger = LoggerFactory.getLogger(BioModels2ExternalResourcesMapper.class)

    void build() {
        String duration
        Instant startTime = Instant.now()
        ctx.persistenceInterceptor?.init()
        def mdDS = ctx.getBean("metadataDelegateService")

        try {
            String query = """SELECT R1 FROM Revision AS R1 JOIN R1.model as M \
WHERE \
	M.deleted = false AND \
	M.firstPublished != NULL AND \
 	R1.revisionNumber = (SELECT MAX(R2.revisionNumber) FROM Revision AS R2 \
                         WHERE R2.model = M and R2.state = '${ModelState.PUBLISHED}') \
ORDER BY M.id"""
            List revisions = Revision.executeQuery(query)
            /*int start = 20
            int stop = 61
            revisions = revisions.subList(start, stop)*/
            final int POOL_SIZE = 8
            GParsPool.withPool(POOL_SIZE) {
                revisions.eachParallel { Revision revision ->
                    long revisionId = revision.id
                    ctx.persistenceInterceptor?.init()
                    try {
                        String pubOrDoiId = null
			Publication publication = revision.model.publication
                        PLP.LinkType linkType = publication?.linkProvider?.linkType
                        if (linkType != PLP.LinkType.MANUAL_ENTRY && publication != null) {
                            pubOrDoiId = publication.link
                        }
                        List<EATC> annotations = mdDS.fetchAnnotations(revisionId)
                        String EXT_RES = System.getenv("EXT_RES")
                        if (!(EXT_RES in ["chebi", "ensembl", "uniprot"])) {
                            // set "uniprot" as the default if the external source hasn't been specified
                            EXT_RES = "uniprot"
                        }
                        List candidateList = annotations.findAll {
                            it.statement.object.datatype == EXT_RES
                        }
                        List iDList = candidateList.collect {
                            it.statement.object.accession
                        }.unique()
                        String modelIdentifier = revision.model.publicationId ?: revision.model.submissionId
                        iDList.each { String resourceId ->
                            println "${modelIdentifier}\t${resourceId}\t${pubOrDoiId}"
                        }
                    } catch (Exception e) {
                        String message = """\
Cannot extract the model: ${revision.model.submissionId}, revision: ${revisionId} because of the below error:
${e}"""
                        logger.error(message)
                        e.printStackTrace()
                    } finally {
                        ctx.persistenceInterceptor?.destroy()
                    }
                }
            }
        } finally {
            ctx.persistenceInterceptor?.destroy()
            duration = Duration.between(startTime, Instant.now())
            String formattedDuration = duration.toString()
            logger.info("Found and mapped the identifiers within $formattedDuration")
        }
    }
}

new BioModels2ExternalResourcesMapper(ctx: ctx).build()
