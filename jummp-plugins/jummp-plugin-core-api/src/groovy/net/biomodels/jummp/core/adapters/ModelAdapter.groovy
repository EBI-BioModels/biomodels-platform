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

import grails.gorm.DetachedCriteria
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Publication
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.identifier.*
import grails.util.Holders
/**
 * @short Adapter class for the Model domain class
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
public class ModelAdapter extends DomainAdapter {
    Model model

    static final Set<String> PERENNIAL_IDENTIFIER_TYPES = ModelIdentifierUtils.perennialFields
    static final Set<String> FIND_BY_PERENNIAL_ID_CRITERIA = populateFindByCriteria()

    def modelService = Holders.getGrailsApplication().mainContext.modelService


    ModelTransportCommand toCommandObject(boolean saveHistory = true) {
        // TODO: is it correct to show the latest upload date as the lastModifiedDate or does it need ACL restrictions?
        Set<String> creators = []
        Set<String> creatorUsernames = []
        if (model.revisions) {
            model.revisions.each { revision ->
                creators.add(revision.owner.person.userRealName)
                creatorUsernames.add(revision.owner.username)
            }
        }
        def latestRev
        def firstRev
        Long modelId = model.id
        boolean modelIsSaved = null != modelId && Model.exists(modelId)
        if (modelIsSaved) {
            latestRev = modelService?.getLatestRevision(model, saveHistory)
        } else {
            // if the model is not saved, there can only be at most one revision
            latestRev = model.revisions ? model.revisions[0] : null
            firstRev = model.revisions ? model.revisions[0] : null
        }
        if (!firstRev && model.revisions) {
            firstRev = model.revisions.sort { it.revisionNumber }.first()
        }
        return new ModelTransportCommand(
            id: model.id,
            submissionId: model.submissionId,
            publicationId: model.publicationId,
            firstPublished: model.firstPublished,
            name: latestRev ? latestRev.name : null,
            state: latestRev ? latestRev.state : null,
            lastModifiedDate: latestRev ? latestRev.uploadDate : null,
            format: latestRev ? getAdapter(latestRev.format).toCommandObject() : null,
            publication: model.publication ? getAdapter(model.publication).toCommandObject() : null,
            deleted: model.deleted,
            submitter: firstRev?.owner.person.userRealName,
            submitterUsername: firstRev?.owner.username,
            submissionDate: firstRev?.uploadDate,
            creators: creators,
            creatorUsernames: creatorUsernames
        )
    }

    /**
     * Convenience method for finding a model based on its externally-defined identifiers.
     *
     * @param perennialId The externally-defined ID by which to look up the model.
     * @return  the model corresponding to the given id, or null if there was no match
     */
     static Model findByPerennialIdentifier(String perennialId) {
         if (!perennialId) {
             return null
         }
         perennialId = perennialId.contains("\\.") ? perennialId : perennialId.split("\\.")[0]
         Model model = Model.createCriteria().get {
             or {
                 FIND_BY_PERENNIAL_ID_CRITERIA.each {
                     eq(it, perennialId)
                 }
             }
         }
         model
    }

    static Set<String> populateFindByCriteria() {
        def result = PERENNIAL_IDENTIFIER_TYPES.collect { it + "Id" }
        ['submissionId', 'publicationId'].each {
            if (!result.contains(it)) {
                result.add it
            }
        }
        return result
    }
}
