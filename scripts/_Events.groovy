/**
 * Copyright (C) 2010-2018 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

includeTargets << new File("./scripts/WeceemExport.groovy")

/**
 * Script to write the git id of the build into _version.gsp
 */
eventCompileStart = { msg ->
    // the only way I could get Groovy to execute a command with an argument containing spaces
    // the main command is to get the hash id of the last commit on the development branch
    def cmd = ["git", "log", "-n", "1", "development",
         "--pretty=format:<a href=\"//bitbucket.org/biomodels/jummp-biomodels/commits/all?search=%h\">%h</a> | %aD"]
    def proc = cmd.execute()
    proc.waitFor()
    ant.mkdir(dir: "grails-app/views/templates/")
    String txt = proc.in.text
    println "commit id: $txt"
    new FileOutputStream("grails-app/views/templates/_version.gsp", false) << txt

    // copy the messages.properties
    /*ant.mkdir(dir: "web-app/js/i18n/")
    ant.copy(file: "grails-app/i18n/messages.properties", todir: "web-app/js/i18n/")*/
}

eventCompileEnd = {
    weceem()
}
