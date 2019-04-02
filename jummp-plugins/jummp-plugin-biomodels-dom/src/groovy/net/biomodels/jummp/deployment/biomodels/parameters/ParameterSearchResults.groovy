package net.biomodels.jummp.deployment.biomodels.parameters

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONElement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author carankalle on 31/10/2018.
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
        if(hitCount == 0) {
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

    private static processExternalLinksForSabioRK(def parsedFields) {

        final String sABIORKUrlPrefix = "http://sabiork.h-its.org/newSearch?q="
        List<String> displayLinks = new ArrayList<>()
        if (parsedFields['external_links'] != null && parsedFields['external_links'].size() > 0) {
            String[] links = parsedFields['external_links'].toString().split(/,/)
            links.each { value ->
                String[] identifiers = value.split('\\|')
                String accession = identifiers[0].replace("\\", "")
                    .replace("[","")
                    .replace("]","")
                displayLinks.add("<a href=\"${sABIORKUrlPrefix}${accession}\" target=\"_blank\">${accession}</a>")
            }

        }
        if (displayLinks.size() > 0) {
            parsedFields['external_links_show'] = displayLinks.join(",")
        } else {
            parsedFields['external_links_show'] = ""

        }

    }

    private static combineReactionAndReactionOriginal(def parsedFields) {


        if (parsedFields['reaction'] == null ||
            parsedFields['reaction'] instanceof JSONArray &&
            parsedFields['reaction'].size()==0  ||
            parsedFields['reaction_original_RAW'] == null) {

            parsedFields['reaction_show'] = ""
        }else{
            parsedFields['reaction_show'] = parsedFields['reaction'] + '<hr/>' + "<span class='legend-green'>"+parsedFields['reaction_original_RAW'] + "</span>"
        }

    }
    private static combineEnityAndEntityIdFields(def parsedFields) {

        if (parsedFields['entity_accession_url'] == null ||
            parsedFields['entity_accession_url'] instanceof JSONArray &&
            parsedFields['entity_accession_url'].size()==0  ||
            parsedFields['entity_id'] == null) {

            parsedFields['entity_show'] ="<span class='legend-green'>"+parsedFields['entity_id'] + "</span>"
        }else{
            parsedFields['entity_show'] = parsedFields['entity_accession_url'] + '<hr/>' + "<span class='legend-green'>"+parsedFields['entity_id'] + "</span>"
        }
    }

    private static combineRateAndRateOriginal(def parsedFields) {

        if (parsedFields['rate'] == null ||
            parsedFields['rate'] instanceof JSONArray &&
            parsedFields['rate'].size()==0  ||
            parsedFields['rate_original_RAW'] == null) {

            parsedFields['rate_show'] = ""
        }else{
            parsedFields['rate_show'] = parsedFields['rate'] + '<hr/>' + "<span class='legend-green'>"+parsedFields['rate_original_RAW'] + "</span>"
        }

    }
    private static isLink(String fieldName) {

        return (fieldName.equalsIgnoreCase("entity_accession_url")
            || fieldName.equalsIgnoreCase("reaction_sbo_term_link")
            || fieldName.equalsIgnoreCase("entity_sbo_term_link"))

    }

    private static String prepareHrefAndLabel(String href) {
        href = href.replaceAll("\\\\", "")
        int lastIndexofUrlPrefix = "http://identifiers.org/".lastIndexOf("/") + 1;
        String urlSuffix = href.substring(lastIndexofUrlPrefix, href.length())
        int firstIndexOfUrlSuffix = urlSuffix.indexOf("/") + 1
        href = href + "|" + urlSuffix.substring(firstIndexOfUrlSuffix, urlSuffix.length())
        return href
    }


    private static String generatePublicationLink(String fieldName, String fieldValue) {
        if (fieldValue == null && fieldValue?.length() == 0) {
            return ""
        }
        String formattedData
        if(fieldName == "publication") {
            if (fieldValue.contains(",")) {
                List<String> formattedList = new ArrayList<>()
                String[] commaSeparatedLinks = fieldValue.split(",")
                commaSeparatedLinks.each { value ->
                    formattedList.add(prepareHrefAndLabel(value));
                }
                formattedData = formattedList.join(", ")
            } else {
                formattedData = prepareHrefAndLabel(fieldValue);
            }
            return formattedData
        }else{
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

                def value = values.first()
                value = convertToLink(fieldName,(String)value)
                value = generatePublicationLink(fieldName, (String)value)
                parsedFields[fieldName] = value.replaceAll("\\\\","")
            }else{
                parsedFields[fieldName] = values
            }
        }
        combineReactionAndReactionOriginal(parsedFields)
        combineEnityAndEntityIdFields(parsedFields)
        combineRateAndRateOriginal(parsedFields)
        processExternalLinksForSabioRK(parsedFields)
        new SearchResultEntry(fields: parsedFields)
    }

}

