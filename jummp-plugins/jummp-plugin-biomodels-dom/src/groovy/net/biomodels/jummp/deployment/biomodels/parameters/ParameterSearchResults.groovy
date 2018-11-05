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
    static final Logger logger = LoggerFactory.getLogger(ParameterSearchResults.class)

    int recordsTotal
    int recordsFiltered
    List<SearchResultEntry> entries

    static ParameterSearchResults fromJson(JSONElement json) {
        Objects.requireNonNull(json)
        int hitCount = json.hitCount
        def parsedEntries = json.entries.collect { e -> parseEntry(e)}
        new ParameterSearchResults(recordsFiltered: hitCount, recordsTotal: hitCount,
            entries: parsedEntries)
    }

    protected static SearchResultEntry parseEntry(def entry) {
        if (!entry) return null

        def parsedFields = [:]
        entry.fields.each { fieldName, values ->
            int valueCount = Objects.requireNonNull(values).length()
            if (1 < valueCount) {
                logger.warn(
                    'More than one value was found in field {}: {}. Only the first will be used',
                    field, values)
            }
            def value = values.first()
            parsedFields[fieldName] = value
        }

        new SearchResultEntry(fields: parsedFields)
    }

}

