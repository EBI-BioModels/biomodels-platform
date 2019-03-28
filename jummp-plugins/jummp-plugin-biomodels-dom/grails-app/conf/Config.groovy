import grails.converters.XML
import net.biomodels.jummp.deployment.biomodels.parameters.ParameterSearchXmlMarshaller

// configuration for plugin testing - will not be included in the plugin zip

log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
           'org.codehaus.groovy.grails.web.pages', //  GSP
           'org.codehaus.groovy.grails.web.sitemesh', //  layouts
           'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
           'org.codehaus.groovy.grails.web.mapping', // URL mapping
           'org.codehaus.groovy.grails.commons', // core / classloading
           'org.codehaus.groovy.grails.plugins', // plugins
           'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
           'org.springframework',
           'org.hibernate',
           'net.sf.ehcache.hibernate'
}

grails.databinding.dateFormats = ["yyyy-MM-dd'T'HH:mm:ss"]
XML.registerObjectMarshaller(new ParameterSearchXmlMarshaller())
grails.hibernate.cache.queries = false

beans {
    cacheManager {
        shared = true
    }
}

grails.cache.config.provider.name = "jummpCacheManager"
grails.cache.ehcache.cacheManagerName = "jummpCacheManager"

if (jummpConfig.jummp.cache.dir) {
    jummp.cache.dir = jummpConfig.jummp.cache.dir
}
