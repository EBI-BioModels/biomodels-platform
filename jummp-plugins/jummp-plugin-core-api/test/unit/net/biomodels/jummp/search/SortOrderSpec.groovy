package net.biomodels.jummp.search

import spock.lang.Specification

class SortOrderSpec extends Specification {
    void "SortOrder survives a Java serialization round trip"() {
        given:
        SortOrder original = new SortOrder("name", "asc")

        when:
        def bytes = new ByteArrayOutputStream()
        new ObjectOutputStream(bytes).withCloseable { it.writeObject(original) }
        // the test runner's context classloader is not the one that loaded SortOrder
        def input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())) {
            @Override
            protected Class<?> resolveClass(ObjectStreamClass desc) {
                Class.forName(desc.name, false, SortOrder.classLoader)
            }
        }
        SortOrder copy = input.withCloseable { it.readObject() } as SortOrder

        then:
        copy == original
        copy.field == "name"
        copy.direction == SortOrder.SortDirection.ASC
    }

    void "equal sort orders share a hash code so cache keys built from them match"() {
        expect:
        new SortOrder("relevance", "desc") == new SortOrder()
        new SortOrder("relevance", "desc").hashCode() == new SortOrder().hashCode()
        new SortOrder("name", "asc") != new SortOrder("name", "desc")
    }
}
