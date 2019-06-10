package net.biomodels.jummp.deployment.biomodels.parameters

import grails.validation.Validateable
import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
* @author carankalle on 31/10/2018.
*/
@Validateable
@ToString(includes = ['query', 'size', 'start', 'sort'])
class ParameterSearchCommand {
    public static final String DEFAULT_QUERY = '*:*'
    public static
    final String BASE_URL = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?" +
        "fields=entity_RAW,entity_id,initial_data_RAW,reaction_RAW,reaction_original_RAW,model,organism,publication,rate_RAW," +
    "rate_original_RAW,parameters_RAW,entity_accession_url,reaction_sbo_term_link,entity_sbo_term_link,external_links"

    String query
    Integer size
    Integer start
    String sort

    static constraints = {
        query nullable: true, validator: { q, cmd ->

            if (null == q || q == "" || q == '*') {
                cmd.query = DEFAULT_QUERY
            }
            return true
        }
        size inList: [10, 25, 50, 100], nullable: true, validator: { val, cmd ->
            if (0 == val || null == val) {
                cmd.size = 10
            }
            return true

        }

        start min: 0, nullable: true, validator: { val, cmd ->
            if (null == val) {
                cmd.start = 0
            }
            return true
        }
        sort nullable: true, validator: {val, cmd ->
            if(null == val || val == "null") {
                cmd.sort = ""
            }
            return true
        }
    }

    @CompileStatic
    URL getSearchUrl(String format) {

        def params = [
            query : query.equals("*:*")?URLEncoder.encode(query,"UTF-8"): URLEncoder.encode('"'+query+'"',"UTF-8") ,
            size  : size,
            start : start,
            sort  : sort,
        ]
        StringBuilder url = new StringBuilder(BASE_URL)
        for (element in params) {
            Object v = element.value
            String k = element.key
            if (v != null)
                url.append('&').append(k).append('=').append(v)
        }
        new URL(url.toString() + "&format=" + format)
    }


    @Override
    public String toString() {
        return "Request {" +
            "query='" + query + '\'' +
            ", size=" + size +
            ", start=" + start +
            ", sort='" + sort + '\'' +
            '}';
    }
}
