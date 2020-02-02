package net.biomodels.jummp.plugins.format

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(CcppFormatService)
class CcppFormatServiceSpec extends Specification {
    @Unroll("run areFilesThisMethod() with the file #filePath: #expected")
    void "test detection of C/C++ files"(String filePath, boolean expected) {
        expect: "CcppFormatService loaded"
        service

        when: "feed the file #filePath to the method"
        File f = new File("test/files/$filePath".toString())
        f.exists()
        boolean result = service.areFilesThisFormat([f])

        then: "the method returns an expected result"
        result == expected

        where: "the table of the needed testing files"
        filePath                 | expected
        "basic.c"                | true
        "compositedp.cpp"        | true
        "hello.txt"              | true    // A plain text containing CPP code
    }
}
