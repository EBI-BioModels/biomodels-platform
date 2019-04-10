/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.deployment.biomodels

import grails.transaction.Transactional
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.plugins.security.User
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * Service for handling associations between models and labels
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional
class ModelTagService {
    private static final Log log = LogFactory.getLog(ModelTagService.class)

    List<String> getTagsByModelId(String modelId) {
        Model model = Model.findBySubmissionId(modelId)
        ModelTag.findAllByModel(model)?.collect {
            it.tag.name
        }
    }

    Map update(Set<String> updatedTags, String modelId, User user) {
        String message = ""
        int statusCode = 0
        Set<ModelTag> modelTags = findAllByModelId(modelId)
        if (modelTags?.size() > 0 || updatedTags?.size() > 0) {
            Set records = updateModelTag(updatedTags, modelId, user)
            if (records?.size() == updatedTags?.size()) {
                statusCode = 200
                if (records?.isEmpty()) {
                    message = "The model has no longer been tagged any label"
                } else {
                    message = "Labels [${updatedTags.join(', ')}] have been applied successfully to the model"
                }
            } else {
                statusCode = 400
                message = """\
There have been errors while trying to update choosen labels for the model '${modelId}'"""
            }
        } else {
            statusCode = 422
            message = """\
Cannot save nothing for labels to the model"""
        }
        Map result = [:]
        result["status"] = statusCode
        result["message"] = message
        result
    }

    private Set<ModelTag> updateModelTag(Set<String> updatedTags, String modelSubmissionId, User user) {
        Model model = Model.findBySubmissionId(modelSubmissionId)
        Set existing = ModelTag.findAllByModel(model).toSet()
        Set<ModelTag> result = new HashSet<ModelTag>()
        if (existing?.isEmpty()) {
            log.info("""\
None of these tags (i.e. ${updatedTags.join(", ")}) have been assigned to the model ${modelSubmissionId} yet, 
so we will persist all the tags into database""")
            result = insertFromTags(model, user, updatedTags)
        } else {
            log.info("Try to merge/reconcile the existing and updated ones")
            Set preserved = existing.findAll {
                it.tag.name in updatedTags && it.model.submissionId == modelSubmissionId
            }
            log.debug("Preserved Tags: ${preserved?.collect { it.tag.name }?.join(', ')}")
            Set removed = existing - preserved
            log.debug("Removed Tags: ${removed.collect { it.tag.name }?.join(', ')}")
            Set preservedTags = preserved.collect { it.tag.name }
            Set insertedTags = updatedTags - preservedTags
            log.debug("Inserted Tags: ${insertedTags.join(', ')}")
            result.addAll(preserved)
            Set newlyInserted = reconcile(model, user, removed, insertedTags)
            result.addAll(newlyInserted)
        }
        result
    }

    private Set<ModelTag> reconcile(Model model, User user, Set<ModelTag> removed, Set<String> insertedTags) {
        // basically we will delete the removed tags and insert the new ones
        ModelTag.deleteAll(removed)
        if (insertedTags?.isEmpty()) {
            return [] as Set
        }
        Set newlyInserted = insertFromTags(model, user, insertedTags)
        newlyInserted
    }

    private Set<ModelTag> insertFromTags(Model model, User user, Set<String> insertedTags) {
        Set<ModelTag> result = new HashSet<ModelTag>()
        insertedTags.each { String name ->
            Tag tagObj = Tag.findOrSaveByName(name)
            tagObj.userCreated = user
            tagObj.save(flush: true)
            ModelTag modelTag = ModelTag.findOrCreateByTagAndModel(tagObj, model)
            if (modelTag.save(flush: true)) {
                result.add(modelTag)
            } else {
                println modelTag.errors.allErrors.inspect().toString()
                log.error("""\
There have been errors while trying to persist tag: '${name}' for the model '${model.submissionId}'""")
            }
        }
        result
    }

    private Set<ModelTag> findAllByModelId(String modelId) {
        Model model = Model.findBySubmissionId(modelId)
        List<ModelTag> records = ModelTag.findAllByModel(model)
        records?.toSet()
    }
}
