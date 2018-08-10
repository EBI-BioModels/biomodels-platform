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

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.util.Holders
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.identifier.ModelIdentifierUtils
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision

/**
 * @short Adapter class for the Model domain class
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
@CompileStatic
class ModelAdapter {
    Model model

    static final Set<String> PERENNIAL_IDENTIFIER_TYPES = ModelIdentifierUtils.perennialFields
    static final Set<String> FIND_BY_PERENNIAL_ID_CRITERIA = populateFindByCriteria()

    static def modelService = Holders.applicationContext.getBean("modelService")

    @CompileStatic
    ModelTransportCommand toCommandObject(boolean saveHistory = true) {
        Set<String> creators = []
        Set<String> creatorUsernames = []
        if (model.revisions?.size() > 0) {
            for (Revision revision: model.revisions) {
                creators.add(revision.owner.person.userRealName)
                creatorUsernames.add(revision.owner.username)
            }
        }
        Revision latestRev
        Revision firstRev
        Long modelId = model.id
        boolean modelIsSaved = null != modelId
        if (modelIsSaved) {
            if (SpringSecurityUtils.ifAnyGranted("ROLE_ADMIN")) {
                latestRev = model.revisions.last()
                firstRev = model.revisions.first()
            } else {
                latestRev = getLatestRevisionForUser(saveHistory)
                firstRev = model.revisions.first()
            }
        } else {
            // if the model is not saved, there can only be at most one revision
            latestRev = model.revisions?.first()
            firstRev = latestRev
        }

        return new ModelTransportCommand(
            id: modelId,
            submissionId: model.submissionId,
            publicationId: model.publicationId,
            firstPublished: model.firstPublished,
            name: latestRev?.name,
            description: latestRev?.description,
            state: latestRev?.state,
            lastModifiedDate: latestRev?.uploadDate,
            format: latestRev ? new ModelFormatAdapter(format: latestRev.format).toCommandObject() : null,
            publication: model.publication ? new PublicationAdapter(publication:  model.publication).toCommandObject() : null,
            deleted: model.deleted,
            submitter: firstRev?.owner?.person?.userRealName,
            submitterUsername: firstRev?.owner?.username,
            submissionDate: firstRev?.uploadDate,
            creators: creators,
            creatorUsernames: creatorUsernames,
            flagLevel: latestRev?.qcInfo?.flag
        )
    }

    @CompileDynamic
    private Revision getLatestRevisionForUser(boolean saveHistory) {
        modelService.getLatestRevision(model, saveHistory)
    }

    /**
     * Convenience method for finding a model based on its externally-defined identifiers.
     *
     * @param perennialId The externally-defined ID by which to look up the model.
     * @return  the model corresponding to the given id, or null if there was no match
     */
    @CompileDynamic
    static Model findByPerennialIdentifier(String perennialId) {
        if (!perennialId) {
            return null
        }
        int dot = perennialId.indexOf('.')
        perennialId = -1 == dot ? perennialId : perennialId.substring(0, dot)
        def results = Model.withCriteria {
            or {
                FIND_BY_PERENNIAL_ID_CRITERIA.each {
                    eq(it, perennialId)
                }
            }
            cache true
        }
        results[0]
    }

    static void printModelIdentifierCacheValue(String id, String method, String msg) {
        def cached = grailsCacheManager.getCache('modelIdentifier').get(id)
        println "${System.currentTimeMillis()}\t${Thread.currentThread().name}\t$method\t$msg: $cached"
    }

    static Set<String> populateFindByCriteria() {
        Set<String> result = new LinkedHashSet<>()
        result.addAll(['submissionId', 'publicationId'])
        for (String pit : PERENNIAL_IDENTIFIER_TYPES) {
            result.add(pit + "Id")
        }
        result
    }
}
