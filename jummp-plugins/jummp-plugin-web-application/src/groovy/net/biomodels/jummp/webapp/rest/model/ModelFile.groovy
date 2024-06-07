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

package net.biomodels.jummp.webapp.rest.model

import net.biomodels.jummp.core.model.RepositoryFileTransportCommand
import net.biomodels.jummp.utils.FileUtils

class ModelFile {
    String name
    String description
    String fileSize
    String mimeType
    String md5sum
    String sha1sum
    String sha256sum

    ModelFile(RepositoryFileTransportCommand file) {
        File f = new File(file.path)
        name = f.getName()
        description = file.description
        mimeType = file.mimeType
        fileSize = f.length()
        sha1sum = FileUtils.checksum(f, "SHA-1")
        sha256sum = FileUtils.checksum(f, "SHA-256")
        md5sum = FileUtils.checksum(f, "MD5")
    }
}
