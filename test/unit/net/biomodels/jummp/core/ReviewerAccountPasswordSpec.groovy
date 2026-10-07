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

import grails.test.mixin.TestFor
import net.biomodels.jummp.utils.MathUtils
import spock.lang.Specification

/**
 * Unit tests for the password generated for reviewer accounts (JBM-808).
 */
@TestFor(ReviewerAccountService)
class ReviewerAccountPasswordSpec extends Specification {

    void "the password is long and passes the strength check applied to user-chosen passwords"() {
        expect:
        (1..200).every {
            String p = service.generateReviewerPassword()
            p.length() == 16 && MathUtils.checkPasswordStrength(p) == MathUtils.PWD_HARD_LEVEL.X_HARD.label
        }
    }

    void "the password avoids look-alike characters and characters that are special in HTML"() {
        when:
        String all = (1..200).collect { service.generateReviewerPassword() }.join()

        then:
        !(all =~ /[0OIl1o<>&"']/)
    }

    void "consecutive passwords differ"() {
        expect:
        service.generateReviewerPassword() != service.generateReviewerPassword()
    }
}
