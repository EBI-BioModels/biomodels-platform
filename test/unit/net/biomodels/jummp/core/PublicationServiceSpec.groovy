/**
 * Copyright (C) 2010-2026 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

package net.biomodels.jummp.core

import grails.plugin.springsecurity.SpringSecurityUtils
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Publication
import spock.lang.Specification

/**
 * Covers JBM-711: PublicationService.canManagePublication()/isCurationStaff(), which decide
 * whether the standalone publication editor lets the current user touch a given publication --
 * curators/admins always can, everyone else only if they own (have WRITE ACL permission on) at
 * least one Model the publication is linked to.
 */
@Mock([Model, Publication])
@TestFor(PublicationService)
class PublicationServiceSpec extends Specification {

    void cleanup() {
        // undo the static metaClass override so it never leaks into other test classes/methods
        GroovySystem.metaClassRegistry.removeMetaClass(SpringSecurityUtils)
    }

    private void stubCurationStaff(boolean isCurationStaff) {
        SpringSecurityUtils.metaClass.'static'.ifAnyGranted = { String roles -> isCurationStaff }
    }

    private void stubWritableModels(List<Model> writableModels) {
        service.aclUtilService = [
            hasPermission: { authentication, domainObject, permission -> domainObject in writableModels }
        ] as Object
        service.springSecurityService = [authentication: "fake-authentication"]
    }

    void "isCurationStaff reflects whatever SpringSecurityUtils reports for admin/curator roles"() {
        given:
        stubCurationStaff(isCurationStaff)

        expect:
        service.isCurationStaff() == isCurationStaff

        where:
        isCurationStaff << [true, false]
    }

    void "canManagePublication rejects a null publication even for curation staff"() {
        given:
        stubCurationStaff(true)

        expect:
        !service.canManagePublication(null)
    }

    void "curators and admins can manage any publication regardless of model ownership"() {
        given: "no model is owned by the current user"
        stubCurationStaff(true)
        stubWritableModels([])
        Publication publication = new Publication(title: "A publication").save(validate: false)

        expect:
        service.canManagePublication(publication)
    }

    void "a plain user can manage a publication linked to a model they have write access to"() {
        given:
        stubCurationStaff(false)
        Publication publication = new Publication(title: "A shared publication").save(validate: false)
        Model ownedModel = new Model(vcsIdentifier: "vcs-owned", submissionId: "MODEL_OWNED",
            publication: publication).save(validate: false)
        stubWritableModels([ownedModel])

        expect:
        service.canManagePublication(publication)
    }

    void "owning write access to just one of several linked models is enough"() {
        given: "the publication is shared by two models, and the user only owns one of them"
        stubCurationStaff(false)
        Publication publication = new Publication(title: "A publication shared by two models").save(validate: false)
        Model ownedModel = new Model(vcsIdentifier: "vcs-owned-2", submissionId: "MODEL_OWNED_2",
            publication: publication).save(validate: false)
        Model otherModel = new Model(vcsIdentifier: "vcs-other-2", submissionId: "MODEL_OTHER_2",
            publication: publication).save(validate: false)
        stubWritableModels([ownedModel])

        expect:
        service.canManagePublication(publication)
    }

    void "a plain user cannot manage a publication whose linked model(s) they do not own"() {
        given:
        stubCurationStaff(false)
        Publication publication = new Publication(title: "Someone else's publication").save(validate: false)
        Model othersModel = new Model(vcsIdentifier: "vcs-others", submissionId: "MODEL_OTHERS",
            publication: publication).save(validate: false)
        stubWritableModels([])

        expect:
        !service.canManagePublication(publication)
    }

    void "a plain user cannot manage a publication with no linked models at all"() {
        given:
        stubCurationStaff(false)
        Publication publication = new Publication(title: "An orphaned publication").save(validate: false)
        stubWritableModels([])

        expect:
        !service.canManagePublication(publication)
    }
}
