package net.biomodels.jummp.search

import groovy.transform.CompileStatic
import uk.ac.ebi.ddi.ebe.ws.dao.config.AbstractEbeyeWsConfig
import uk.ac.ebi.ddi.ebe.ws.dao.config.EbeyeWsConfigProd

/**
 * Chooses the EBI Search server that {@link OmicsdiBasedSearch} queries.
 *
 * The server comes from the setting <code>jummp.search.omicsdi.baseUrl</code>, for example
 * <code>https://www.ebi.ac.uk</code> or <code>http://localhost:8000</code>. Without the setting the
 * production server, https://www.ebi.ac.uk, is used in every environment.
 */
@CompileStatic
class EbiSearchServer {
    static final String DEFAULT_BASE_URL = "https://www.ebi.ac.uk"

    /**
     * @param baseUrl the value of the setting, which is a ConfigObject and not a String when it is missing
     * @return the configuration for the server in the setting, or for www.ebi.ac.uk if there is none
     * @throws IllegalArgumentException if the setting is not an http or https address of a server,
     *      with an optional numeric port, no credentials and no path
     */
    static AbstractEbeyeWsConfig configFor(Object baseUrl) {
        String setting = baseUrl instanceof CharSequence ? baseUrl.toString().trim() : ""
        if (!setting) {
            return new EbeyeWsConfigProd()
        }
        URI uri
        try {
            uri = new URI(setting)
        } catch (URISyntaxException e) {
            // the exception is not chained: its message repeats the setting, possibly with credentials
            throw new IllegalArgumentException(message(setting))
        }
        String path = uri.path ?: ""
        boolean valid = uri.scheme in ["http", "https"] && uri.authority ==~ AUTHORITY && path in ["", "/"] &&
                !uri.query && !uri.fragment
        if (!valid) {
            throw new IllegalArgumentException(message(setting))
        }
        return new Configured(uri.scheme, uri.authority)
    }

    /**
     * A host name, IPv4 address or bracketed IPv6 address, then an optional port. Underscores are allowed
     * because docker-compose service names have them, and java.net.URI does not report such names as a host.
     * Credentials (user:password@) do not match, and neither do an empty host or a port that is not a number.
     */
    private static final java.util.regex.Pattern AUTHORITY =
            ~/(?:[A-Za-z0-9._-]+|\[[0-9A-Fa-f:.]+\])(?::\d{1,5})?/

    private static String message(String setting) {
        // never echo credentials from the setting into the log
        String shown = setting.replaceFirst(/(?<=\/\/)[^\/@]*@/, "***@")
        "The setting 'jummp.search.omicsdi.baseUrl' is '${shown}'. It must be the address of the " +
                "EBI Search server, such as ${DEFAULT_BASE_URL}, with http or https, an optional numeric port, " +
                "no credentials and no path."
    }

    @CompileStatic
    private static class Configured extends AbstractEbeyeWsConfig {
        Configured(String protocol, String hostName) {
            super(protocol, hostName)
        }
    }
}
