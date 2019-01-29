package net.biomodels.jummp.model

import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import net.biomodels.jummp.core.model.PublicationTransportCommand
import org.springframework.context.MessageSource
import spock.lang.Specification

@TestMixin(DomainClassUnitTestMixin)
class PublicationTransportCommandSpec extends Specification {
    def "test parseAuthors method populates the author list successfully"() {
        given:
        def cmd = new PublicationTransportCommand()
        mockForConstraintsTests(PublicationTransportCommand, [cmd])
        def authorsXml = createAuthorsXml("Smith J")
        def slurper = new XmlSlurper().parseText(authorsXml)
        when:
        cmd.parseAuthors(slurper)

        then:
        !cmd.validate() // only the authors are set
        cmd.errors.getFieldErrors('authors').size() == 0
        cmd.authors.size() == 1

        and:
        def firstAuthor = cmd.authors.get(0)
        !firstAuthor.hasErrors()
        firstAuthor.userRealName == 'Smith J'
    }

    def "test parseAuthors method ignores publication authors with empty names"() {
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
        cmd.parseAuthors slurper

        then:
        !cmd.validate()
        cmd.errors.getFieldErrors("authors").size() == 1
        cmd.authors.size() == 2
        cmd.authors.get(0).validate()
        !cmd.authors.get(1).validate()
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
}
