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


package net.biomodels.jummp.core

import grails.test.runtime.FreshRuntime
import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.core.model.PublicationTransportCommand as PTC
import net.biomodels.jummp.plugins.security.Person

@FreshRuntime
class PubMedServiceSpec extends IntegrationSpec {
    def service
    def pubMedService
    def setup() {
        service = new PubMedService()
    }

    def "search a PubMed publication"() {
        given: "a PubMed ID"
        String pubMedID = "23664840"

        when: "make a call to fetchPublicationData() method"
        PTC ptc = pubMedService.fetchPublicationData(pubMedID)

        then: "an actual object should be returned"
        null != ptc
    }

    def "fetch a publication and verify the authors"() {
        given: "a PubMed ID"
        String pubMedId = "30218022"

        when: "make a call to fetchPublicationData() method"
        PTC ptc = pubMedService.fetchPublicationData(pubMedId)

        then: "receive an actual object and author name is not null"
        null != ptc
        ptc.validate()
        println ptc.authors.last().userRealName
        ptc.authors.first().userRealName != null
    }
}
