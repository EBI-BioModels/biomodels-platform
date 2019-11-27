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
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream("./models2pathways.tsv")
        println(inputStream)
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
