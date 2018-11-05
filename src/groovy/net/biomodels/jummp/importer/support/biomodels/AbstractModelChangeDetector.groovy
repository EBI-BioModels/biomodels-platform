/*
 * Copyright (C) 2010-2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.importer.support.biomodels

import groovy.transform.CompileStatic
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This abstract class aims to generalising model change detector. It is extended by concrete classes
 * which handle a single updated attribute such as model name, publication identifiers.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 *
 * Created by Mihai Glonț on 08/02/18.
 * Updated by Tung nguyen on 04/06/18.
 */
@CompileStatic
abstract class AbstractModelChangeDetector implements ModelChangeDetector {
    protected final Logger logger = LoggerFactory.getLogger(AbstractModelChangeDetector.class)
    ModelChangeDetector next

    @Override
    /**
     * Default implementation of {@link ModelChangeDetector#hasChanged(net.biomodels.jummp.importer.support.biomodels.ModelComparisonContext)}.
     *
     * Concrete subclasses are expected to return true if they find a change.
     * Otherwise, they should not return false directly but should invoke
     * this method (i.e. return super.hasChanged(context)), so that subsequent
     * detectors' hasChanged method can be called. If there are no further
     * detectors to delegate to, this method will return false.
     */
    boolean hasChanged(ModelComparisonContext comparison) {
        boolean result = next?.hasChanged(comparison)
        if (result) {
            logger.info("{} reports that {} has changed", this, comparison.properties)
        }
        result
    }
}
