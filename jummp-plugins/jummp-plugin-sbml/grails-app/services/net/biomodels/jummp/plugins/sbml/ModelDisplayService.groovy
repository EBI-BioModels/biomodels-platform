package net.biomodels.jummp.plugins.sbml

import grails.converters.JSON
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.web.json.JSONElement
/**
 * Service for accessing Model Display data.
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Chinmay Arankalle <carankalle@ebi.ac.uk>
 */
@CompileStatic
class ModelDisplayService {
    static transactional = false
    static String DELIMITER = '; '
    GrailsApplication grailsApplication
    String accessUrl

    @CompileStatic(TypeCheckingMode.SKIP)
    def getComponents(String modelId, int revisionNumber, int skip, int limit, String typeName) throws IOException {
        accessUrl = grailsApplication.config.jummp.model.modeldisplay.server.access
        String url = "$accessUrl?publicationId=$modelId&skip=$skip&limit=$limit&typeName=$typeName"
        if (revisionNumber>0) {
            url = "$url&revisionNumber=$revisionNumber"
        }
        def rawComponents = null
        try {
            rawComponents = new URL(url).text
        } catch (FileNotFoundException fe) {
            throw new IOException("Could not fetch components for model $modelId: ${fe.getMessage()}", fe)
        }

        Map formattedComponents = fromJson(JSON.parse(rawComponents))
        return formattedComponents
    }

    private static Map fromJson(JSONElement rawJson) {
        Map formattedComponents = formatRawJson(rawJson)
        return formattedComponents

    }

    @CompileDynamic
    private static Map<String, Object> formatRawJson(JSONElement rawJson) {
        Map<String, Object> formattedComponents = [:]
        for (JSONElement jsonElement : rawJson.components) {
            String typeName = jsonElement.typeName.toLowerCase()
            List<Object> typeList = []
            if (formattedComponents[typeName]) {
                typeList = formattedComponents[typeName] as List<Object>
            }
            formattedComponents = addToFormattedComponents(typeName, typeList, jsonElement, formattedComponents)

        }
        formattedComponents['speciesRecordsTotal'] = rawJson.speciesRecordsTotal
        formattedComponents['reactionsRecordsTotal'] = rawJson.reactionsRecordsTotal
        return formattedComponents
    }

    @CompileDynamic
    private static Map<String, Object> addToFormattedComponents(String typeName, List<Object> typeList, JSONElement jsonElement, Map<String, Object> formattedComponents) {
        typeList.add(
            convertLinks(jsonElement.type))
        formattedComponents[typeName] = typeList
        formattedComponents
    }

    @CompileDynamic
    private static JSONElement convertLinks(JSONElement type) {
        if (null != type.resolvedAccessionUrls) {
            type.resolvedAccessionUrlsShow = generateUrlsWithAnchorTag(type.resolvedAccessionUrls)
        }
        return type
    }

    private static String generateUrlsWithAnchorTag(String urlString) {
        if (!urlString.isEmpty() && urlString.contains(DELIMITER)) {
            String[] urls = urlString.split(DELIMITER)
            List<String> urlList = new ArrayList<>();
            final String labelDelimiter = "\\|"
            for (String url : urls) {
                String[] urlParts = url.split(labelDelimiter)
                if (urlParts.length > 1) {
                    urlList.add('<a style="color:black" href="' + urlParts[0] + '" target="_blank">' + urlParts[1] + '</a>')
                } else {
                    urlList.add(url)
                }
            }
            return urlList.join(DELIMITER)
        } else {
            return urlString
        }
    }
}
