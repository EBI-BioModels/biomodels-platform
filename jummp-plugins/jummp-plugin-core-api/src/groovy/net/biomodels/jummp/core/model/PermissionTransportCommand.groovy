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
**/





package net.biomodels.jummp.core.model

/**
 * @short Simple representation of a model permission.
 *
 * Captures information required on the model share page --  who has access to a model,
 * the access type and whether this permission can be revoked.
 *
 * @author raza ali <raza.ali@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tnguyen@ebi.ac.uk>
 */
class PermissionTransportCommand {
    // the user full name or user real name
    String name
    // the user name
    String username
    // the user id
    int id
    boolean read = false
    boolean write = false
    boolean disabledEdit = false
    boolean show = true
}
