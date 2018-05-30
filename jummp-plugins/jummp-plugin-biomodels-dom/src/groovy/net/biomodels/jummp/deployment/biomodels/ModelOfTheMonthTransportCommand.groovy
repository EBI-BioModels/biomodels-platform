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

package net.biomodels.jummp.deployment.biomodels

/**
 * @short Data transfer object (DTO) for ModelOfTheMonth domain class.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
@groovy.transform.CompileStatic
class ModelOfTheMonthTransportCommand implements Serializable {
    static final String URL_SEED =
            "http://www.ebi.ac.uk/biomodels-main/static-pages.do?page=ModelMonth%2F"
    static final String FALLBACK_URL = "http://www.ebi.ac.uk/biomodels-main/modelmonth"
    String authors
    String date

    final String getFormattedURL() {
        return date ? "$URL_SEED$date" : FALLBACK_URL
    }

    String toString() {
        "$date"
    }
}
