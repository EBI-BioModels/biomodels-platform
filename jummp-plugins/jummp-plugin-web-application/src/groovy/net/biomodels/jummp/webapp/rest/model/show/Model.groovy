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

package net.biomodels.jummp.webapp.rest.model.show

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.user.PersonTransportCommand

class Model {
    String name
    String description
    Format format
    Publication publication
    ModelFiles files
    History history
    Date firstPublished
    /** perennial model identifiers */
    String submissionId
    String publicationId

    Model(RevisionTransportCommand revision, boolean isPrivate) {
        ModelTransportCommand model = revision.model
        name = revision.name
        description = revision.description
        format = new Format(revision.format)
        if (model.publication) {
            PublicationTransportCommand pubTC = model.publication
            publication = new Publication(pubTC)
            publication.link = model.publication.linkProvider.identifiersPrefix ?
                          pubTC.linkProvider.identifiersPrefix + pubTC.link :
                          pubTC.link
            pubTC.authors.each { PersonTransportCommand personTC ->
                publication.authors << new PublicationAuthor(personTC)
            }
        }
        if (isPrivate) {
            files = new ModelFiles()
        } else {
            files = new ModelFiles(revision.files.findAll{ !it.hidden })
        }
        history = new History(model.submissionId)
        submissionId = model.submissionId
        publicationId = model.publicationId
        firstPublished = model.firstPublished
    }

    String outputModelAsString(String contentType) {
        // the contentType is either "application/json" or "application/xml"
        ObjectMapper mapper = contentType == "application/json" ? new ObjectMapper() : new XmlMapper()
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        String result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
        result
    }
}
