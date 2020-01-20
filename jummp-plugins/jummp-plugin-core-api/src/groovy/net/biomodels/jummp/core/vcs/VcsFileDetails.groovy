/**
* Copyright (C) 2010-2013 EMBL-European Bioinformatics Institute (EMBL-EBI),
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





package net.biomodels.jummp.core.vcs

/**
 * @short Small class to encapsulate file details field in the VCS
 *
 * Small wrapper to encapsulate file details.
 *
 * The object can also be used as a command object for the web interface.
 * For example: The information shown in the History tab of the model display page
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Tung Nguyen <tnguyen@ebi.ac.uk>
 */
class VcsFileDetails implements Serializable {
    private static final long serialVersionUID = 1L
    // this property is equivalent to commit hash id
    String revisionId
    // this property is equivalent to the timestamp
    long commit
    // this property holds the commit message
    String msg
}
