import net.biomodels.jummp.core.annotation.ElementAnnotationTransportCommand as EATC
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RedisCacheManager {
    private final Logger LOGGER = LoggerFactory.getLogger(RedisCacheManager.class)
    def ctx

    void run() {
        println "Extracting all annotations of a model"
        retrieveAnnotations()
    }

    void retrieveAnnotations() {
        def mDS = ctx.getBean("modelDelegateService")
        List<String> listAllIdentifiers = mDS.getAllModelIdentifiers()
        List tenFirstIds = listAllIdentifiers.take(5)
        //String modelId = "MODEL8389825246"
        tenFirstIds.each { String modelId ->
            doRetrieveAnnotations modelId
        }
    }

    def doRetrieveAnnotations(String modelId) {
        def mdDS = ctx.getBean("metadataDelegateService")
        def redis = ctx.getBean("redisService")

        Model model
        if (modelId.startsWith("MODEL")) {
            model = Model.findBySubmissionId(modelId)
        } else {
            model = Model.findByPublicationId(modelId)
        }
        if (!model) {
            LOGGER.info("$modelId doesn't exist.")
            return
        }
        Revision[] pairFirstLastRev = getFirstAndLastRevision(model)
        Revision latest = pairFirstLastRev[1]
        def revTC = new RevisionAdapter(revision: latest, latest: true).toCommandObject()
        List<EATC> annotations = mdDS.fetchAnnotations(revTC)
        List statements = annotations*.statement
        statements = statements.unique { it.object.uri }
        String hasTaxon = ""
        String strOfAnnotations = ""
        statements.each {
            //println "${model.submissionId}\t${it.predicate.accession}\t${it.object.datatype}\t${it.object.uri}"
            if (it.predicate.accession == "hasTaxon" && it.object.datatype == "taxonomy") {
                hasTaxon = "${it.object.accession}|${it.object.name}|${it.object.uri}"
            }
            strOfAnnotations += "${it.predicate.accession}\t${it.object.datatype}\t${it.object.uri}\t${it.object.name}|"
        }

        // remove the last pile - vertical line
        if (strOfAnnotations) {
            strOfAnnotations = strOfAnnotations.substring(0, strOfAnnotations.length() - 1)
        }
        redis.doRedisHSetNX(model.submissionId, "annotations", strOfAnnotations)

        if (hasTaxon) {
            redis.doRedisHSetNX(model.submissionId, "organism", hasTaxon)
        }

        //println statements.size()
    }

    protected Revision[] getFirstAndLastRevision(Model model) {
        Set<Revision> revisions = model.revisions.sort { Revision r1, Revision r2 ->
            r1.revisionNumber <=> r2.revisionNumber
        }
        Revision firstRevision = revisions.first() as Revision
        Revision lastRevision = revisions.last() as Revision
        [firstRevision, lastRevision] as Revision[]
    }
}

new RedisCacheManager(ctx: ctx).run()
