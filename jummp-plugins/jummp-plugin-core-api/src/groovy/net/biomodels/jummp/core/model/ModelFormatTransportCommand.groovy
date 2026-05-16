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
 * @short Wrapper for a ModelFormat to be transported through JMS.
 *
 * Small wrapper class to decouple the ModelFormat from the Database.
 * Changes to instances of this class are not populated to the database.
 *
 * The object can also be used as a command object for the web interface.
 *
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 * @author Mihai Glonț  <mihai.glont@ebi.ac.uk>
 */
class ModelFormatTransportCommand implements Serializable {
    private static final long serialVersionUID = 1L
    /**
     * The id in the database
     */
    Long id
    /**
     * A machine readable format name, to be used in the application. E.g. SBML
     */
    String identifier
    /**
     * A human readable more spoken name. E.g. Systems Biology Markup Language
     */
    String name
    /**
     * The version of the format in question.
     */
    String formatVersion

    String toString() {
        "$id: $name".toString()
    }
}
