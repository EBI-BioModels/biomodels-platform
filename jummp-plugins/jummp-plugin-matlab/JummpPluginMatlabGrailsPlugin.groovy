import org.springframework.beans.factory.NoSuchBeanDefinitionException

class JummpPluginMatlabGrailsPlugin {
    // the plugin version
    def version = "0.1"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "2.5 > *"
    def loadAfter = ['jummp-plugin-configuration']
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
        "grails-app/views/error.gsp"
    ]

    def title = "Jummp plugin for Matlab/Octave"
    def author = "Mihai Glonț"
    def authorEmail = "mihai.glont@ebi.ac.uk"
    def description = '''\
Jummp support for Matlab (Octave) models.
'''

    // URL to the plugin's documentation
    def documentation = "http://bitbucket.org/jummp/jummp"

    // License: one of 'APACHE', 'GPL2', 'GPL3'
    def license = "AGPL3"

    // Details of company behind the plugin (if there is one)
    def organization = [ name: "EMBL-EBI", url: "http://www.ebi.ac.uk/" ]

    // Any additional developers beyond the author specified above.
//    def developers = [ [ name: "Joe Bloggs", email: "joe@bloggs.net" ]]

    // Location of the plugin's issue tracker.
    def issueManagement = [ system: "JIRA", url: "http://jummp-repo.atlassian.net/" ]

    // Online location of the plugin's browseable source code.
    def scm = [ url: "http://bitbucket.org/jummp/jummp" ]

    def doWithApplicationContext = { ctx ->
        registerFormatSupport(ctx)
    }

    def onChange = { event ->
        registerFormatSupport event.ctx
    }

    def onConfigChange = { event ->
        registerFormatSupport event.ctx
    }

    private void registerFormatSupport(def ctx) {
        try {
            def service = ctx.getBean("modelFileFormatService")
            def formatCmd = service.registerModelFormat("matlab", "MATLAB (Octave)")
            service.handleModelFormat(formatCmd, "matlabService", "matlab")
        } catch(NoSuchBeanDefinitionException ignored) {
            // running as standalone plugin
        }
    }
}
