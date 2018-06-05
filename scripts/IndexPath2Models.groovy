import groovyx.gpars.GParsPool
import java.time.Duration
import java.time.Instant
import net.biomodels.jummp.model.*
import net.biomodels.jummp.core.model.*
import net.biomodels.jummp.core.adapters.*
import net.biomodels.jummp.core.model.identifier.decorator.AbstractAppendingDecorator as AAD
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken as UPT
import org.springframework.security.core.context.SecurityContextHolder as SCH

RevisionTransportCommand.context = ctx
AAD.context = ctx

// don't let Camel shut itself down within 5 minutes of the importer finishing.
// wait for all models to be indexed instead.
def camelContext = ctx.camelContext
camelContext.shutdownStrategy.setTimeout(Long.MAX_VALUE)

revisionIds = Model.findAllBySubmissionIdLike("BMID%").collect { it.revisions.first().id }

start = Instant.now()
GParsPool.withPool {
    revisionIds.eachParallel { id ->
        // must be done on every worker thread because ACL permissions get checked during indexing
        SCH.context.authentication = ctx.authenticationManager.authenticate(
                new UPT('administrator', 'administrator')
        )
        try {
            Revision.withTransaction {
                def r = Revision.get(id)
                def rtc = new RevisionAdapter(revision: r).toCommandObject()
                ctx.searchService.updateIndex(rtc)
           }
        } catch (Throwable e) {
            throw new IllegalStateException("Exception encountered while processing revision $id", e)
        }
    }
}
duration = Duration.between(start, Instant.now())
println "Scheduled ${revisionIds.size()} revisions for indexing in $duration"
