package net.biomodels.jummp.deployment.biomodels.parameters

import grails.validation.Validateable
import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 * Created by carankalle on 31/10/2018.
 */
@Validateable
@ToString(includes = ['query', 'size', 'responseformat', 'start', 'sort'])
class ParameterSearchCommand {
    public static final String DEFAULT_QUERY = '*:*'
    public static
    final String BASE_URL = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?" +
        "fields=entity_RAW,entity_id,initial_data_RAW,reaction_RAW,model,publication,rate_RAW,parameters_RAW,entity_accession_url,reaction_sbo_term_link,entity_sbo_term_link"
    String query
    Integer size
    Integer start
    String sort
    String responseformat

    static constraints = {
        query nullable: true, validator: { q, cmd ->

            if (null == q || q == "" || q == '*') {
                cmd.query = DEFAULT_QUERY
            }
            return true
        }
        size inList: [10, 25, 50, 100]
        start min: 0
        responseformat nullable: true , validator: { f, cmd ->

            if (f == "" || null == f) {
                cmd.responseformat = "json"
            }
            return true
        }
    }

    @CompileStatic
    URL getSearchUrl() {

        def params = [
            query : query,
            size  : size,
            start : start,
            sort  : sort,
            format : responseformat
        ]
        StringBuilder url = new StringBuilder(BASE_URL)
        for (element in params) {
            Object v = element.value
            String k = element.key
            url.append('&').append(k).append('=').append(v)
        }
        new URL(url.toString())
    }
}
