package net.biomodels.jummp.model

import spock.lang.Specification

class ConstraintUnitSpec extends Specification {
    String getLongString(Integer length) {
        'a' * length
    }

    String getEmail(Boolean valid) {
        valid ? "admin@test.com" : "admin@test"
    }

    String getUrl(Boolean valid) {
        valid ? "https://www.ebi.ac.uk/biomodeks" : "https:/ww.helloworld.com"
    }

    void validateConstraints(obj, field, error) {
        def validated = obj.validate()
        if (!validated) {
            assert !validated
            assert null != obj.errors[field]
            assert error == obj.errors.allErrors[0].codes.last()
        } else {
            assert validated
        }
    }
}
