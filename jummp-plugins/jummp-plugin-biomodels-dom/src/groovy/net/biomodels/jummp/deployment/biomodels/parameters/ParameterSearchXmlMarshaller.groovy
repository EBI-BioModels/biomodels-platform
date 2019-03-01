package net.biomodels.jummp.deployment.biomodels.parameters

import grails.converters.XML
import org.codehaus.groovy.grails.web.converters.marshaller.ObjectMarshaller

/**
 * @short Custom XML marshaller that takes into ParameterSearchResults model
 *
 * This class overrides the default XML renderer for a Model to ensure that its perennial
 * identifier or identifiers.
 *
 * @author Chinmay Arankalle <carankalle@ebi.ac.uk>
 */
class ParameterSearchXmlMarshaller implements ObjectMarshaller<XML> {
    /* use a formatted output. */
    protected boolean prettyPrint = true

    @Override
    boolean supports(Object o) {
        o instanceof ParameterSearchResults
    }

    static String getElementName(Object o) {
        return 'field'
    }

    @Override
    void marshalObject(Object o, XML converter) {
        final ParameterSearchResults searchResults = (ParameterSearchResults) o

        converter.startNode 'recordsTotal'
        converter.chars String.valueOf(searchResults.recordsTotal)
        converter.end()
        converter.startNode 'recordsFiltered'
        converter.chars String.valueOf(searchResults.recordsTotal)
        converter.end()
        converter.startNode 'entries'

        if (searchResults.entries) {
            for (SearchResultEntry searchResultEntry in searchResults.entries) {
                if (searchResultEntry) {
                    def fieldsMap = searchResultEntry.getFields()
                    converter.build {
                        fields {
                            for (String key : fieldsMap.keySet()) {
                                "$key" fieldsMap[key]


                            }
                        }
                    }

                }
            }
        }
        converter.end()

    }


}
