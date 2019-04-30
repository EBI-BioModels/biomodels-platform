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





package net.biomodels.jummp.core.model


/**
 * @short Wrapper for a RepositoryFile that can be transported through JMS.
 *
 * Small wrapper class to decouple the RepositoryFile from the database.
 * Changes to instances of this class are not populated to the database.
 *
 * The object can also be used as a command object for the web interface.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @date 20130613
 */
class RepositoryFileTransportCommand implements Serializable {
    private static final long serialVersionUID = 1L

    Long id
    String path
    // this property is used for tracking the actual file name of the file
    String filename
    long size
    boolean showPreview
    String description
    boolean hidden
    boolean mainFile
    boolean userSubmitted
    String mimeType
    RevisionTransportCommand revision
    
    boolean equals(Object obj) {
        String fileName=(new File(path)).getName()
        String testName=(new File(obj.path)).getName()
        return fileName == testName
    }
    
}
