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











<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="layout" content="${session['branding.style']}/main" />
        <title>Reset Password | BioModels</title>
        <g:render template="/usermanagement/head"/>
        <style>
        	.verysecure {
        		visibility:hidden;
        	}
        </style>
    </head>
     <body>
        <div class="row">
            <div class="small-12 medium-6 medium-centered large-4 large-centered columns">
            <h3 class="text-center">Reset your password</h3>
            <g:form name="resetForm" action="newPassword" class="log-in-form" useToken="true"
                    onsubmit="return validateForm()">
                <label for="username">
                    <span class="required"><g:message code="user.signup.ui.username"/></span>
                    <g:textField name="username" value="${username}" readonly="true" autocomplete="username" />
                </label>
                <label for="newPassword">
                    <span class="required"><g:message code="user.administration.updatePassword.newPassword"/></span>
                    <g:passwordField name="newPassword" id="newPassword" autocomplete="new-password"
                                     placeholder="Enter a new password"/>
                    <span id="toggle-password"
                          class="fa fa-fw fa-eye field-icon toggle-password"></span>
                </label>
                <div class="help-text" id="new-password-help" style="color: red !important;"></div>

                <label for="newPasswordRpt">
                    <span class="required"><g:message code="user.administration.updatePassword.newPasswordRpt"/></span>
                    <g:passwordField name="newPasswordRpt" id="newPasswordRpt" autocomplete="new-password-repeat"
                                     placeholder="Re-enter the new password"/>
                </label>
                <p class="help-text" id="new-password-rpt-help" style="color: red !important;"></p>
                <div class="buttons">
                    <input type="submit" class="button" value="Reset Password"/>
                </div>
                <label>
                    <input class="verysecure" name="hashCode" value="${hashCode}"/>
                </label>
            </g:form>
            </div>
        </div>

 <g:render template="/usermanagement/common-scripts"/>

<g:javascript>
    const helpText = $('.help-text');
    const newPassword = $('#newPassword');
    const newPasswordHelp = $("#new-password-help");
    const newPasswordRpt = $('#newPasswordRpt');
    const newPasswordRptHelp = $("#new-password-rpt-help");

    newPassword.on("change blur keyup keydown keypress", function() {
        validateNewPassword(newPassword, newPasswordHelp, false);
    });

    newPasswordRpt.on("change blur keyup keydown keypress", function() {
        validateNewPasswordRpt(newPassword, newPasswordRpt, newPasswordRptHelp);
    });

    function validateForm() {
        console.log("Validating the form of changing password...");
        const newPassCheck = validateNewPassword(newPassword, newPasswordHelp, true);
        const newPasswordRptCheck = validateNewPasswordRpt(newPassword, newPasswordRpt, newPasswordRptHelp);
        const retVal = newPassCheck && newPasswordRptCheck;
        console.log(retVal);
        return retVal;
    }
</g:javascript>
 <g:render template="/usermanagement/foot"/>
     </body>
</html>
<content tag="title">
	<g:message code="user.resetpassword.ui.heading"/>
</content>
