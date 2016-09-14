/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





package net.biomodels.jummp.search

import net.biomodels.jummp.core.ModelSearchStrategy
import net.biomodels.jummp.core.events.ModelOperationEvent
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.context.ApplicationListener

/**
 * @short Singleton-scoped facade for interacting with a OmicsdiHolder's instance.
 *
 * This class provides means of indexing and querying generic information about
 * models.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 * @date   12/09/2016
 */

class OmicsdiBasedSearch implements ModelSearchStrategy, ApplicationListener<ModelOperationEvent> {
    /**
     * The class logger.
     */
    static final Log log = LogFactory.getLog(SolrBasedSearch)
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()
    /**
     * Flag indicating the logger's verbosity threshold.
     */
    static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * The OmicsDI Request Handler to use for handling searches.
     */
    def omicsDiHolder

    void onApplicationEvent(ModelOperationEvent event) {

    }

    void clearIndex() {
        omicsDiHolder.doSomeStuff()
    }

    void clearAnnotationStatementsFromDatabase() {

    }

    void regenerateIndices() {
    }

    Collection<ModelTransportCommand> searchModels(String query) {
        return new ArrayList<ModelTransportCommand>()
    }

    void updateIndex(RevisionTransportCommand revision) {
    }
}
