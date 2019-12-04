/*
    Copyright (C) 2019 EMBL-European Bioinformatics Institute (EMBL-EBI),
    Deutsches Krebsforschungszentrum (DKFZ)

This file is part of Jummp.

    Jummp is free software; you can redistribute it and/or modify it under the
terms of the GNU Affero General Public License as published by the Free
Software Foundation; either version 3 of the License, or (at your option) any
later version.

    Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
details.

    You should have received a copy of the GNU Affero General Public License along
with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.

Additional permission under GNU Affero GPL version 3 section 7

If you modify Jummp, or any covered work, by linking or combining it with
Apache Commons (or a modified version of that library), containing parts
covered by the terms of Apache License v2.0, the licensors of this
Program grant you additional permission to convey the resulting work.
    {Corresponding Source for a non-source form of such a combination shall include
        the source code for the parts of Apache Commons used as well as that of
        the covered work.}
*/

package net.biomodels.jummp.models
/**
 * @author carankalle on 26/11/2019.
 */
class DefaultReactomeMapper implements ReactomeMapper {
    Map<String, String> modelPathwayMap = new HashMap<String, String>()

    Map<String, String> getModelPathwayMap() {
        return modelPathwayMap
    }

    DefaultReactomeMapper() {
        extractModel2PathwayData()
    }

    @Override
    void extractModel2PathwayData() {
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("models2pathways.tsv")
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))
        try  {
            for(String line : reader.lines().iterator()) {
                parseLineAndPrepareMap(line)
            }
        } catch (IOException ie) {
            System.err.printf("Unable to parse reactome mapping file, %s", ie.getMessage())
        } finally{
            inputStream.close()
            reader.close()
        }
    }

    private void parseLineAndPrepareMap(String line) {
        String[] lineArr = line.split('\t')
        if (!modelPathwayMap.containsKey(lineArr[0])) {
            modelPathwayMap[lineArr[0]] = lineArr[1]
        }

    }

}
