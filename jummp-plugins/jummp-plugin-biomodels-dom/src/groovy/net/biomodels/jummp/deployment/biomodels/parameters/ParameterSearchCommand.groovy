package net.biomodels.jummp.deployment.biomodels.parameters

import grails.validation.Validateable
import groovy.transform.CompileStatic
import groovy.transform.ToString

/**
 * Created by carankalle on 31/10/2018.
 */
@Validateable
@ToString(includes = ['query', 'size', 'start', 'sort'])
class ParameterSearchCommand {
    public static final String DEFAULT_QUERY = '*:*'
    public static
    final String BASE_URL = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?format=json&fields=entity,entity_id,reaction,model,publication,rate,parameters"
    String query
    Integer size
    Integer start
    String sort


    static constraints = {
        query nullable: true, validator: { q,cmd ->

            if(null == q || q == "" || q == "null" || q == '*'){
                cmd.query = DEFAULT_QUERY
            }
            return true
        }
        size inList: [10, 25, 50, 100]
        start min: 0
    }

    @CompileStatic
    URL getSearchUrl() {

        def params = [
            query: query,
            size : size,
            start: start,
            sort : sort
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
