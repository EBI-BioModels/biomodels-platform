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

import grails.test.mixin.*
import grails.test.mixin.services.ServiceUnitTestMixin
import net.biomodels.jummp.model.*
import net.biomodels.jummp.plugins.security.*
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.support.GrailsUnitTestMixin} for usage instructions
 */
@TestMixin(ServiceUnitTestMixin)
@TestFor(ModelOfTheMonthService)
@Mock([Person, User, ModelFormat, Revision, Model, ModelOfTheMonth])
class ModelOfTheMonthServiceSpec extends Specification {

    def setup() {
        def fmt = new ModelFormat(identifier: 'foo', name: 'foo', formatVersion: '1.0')
        fmt.save()
        def p = new Person(userRealName: 'Mary')
        p.save()

        def u = new User(person: p, email: 'mary@example.com', enabled: true, password:
                'changeme', accountExpired: false, accountLocked: false,
                passwordExpired: false)
        def m1 = new Model(vcsIdentifier: '/m1', submissionId: 'm1')
        def r1 = new Revision( vcsId: '1', revisionNumber: 1, minorRevision: false,
                name: 'r1', description: '', comment: '', uploadDate: new Date(),
                format: fmt, owner: u)
        m1.addToRevisions r1
        m1.save()

        def m2 = new Model(vcsIdentifier: '/m2', submissionId: 'm2')
        def r2 = new Revision( vcsId: 'z', revisionNumber: 1, minorRevision: false,
                name: 'r2', description: '', comment: '', uploadDate: new Date(),
                format: fmt, owner: u)
        m2.addToRevisions r2
        m2.save()

        def mom1 = new ModelOfTheMonth(authors: 'Rose', title: 'm1', publicationDate:
                Date.parse("yyyy-MM-dd", "2000-01-01"), lastUpdated: new Date())
        mom1.addToModels m1

        def mom2 = new ModelOfTheMonth(authors: 'John', publicationDate: new Date(),
                title: 'm2', lastUpdated: new Date())
        mom2.addToModels m2

        assert [mom1, mom2]*.save()
    }

    void "we can find MoM entries for a model"() {
        given: 'a model for which there is a Model of the Month entry'
        def model = Model.findBySubmissionId('m1')

        when: 'we look up the MoM records'
        List result = service.fetchEntriesForModel(model.id)

        then: 'there is a single entry'
        1 == result.size()

        and: 'it is the expected one'
        def momCmd = result[0]
        'Rose' == momCmd.authors
        '2000-01' == momCmd.formattedEntryDate
    }

    void "no results are returned for models that do not exist in the database"() {
        final long NON_EXISTENT_ID = -1
        expect:
        !Model.exists(NON_EXISTENT_ID)
        [] == service.fetchEntriesForModel(NON_EXISTENT_ID)
        [] == service.fetchEntriesForModel(null)
    }
}
