/**
 * Copyright (C) 2010-2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 */

package net.biomodels.jummp.models

import groovy.transform.CompileStatic
import net.biomodels.jummp.deployment.biomodels.ReactomeService
import org.springframework.beans.factory.FactoryBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Factory bean for (prototype-scoped) ReactomeService instances.
 *
 * Parses the Reactome mapping file (models2pathways.tsv), storing the first Reactome pathway ID for a model
 * and stores this information into a map that is passed to every ReactomeService instance.
 *
 * @author carankalle on 26/11/2019.
 * @author mglont on 05/12/2019.
 * @see {@link ReactomeService}
 */
@CompileStatic
class ReactomeServiceFactoryBean implements FactoryBean<ReactomeService>, ApplicationContextAware, InitializingBean {
    Map<String, List<String>> modelPathwayMap = new HashMap<>()
    ApplicationContext applicationContext

    @Override
    ReactomeService getObject() throws Exception {
        new ReactomeService(modelPathwayMap)
    }

    @Override
    Class<?> getObjectType() {
        return ReactomeService.class
    }

    @Override
    boolean isSingleton() {
        false // prototype beans
    }

    @Override
    void afterPropertiesSet() throws Exception {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("models2pathways.tsv")
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))
        try {
            String line
            while ((line = reader.readLine())) {
                parseLineAndPrepareMap(line)
            }
        } catch (IOException ie) {
            System.err.printf("Unable to parse reactome mapping file, %s", ie.getMessage())
        } finally {
            inputStream.close()
            reader.close()
        }
    }

    private void parseLineAndPrepareMap(String line) {
        String[] lineArr = line.split('\t')
        String modelId = lineArr[0]
        String reactomeId = lineArr[1]
        String pathwayName = lineArr[4]
        String label
        if(null != pathwayName) {
            label = pathwayName + "|" + reactomeId
        } else {
            label = reactomeId
        }
        if (modelPathwayMap.containsKey(modelId)) {
            List<String> reactomeIds = modelPathwayMap[modelId]
            if(null == reactomeIds) {
                reactomeIds = new ArrayList<>()
            }
            reactomeIds.add(label)
            modelPathwayMap[modelId] = reactomeIds
        } else {
            List<String> reactomeIds = new ArrayList<String>()
            reactomeIds.add(label)
            modelPathwayMap[modelId] = reactomeIds
        }

    }
}
