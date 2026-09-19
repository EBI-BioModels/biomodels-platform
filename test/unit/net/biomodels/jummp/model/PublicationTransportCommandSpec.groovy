package net.biomodels.jummp.model

import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import net.biomodels.jummp.core.model.PublicationTransportCommand
import spock.lang.Specification

@TestMixin(DomainClassUnitTestMixin)
class PublicationTransportCommandSpec extends Specification {
    def "test extractAuthorsFromPubMed method populates the author list successfully"() {
        given:
        def cmd = new PublicationTransportCommand()
        mockForConstraintsTests(PublicationTransportCommand, [cmd])
        def authorsXml = createAuthorsXml("Smith J")
        def slurper = new XmlSlurper().parseText(authorsXml)
        when:
        cmd.extractAuthorsFromPubMed(slurper)

        then:
        !cmd.validate() // only the authors are set
        cmd.errors.getFieldErrors('authors').size() == 0
        cmd.authors.size() == 1

        and:
        def firstAuthor = cmd.authors.get(0)
        !firstAuthor.hasErrors()
        firstAuthor.userRealName == 'Smith J'
    }

    def "test extractAuthorsFromPubMed method ignores publication authors with empty names"() {
        given:
        // PublicationTransportCommand has no messageSource property (removed at some point) -
        // the mock previously assigned onto cmd.messageSource here was dead weight even before
        // that: this test's assertions never touch validation error message text.
        def authorsXml = createAuthorsXml("Smith J", "")
        def slurper = new XmlSlurper().parseText(authorsXml)
        def cmd = new PublicationTransportCommand()

        when:
        cmd.extractAuthorsFromPubMed slurper

        then:
        !cmd.validate()
        cmd.errors.getFieldErrors("authors").size() == 1
        cmd.authors.size() == 2
        cmd.authors.get(0).validate()
        !cmd.authors.get(1).validate()
    }

    def "test extractManuscriptInfoFromPubMed"() {
        given:
        def manuscriptDetails = [ pages: "1-10", title: "my awesome paper",
            affiliation: "home", synopsis: "boring abstract",
            journalInfo: [
                monthOfPublication: "23",
                yearOfPublication: "1975"
            ]
        ]
        def xml = createManuscriptInfoXml(manuscriptDetails)
        def slurper = new XmlSlurper().parseText(xml)
        def cmd = new PublicationTransportCommand()

        when:
        cmd.extractManuscriptInfoFromPubMed(slurper)

        then:
        !cmd.validate()
        ["pages", "title", "affiliation", "synopsis"].each { field ->
            cmd.errors.getFieldErrors(field).size() == 0
        }
        cmd.errors.getFieldErrors("authors").size() == 1
    }

    def "test extractManuscriptInfoFromPubMed takes the publisher and year of a preprint that has no journalInfo"() {
        given: "the shape of the EuropePMC record for the bioRxiv preprint 10.1101/2024.03.15.585236 (JBM-655)"
        def slurper = new XmlSlurper().parseText("""<responseWrapper><resultList><result>
<id>PPR822903</id><source>PPR</source>
<title>Curating models from BioModels</title>
<pubYear>2024</pubYear>
<bookOrReportDetails><publisher>bioRxiv</publisher><yearOfPublication>2024</yearOfPublication></bookOrReportDetails>
</result></resultList></responseWrapper>""")
        def cmd = new PublicationTransportCommand()

        when:
        cmd.extractManuscriptInfoFromPubMed(slurper)

        then:
        cmd.title == "Curating models from BioModels"
        cmd.journal == "bioRxiv"
        cmd.year == 2024
        !cmd.isEmpty()
    }

    def "test extractManuscriptInfoFromPubMed prefers the journal of journalInfo over the publisher"() {
        given:
        def slurper = new XmlSlurper().parseText("""<responseWrapper><resultList><result>
<title>A paper</title><pubYear>2020</pubYear>
<journalInfo><yearOfPublication>2019</yearOfPublication><journal><title>Bioinformatics</title></journal></journalInfo>
<bookOrReportDetails><publisher>Some Publisher</publisher></bookOrReportDetails>
</result></resultList></responseWrapper>""")
        def cmd = new PublicationTransportCommand()

        when:
        cmd.extractManuscriptInfoFromPubMed(slurper)

        then:
        cmd.journal == "Bioinformatics"
        cmd.year == 2019
    }

    def "test extractManuscriptInfoFromPubMed leaves an empty result empty so that the DOI fallback kicks in"() {
        given: "EuropePMC answers a DOI it does not index with zero hits"
        def slurper = new XmlSlurper().parseText(
            "<responseWrapper><hitCount>0</hitCount><resultList/></responseWrapper>")
        def cmd = new PublicationTransportCommand()

        when:
        cmd.extractManuscriptInfoFromPubMed(slurper)

        then:
        cmd.isEmpty()
        cmd.journal == null
        cmd.year == null
    }

    /**
     * @param authors each entry is the full name extractAuthorsFromPubMed() is expected to
     *        reconstruct, e.g. "Smith J" (or "" for an intentionally blank/invalid author).
     *        extractAuthorsFromPubMed() builds userRealName as "$firstName $lastName" from the
     *        PubMed response's <firstName>/<lastName> elements (not <fullName> - see the TODO in
     *        PublicationTransportCommand.extractAuthorsFromPubMed - the extraction moved away
     *        from <fullName> at some point after this test was written), so each name here is
     *        split on the first space to populate those two elements and reproduce it exactly.
     */
    private static String createAuthorsXml(String... authors) {
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><responseWrapper><resultList>
<result>
<authorList>
${authors.collect { String a ->
    List<String> parts = a ? a.split(' ', 2) as List : ['', '']
    String firstName = parts[0]
    String lastName = parts.size() > 1 ? parts[1] : ''
    "<author><firstName>$firstName</firstName><lastName>$lastName</lastName></author>"
}.join('\n')}
</authorList>
</result>
</resultList></responseWrapper>""".toString()
    }

    private static String createManuscriptInfoXml(Map args) {
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><responseWrapper><resultList>
<result>
<pages>$args.pages</pages>
<title>$args.title</title>
<affiliation>$args.affiliation</affiliation>
<synopsis>$args.synopsis</synopsis>
${
    if (args.journalInfo) {
        return "<journalInfo><monthOfPublication>${args.journalInfo.monthOfPublication}</monthOfPublication>" +
            "<yearOfPublication>${args.journalInfo.yearOfPublication}</yearOfPublication>" +
            "</journalInfo>"
    }
}
</result>
</resultList></responseWrapper>""".toString()
    }
}
