import net.biomodels.jummp.core.annotation.ElementAnnotationTransportCommand as EATC
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RedisCacheManager {
    private final Logger LOGGER = LoggerFactory.getLogger(RedisCacheManager.class)
    def ctx

    void main() {
        println "Extracting all annotations of a model"
        retrieveAnnotations()
    }

    void retrieveAnnotations() {
        println "Started the job at ${new Date()}..."
        def mDS = ctx.getBean("modelDelegateService")
        List<String> listAllIdentifiers = mDS.getAllModelIdentifiers() // 3084 as of writing this comment
        //List tenFirstIds = listAllIdentifiers.take(5)
        //String modelId = "MODEL8389825246"
        listAllIdentifiers = listAllIdentifiers.subList(1900, 2000)
        // 0..99: subList(0, 100)
        // 100..199: subList(100, 200)
        // 199..499: subList(200, 500)
        // 499..999: subList(500, 1000)
        // 999..1499: subList(1000, 1500)
        // 1499..1799: subList(1500, 1800)
        // 1799..1999: subList(1800, 2000)
        // 1999..2005: subList(2000, 2005)
        // 2005..2200: subList(2006, 2200)
        // 2200..2500: subList(2200, 2500)
        // 2500..2700: subList(2500, 2700)
        // 2700..2900: subList(2700, 2900)
        // 2900..3084: subList(2900, 3084)
        //listAllIdentifiers = listAllIdentifiers.subList(2900, 3084)
        //listAllIdentifiers = ["BIOMD0000000001", "BIOMD0000000002"]
        //listAllIdentifiers = listAllIdentifiers.findAll { it.startsWith
        //("BIOMD") }
        listAllIdentifiers = ["BIOMD0000000001"]
        listAllIdentifiers.each { String modelId ->
            doRetrieveAnnotations modelId
        }
    }

    void doRetrieveAnnotations(String modelId) {
        def mDS = ctx.getBean("modelDelegateService")
        def mdDS = ctx.getBean("metadataDelegateService")

        Model model
        if (modelId.startsWith("MODEL")) {
            model = Model.findBySubmissionId(modelId)
        } else {
            model = Model.findByPublicationId(modelId)
        }
        if (!model) {
            LOGGER.info("$modelId doesn't exist.")
            println("$modelId doesn't exist.")
            return
        }
        Revision[] pairFirstLastRev = mDS.getFirstAndLastRevision(model)
        Revision latest = pairFirstLastRev[1]
        println "Retrieving the organism and annotations of $modelId.${latest.revisionNumber}, caching them on Redis Server"
        def revTC = new RevisionAdapter(revision: latest, latest: true).toCommandObject()
        Map mapResult = mdDS.cacheAnnotationsAndOrganismOnRedis(revTC)
        if (mapResult.containsKey("organism")) {
            println("Organism: ${mapResult.get('organism')}")
        }
        String strAnnotations = mapResult.get("annotations")
        List<String> annotations = strAnnotations.tokenize("|")
        annotations.each {
            println(it)
        }
    }
}

new RedisCacheManager(ctx: ctx).main()
