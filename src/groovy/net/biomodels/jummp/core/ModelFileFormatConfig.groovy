/**
 * Copyright (C) 2010-2017 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 **/





package net.biomodels.jummp.core
/**
 * @short Class written as a spring-bean singleton to hold modelfileformatservice config data
 *
 * The class exists to store config required by the modelfileformatservice, which was getting lost
 * after jummp was left running for a long time.
 *
 * @author Raza Ali, raza.ali@ebi.ac.uk
 * @date 9/09/2013
 */
class ModelFileFormatConfig {
    /**
     * The registered services to handle ModelFormats
     */
    static final Map<String, String> services = new HashMap()

    /**
     * The registered plugins to handle ModelFormats visualisations
     */
    static final Map<String, String> controllers = new HashMap()

    public Map<String, String> getServices() {
        services
    }

    public Map<String, String> getControllers() {
        controllers
    }

    public void status() {
        println("MODEL FILE FORMAT CONFIG STATUS:")
        println("SERVICES: ${services.inspect()}")
        println("CONTROLLERS: ${controllers.inspect()}")
    }
}
