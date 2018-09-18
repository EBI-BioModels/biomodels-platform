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
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@grails.validation.Validateable
class ModelOfTheMonthTransportCommand implements Serializable {
    static final String URL_SEED =
            "content/model-of-the-month?"
    static final String FALLBACK_URL = "content/model-of-the-month?all=yes"
    public static final String SEP = '-'

    Long id
    String title
    String authors
    Date publicationDate
    Date lastUpdated
    String date
    String shortDescription
    byte[] previewImage
    String mimeType
    boolean updated
    Map<Long, String> models

    static constraints = {
        importFrom(ModelOfTheMonth)
        id nullable: true
        mimeType nullable: true
    }

    final String getFormattedURL() {
        if (date?.isEmpty() || !date?.contains(SEP)) return FALLBACK_URL
        String[] yearAndMonth = date?.split(SEP)
        if (yearAndMonth?.length > 2) {
            return FALLBACK_URL
        }

        String year = yearAndMonth[0]
        String month = yearAndMonth[1]

        return "${URL_SEED}year=$year&month=$month"
    }

    String getMonth() {
        publicationDate?.format(ModelOfTheMonth.DATE_FORMAT_PATTERN)
    }
}
