/**
 * Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 */

package net.biomodels.jummp

import grails.converters.JSON
import grails.converters.XML
import grails.util.Environment
import grails.util.Holders
import net.biomodels.jummp.utils.WebServiceFetcher as WSF
import net.biomodels.jummp.core.constants.BioModels
import org.codehaus.groovy.grails.plugins.support.aware.GrailsConfigurationAware

/**
 * Initialised common resources using amid controllers
 *
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
class CommonController implements GrailsConfigurationAware {
    static String bmStaticAssetsURL
    static String layout
    static String serverURL
    static String manualURL
    static String theme
    static String deployTarget
    static String EBI_BM_FTP
    static String EBI_BM_FTP_REPO
    static Map COMMON_PROPERTIES = [:]

    def grailsApplication
    def redisService

    @Override
    void setConfiguration(ConfigObject co) {
        layout = "${co.jummp.branding.style}/main"
        grailsApplication = Holders.grailsApplication
        manualURL = grailsApplication.config.jummp.context.help.root
        serverURL = grailsApplication.config.grails.serverURL
        deployTarget = "local"
        EBI_BM_FTP = co.jummp.model.ftp.location

        if (serverURL.contains("wwwdev.ebi.ac.uk/biomodels")) {
            bmStaticAssetsURL = "${BioModels.BM_DEV_ROOT_URL}/static-assets"
            deployTarget = "dev"
            EBI_BM_FTP = BioModels.EBI_BMDEV_PUBLIC_FTP
        } else if (Environment.current == Environment.PRODUCTION && serverURL.contains("www.ebi.ac.uk/biomodels")) {
            bmStaticAssetsURL = "${BioModels.BM_ROOT_URL}/static-assets"
            deployTarget = "prod"
            EBI_BM_FTP = BioModels.EBI_BMPROD_PUBLIC_FTP
        } else if (serverURL.contains("biomodels.org") ||
                serverURL.contains("biomodels.net") ||
                serverURL.contains("dmscambs.co.uk")) {
            // Presumably, biomodels.org and biomodels.net will be the official addresses after the migration
            // Before switching to these domains, we use dmscambs.co.uk temporarily
            deployTarget = "prod"
            // the following properties must be updated
            // bmStaticAssetsURL
            // EBI_BM_FTP -- should be renamed to make it neutrally
        } else {
            // local or dev target
            bmStaticAssetsURL = "${BioModels.BM_DEV_ROOT_URL}/static-assets"
        }
        EBI_BM_FTP_REPO = EBI_BM_FTP ? WSF.addPath(new URI(EBI_BM_FTP), "repository").toString() : ""
        theme = grailsApplication.config.jummp.branding.style
        if (!theme) theme = "default"
        COMMON_PROPERTIES = [
            "bmStaticAssetsURL": bmStaticAssetsURL,
            "layout": layout,
            "manualURL": manualURL,
            "serverURL": serverURL,
            "theme": theme,
            "deployTarget": deployTarget,
            "EBI_BM_FTP": EBI_BM_FTP,
            "EBI_BM_FTP_REPO": EBI_BM_FTP_REPO
        ]
    }

    protected handleRestApi(final Map resultMap) {
        // PageFragmentCachingFilter throws a NPE for unsupported format parameter values
        if (!(response.format in ['json', 'xml'])) {
            render view: '/errors/error415', status: 415
            return
        }
        withFormat {
            json { render resultMap as JSON }
            xml { render resultMap as XML }
            '*' { render status: 415, view: "/errors/error415" }
        }
    }

    void checkReadOnlyMode() {
        boolean readonly = redisService.doRedisGet("readonly") as boolean
        if (readonly) {
            forward(controller: "system", action: "readonly")
        }
    }
}
