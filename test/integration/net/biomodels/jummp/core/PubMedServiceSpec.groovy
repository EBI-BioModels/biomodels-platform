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

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.core.model.PublicationTransportCommand as PTC
import net.biomodels.jummp.plugins.security.Person

@TestFor(PubMedService)
@Mock([Person])
class PubMedServiceSpec extends IntegrationSpec {
    def "search a PubMed publication"() {
        given: "a PubMedID"
        String pubMedID = "23664840"
        when: "make a call to fetchPublicationData() method"
        PTC ptc = service.fetchPublicationData(pubMedID)
        then: "an actual object should be returned"
        null != ptc
    }

    def "fetch a publication and verify the authors"() {
        given: "a PubMED ID"
        String pubMedId = "30218022"
        when: "make a call to fetchPublicationData() method"
        PTC ptc = service.fetchPublicationData(pubMedId)
        then: "receive an actual object and author name is not null"
        null != ptc
        ptc.validate()
        println ptc.authors.last().userRealName
        ptc.authors.first().userRealName != null
    }

    def "fetch a publication which one of the authors has ORCID persisted in the database"() {
        given: "have a PubMedID"
        String pubMedID = "23664840"

        when: "try to fetch a publication which one of the authors has ORCID persisted in the database"
        Person zanghellini = new Person(userRealName: "Zanghellini J", orcid: "0000-0002-1964-2455")
        assert null != zanghellini
        zanghellini.save()
        assert 1 == Person.count()
        PTC ptc = service.fetchPublicationData(pubMedID)
        assert null != ptc

        then: "author named Zanghellini J is reused rather than created newly"
        ptc.authors.size() == 3
        Person.count() == 3
    }
}
