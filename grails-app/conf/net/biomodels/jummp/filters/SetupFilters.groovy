/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Spring Framework (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Spring Framework used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.filters

import org.springframework.beans.factory.InitializingBean
import org.codehaus.groovy.grails.commons.ConfigurationHolder as CH

/**
 * @short Filter to redirect to SetupController
 *
 * The filter checks whether the application instance has been configured yet.
 * That is, it tests for the availability of the file @c ~/.jummp.properties.
 * When the file does not exist, all requests (except Ajax requests) are redirected
 * to the setup webflow of SetupController.
 *
 * When the file exists but the property @c jummp.firstRun is set to true all requests
 * are redirected to the firstRun webflow of SetupController.
 *
 * When the file exists and the firstRun property is false all access to SetupController
 * are blocked as the application is already configured.
 *
 * @see net.biomodels.jummp.plugins.configuration.SetupController
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
class SetupFilters implements InitializingBean {
    private boolean configFileExists = false
    private boolean firstRun = false
    def configurationService
    def grailsApplication

    public void afterPropertiesSet() throws Exception {
        String configPath=configurationService.getConfigFilePath()
        if (configPath) {
        	configFileExists=true
            Properties props = new Properties()
            props.load(new FileInputStream(configPath))
           // firstRun = Boolean.parseBoolean(props.getProperty("jummp.firstRun"))
           firstRun = false //disabling the non-functioning admin creation mechanism
        } else {
            firstRun = false
        }
    }

    def filters = {
    		// special workaround for the root view: http://jira.grails.org/browse/GRAILS-10184
    		maintenanceRoot(uri: '/') {
    			before = {
    				if (grailsApplication.mainContext.getBean("maintenanceMode").getMode()) {
    					redirect(controller: 'maintenance', action:"index" )
    					return true; 
    				}
    			}
    		}
    		maintenanceMode(controllerExclude: 'maintenance') {
    			before = {
    				if (grailsApplication.mainContext.getBean("maintenanceMode").getMode() && controllerName) {
    					redirect(controller: 'maintenance', action:"index" )
    					return false;
    				}
    			}
    		}
    		if (configFileExists) {
            if (firstRun) {
                setupFilter1(controllerExclude: 'setup') {
                    before = {
                    	redirect(controller: 'setup', action: "firstRun")
                        return true
                    }
                }
                flowFilter(controller: 'setup', action: 'setup') {
                    before = {
                    	redirect(controller: 'setup', action: "firstRun")
                        return true
                    }
                }

            } else {
                setupFilter2(controller: 'setup', action: '*') {
                    before = {
                    	redirect(uri: '/')
                        return false
                    }
                }
            }
        } else {
            /*setupFilter3(controller: 'setup', invert: true) {
                before = {
                   	redirect(controller: 'setup')
                    return true
                }
            }*/
            firstRun(controller: 'setup', action: 'firstRun') {
                before = {
                   	redirect(controller: 'setup', action: 'setup')
                    return true
                }
            }
        }
    }
}
