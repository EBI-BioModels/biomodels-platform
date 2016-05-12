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
 */

package net.biomodels.jummp.annotation

import eu.ddmore.metadata.service.MetadataWriterImpl
import net.biomodels.jummp.core.MetadataSavingStrategy
import net.biomodels.jummp.core.annotation.StatementTransportCommand
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.core.model.RevisionTransportCommand

/**
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk> on 12/04/2016.
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk> on 12/04/2016.
 */
class SBMLWriter implements  MetadataSavingStrategy {
    @Override
    List<RepositoryFileTransportCommand> marshallAnnotations(RevisionTransportCommand revisionTC,
                                                             List<StatementTransportCommand> statementTransportCommands,
                                                             boolean isUpdate = false)
        throws UnsupportedOperationException {
        return null
    }

    public MetadataWriterImpl createMetadataWriter(RevisionTransportCommand revisionTC,
                                                   List<StatementTransportCommand> statements){
        return new MetadataWriterImpl()
    }
}
