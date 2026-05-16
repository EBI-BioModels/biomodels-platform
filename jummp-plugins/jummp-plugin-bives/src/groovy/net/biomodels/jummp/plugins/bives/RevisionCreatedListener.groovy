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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * groovy, Spring Framework, Grails, Bives (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, GNU GPL v3.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of groovy, Spring Framework, Grails, Bives used as well as
 * that of the covered work.}
 **/

package net.biomodels.jummp.plugins.bives

import net.biomodels.jummp.core.events.RevisionCreatedEvent
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationListener

/**
 * @short Listener for new revisions
 *
 * @author Robert Haelke, robert.haelke@googlemail.com
 * @author Mihai Glonț <mglont@ebi.ac.uk>
 * @author Tung Nguyen <tnguyen@ebi.ac.uk>
 * @date   04/12/2013
 */
class RevisionCreatedListener implements ApplicationListener<RevisionCreatedEvent> {
    /**
     * Dependency Injection of ModelDelegateService
     */
    def modelDelegateService
    /**
     * Dependency Injection of DiffDataService
     */
    def diffDataService

    private static final Logger LOGGER = LoggerFactory.getLogger(RevisionCreatedListener.class)

    void onApplicationEvent(RevisionCreatedEvent event) {
        if (event instanceof RevisionCreatedEvent) {
            LOGGER.info("The event identified by $event has been exposed at creating the revision $event.revision")
            /*RevisionTransportCommand revision = ((RevisionCreatedEvent) event).revision
            RepositoryFileTransportCommand files = revision.files.find{it.mainFile}
            //ensure there is a previous revision
            RevisionTransportCommand previous = modelDelegateService.getRevision(revision.model.id, revision.revisionNumber - 1)
            RepositoryFileTransportCommand previousFiles = previous.files.find{it.mainFile}
            File oldFile = new File(previousFiles.path)
            File newFile = new File(files.path)
            def diff = new SBMLDiff(oldFile, newFile)
            if (diff) {
                diff.mapTrees()
                println "BiVeS says the diff is " + diff.getHTMLReport()
            } else {
                println "no diff to create"
            }
            */
        }
    }
}
