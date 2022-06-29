package net.biomodels.jummp.plugins.sbml

import groovy.transform.CompileStatic
import net.biomodels.jummp.deployment.biomodels.ParameterSearchService
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchCommand
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchResults
import net.biomodels.jummp.deployment.biomodels.parameters.SearchResultEntry
import net.biomodels.jummp.plugins.sbml.parameters.BPComponentReactions
import net.biomodels.jummp.plugins.sbml.parameters.BPComponentSpecies


class BpToModelDisplayService {
    static transactional = false

    @CompileStatic
    Map getComponentsFromBP(String modelId) throws IOException {
        ParameterSearchCommand command = new ParameterSearchCommand()
        command.query = modelId
        ParameterSearchService parameterSearchService = new ParameterSearchService()
        ParameterSearchResults results = parameterSearchService.getJSONData(command, modelId)
        if (null == results) {
            throw new RuntimeException("No records to display")
        }
        Map componentsMap = [:]
        componentsMap["species"] = extractUniqueSpecies(results)
        componentsMap["reactions"] = extractUniqueReactions(results)
        return componentsMap

    }

    private Map extractUniqueSpecies(ParameterSearchResults results) {
        Map map = [:]
        int count = 0
        for (SearchResultEntry searchResultEntry : results.entries) {
            BPComponentSpecies bpComponentSpecies = new BPComponentSpecies(
                initialData: searchResultEntry.fields["initial_data"],
                speciesAnnotationShow: searchResultEntry.fields["entity_show"])
            map[searchResultEntry.fields["entity_id"]] = bpComponentSpecies
            if(++count > 200) {
                break
            }
        }

        return map
    }

    private Map extractUniqueReactions(ParameterSearchResults results) {
        Map map = [:]
        int count = 0
        for (SearchResultEntry searchResultEntry : results.entries) {
            BPComponentReactions bmpComponentReactions = new BPComponentReactions(
                reaction: searchResultEntry.fields["reaction"],
                rate: searchResultEntry.fields["rate"],
                parameters : searchResultEntry.fields["parameters"])
            map[searchResultEntry.fields["reaction"]] = bmpComponentReactions
            if(++count > 200) {
                break
            }
        }

        return map
    }

}
