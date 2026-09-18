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
 **/


package net.biomodels.jummp.core

import grails.test.runtime.FreshRuntime
import grails.test.spock.IntegrationSpec
import net.biomodels.jummp.core.model.PublicationTransportCommand as PTC
import net.biomodels.jummp.model.PublicationLinkProvider as PubLP
import spock.lang.Unroll

@FreshRuntime
class PubMedServiceSpec extends IntegrationSpec {
    def pubMedService

    def "search a PubMed publication"() {
        given: "a PubMed ID"
        String pubMedID = "23664840"

        when: "make a call to fetchPublicationData() method"
        PTC ptc = pubMedService.fetchPublicationData(pubMedID)

        then: "an actual object should be returned"
        null != ptc
        3 == ptc.authors.size()
    }

    @Unroll("test fetchPublicationData() with PubMed ID = #identifier, authorSize = #authorSize, firstAuthor = #firstAuthorRealName, lastAuthor = #lastAuthorRealName")
    def "fetch a publication and verify the authors with various PubMed #identifier, #authorSize, #firstAuthorRealName, #lastAuthorRealName"() {
        given: "a PubMed ID = #identifier"
        String pubMedId = identifier

        when: "make a call to fetchPublicationData() method"
        PTC ptc = pubMedService.fetchPublicationData(pubMedId)

        then: "get an actual object"
        null != ptc
        ptc.validate()
        authorSize == ptc.authors.size()
        firstAuthorRealName == ptc.authors.first().userRealName
        lastAuthorRealName == ptc.authors.last().userRealName
        where: "parameters can be given in the table"
        identifier | authorSize | firstAuthorRealName | lastAuthorRealName
        "30218022" | 10         | "Seif Y"            | "Monk JM"
        "27906974" | 5          | "Scheidel J"        | "Koch I"
        "29843739" | 6          | "Pereira B"         | "Carneiro S"
        "28625987" | 8          | "Guarnieri MT"      | "Beckham GT"
        "31079267" | 11         | "Shimizu K"         | "Kikkawa F"
    }

    def "fetchPublicationData(id) degrades gracefully instead of NPEing when the EuropePMC lookup fails"() {
        given: "the underlying EuropePMC lookup fails (e.g. malformed URL, timeout, non-2xx response)"
        pubMedService.metaClass.lookupPublicationDataInPubMed = { String url -> null }

        when: "fetching publication data"
        PTC ptc = pubMedService.fetchPublicationData("00000000")

        then: "null is returned instead of throwing"
        null == ptc
        noExceptionThrown()

        cleanup:
        pubMedService.metaClass = null
    }

    def "fetchPublicationData(id, linkType) degrades gracefully instead of NPEing when the EuropePMC lookup fails"() {
        given: "the underlying EuropePMC lookup fails (e.g. malformed URL, timeout, non-2xx response)"
        pubMedService.metaClass.lookupPublicationDataInPubMed = { String url -> null }

        when: "fetching publication data via the PubLP.LinkType overload"
        PTC ptc = pubMedService.fetchPublicationData("00000000", PubLP.LinkType.PUBMED)

        then: "null is returned instead of throwing"
        null == ptc
        noExceptionThrown()

        cleanup:
        pubMedService.metaClass = null
    }
}
