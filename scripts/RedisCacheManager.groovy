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
            List stmts = doRetrieveAnnotations modelId
            if (stmts.isEmpty()) {
                println modelId
            }
        }
    }

    List doRetrieveAnnotations(String modelId) {
        def mDS = ctx.getBean("modelDelegateService")
        def mdDS = ctx.getBean("metadataDelegateService")
        def redis = ctx.getBean("redisService")
        println "Retrieving annotations of $modelId"

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
        println "${modelId}.${latest.revisionNumber}"
        def revTC = new RevisionAdapter(revision: latest, latest: true).toCommandObject()
        List<EATC> annotations = mdDS.fetchAnnotations(revTC)
        List statements = annotations*.statement
        statements = statements.unique { it.object.uri }
        String hasTaxon = ""
        String strOfAnnotations = ""
        statements.each {
            println """${model.submissionId}\t${it.predicate.accession}\t${it.
                    object.datatype}\t${it.object.accession}\t${it.object.uri}"""
            if (it.predicate.accession == "hasTaxon" && it.object.datatype == "taxonomy") {
                hasTaxon = "${it.object.accession}|${it.object.name}|${it.object.uri}"
            }
            strOfAnnotations += """${it.predicate.accession}\t${it.object.
                    datatype}\t${it.object.accession}\t${it.object.uri}\t${it.
                    object.name}|"""
        }

        // remove the last pile - vertical line
        if (strOfAnnotations) {
            strOfAnnotations = strOfAnnotations.substring(0, strOfAnnotations.length() - 1)
        }
        redis.doRedisHSetNX(model.submissionId, "annotations", strOfAnnotations)
        if (model.publicationId) {
            redis.doRedisHSetNX(model.publicationId, "annotations", strOfAnnotations)
        }

        if (hasTaxon) {
            redis.doRedisHSetNX(model.submissionId, "organism", hasTaxon)
            if (model.publicationId) {
                redis.doRedisHSetNX(model.publicationId, "organism", hasTaxon)
            }
        }

        statements
    }
}

new RedisCacheManager(ctx: ctx).main()
