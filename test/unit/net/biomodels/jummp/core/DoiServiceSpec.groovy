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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Grails, JUnit (or a modified version of that library), containing parts
* covered by the terms of Common Public License, Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Grails, JUnit used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.model.PublicationLinkProvider as PubLP
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Covers JBM-655: DoiService used to parse BibTeX, which broke as soon as doi.org changed the layout of its
 * output (Crossref: a single line, sometimes led by a space), so fetching the details of a DOI ended in a
 * NullPointerException or a NumberFormatException. It now reads CSL-JSON. The samples below are trimmed
 * copies of what doi.org returned for the DOIs reported in JBM-655.
 */
@TestFor(DoiService)
@Mock([PubLP])
class DoiServiceSpec extends Specification {
    // Crossref, journal article, 10.1038/s42256-021-00335-w
    static final String CROSSREF_ARTICLE = '''{"type":"journal-article",
"title":"A machine learning platform to estimate anti-SARS-CoV-2 activities",
"container-title":"Nature Machine Intelligence","publisher":"Springer Science and Business Media LLC",
"volume":"3","issue":"6","page":"527-535","issued":{"date-parts":[[2021,5,3]]},
"author":[{"ORCID":"https://orcid.org/0000-0003-0417-3120","authenticated-orcid":false,"given":"Govinda B.",
"family":"KC","sequence":"first","affiliation":[]},
{"given":"Giovanni","family":"Bocci","sequence":"additional","affiliation":[{"name":"University of New Mexico"}]}],
"DOI":"10.1038/s42256-021-00335-w"}'''

    // Crossref, preprint with an empty container-title, 10.1101/2024.03.15.585236
    static final String CROSSREF_PREPRINT = '''{"type":"posted-content",
"title":"Curating models from BioModels: Developing a workflow for creating OMEX files",
"container-title":[],"publisher":"openRxiv","issued":{"date-parts":[[2024,3,17]]},
"author":[{"ORCID":"https://orcid.org/0000-0001-6738-9979","given":"Jin","family":"Xu","affiliation":[]},
{"ORCID":"https://orcid.org/0000-0001-7002-6386","given":"Lucian","family":"Smith","affiliation":[]}],
"DOI":"10.1101/2024.03.15.585236"}'''

    // DataCite, 10.48550/arXiv.2007.02835 - no volume, issue, pages, container-title, month or ORCID
    static final String DATACITE_ARXIV = '''{"type":"article",
"title":"Self-Supervised Graph Transformer on Large-Scale Molecular Data","publisher":"arXiv",
"issued":{"date-parts":[[2020]]},
"author":[{"family":"Rong","given":"Yu"},{"family":"Bian","given":"Yatao"}],
"DOI":"10.48550/ARXIV.2007.02835"}'''

    // what doi.org answers when the DOI, or its registration agency, is unknown
    static final String DOI_NOT_FOUND_PAGE = '''<!DOCTYPE html>
<html lang="en-us"><head><title>Error: DOI Prefix [10.9999] Not Found</title></head><body></body></html>'''

    void setup() {
        new PubLP(linkType: PubLP.LinkType.DOI).save(validate: false, flush: true)
    }

    void "a Crossref journal article is read from its CSL-JSON record"() {
        when:
        PubTC pubTC = service.buildPubTCFromCslJson("10.1038/s42256-021-00335-w", CROSSREF_ARTICLE)

        then:
        pubTC.title == "A machine learning platform to estimate anti-SARS-CoV-2 activities"
        pubTC.journal == "Nature Machine Intelligence"
        pubTC.volume == "3"
        pubTC.issue == "6"
        pubTC.pages == "527-535"
        pubTC.year == 2021
        pubTC.month == "5"
        pubTC.day == 3
        pubTC.link == "10.1038/s42256-021-00335-w"
        pubTC.authors*.userRealName == ["Govinda B. KC", "Giovanni Bocci"]
        and: "the ORCID is stored without the URL prefix that Person.orcid rejects"
        pubTC.authors[0].orcid == "0000-0003-0417-3120"
        pubTC.authors[1].orcid == null
        pubTC.authors[1].institution == "University of New Mexico"
        pubTC.validate()
    }

    void "a preprint without a container-title takes the publisher as its journal"() {
        when:
        PubTC pubTC = service.buildPubTCFromCslJson("10.1101/2024.03.15.585236", CROSSREF_PREPRINT)

        then:
        pubTC.title.startsWith("Curating models from BioModels")
        pubTC.journal == "openRxiv"
        pubTC.volume == "N/A"
        pubTC.year == 2024
        pubTC.month == "3"
        pubTC.authors*.userRealName == ["Jin Xu", "Lucian Smith"]
        pubTC.validate()
    }

    void "a DataCite record with only a year and no ORCIDs is read without leaving anything out of range"() {
        when:
        PubTC pubTC = service.buildPubTCFromCslJson("10.48550/arXiv.2007.02835", DATACITE_ARXIV)

        then:
        pubTC.journal == "arXiv"
        pubTC.year == 2020
        pubTC.month == null
        pubTC.day == null
        pubTC.pages == "N/A"
        pubTC.authors*.userRealName == ["Yu Rong", "Yatao Bian"]
        pubTC.authors.every { it.orcid == null }
        pubTC.validate()
    }

    void "an organisation is kept as an author under its literal name"() {
        given:
        String json = '''{"title":"A consortium paper","publisher":"P","issued":{"date-parts":[[2020]]},
"author":[{"literal":"The BioModels Consortium"},{"given":"","family":""}]}'''

        when:
        PubTC pubTC = service.buildPubTCFromCslJson("10.1/x", json)

        then: "an author without any name is dropped instead of making the whole record invalid"
        pubTC.authors*.userRealName == ["The BioModels Consortium"]
        pubTC.validate()
    }

    void "an ORCID that Person would reject is dropped instead of invalidating the record"() {
        given:
        String json = '''{"title":"T","publisher":"P","issued":{"date-parts":[[2020]]},
"author":[{"given":"A","family":"B","ORCID":"https://orcid.org/not-an-orcid"}]}'''

        when:
        PubTC pubTC = service.buildPubTCFromCslJson("10.1/x", json)

        then:
        pubTC.authors[0].orcid == null
        pubTC.validate()
    }

    @Unroll
    void "an unusable response (#label) yields null rather than an exception"() {
        expect:
        service.buildPubTCFromCslJson("10.9999/nope", response) == null

        where:
        label                     | response
        "null"                    | null
        "empty"                   | ""
        "only whitespace"         | " \n "
        "doi.org's HTML error page" | DOI_NOT_FOUND_PAGE
        "truncated JSON"          | '{"title":"T","author":['
        "a JSON array"            | '[1, 2]'
        "a record with no title"  | '{"publisher":"P"}'
    }

    void "fetchPublicationData returns the publication for a DOI whose record is complete"() {
        given:
        service.metaClass.lookupPublicationDataFromDOI = { String doi -> CROSSREF_PREPRINT }

        when:
        PubTC pubTC = service.fetchPublicationData("10.1101/2024.03.15.585236")

        then:
        pubTC.journal == "openRxiv"
        pubTC.authors.size() == 2
    }

    void "fetchPublicationData returns null, not a NullPointerException, for an unknown DOI"() {
        given:
        service.metaClass.lookupPublicationDataFromDOI = { String doi -> DOI_NOT_FOUND_PAGE }

        expect:
        service.fetchPublicationData("10.9999/nope") == null
    }

    void "fetchPublicationData returns null when the record has no authors, which a publication requires"() {
        given:
        service.metaClass.lookupPublicationDataFromDOI = { String doi ->
            '{"title":"T","publisher":"P","issued":{"date-parts":[[2020]]},"author":[]}'
        }

        expect:
        service.fetchPublicationData("10.1/x") == null
    }
}
