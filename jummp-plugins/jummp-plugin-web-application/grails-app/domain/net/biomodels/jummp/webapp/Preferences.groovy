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
**/





package net.biomodels.jummp.webapp
import net.biomodels.jummp.plugins.security.User
/**
 * @short Representation of a preferences of a user
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
class Preferences implements Serializable {
    int numResults
    static belongsTo = [user:User]

    static constraints = {
        numResults(nullable: false)
    }

    public static Preferences getDefaults() {
    	return new Preferences([numResults: 10])
    }

	public static List getOptions(String preference) {
		if (preference == "numResults") {
            // remember to change places referring to these values, for instance, SearchController
            // the minimum or maximum value in the following list must comply with the one used for the references
			return [10, 20, 50, 75, 100]
		}
		return null
	}
}
