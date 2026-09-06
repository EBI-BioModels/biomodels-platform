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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Grails (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Grails used as well as
* that of the covered work.}
**/

/**
* @author Raza Ali <raza.ali@ebi.ac.uk>
* @author Mihai Glonț <mihai.glont@ebi.ac.uk>
* @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
*/

package net.biomodels.jummp.model

import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import spock.lang.Unroll

@TestFor(Publication)
@Mock([PublicationLinkProvider])
class PublicationSpec extends ConstraintUnitSpec {
    def setup() {
        // mock a publication with some data (put unique violations in here so they can be tested, the others aren't needed)
        mockForConstraintsTests(Publication, [new Publication(link: '123456789', linkProvider: 1)])
    }

    @Unroll("test Publication all constraints when #field is #error")
    def "test validation on all constraints #field is #error"() {
        when: "instantiate a publication with the minimal given data and mock constraints on it"
        Publication publication = new Publication(title: "BioModels: ten-year anniversary")
        publication."$field" = value
        mockForConstraintsTests(Publication, [publication])

        then: "validate the newly instantiated publication"
        validateConstraints(publication, field, error)

        where: "with the testing data are given here"
        error               | field         | value
        'nullable'          | 'journal'     | null
        'blank'             | 'journal'     | ''
        'nullable'          | 'title'       | null
        'blank'             | 'title'       | ''
        'nullable'          | 'affiliation' | null
        'blank'             | 'affiliation' | ''
        'maxSize.exceeded'  | 'affiliation' | getLongString(1001)
        'nullable'          | 'synopsis'    | null
        'maxSize.exceeded'  | 'synopsis'    | getLongString(5001)
        'nullable'          | 'year'        | null
        'nullable'          | 'month'       | null
        'nullable'          | 'day'         | null
        'nullable'          | 'volume'      | null
        'nullable'          | 'issue'       | null
        'nullable'          | 'pages'       | null
        'nullable'          | 'linkProvider'| null
        'nullable'          | 'link'        | null
    }

    @Unroll("test Publication link and linkProvider have no unique constraint at the domain level")
    def "test Publication link and linkProvider have no unique constraint at the domain level"() {
        // Publication.link carries no `unique:` constraint in the current AnnotationStore domain
        // class (link(nullable: true), nothing else) - this test used to assert the opposite,
        // expecting pub2.validate() to fail on a duplicate link+linkProvider. That's apparently
        // been the case since domain classes were extracted into the separate AnnotationStore
        // project (2015, commit 3e92bbf70) - see JBM-743 for the follow-up on whether that
        // constraint should be restored there. Deduplication in this app is instead handled
        // procedurally, in PublicationService.findByPublicationTransportCommand(), which looks up
        // an existing Publication by link+linkType before creating a new one.
        given: "instantiate two mocked publications sharing the same link and link provider"
        PublicationLinkProvider provider = new PublicationLinkProvider(linkType: PublicationLinkProvider.LinkType.PUBMED, pattern: "^\\d+\$")
        mockForConstraintsTests(Publication)
        Publication pub1 = new Publication(title: "Title 1", linkProvider: provider, link: '123456789')

        when: "validate and save these publications"
        assertTrue pub1.validate()
        pub1.save(flush: true)
        1 == Publication.count()
        Publication pub2 = new Publication(title: "Title 2", linkProvider: provider, link: '123456789')
        mockForConstraintsTests(Publication, [pub2, pub1])

        then: "both validate successfully - the domain model does not itself enforce uniqueness"
        pub1.validate()
        pub2.validate()
        !pub2.hasErrors()
    }
}
