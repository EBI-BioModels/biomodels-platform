import groovyx.gpars.GParsPool
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.model.Model
import net.biomodels.jummp.model.Revision

def modelService = ctx.getBean("modelService")

List curatedModels = Model.findAll { 
    publicationId != null && publicationId != '' && deleted == false
}   
curatedModels = curatedModels.sort { a, b ->
    Integer n1 = a.publicationId.minus("BIOMD") as Integer
    Integer n2 = b.publicationId.minus("BIOMD") as Integer
    n1 <=> n2
}
/*println curatedModels?.size()
println curatedModels.first().dump()
println curatedModels.first().revisions*.state
println curatedModels.first().revisions*.revisionNumber
println curatedModels.first().revisions*.revisionNumber.max()*/
final int POOL_SIZE = 8
GParsPool.withPool(POOL_SIZE) {
    curatedModels.eachParallel { Model model ->
        synchronized(this) {
        Model.withTransaction {
            def revisions = model.revisions
            def revs = revisions.findAll { 
                it.state == ModelState.PUBLISHED
            }
            revs = revs.sort { a, b ->
                a.revisionNumber <=> b.revisionNumber
            }
            //println "${model.publicationId}: ${revs*.revisionNumber}"
            Revision latest = revs.last()
            Integer maxRevNum = model.revisions.collect { it.revisionNumber }.max()
            if (latest.revisionNumber < maxRevNum) {
                println "${model.publicationId}: ${latest.revisionNumber} : ${maxRevNum}"
            }
        }
        }
    } 
}
return

