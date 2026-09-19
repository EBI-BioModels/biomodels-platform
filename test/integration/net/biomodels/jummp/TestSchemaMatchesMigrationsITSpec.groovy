package net.biomodels.jummp

import grails.test.spock.IntegrationSpec
import groovy.sql.Sql
import net.biomodels.jummp.plugins.security.Person

/**
 * Covers JBM-784: the test database is built by Hibernate from the domain classes (databaseMigrations cannot be used
 * there, see DataSource.groovy), so a column that a migration widened is still at its default size in the tests. BootStrap
 * makes those columns as wide as they are in a database that has run the migrations.
 */
class TestSchemaMatchesMigrationsITSpec extends IntegrationSpec {
    def dataSource

    private int columnLength(final String table, final String column) {
        new Sql(dataSource).firstRow("""SELECT CHARACTER_MAXIMUM_LENGTH AS len FROM INFORMATION_SCHEMA.COLUMNS
            WHERE TABLE_NAME = ? AND COLUMN_NAME = ?""", [table.toUpperCase(), column.toUpperCase()]).len as int
    }

    void "person.institution is as wide as 20240605_widenPersonInstitution makes it"() {
        expect:
        columnLength("person", "institution") == 1024
    }

    void "an affiliation longer than the 255 characters Hibernate would default to can be stored"() {
        given: "an affiliation as long as the 289-character one Europe PMC has for PubMed ID 25414348"
        String affiliation = (["European Molecular Biology Laboratory, European Bioinformatics Institute (EMBL-EBI)"] * 4)
            .join(", ")
        assert affiliation.length() > 255 && affiliation.length() <= 1024

        when:
        Person person = new Person(userRealName: "JBM-784 probe", institution: affiliation)
        person.save(flush: true, failOnError: true)

        then:
        Person.get(person.id).institution == affiliation
    }
}
