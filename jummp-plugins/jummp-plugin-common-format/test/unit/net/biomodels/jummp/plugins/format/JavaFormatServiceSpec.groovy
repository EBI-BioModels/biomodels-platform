package net.biomodels.jummp.plugins.format

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(JavaFormatService)
class JavaFormatServiceSpec extends Specification {
    @Unroll("run areFilesThisMethod() with the file #filePath: #expected")
    void "test detection of Java files"(String filePath, boolean expected) {
        expect: "JavaFormatService loaded"
        service

        when: "feed the file #filePath to the method"
        File f = new File("test/files/$filePath".toString())
        f.exists()
        boolean result = service.areFilesThisFormat([f])

        then: "the method returns an expected result"
        result == expected

        where: "the table of the needed testing files"
        filePath                 | expected
        "MIMEType.java"          | true
        "GameTutorial.txt"       | false    // A plain text containing Java code
    }
}
