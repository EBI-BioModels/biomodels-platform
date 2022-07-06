import grails.util.Environment

import static grails.util.Metadata.current as metaInfo

class AppStatus {
    def ctx

    void start() {
        header 'Application Status'
        row 'App version', metaInfo['app.version']
        row 'Grails version', metaInfo['app.grails.version']
        row 'Groovy version',
            GroovySystem.version
        row 'JVM version', System.getProperty('java.version')
        row 'Reloading active', Environment.reloadingAgentEnabled
        row 'Controllers', ctx.grailsApplication.controllerClasses.size()
        row 'Domains', ctx.grailsApplication.domainClasses.size()
        row 'Services', ctx.grailsApplication.serviceClasses.size()
        row 'Tag Libraries', ctx.grailsApplication.tagLibClasses.size()

        println()

        header 'Installed Plugins'
        ctx.getBean('pluginManager').allPlugins.each {
            plugin ->
                row plugin.name, plugin.version
        }
    }

    void row(final String label, final value) {
        println label.padRight(18) + ' : ' + value.toString().padLeft(8)
    }

    void header(final String title) {
        final int length = 29
        println '-' * length
        println title.center(length)
        println '-' * length
    }
}

new AppStatus(ctx: ctx).start()
// how to run: ./grailsw run-script scripts/AppStatus.groovy
