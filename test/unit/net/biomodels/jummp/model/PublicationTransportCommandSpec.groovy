package net.biomodels.jummp.model

import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import net.biomodels.jummp.core.model.PublicationTransportCommand
import org.springframework.context.MessageSource
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
        def authorsXml = createAuthorsXml("Smith J", "")
        def slurper = new XmlSlurper().parseText(authorsXml)
        def cmd = new PublicationTransportCommand()
        def mockMessageSource = mockFor(MessageSource)
        mockMessageSource.demand.getMessage { String code, Object[] args, Locale l ->
            "error $code: $args".toString()
        }
        cmd.messageSource = mockMessageSource.createMock()

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

    private static String createAuthorsXml(String... authors) {
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><responseWrapper><resultList>
<result>
<authorList>
${authors.collect { String a ->
    "<author><fullName>$a</fullName></author>"
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
