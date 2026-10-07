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

import grails.test.mixin.TestMixin
import grails.test.mixin.integration.IntegrationTestMixin
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.ModelFormat
import net.biomodels.jummp.model.Revision
import net.biomodels.jummp.plugins.security.User
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.springframework.security.acls.domain.BasePermission
import org.springframework.transaction.TransactionDefinition

import static org.junit.Assert.*

/**
 * Checks that a reviewer account only ever covers the models it was requested for (JBM-807).
 */
@TestMixin(IntegrationTestMixin)
class ReviewerAccountServiceTests extends JummpIntegrationTest {
    def reviewerAccountService
    def aclUtilService
    def txSettings = [propagationBehavior: TransactionDefinition.PROPAGATION_REQUIRES_NEW,
                      isolationLevel     : TransactionDefinition.ISOLATION_READ_COMMITTED]

    @Before
    void setUp() {
        User.withTransaction(txSettings) {
            createUserAndRoles()
            ["M1", "M2", "M3", "M4"].each { createModel(it) }
        }
        authenticateAsAdmin()
    }

    @After
    void tearDown() {
        cleanupDatabase()
    }

    private Model createModel(String submissionId) {
        Model model = new Model(vcsIdentifier: "${submissionId}/", submissionId: submissionId)
        Revision revision = new Revision(model: model, vcsId: "1", revisionNumber: 1,
            owner: User.findByUsername("testuser"), minorRevision: false, name: submissionId,
            description: "", comment: "", uploadDate: new Date(),
            format: ModelFormat.findByIdentifierAndFormatVersion("UNKNOWN", "*"))
        model.addToRevisions(revision)
        model.save(flush: true, failOnError: true)
    }

    private boolean canRead(ReviewerAccountInfo info, String submissionId) {
        Model model = Model.findBySubmissionId(submissionId)
        authenticate(info.user.username, info.password)
        boolean result = aclUtilService.hasPermission(springSecurityService.authentication, model, BasePermission.READ)
        authenticateAsAdmin()
        result
    }

    @Test
    void "sets sharing the first and last model get separate accounts with their own access"() {
        ReviewerAccountInfo abc = reviewerAccountService.createReviewerAccount("M1,M2,M4")
        ReviewerAccountInfo adc = reviewerAccountService.createReviewerAccount("M1,M3,M4")

        assertNotEquals(abc.user.username, adc.user.username)
        assertTrue(canRead(abc, "M2"))
        assertFalse(canRead(abc, "M3"))
        assertTrue(canRead(adc, "M3"))
        assertFalse(canRead(adc, "M2"))
    }

    @Test
    void "asking again for the same models in another order reuses the account"() {
        ReviewerAccountInfo first = reviewerAccountService.createReviewerAccount("M1,M2")
        long users = User.count()

        ReviewerAccountInfo again = reviewerAccountService.createReviewerAccount("M2 ,   M1")

        assertEquals(first.user.username, again.user.username)
        assertEquals(users, User.count())
        assertTrue(canRead(again, "M1"))
        assertTrue(canRead(again, "M2"))
    }
}
