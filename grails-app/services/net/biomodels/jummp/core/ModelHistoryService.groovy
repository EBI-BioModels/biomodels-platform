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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Perf4j (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Perf4j used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import grails.transaction.Transactional
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelHistoryItem
import net.biomodels.jummp.plugins.security.User
import org.perf4j.aop.Profiled
import org.springframework.transaction.annotation.Propagation

/**
 * @short This class provides the service for the per user Model History.
 *
 * Whenever a user accesses a Model other services can use this service to
 * track that the user accessed the Model. The Model History itself is a
 * queue of the most recently used Models. The service provides methods to
 * retrieve the list of most recently accessed Models.
 *
 * The Model History functionality can be disabled through the config option
 * jummp.model.history.maxElements and is implicitly disabled for not logged in
 * users.
 *
 * @author Martin Graesslin <m.graesslin@dkfz.de>
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @see ModelHistoryItem
 */
@Transactional
class ModelHistoryService {
    /**
     * Dependency Injection of Spring Security Service
     */
    def springSecurityService
    /**
     * Dependency Injection of Grails Application
     */
    @SuppressWarnings("GrailsStatelessService")
    def grailsApplication

    /**
     * Adds a Model to the user's Model history.
     * If the Model is already included in the user's history, it will only be touched,
     * that is, it becomes the most recent accessed Model.
     *
     * If the history contains already the maximum number of elements, the oldest Model
     * is removed from the history.
     *
     * If the user is not logged in or if the feature is disabled completely this method
     * does nothing.
     * @param model The Model to add to the current user's history
     */
    @Profiled(tag="modelHistoryService.addModelToHistory")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void addModelToHistory(Model model) {
        // don't do anything for anonymous Users
        if (!springSecurityService.isLoggedIn()) {
            return
        }
        final int maxNumber = grailsApplication.config.get(
                "jummp.model.history.maxElements", 10)
        if (maxNumber == 0) {
            // Model History is disabled - do not return list
            return
        }
        User user = User.findByUsername(springSecurityService.authentication.name)
        ModelHistoryItem item = ModelHistoryItem.findByUserAndModel(user, model)
        if (item) {
            // we have already an item for this user and model: just touch the date
            item.touch()
            return
        }
        // test the number of items in the users history
        while (ModelHistoryItem.countByUser(user) >= maxNumber) {
            // exceeded the maximum number - drop oldest item
            ModelHistoryItem.findByUser(user, [sort: 'lastAccessedDate' ]).delete(flush: true)
        }
        ModelHistoryItem newItem = ModelHistoryItem.create(model, user)
        newItem.save()
    }

    /**
     *
     * @return Latest accessed Model for current user
     */
    @Profiled(tag="modelHistoryService.lastAccessedModel")
    @Transactional(readOnly = true)
    ModelTransportCommand lastAccessedModel() {
        // don't do anything for anonymous Users
        if (!springSecurityService.isLoggedIn()) {
            return new ModelTransportCommand()
        }
        if (grailsApplication.config.get("jummp.model.history.maxElements") == 0) {
            // if feature disabled, return empty ModelTransportCommand
            return new ModelTransportCommand()
        }
        User user = User.findByUsername(springSecurityService.authentication.name)
        ModelHistoryItem item = ModelHistoryItem.findByUser(user,
                [sort: "lastAccessedDate", order: "desc"])
        if (!item) {
            // history is empty
            return new ModelTransportCommand()
        }
        ModelTransportCommand cmd = turnModelToCommandObject(item.model, false)
        return cmd
    }

    /**
     *
     * @return The current user's Model history
     */
    @Profiled(tag="modelHistoryService.history")
    @Transactional(readOnly = true)
    List<ModelTransportCommand> history() {
        // don't do anything for anonymous Users
        if (!springSecurityService.isLoggedIn()) {
            return []
        }
        final int LIMIT = grailsApplication.config.get("jummp.model.history.maxElements", 10)
        if ( LIMIT == 0) {
            // if feature disabled, return empty list
            return []
        }
        User user = User.findByUsername(springSecurityService.authentication.name)
        List<ModelHistoryItem> history = ModelHistoryItem.findAllByUser(user,
                [sort: "lastAccessedDate", order: "desc", max: LIMIT])
        List<ModelTransportCommand> retList = []
        history.each { ModelHistoryItem it ->
            if (!it.model.deleted) {
                ModelTransportCommand cmd = turnModelToCommandObject(it.model, false)
                retList.add cmd
            }
        }
        return retList
    }

    private ModelTransportCommand turnModelToCommandObject(Model m, boolean toHistory = true) {
        new ModelAdapter(model: m).toCommandObject(toHistory)
    }
}
