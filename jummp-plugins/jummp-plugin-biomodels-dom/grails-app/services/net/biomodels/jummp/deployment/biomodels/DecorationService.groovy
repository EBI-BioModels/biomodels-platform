/**
* Copyright (C) 2010-2017 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.transaction.Transactional
import groovy.time.TimeCategory
import net.biomodels.jummp.core.adapters.ModelAdapter
import net.biomodels.jummp.core.model.ModelTransportCommand
import net.biomodels.jummp.model.Model
import org.perf4j.aop.Profiled

/**
 * @short Service responsible for retrieving necessary data to design front page and
 * other static pages.
 *
 * This class  is used for accessing database and resulting required data aiming to
 * populate in home page. The example of this use is to get the recently published and accessed models.
 *
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional(readOnly = true)
class DecorationService {
    /**
     * get 7 of the most accessed models from the last six months
     * @return A map of ModelTransportCommand associating with their hits
     */
    @Profiled(tag = 'decorationService.getRecentlyAccessedModels')
    Map<String, String> getRecentlyAccessedModels() {
        String query ='''
SELECT
    coalesce(m.publicationId, m.submissionId) as modelId,
    rev.name
FROM
    ModelAudit AS ma
    JOIN ma.model AS m
    JOIN m.revisions AS rev
WHERE
  ma.dateCreated BETWEEN :then AND :now AND
  ma.success = 1 AND
  rev.id IN(
     SELECT aoi.objectId
        FROM
            AclEntry AS ace
            JOIN ace.aclObjectIdentity AS aoi
            JOIN aoi.aclClass AS aclClass
            JOIN ace.sid AS sid
        WHERE
            aclClass.className = 'net.biomodels.jummp.model.Revision'
            AND sid.sid = 'ROLE_ANONYMOUS'
            AND ace.mask = 1)
GROUP BY rev.model
'''
        def now = new Date()
        def then = null
        use(TimeCategory) {
            then = now - 6.months
        }
        def matchedModels = Model.executeQuery(query, [then: then, now: now, max: 7]) as List<List>
        Map<String, String> returnedModels = new LinkedHashMap<>()
        matchedModels.each { List<String> row ->
            String id = row[0]
            String name = row[1]
            returnedModels.put(id, name)
        }
        returnedModels
    }

    /**
     * get 7 of the most recently published models
     * @return A map of ModelTransportCommand associating with latest published date
     */
    @Profiled(tag = 'decorationService.getRecentlyPublishedModels')
    Map<ModelTransportCommand, ModelLatestPublished> getRecentlyPublishedModels() {
        String query = '''
SELECT model, max(model.firstPublished), rev.name
FROM Model AS model
JOIN model.revisions AS rev
WHERE
  rev.id IN(
     SELECT aoi.objectId
        FROM
			AclEntry AS ace
			JOIN ace.aclObjectIdentity AS aoi
            JOIN aoi.aclClass AS aclClass
            JOIN ace.sid AS sid
        WHERE
            aclClass.className = 'net.biomodels.jummp.model.Revision'
            AND sid.sid = 'ROLE_ANONYMOUS'
            AND ace.mask = 1)
GROUP BY model
ORDER BY model.firstPublished DESC'''
        def matchedModels = Model.executeQuery(query, [max: 7])
        Map<ModelTransportCommand, ModelLatestPublished> returnedModels = new HashMap<ModelTransportCommand, ModelLatestPublished>()
        matchedModels.each {
            Model model = it[0]
            Date latestPublished = it[1]
	        ModelTransportCommand mtc = new ModelAdapter(model: model).toCommandObject(false)
            String modelName = it[2]
            returnedModels.put(mtc, new ModelLatestPublished(modelName, latestPublished))
        }
        returnedModels
    }
}

class ModelLatestPublished {
    String modelName
    Date latestAccessedDate

    ModelLatestPublished(String modelName, Date latestAccessedDate) {
        this.modelName = modelName
        this.latestAccessedDate = latestAccessedDate
    }
}
