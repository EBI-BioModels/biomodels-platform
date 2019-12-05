import grails.converters.XML
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchXmlMarshaller
import net.biomodels.jummp.models.ReactomeServiceFactoryBean

class JummpPluginBiomodelsDomGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.3 > *"
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def loadAfter = ["jummp-plugin-core-api"]

    // TODO Fill in these fields
    def title = "BioModels database-specific domain classes" // Headline display name of the plugin
    def author = "Raza Ali"
    def authorEmail = "raza.ali@ebi.ac.uk"
    def description = '''\
Plugin to define domain classes specific to the biomodels database
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/jummp-plugin-biomodels-dom"

    // Extra (optional) plugin metadata

    // License: one of 'APACHE', 'GPL2', 'GPL3'
//    def license = "APACHE"

    // Details of company behind the plugin (if there is one)
//    def organization = [ name: "My Company", url: "http://www.my-company.com/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
//    def issueManagement = [ system: "JIRA", url: "http://jira.grails.org/browse/GPMYPLUGIN" ]

    // Online location of the plugin's browseable source code.
//    def scm = [ url: "http://svn.codehaus.org/grails-plugins/" ]

    def doWithWebDescriptor = { xml ->
        // TODO Implement additions to web.xml (optional), this event occurs before
    }

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
        def df = "yyyy-MM-dd'T'HH:mm:ss"
        if (!application.config.grails.databinding.dateFormats.contains(df)) {
            application.config.grails.databinding.dateFormats.add(df)
        }
        reactomeService(ReactomeServiceFactoryBean) { bean ->
            bean.scope = "prototype"
        }
    }

    def doWithDynamicMethods = { ctx ->
        // TODO Implement registering dynamic methods to classes (optional)
    }

    def doWithApplicationContext = { ctx ->
        // TODO Implement post initialization spring config (optional)
        XML.registerObjectMarshaller(new ParameterSearchXmlMarshaller())
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }

    def onShutdown = { event ->
        // TODO Implement code that is executed when the application shuts down (optional)
    }
}
