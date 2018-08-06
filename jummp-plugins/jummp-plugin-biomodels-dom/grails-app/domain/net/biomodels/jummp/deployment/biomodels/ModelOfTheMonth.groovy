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

package net.biomodels.jummp.deployment.biomodels

import grails.persistence.Entity
import net.biomodels.jummp.model.Model

/**
 * @short Domain class for storing model of the month information
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Entity
class ModelOfTheMonth implements Serializable {
    static hasMany = [models: Model]
    final String DATE_FORMAT_PATTERN = 'yyyy-MM'

    String title
    String authors
    Date publicationDate
    Date lastUpdated
    String shortDescription
    byte[] previewImage

    static constraints = {
        shortDescription nullable: true, blank: true, maxSize: 1024
        previewImage nullable: true, blank: true
    }

    ModelOfTheMonthTransportCommand toCommandObject() {
        String date = publicationDate?.format(DATE_FORMAT_PATTERN)
        new ModelOfTheMonthTransportCommand(authors: authors, date: date)
    }
}
