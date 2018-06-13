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
 **/



package net.biomodels.jummp.core.model

/**
 * All our external applications have to be declared here
 */
enum LSFApplication {
    MODEL_CLASSIFIER("model_classifier", "start.sh", "stop.sh", 10 * 60)

    /**
     * Folder name of the external application
     * This folder must be placed in lsf.application.path
     */
    private String name

    /**
     * Start script name
     * This script use to start the application
     * And it must be placed in the folder name of the application
     */
    private String startScript

    /**
     * Stop script name
     * This script use to terminate the application
     * And it must be placed in the folder name of the application
     */
    private String stopScript

    /**
     * Maximum time to start this application (in second)
     * beyond this time, the application consider as failed
     */
    private int maxTimeStart

    LSFApplication(String name, String startScript, String stopScript, int maxTimeStart) {
        this.name = name
        this.startScript = startScript
        this.stopScript = stopScript
        this.maxTimeStart = maxTimeStart
    }

    String getName() {
        return name
    }

    String getStartScript() {
        return startScript
    }

    String getStopScript() {
        return stopScript
    }

    int getMaxTimeStart() {
        return maxTimeStart
    }
}
