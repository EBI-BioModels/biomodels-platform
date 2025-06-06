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
    <title>${title}</title>
    <g:render template="/usermanagement/head"/>
</head>
<body>
    <div class="row">
        <div class="small-12 medium-6 medium-centered large-4 large-centered columns">
            <h3>Change your password</h3>
            <g:form action="updatePassword" useToken="true" onsubmit="return validateForm()">
                <div class="row column edit-password-form">
                    <label class="required" for="oldPassword">
                        <g:message code="user.administration.updatePassword.oldPassword"/></label>
                    <g:passwordField id="oldPassword" name="oldPassword" required="required"
                                     placeholder="Current password"/>
                    <p class="help-text" id="old-password-help" style="color: red !important;"></p>
                    <label class="required" for="newPassword">
                        <g:message code="user.administration.updatePassword.newPassword"/></label>
                    <g:passwordField id="newPassword" name="newPassword" required="required"
                                     placeholder="New password"/>
                    <span><i id="toggler" class="far fa-eye"></i></span>
                    <div class="help-text" id="new-password-help" style="color: red !important;"></div>
                    <label class="required" for="newPasswordRpt">
                        <g:message code="user.administration.updatePassword.newPasswordRpt"/></label>
                    <g:passwordField id="newPasswordRpt" name="newPasswordRpt" required="required"
                                     placeholder="Re-enter new password"/>
                    <p class="help-text" id="new-password-rpt-help" style="color: red !important;"></p>
                    <p class="buttons">
                        <input type="submit" class="button"
                               value="${g.message(code: 'user.administration.updatePassword.submit')}"/>
                    </p>
                </div>
            </g:form>
        </div>
    </div>
    <g:render template="/usermanagement/common-scripts"/>
    <g:javascript>
        const helpText = $('.help-text');
        const oldPassword = $('#oldPassword');
        const newPassword = $('#newPassword');
        const newPasswordRpt = $('#newPasswordRpt');
        const newPasswordHelp = $("#new-password-help");
        const oldPasswordHelp = $("#old-password-help");
        const newPasswordRptHelp = $("#new-password-rpt-help");

        $(document).ready(function(){
            doShowOrHideAllHelp(false);
        });

        function doShowOrHideAllHelp(flag) {
            flag ? helpText.show()  : helpText.hide();
        }

        oldPassword.on("change blur keyup keydown keypress", function() {
            validateOldPassword(oldPassword, oldPasswordHelp);
        });

        newPassword.on("change blur keyup keydown keypress", function() {
            validateNewPassword(newPassword, newPasswordHelp, false);
        });

        newPasswordRpt.on("change blur keyup keydown keypress", function() {
            validateNewPasswordRpt(newPassword, newPasswordRpt, newPasswordRptHelp);
        });

        function validateOldPassword(oldPassword, oldPasswordHelp) {
            const oldPasswordVal = oldPassword.val();
            let retVal;
            if (oldPasswordVal.length === 0) {
                oldPasswordHelp.show();
                oldPasswordHelp.text("Please enter your current password!");
                retVal = false;
            } else {
                oldPasswordHelp.hide();
                retVal = true;
            }
            return retVal;
        }

        function validateForm() {
            console.log("Validating the form of changing password...");
            const oldPassCheck = validateOldPassword(oldPassword, oldPasswordHelp);
            const newPassCheck = validateNewPassword(newPassword, newPasswordHelp, true);
            const newPasswordRptCheck = validateNewPasswordRpt(newPassword, newPasswordRpt, newPasswordRptHelp);
            const retVal = oldPassCheck && newPassCheck && newPasswordRptCheck;
            console.log(retVal);
            return retVal;
        }

    </g:javascript>
</body>
</html>
<content tag="title">
    <g:message code="user.administration.updatePassword.heading"/>
</content>
