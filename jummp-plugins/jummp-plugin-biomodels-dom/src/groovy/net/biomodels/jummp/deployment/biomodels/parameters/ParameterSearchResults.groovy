package net.biomodels.jummp.deployment.biomodels.parameters

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.util.JavaScriptUtils

/**
 * @author carankalle on 31/10/2018.
 */
class ParameterSearchResults {
    static final Logger logger = LoggerFactory.getLogger(ParameterSearchResults.class)
    static final String fieldSeparator = ';'
    int recordsTotal
    int recordsFiltered
    List<SearchResultEntry> entries

    static ParameterSearchResults fromJson(JSONElement json) {
        Objects.requireNonNull(json)
        int hitCount = json.hitCount
        if (hitCount == 0) {
            return new ParameterSearchResults(recordsFiltered: hitCount, recordsTotal: hitCount,
                entries: [])
        }
        def parsedEntries = json.entries.collect { e -> parseEntry(e) }
        return new ParameterSearchResults(recordsFiltered: hitCount, recordsTotal: hitCount,
            entries: parsedEntries)
    }

    private static String convertToLink(String fieldName, String value) {
        if (isLink(fieldName)) {
            if (null == value || value?.isEmpty()) return ""

            value = value.replace("\\", "")
            String[] values = value.split(fieldSeparator)
            List<String> links = new ArrayList<>()

            for (String subvalue : values) {
                if (subvalue.contains('|')) {

                    def (href, label) = subvalue.tokenize('|')
                    links.add("<a style='color:black' target='_blank' href='${href}' > ${label} </a>")
                } else {
                    links.add(subvalue)
                }
            }
            return links.join(fieldSeparator + " ")
        } else {
            return value
        }
    }

    private static processExternalLinks(def parsedFields) {
        final String sabioRKPrefix = "http://sabiork.h-its.org/newSearch?q="
        final String reactomePrefix = "https://reactome.org/content/query?q="
        final String openTargetsPrefix = "https://platform.opentargets.org/target"
        List<String> displayLinks = new ArrayList<>()
        if (parsedFields['external_links'] != null && parsedFields['external_links'].size() > 0) {
            String[] links = parsedFields['external_links'].toString().split(fieldSeparator)
            links.each { value ->
                String finalLink = ""
                String suffixValue = value
                String[] suffixValues = value.split(':')

                if (suffixValues.length == 3) {
                    suffixValue = suffixValues[1] + ":" + suffixValues[2]
                } else if (suffixValues.length == 2) {
                    suffixValue = suffixValues[1]
                }

                if (value.contains("reactome")) {
                    finalLink = reactomePrefix + suffixValue
                } else if (value.contains("sabiork")) {
                    finalLink = sabioRKPrefix + suffixValue
                } else if (value.contains("opentargets")) {
                    finalLink = "$openTargetsPrefix/$suffixValue"
                    value = "OpenTargets:$suffixValue"
                }

                if (finalLink != "") {
                    displayLinks.add("<a href=\"${finalLink}\" target=\"_blank\">${value}</a>")
                }
            }
        }
        if (displayLinks.size() > 0) {
            parsedFields['external_links_show'] = displayLinks.join(fieldSeparator + " ")
        } else {
            parsedFields['external_links_show'] = ""
        }
    }


    private static boolean haveNoField(parsedFields, String fieldName, String fallbackFieldName) {
        parsedFields[fieldName] == null ||
            parsedFields[fieldName] instanceof JSONArray &&
            parsedFields[fieldName].size() == 0 ||
            parsedFields[fallbackFieldName] == null
    }

    private static combineEnityAndEntityIdFields(def parsedFields) {
        if (haveNoField(parsedFields, 'entity_accession_url', 'entity_id')) {
            parsedFields['entity_show'] = buildShowString(parsedFields['entity_id'].toString(), "")
        } else {
            parsedFields['entity_show'] = buildShowString(parsedFields['entity_id'].toString(), parsedFields['entity_accession_url'].toString())
        }
    }

    private static combineRateAndRateOriginal(def parsedFields) {
        if (haveNoField(parsedFields, 'rate', 'rate_original_RAW')) {
            parsedFields['rate_show'] = ""
        } else {
            parsedFields['rate_show'] = buildShowString(parsedFields['rate_original_RAW'].toString(), parsedFields['rate'].toString())
        }
    }

    private static buildShowString(String authorGivenValues, String resolvedValues) {
        if ("" != resolvedValues) {
            return "<span class='legend-green'>" + authorGivenValues + "</span><br/><br/>" + resolvedValues
        } else {
            return "<span class='legend-green'>" + authorGivenValues + "</span>"
        }
    }

    private static isLink(String fieldName) {
        return (fieldName.equalsIgnoreCase("entity_accession_url")
            || fieldName.equalsIgnoreCase("reaction_sbo_term_link")
            || fieldName.equalsIgnoreCase("entity_sbo_term_link"))
    }

    private static String prepareHrefAndLabel(String href) {
        href = href.replaceAll("\\\\", "")
        int lastIndexOfUrlPrefix = "http://identifiers.org/".lastIndexOf("/") + 1
        String urlSuffix = href.substring(lastIndexOfUrlPrefix, href.length())
        int firstIndexOfUrlSuffix = urlSuffix.indexOf("/") + 1
        href = href + "|" + urlSuffix.substring(firstIndexOfUrlSuffix, urlSuffix.length())
        return href
    }

    private static String escapeHtmlFieldValue(String fieldName, String fieldValue) {
        if (fieldValue == null && fieldValue?.length() == 0) {
            return ""
        }
        if (fieldName == "initial_data_RAW" || fieldName == "parameters") {
            JavaScriptUtils.javaScriptEscape(fieldValue)
        }
        return fieldValue
    }

    private static String generatePublicationLink(String fieldName, String fieldValue) {
        if (fieldValue == null && fieldValue?.length() == 0) {
            return ""
        }
        String formattedData
        if (fieldName == "publication") {
            if (fieldValue.contains(fieldSeparator)) {
                List<String> formattedList = new ArrayList<>()
                String[] commaSeparatedLinks = fieldValue.split(fieldSeparator)
                commaSeparatedLinks.each { value ->
                    formattedList.add(prepareHrefAndLabel(value))
                }
                formattedData = formattedList.join(fieldSeparator)
            } else {
                formattedData = prepareHrefAndLabel(fieldValue)
            }
            return formattedData
        } else {
            return fieldValue
        }
    }

    protected static SearchResultEntry parseEntry(def entry) {
        if (!entry) return null

        def parsedFields = [:]
        entry.fields.each { String fieldName, values ->
            int valueCount = Objects.requireNonNull(values).length()
            if (0 < valueCount) {
                if (1 < valueCount) {
                    logger.warn(
                        'More than one value was found in field {}: {}. Only the first will be used',
                        fieldName, values)
                }

                String value
                if (values instanceof String) {
                    value = values
                } else {
                    value = values.first()
                }
                value = convertToLink(fieldName, (String) value)
                value = generatePublicationLink(fieldName, (String) value)
                value = escapeHtmlFieldValue(fieldName, (String) value)
                parsedFields[fieldName] = value.replaceAll("\\\\", "")
            } else {
                parsedFields[fieldName] = values
            }
        }
        combineEnityAndEntityIdFields(parsedFields)
        combineRateAndRateOriginal(parsedFields)
        processExternalLinks(parsedFields)
        new SearchResultEntry(fields: parsedFields)
    }
}

