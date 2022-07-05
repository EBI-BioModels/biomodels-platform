/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.transaction.Transactional
import net.biomodels.jummp.scms.CmsContent
import org.weceem.content.WcmContent

@Transactional
class FeatureService {

    /**
     * Retrieves the content of the COVID-19 page under Browse menu
     *
     * <p>Temporarily, we store the content of this page as a News item. Using WcmContent domain class, it can be
     * retrieved by running the query. That News item has been set status Reviewed and had to keep the aliasuri as
     * covid-19 to make sure the related service still working.
     *
     * @return A String representing the content of the page
     */
    String getCovid19PageContent() {
        def newsQuery = """from WcmContent where aliasURI = :aliasuri and status.code = :code \
order by createdOn desc"""
        def newsItem = WcmContent.executeQuery(newsQuery, [aliasuri: 'covid-19', code: 200], [max: 1])
        newsItem[0]?.content
    }

    String getContentForReproducibilityPage() {
        def newsQuery = """from WcmContent where aliasURI = :aliasuri and status.code = :code \
order by createdOn desc"""
        def newsItem = WcmContent.executeQuery(newsQuery, [aliasuri: 'reproducibility', code: 200], [max: 1])
        newsItem[0]?.content
    }

    String getContentForFROGPage() {
        def newsQuery = """from WcmContent where aliasURI = :aliasuri and status.code = :code \
order by createdOn desc"""
        def newsItem = WcmContent.executeQuery(newsQuery, [aliasuri: 'fbc', code: 200], [max: 1])
        newsItem[0]?.content
    }

    List getContentForModelOfTheYear2022CompetitionPage() {
        def newsQuery = """FROM CmsContent where aliasURI = :aliasuri \
order by createdOn desc"""
        def newsItem = CmsContent.executeQuery(newsQuery, [aliasuri: 'model-of-the-year-2022-competition'], [max: 1])
        [newsItem[0]?.id, newsItem[0]?.content]
    }
}
