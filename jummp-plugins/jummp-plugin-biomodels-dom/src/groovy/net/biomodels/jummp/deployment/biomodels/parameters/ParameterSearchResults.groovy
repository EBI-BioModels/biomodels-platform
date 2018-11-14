package net.biomodels.jummp.deployment.biomodels.parameters

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.codehaus.groovy.grails.web.json.JSONElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Created by carankalle on 31/10/2018.
 */
class ParameterSearchResults {
    static
    final Logger logger = LoggerFactory.getLogger(ParameterSearchResults.class)

    int recordsTotal
    int recordsFiltered
    List<SearchResultEntry> entries

    static ParameterSearchResults fromJson(JSONElement json) {
        Objects.requireNonNull(json)
        int hitCount = json.hitCount
        def parsedEntries = json.entries.collect { e -> parseEntry(e) }
        new ParameterSearchResults(recordsFiltered: hitCount, recordsTotal: hitCount,
            entries: parsedEntries)
    }

    private static convertToLink(String fieldName, String value) {

        if (isLink(fieldName)) {
            value = value.replace("\\", "")
            String[] values = value.split(',')
            List<String> links = new ArrayList<>()

            for (String subvalue : values) {
                if (subvalue.contains('|')) {

                    def (href, label) = subvalue.tokenize('|')
                    links.add("<a target='_blank' href='${href}' > ${label} </a>")
                } else {
                    links.add(subvalue)
                }
            }
            return links.join(',')
        } else {
            return value
        }
    }

    private static isLink(String fieldName) {

        return (fieldName.equalsIgnoreCase("entity_accession_url")
            || fieldName.equalsIgnoreCase("reaction_sbo_term_link")
            || fieldName.equalsIgnoreCase("entity_sbo_term_link"))

    }

    protected static SearchResultEntry parseEntry(def entry) {
        if (!entry) return null

        def parsedFields = [:]
        entry.fields.each { fieldName, values ->
            int valueCount = Objects.requireNonNull(values).length()
            if (0 < valueCount) {
                if (1 < valueCount) {
                    logger.warn(
                        'More than one value was found in field {}: {}. Only the first will be used',
                        field, values)
                }

                def value = values.first()
                value = convertToLink(fieldName, value)
                parsedFields[fieldName] = value
            }else{
                parsedFields[fieldName] = values
            }
        }

        new SearchResultEntry(fields: parsedFields)
    }

}

