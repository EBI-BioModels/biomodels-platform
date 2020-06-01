/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 * groovy, Spring Framework, Grails, Bives (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, GNU GPL v3.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 *{Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of groovy, Spring Framework, Grails, Bives used as well as
 * that of the covered work.}**/

package net.biomodels.jummp.core.subscribers

import net.biomodels.jummp.core.events.RevisionCreatedEvent
import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener

/**
 * @short Subscribes to the event of creating a new revision
 *
 * <p>This class implements a Subscriber of Publisher/Subscriber pattern which is used to
 * grant writable permission to fellow curators if the revision is created and submitted
 * by a curator. The model and revision will be shared to our curators when users submit
 * for a publication. However, a curator submitting and publishing a model naturally does
 * not need to so by himself. Therefore, an automatic process granting writable permission
 * fellow curators is reasonable.
 *
 * @author <a href="mailto:nvntung@gmail.com">Tung Nguyen</a>
 * @date 22/05/2020
 */
class ShareRevisionToFellowCurators implements ApplicationListener<RevisionCreatedEvent> {
    private static Logger logger = LoggerFactory.getLogger(ShareRevisionToFellowCurators.class)
    def modelDelegateService

    void onApplicationEvent(RevisionCreatedEvent event) {
        if (event instanceof RevisionCreatedEvent) {
            logger.debug("""\
The event identified by $event has been exposed at creating the revision ${event.revision?.identifier()}""")
            RevisionTransportCommand command = ((RevisionCreatedEvent) event).revision
            if (command) {
                String owner = command.owner
                String modelId = command.identifier()
                logger.debug("""\
Curator ${owner} has shared the model ${modelId} to other curators with writable access""")
                modelDelegateService.submitModelRevisionForPublication(command)
            } else {
                String message = """\
Unknown error (i.e. model revision is null) has happened when trying to share a model submitted by a curator to fellow
curators"""
                logger.error(message)
            }
        }
    }
}
