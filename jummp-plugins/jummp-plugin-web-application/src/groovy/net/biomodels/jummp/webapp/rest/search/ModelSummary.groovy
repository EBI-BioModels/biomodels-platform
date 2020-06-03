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

package net.biomodels.jummp.webapp.rest.search

import net.biomodels.jummp.core.adapters.ModelFormatAdapter
import net.biomodels.jummp.core.model.ModelFormatTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand as MTC
import net.biomodels.jummp.model.ModelFormat
import org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ModelSummary {
    String id
    String url
    String name
    String format
    String submitter
    Date submissionDate
    Date lastModified

    private final static Logger logger = LoggerFactory.getLogger(ModelSummary.class)

    ModelSummary(MTC model) {
        id = (model.publicationId) ?: (model.submissionId)
        def linker = new ApplicationTagLib()
        url = linker.createLink(controller: 'model', action: 'show', absolute: 'true', id: id)
        name = model.name

        ModelFormatTransportCommand modelFormat
        if (model.format) {
            modelFormat = model.format
        } else {
            logger.debug("Model ${id} (${name}) was not assigned any format")
            ModelFormat other = ModelFormat.findByIdentifier("UNKNOWN")
            modelFormat = new ModelFormatAdapter(format: other).toCommandObject()
        }
        format = modelFormat.name
        submitter = model.submitter
        submissionDate = model.submissionDate
        lastModified = model.lastModifiedDate
    }
}
