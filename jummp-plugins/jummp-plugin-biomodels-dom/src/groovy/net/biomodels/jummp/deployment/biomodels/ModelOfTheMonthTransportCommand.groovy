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

import grails.util.Holders

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

    /**
     * Dependency injection of modelDelegateService
     */
    transient def modelDelegateService = Holders.grailsApplication.mainContext.modelDelegateService

    Long id
    String title
    String authors
    Date publicationDate
    Date lastUpdated
    /**
     * The calendar month for this entry -- e.g. 2010-05
     */
    String formattedEntryDate
    String shortDescription
    String previewImage
    String mimeType
    boolean updated
    /**
     * The keys represent the models' id (primary key) and the values are the models' perennial identifier.
     */
    transient Map<Long, String> associatedModelMap
    String models

    static constraints = {
        importFrom(ModelOfTheMonth)
        id nullable: true
        mimeType nullable: true
        models blank: false, validator: { String ids, ModelOfTheMonthTransportCommand cmd ->
            def modelIdList = Arrays.asList(ids.split(', '))
            def associationMap = cmd.modelDelegateService.findModelsByPerennialId(modelIdList)
            if (modelIdList.size() == associationMap?.size()) {
                cmd.associatedModelMap = associationMap
                return true
            }
            return false
        }
        formattedEntryDate nullable: true, validator: { String ignoredValue, ModelOfTheMonthTransportCommand cmd ->
            if (!cmd.publicationDate) {
                return false
            }
            cmd.formattedEntryDate = cmd.publicationDate.format(ModelOfTheMonth.DATE_FORMAT_PATTERN)
            return true
        }
        previewImage nullable: true, validator: {String img, ModelOfTheMonthTransportCommand cmd ->
            def mimeTypePattern = /^image\//
            def m = cmd.mimeType =~ mimeTypePattern
            return m.count >= 0
        }
    }

    final String getFormattedURL() {
        if (formattedEntryDate?.isEmpty() || !formattedEntryDate?.contains(SEP)) return FALLBACK_URL
        String[] yearAndMonth = formattedEntryDate?.split(SEP)
        if (yearAndMonth?.length > 2) {
            return FALLBACK_URL
        }

        String year = yearAndMonth[0]
        String month = yearAndMonth[1]

        return "${URL_SEED}year=$year&month=$month"
    }

    String getYearMonth() {
        formattedEntryDate
    }
}
