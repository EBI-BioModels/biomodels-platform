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





package net.biomodels.jummp.deployment.biomodels

import grails.transaction.Transactional
import grails.util.Holders
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision

@Transactional
class DecorationService {

    static String getPopularModels() {
        String query = '''
SELECT ma.model, COUNT(*) as hits
FROM ModelAudit AS ma
JOIN ma.model AS model
JOIN model.revisions rev
WHERE
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
GROUP BY ma.model
ORDER BY hits DESC
'''
        def matchedModels = Model.executeQuery(query, [max: 10])
        String result = ""
        def g = Holders.grailsApplication.mainContext.getBean('org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib')
        if (matchedModels.size()) {
            matchedModels.each {
                Model model = it[0]
                int nbAccessed = it[1]
                Revision theLatestRevision = model.revisions.last()
                String modelUrl = g.createLink(controller: 'model', id: model.publicationId ?: model.submissionId, action: 'show')
                String modelLink= '<a href=' + '"' + modelUrl + '">' + theLatestRevision.name + '</a>'
                result += " (" + nbAccessed.toString() + ")\t" + modelLink + "<br/>"
            }
        }
        return result
    }

    static String getLatestPublishedModels() {
        String query = '''
SELECT model, max(rev.uploadDate), max(rev.revisionNumber)
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
ORDER BY rev.uploadDate DESC'''
        def matchedModels = Model.executeQuery(query, [max: 10])
        String result = ""
        def g = Holders.grailsApplication.mainContext.getBean('org.codehaus.groovy.grails.plugins.web.taglib.ApplicationTagLib')
        if (matchedModels.size()) {
            matchedModels.each {
                Model model = it[0]
                Date uploadDate = it[1]
                int maxRevisionNumber = it[2]
                Revision theLatestRevision = model.revisions.find({it.revisionNumber == maxRevisionNumber})
                String modelUrl = g.createLink(controller: 'model', id: model.publicationId ?: model.submissionId, action: 'show')
                String modelLink= '<a href=' + '"' + modelUrl + '">' + theLatestRevision.name + '</a>'
                result += uploadDate.format("dd MMM yyyy").toString() + " " + modelLink + "<br/>"
            }
        }
        return result
    }
}
