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

package net.biomodels.jummp.webapp

/**
 * @short Command object for User registration
 */
@grails.validation.Validateable
class UpdatePasswordCommand {
    String oldPassword
    String newPassword
    String newPasswordRpt

    static constraints = {
        oldPassword(nullable: false, blank: false)
        newPassword(nullable: false, blank: false)
        newPasswordRpt validator: { repeatValue, cmd ->
        	return repeatValue && cmd.newPassword == repeatValue
        }
    }
    UpdatePasswordCommand sanitise() {
        UpdatePasswordCommand cmd = new UpdatePasswordCommand()
        cmd.oldPassword = this.oldPassword.decodeHTML()
        cmd.newPassword = this.newPassword.decodeHTML()
        cmd.newPasswordRpt = this.newPasswordRpt.decodeHTML()
        return cmd
    }
}
