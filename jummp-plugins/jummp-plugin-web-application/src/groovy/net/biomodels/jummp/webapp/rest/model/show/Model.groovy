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

import com.wordnik.swagger.annotations.*
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.core.model.ModelTransportCommand

@ApiModel(value = "Model")
class Model {
    @ApiModelProperty(value = "model name", required = true)
    String name
    @ApiModelProperty(value = "model description", required = true)
    String description
    @ApiModelProperty(value = "the format of the model")
    Format format
    @ApiModelProperty(value = "the scientific publication which describes this model.", required = false)
    String publication
    @ApiModelProperty(value = "the files that this model comprises")
    ModelFiles files
    @ApiModelProperty(value = "the version history of this model")
    History history
    Date firstPublished
    /** perennial model identifiers */
    String submissionId
    String publicationId

    public Model(RevisionTransportCommand revision, boolean isPrivate) {
        ModelTransportCommand model = revision.model
        name = revision.name
        description = revision.description
        format = new Format(revision.format)
        if (model.publication) {
            publication = model.publication.linkProvider.identifiersPrefix ?
                          model.publication.linkProvider.identifiersPrefix + model.publication.link :
                          model.publication.link
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
}
