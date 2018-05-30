<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>











<div class="dialog">
    <table class="responsive-table center">
        <tbody>
            <tr class="prop">
                <td class="name"><label for="registration">Registration of new Users:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'registration', 'errors')}">
                    <input type="checkbox" name="registration" id="registration" ${!userRegistration || userRegistration.registration ? 'checked="checked"' : ''} title="Registration of new Users"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="registration">Users curators by default:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'curator', 'errors')}">
                    <input type="checkbox" name="curator" id="curator" ${!userRegistration || userRegistration.curator ? 'checked="checked"' : ''} title="Users curators by default?"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="sendEmail">Send Email on User Registration:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'sendEmail', 'errors')}">
                    <input type="checkbox" name="sendEmail" id="sendEmail" ${!userRegistration || userRegistration.sendEmail ? 'checked="checked"' : ''} title="Send Email on User Registration"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="sendToAdmin">Send Email to Admin User instead of new user:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'sendToAdmin', 'errors')}">
                    <input type="checkbox" name="sendToAdmin" id="sendToAdmin" ${userRegistration && userRegistration.sendToAdmin ? 'checked="checked"' : ''} title="Send Email to Admin User instead of new user"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="senderAddress">Sender Address:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'senderAddress', 'errors')}">
                    <input type="text" name="senderAddress" id="senderAddress" value="${userRegistration?.senderAddress ?: 'user@example.org' }"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="adminAddress">Admin Address:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'adminAddress', 'errors')}">
                    <input type="text" name="adminAddress" id="adminAddress" value="${userRegistration?.adminAddress ?: 'admin@example.org'}"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="subject">Subject:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'subject', 'errors')}">
                    <input type="text" name="subject" id="subject" value="${userRegistration?.subject ?: 'Example Subject: New User'}"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="body">Body:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'body', 'errors')}">
                    <textarea id="body" rows="20" cols="40" name="body">
                        ${userRegistration?.body ?: 'Example Text: New User'}
                    </textarea>
                </td>
            </tr>
           <tr class="prop">
                <td class="name"><label for="resetSubject">Reset Password Subject:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'resetSubject', 'errors')}">
                    <input type="text" name="resetSubject" id="resetSubject" value="${userRegistration?.resetSubject ?: 'Example Subject: Reset Password'}"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="body">Reset Body:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'resetBody', 'errors')}">
                    <textarea id="resetBody" rows="20" cols="40" name="resetBody">
                        ${userRegistration?.resetBody ?: 'Example Text: Password reset body'}
                    </textarea>
                </td>
            </tr>
          <%--   <tr class="prop">
                <td class="name"><label for="url">URL for Account Verification:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'url', 'errors')}">
                    <input type="text" name="url" id="url" value="${userRegistration?.url ?: 'http://example.org:8080/jummp/'}"/> <span>register/validate/{{CODE}}</span>
                </td>
            </tr>
            <tr class="prop">
                <td colspan="2">The following Config Options are for account verification if an Administrator registers a new user account.</td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="activationSubject">Subject for Activation:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'activationSubject', 'errors')}">
                    <input type="text" name="activationSubject" id="activationSubject" value="${userRegistration?.activationSubject ?: 'New User, activate your account.'}"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="activationBody">Body for Account Verification:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'activationBody', 'errors')}">
                    <textarea id="activationBody" rows="20" cols="40" name="activationBody">${userRegistration?.activationBody ?: 'Please activate your account.'}</textarea>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="activationUrl">URL for Account Activation:</label></td>
                <td class="value ${hasErrors(bean: userRegistration, field: 'activationUrl', 'errors')}">
                    <input type="text" name="activationUrl" id="activationUrl" value="${userRegistration?.activationUrl ?: 'http://example.org:8080/jummp/'}"/><span>register/confirmRegistration/{{CODE}}</span>
                </td>
            </tr> --%>
        </tbody>
    </table>
</div>
