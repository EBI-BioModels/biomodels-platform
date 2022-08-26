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
import net.biomodels.jummp.core.model.ModelTransportCommand as ModelTC
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelTag
import net.biomodels.jummp.model.Tag
import net.biomodels.jummp.plugins.security.User
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.transaction.TransactionDefinition

/**
 * Service for handling the associations between models and labels/tags
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional
class ModelTagService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModelTagService.class)


    List<String> getTagsByModelId(String modelId) {
        Model model = Model.findBySubmissionId(modelId)
        List<String> result = findAllByModel(model)?.collect {
            it.tag.name
        }
        result.sort()
    }

    Map saveOrUpdate(ModelTagTransportCommand command, User user) {
        String message = ""
        int statusCode = 0
        Model model = Model.findBySubmissionId(command.modelId)
        Set<ModelTag> modelTags = findAllByModel(model)
        /**
         * We have some common use cases
         *
         * Use case 1: Update the model associated some tags
         * Use case 2: Remove all tags associated with the model
         */
        if (modelTags?.size() > 0 || command.tags?.size() > 0) {
            Set records = doSaveOrUpdate(model, modelTags, command, user)
            if (records?.size() == command.tags?.size()) {
                statusCode = 200
                if (records?.isEmpty()) {
                    message = "The model has no longer been tagged any label"
                } else {
                    String chainOfTags = command.tags.collect { it.name }.join(', ')
                    message = "Labels [${chainOfTags}] have been applied successfully to the model"
                }
            } else {
                statusCode = 400
                message = """\
There have been errors while trying to update choosen labels for the model '${command.modelId}'"""
            }
        } else {
            /**
             * This case means uses are trying to click Save button on the model having not been associated
             * any tags yet
             */
            statusCode = 422
            message = "Cannot save nothing for labels to the model"
        }
        Map result = [:]
        result["status"] = statusCode
        result["message"] = message
        result
    }

    Map update(Set<String> updatedTags, String modelId, User user) {
        String message = ""
        int statusCode = 0
        Model model = Model.findBySubmissionId(modelId)
        Set<ModelTag> modelTags = findAllByModel(model)
        if (modelTags?.size() > 0 || updatedTags?.size() > 0) {
            Set records = updateModelTag(updatedTags, model, user)
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

    Map remove(Set<String> tags, String modelId, User user) {
        String message = ""
        int statusCode = 0
        Model model = Model.findBySubmissionId(modelId)
        if (!model) {
            message = "Requested model $modelId doesn't exist."
            statusCode = -1
        } else if (tags?.isEmpty()) {
            message = "None of tags is removed."
        } else {
            Set<ModelTag> tagsRemoved = new HashSet<>()
            String query = "from ModelTag as mt where mt.model = :model and mt.tag.name in :tags"
            tagsRemoved = ModelTag.executeQuery(query, [model: model, tags: tags]).toSet()
            removeModelTag(tagsRemoved, user)
        }
        Map result = [message: message, statusCode: statusCode]
        result
    }

    /**
     * This method looks for all tags assigned to a given model
     * @param model ModelTC object
     * @return a set of TagTransportCommand objects
     */
    Set<TagTransportCommand> findTagsByModel(ModelTC model) {
        String query = "from ModelTag as mt where mt.model.submissionId=? order by mt.tag.name"
        List<ModelTag> result = ModelTag.findAll(query, [model.submissionId])
        List<TagTransportCommand> tagCmdList = result.collect {
            TagTransportCommand.fromTag(it.tag)
        }
        List sortedTagList = tagCmdList.sort { cmd1, cmd2 -> cmd1.name <=> cmd2.name }
        Set tagSet = new LinkedHashSet()
        tagSet.addAll(sortedTagList)
        tagSet
    }

    private void removeModelTag(Set<ModelTag> modelTags, User user) {
        modelTags.each { mt ->
            String queryString = "delete ModelTag mt where mt.model = :model and mt.tag = :tag"
            ModelTag.executeUpdate(queryString, [model: mt.model, tag: mt.tag])
            LOGGER.info("Removed the '${mt.tag.name}' tag from the model ${mt.model.submissionId} by ${user.username}")
        }
    }

    private Set<ModelTag> updateModelTag(Set<String> updatedTags, Model model, User user) {
        Set existing = findAllByModel(model)
        Set<ModelTag> result = new HashSet<ModelTag>()
        if (existing?.isEmpty()) {
            LOGGER.info("""\
These tags (i.e. ${updatedTags.join(", ")}) haven't presented in BioModels. \
Therefore, they are going to be persisted into BioModels and assigned to the model ${model.submissionId}.""")
            result = insertFromTags(model, user, updatedTags)
        } else {
            LOGGER.info("Try to merge/reconcile the existing and updated ones.")
            Set preserved = existing.findAll {
                it.tag.name in updatedTags && it.model.submissionId == model.submissionId
            }
            LOGGER.debug("Preserved Tags: ${preserved?.collect { it.tag.name }?.join(', ')}")
            Set removed = existing - preserved
            LOGGER.debug("Removed Tags: ${removed.collect { it.tag.name }?.join(', ')}")
            Set preservedTags = preserved.collect { it.tag.name }
            Set insertedTags = updatedTags - preservedTags
            LOGGER.debug("Inserted Tags: ${insertedTags.join(', ')}")
            result.addAll(preserved)
            Set newlyInserted = reconcile(model, user, removed, insertedTags)
            result.addAll(newlyInserted)
        }
        result
    }

    private Set doSaveOrUpdate(Model model, Set modelTags, ModelTagTransportCommand command, User user) {
        if (modelTags?.size()) {
            ModelTag.deleteAll(modelTags)
        }
        Set<ModelTag> result = new LinkedHashSet<ModelTag>()
        if (command.tags?.size()) {
            Set newTags = persistModelAndTagFromCommandObject(model, command, user)
            result.addAll(newTags)
        }
        result
    }

    private Set<ModelTag> persistModelAndTagFromCommandObject(Model model, ModelTagTransportCommand cmd, User user) {
        List result = cmd.tags.collect { TagTransportCommand tagCmd ->
            Map cond = [name: tagCmd.name]
            if (tagCmd?.id) {
                cond["id"] = tagCmd.id
            }
            Tag tag = Tag.findOrCreateWhere(cond)
            if (!tag.id) {
                tag.description = tagCmd.description
                tag.userCreated = user
                tag.dateCreated = new Date()
                tag.dateModified = new Date()
                tag.save(flush: true)
            }
            new ModelTag(model: model, tag: tag)
        }
        ModelTag.saveAll(result).toSet()
    }

    private Set<ModelTag> insertFromTags(Model model, User user, Set<String> insertedTags) {
        Set<ModelTag> result = new HashSet<ModelTag>()

        insertedTags.each { String name ->
            Tag tagObj = Tag.findOrCreateWhere(name: name)
            if (!tagObj.id) {
                tagObj.description = "Collection of the models relevant to $name"
                tagObj.userCreated = user
                tagObj.dateCreated = new Date()
                tagObj.dateModified = new Date()
                tagObj = doSave(tagObj)
            }
            ModelTag modelTag = ModelTag.findOrCreateByTagAndModel(tagObj, model)
            if (modelTag.save(flush: true)) {
                result.add(modelTag)
            } else {
                println modelTag.errors.allErrors.inspect().toString()
                LOGGER.error("""\
There have been errors while trying to persist tag: '${name}' for the model '${model.submissionId}'.""")
            }
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

    private Set<ModelTag> findAllByModel(Model model) {
        List<ModelTag> records = ModelTag.findAllByModel(model)
        records?.toSet()
    }

    private Tag doSave(Tag tagObj) {
        def txDef = [
            // this tx will use a different session than the current one
            propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW
        ]
        Tag.withTransaction(txDef) {
            tagObj.save(flush: true)
        }
        tagObj
    }
}
