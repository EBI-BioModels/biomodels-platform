import net.biomodels.jummp.core.annotation.ElementAnnotationTransportCommand as EATC
import net.biomodels.jummp.core.adapters.RevisionAdapter
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision

class RedisCacheManager {
    def ctx

    void run() {
        println "Extracting all annotations of a model"
        retrieveAnnotations()
    }

    void retrieveAnnotations() {
        String modelId = "MODEL8389825246"
        doRetrieveAnnotations modelId
    }

    def doRetrieveAnnotations(String modelId) {
        def mS = ctx.getBean("modelService")
        def mdDS = ctx.getBean("metadataDelegateService")

        Model model = Model.findBySubmissionId(modelId)
        Revision[] pairFirstLastRev = getFirstAndLastRevision(model)
        Revision latest = pairFirstLastRev[1]
        def revTC = new RevisionAdapter(revision: latest, latest: true).toCommandObject()
        long revisionId = latest?.id
        List<EATC> annotations = mdDS.fetchAnnotations(revTC)
        List statements = annotations*.statement
        statements = statements.unique { it.object.uri }
        statements.each {
            println "${it.predicate.accession}\t${it.object.datatype}\t${it.object.uri}"
        }
        println statements.size()
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
