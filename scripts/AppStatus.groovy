import grails.util.Environment

import static grails.util.Metadata.current as metaInfo

includeTargets << grailsScript('_GrailsBootstrap')

target(main: 'Prints the application status: versions, artefact counts and installed plugins') {
    depends(classpath, checkVersion, configureProxy, enableExpandoMetaClass, compile, bootstrapOnce)

    header 'Application Status'
    row 'App version', metaInfo['app.version']
    row 'Grails version', metaInfo['app.grails.version']
    row 'Groovy version',
        GroovySystem.version
    row 'JVM version', System.getProperty('java.version')
    row 'Reloading active', Environment.reloadingAgentEnabled
    row 'Controllers', appCtx.grailsApplication.controllerClasses.size()
    row 'Domains', appCtx.grailsApplication.domainClasses.size()
    row 'Services', appCtx.grailsApplication.serviceClasses.size()
    row 'Tag Libraries', appCtx.grailsApplication.tagLibClasses.size()

    println()

    header 'Installed Plugins'
    appCtx.getBean('pluginManager').allPlugins.each {
        plugin ->
            row plugin.name, plugin.version
    }
}

setDefaultTarget(main)

void row(final String label, final value) {
    println label.padRight(18) + ' : ' + value.toString().padLeft(8)
}

void header(final String title) {
    final int length = 29
    println '-' * length
    println title.center(length)
    println '-' * length
}
// how to run: ./grailsw app-status
