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
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Unit tests for the username of reviewer accounts (JBM-807).
 */
@TestFor(ReviewerAccountService)
class ReviewerAccountServiceSpec extends Specification {

    void "a single model gets the same kind of name as several models"() {
        when:
        String name = service.formatReviewerAccountName(["MODEL1"])

        then:
        name ==~ /reviewer-[0-9a-f]{12}/
        name != service.formatReviewerAccountName(["MODEL1", "MODEL2"])
    }

    void "the name does not depend on the order or repetition of the model identifiers"() {
        expect:
        service.formatReviewerAccountName(["MODEL1", "MODEL2"]) == service.formatReviewerAccountName(["MODEL2", "MODEL1"])
        service.formatReviewerAccountName(["MODEL1", "MODEL2"]) == service.formatReviewerAccountName(["MODEL2", "MODEL1", "MODEL1"])
        service.formatReviewerAccountName(["MODEL1", "MODEL1"]) == service.formatReviewerAccountName(["MODEL1"])
    }

    void "sets of models that share the first and last identifier get different names"() {
        when:
        String abc = service.formatReviewerAccountName(["MODEL1", "MODEL2", "MODEL4"])
        String adc = service.formatReviewerAccountName(["MODEL1", "MODEL3", "MODEL4"])

        then:
        abc != adc
    }

    void "the name of a multi-model account does not disclose the model identifiers"() {
        when:
        String name = service.formatReviewerAccountName(["MODEL2401190005", "MODEL2401190006"])

        then:
        name ==~ /reviewer-[0-9a-f]{12}/
        !name.contains("MODEL")
    }

    void "the input list is left untouched"() {
        given:
        List<String> ids = ["MODEL2", "MODEL1", "MODEL2"]

        when:
        service.formatReviewerAccountName(ids)

        then:
        ids == ["MODEL2", "MODEL1", "MODEL2"]
    }

    @Unroll
    void "model identifiers are parsed from '#input'"() {
        expect:
        ReviewerAccountService.parseCommaSeparatedModelIdList(input) == expected

        where:
        input                                         | expected
        "MODEL1"                                      | ["MODEL1"]
        "MODEL123, MODEL3232    ,   MODEL323"         | ["MODEL123", "MODEL323", "MODEL3232"]
        "  MODEL2 ,\tMODEL1\n"                        | ["MODEL1", "MODEL2"]
        "MODEL2,MODEL1,MODEL2"                        | ["MODEL1", "MODEL2"]
        "MODEL1,, MODEL2 ,"                           | ["MODEL1", "MODEL2"]
        "   "                                         | []
        ""                                            | []
        null                                          | null
    }

    @Unroll
    void "formatReviewerAccountName rejects #description"() {
        when:
        service.formatReviewerAccountName(input)

        then:
        thrown(IllegalArgumentException)

        where:
        description  | input
        "null"       | null
        "an empty list" | []
    }
}
