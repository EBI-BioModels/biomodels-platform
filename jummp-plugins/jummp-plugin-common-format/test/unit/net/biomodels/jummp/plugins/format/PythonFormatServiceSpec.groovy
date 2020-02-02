package net.biomodels.jummp.plugins.format

import grails.test.mixin.TestFor
import spock.lang.Specification
import spock.lang.Unroll

@TestFor(PythonFormatService)
class PythonFormatServiceSpec extends Specification {
    @Unroll("run areFilesThisMethod() with the file #filePath: #expected")
    void "test detection of Python files"(String filePath, boolean expected) {
        expect: "PythonFormatService loaded"
        service

        when: "feed the file #filePath to the method"
        File f = new File("test/files/$filePath".toString())
        f.exists()
        boolean result = service.areFilesThisFormat([f])

        then: "the method returns an expected result"
        result == expected

        where: "the table of the needed testing files"
        filePath                 | expected
        "test_mime_type.py"      | true
        "read_write_csv.txt"     | true    // A plain text containing Python code
    }
}
