/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.web.ControllerUnitTestMixin} for usage instructions
 *
 * @testers     Tung Nguyen <tnguyen@ebi.ac.uk>
 */
@TestFor(P2mController)
@Mock([P2mService, P2MMapping])
class P2mControllerSpec extends Specification {

    void setup() {
        P2MMapping.findOrSaveByRepresentativeAndMember("BMID0001", "BMID0021")
        P2MMapping.findOrSaveByRepresentativeAndMember("BMID0001", "BMID0022")
        P2MMapping.findOrSaveByRepresentativeAndMember("BMID0002", "BMID0030")
        P2MMapping.findOrSaveByRepresentativeAndMember("BMID0002", "BMID0031")
        P2MMapping.findOrSaveByRepresentativeAndMember("BMID0002", "BMID0032")
    }

    void "test index action"() {
        when: "hit index action"
        controller.index()
        then: "load index view"
        '/p2m/index' == view
        model.title.contains("Path2Models project page")
    }

    void "test missing action returning JSON format"() {
        given:
        def p2mService = mockFor(P2mService)
        p2mService.demand.findMissing { ->
            println "mock findMissing service"
            ["BMID0002", "BMID0021", "BMID0022"] as List
        }
        controller.p2mService = p2mService.createMock()
        when:
        response.format = "json"
        controller.missing()
        then:
        response.text
        response.json.missing
    }

    void "test missing action returning XML format"() {
        given:
        def p2mService = mockFor(P2mService)
        p2mService.demand.findMissing { ->
            println "mock findMissing service"
            ["BMID0002", "BMID0021", "BMID0022"] as List
        }
        controller.p2mService = p2mService.createMock()
        when:
        response.format = "xml"
        controller.missing()
        then:
        response.text
    }

    void "test browse action"() {
        given:
        def p2mService = mockFor(P2mService)
        p2mService.demand.getModelCategoryMap() { pattern ->
            [
                "BMID000000142512": ['Cucumis'] as Set,
                "BMID000000142019": ['Deferribacter'] as Set
            ]
        }
        controller.p2mService = p2mService.createMock()

        when:
        def model = controller.browse()

        then:
        model.title == "Browse Path2Models"
        model.categories.size() == 2
        model.genus
    }
}
