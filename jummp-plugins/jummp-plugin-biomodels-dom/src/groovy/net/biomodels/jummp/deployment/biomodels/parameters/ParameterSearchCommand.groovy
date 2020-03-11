package net.biomodels.jummp.deployment.biomodels.parameters

import grails.util.Environment
import grails.validation.Validateable
import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.codehaus.groovy.grails.plugins.codecs.HTMLEncoder

import java.util.regex.Pattern

/**
 * @author carankalle on 31/10/2018.
 */
@Validateable
@ToString(includes = ['query', 'size', 'start', 'sort', 'is_curated'])
class ParameterSearchCommand {
    public static final String DEFAULT_QUERY = '*:*'
    public static final String BASE_URL_PARAMS = "fields=$EBI_SEARCH_FIELDS_CSV".toString()
    public static final String EBI_SEARCH_DEV_BASE_URL = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters"
    public static final String EBI_SEARCH_BASE_URL = "https://www.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters"
    public static final String EBI_SEARCH_FIELDS_CSV = "entity_RAW,entity_id,initial_data_RAW,reaction_RAW,reaction_original_RAW,model,organism,publication,rate_RAW,rate_original_RAW,parameters_RAW,entity_accession_url,reaction_sbo_term_link,entity_sbo_term_link,external_links"
    // catch queries like entity_accession_url:CHEBI:12345 or SBO:1234567, but not organism:Homo*
    private static final Pattern shouldEscapeColonPattern = ~/([a-zA-Z0-9_]+:)?([A-Z]+):(\d+)/
    // put a backslash (\) before the last colon so that Lucene doesn't treat it as a special character
    private static final String escapedColonReplacement = '$1$2\\\\:$3' // note the single quotes to avoid Groovy string interpolation

    String query
    Integer size
    Integer start
    String sort
    Boolean is_curated = true

    static constraints = {
        query nullable: true, validator: { q, cmd ->

            if (null == q || q == "" || q == '*') {
                cmd.query = DEFAULT_QUERY
            } else {
                cmd.query = htmlEncodeValue(q)
            }
            return true
        }
        size inList: [10, 25, 50, 100], nullable: true, validator: { val, cmd ->

            if (0 == val || null == val) {
                cmd.size = 10
            } else {
                cmd.size = Integer.parseInt(htmlEncodeValue(val) as String)
            }
            return true

        }

        start min: 0, nullable: true, validator: { val, cmd ->
            if (null == val) {
                cmd.start = 0
            } else {
                cmd.start = Integer.parseInt(htmlEncodeValue(val) as String)
            }
            return true
        }
        sort nullable: true, validator: {val, cmd ->
            if(null == val || val == "null") {
                cmd.sort = ""
            } else {
                cmd.sort = htmlEncodeValue(val)
            }
            return true
        }
        is_curated nullable: true, validator: {val, cmd ->
            if(null == val) {
                cmd.is_curated = true
            } else {
                cmd.is_curated = val
            }
            return true
        }
    }

    @CompileStatic
    boolean isDefaultQuery() {
        DEFAULT_QUERY == query
    }

    @CompileStatic
    static def htmlEncodeValue(def object) {
        if (null == object) {
            return null
        }
        HTMLEncoder htmlEncoder = new HTMLEncoder()
        htmlEncoder.encode(object)
    }

    @CompileStatic
    URL getSearchUrl(String format) {
        def params = [
            query : calculateQueryString(),
            size  : size,
            start : start,
            sort  : sort,
            format : format
        ]
        StringBuilder url = new StringBuilder(getEbiSearchUrl())
        for (element in params) {
            Object v = element.value
            String k = element.key
            if (v != null)
                url.append('&').append(k).append('=').append(v)
        }
        new URL(url.toString())
    }

    private String calculateQueryString() {
        String decoded = URLDecoder.decode(query, "UTF-8") // prevent double encoding
        final String andCurated = " AND is_curated:${is_curated}"
        String queryAnd = new StringBuilder(decoded.length() + andCurated.length())
            .append(decoded)
            .append(andCurated)
            .toString()
        if (query != DEFAULT_QUERY) {
            queryAnd = escapeLuceneFieldSeparator(queryAnd.toString())
        }
        URLEncoder.encode(queryAnd, "UTF-8")
    }

    @CompileStatic
    private static String escapeLuceneFieldSeparator(String query) {
        def matcher = query =~ shouldEscapeColonPattern
        def out = new StringBuffer()

        while (matcher) {
            matcher.appendReplacement(out, escapedColonReplacement)
        }
        matcher.appendTail(out)
        out.toString()
    }


    @Override
    String toString() {
        "Request {" +
            "query='" + query + '\'' +
            ", size=" + size +
            ", start=" + start +
            ", sort='" + sort + '\'' +
            ", is_curated='" + is_curated + '\'' +
            '}'
    }

    @CompileStatic
    static String getEbiSearchUrl() {
        String base = EBI_SEARCH_BASE_URL
        if (Environment.current != Environment.PRODUCTION) {
            base = EBI_SEARCH_DEV_BASE_URL
        }
        return "$base?$BASE_URL_PARAMS"
    }
}
