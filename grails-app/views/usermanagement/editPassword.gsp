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




<%@ page import="net.biomodels.jummp.utils.MathUtils" contentType="text/html;charset=UTF-8" %>
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
                                     placeholder="Retype new password"/>
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

        $(document).ready(function(){
            doShowOrHideAllHelp(false);
        });

        function doShowOrHideAllHelp(flag) {
            flag ? helpText.show()  : helpText.hide();
        }

        oldPassword.on("change blur keyup keydown keypress", function() {
            validateOldPassword();
        });

        newPassword.on("change blur keyup keydown keypress", function() {
            validateNewPassword();
        });

        newPasswordRpt.on("change blur keyup keydown keypress", function() {
            validateNewPasswordRpt();
        });

        function validateOldPassword() {
            const oldPasswordVal = oldPassword.val();
            let retVal;
            const oldPasswordHelp = $("#old-password-help");
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

        function validateNewPassword(showWarning = false) {
            const newPasswordVal = newPassword.val();
           // the function below was defined in the common-script template in jummp-plugin-web-app
            verifyCompromisedPassword(newPasswordVal);
            let s = checkPasswordStrength(newPasswordVal);
            let retVal = s.tips.length === 0;
            const newPasswordHelp = $("#new-password-help");
            let colourCode;
            switch (s.strengthLevel) {
                case "${MathUtils.PWD_HARD_LEVEL.EASY.label}":
                    colourCode = "red";
                    if (showWarning) {
                        toastr.error(s.strengthLevel);
                    }
                    break;
                case "${MathUtils.PWD_HARD_LEVEL.MEDIUM.label}":
                    colourCode = "orange";
                    if (showWarning) {
                        toastr.warn(s.strengthLevel);
                    }
                    break;
                case "${MathUtils.PWD_HARD_LEVEL.HARD.label}":
                    colourCode = "cornflowerblue";
                    if (showWarning) {
                        toastr.info(s.strengthLevel);
                    }
                    break;
                case "${MathUtils.PWD_HARD_LEVEL.X_HARD.label}":
                    colourCode = "green";
                    if (showWarning) {
                        toastr.success(s.strengthLevel);
                    }
                    break;
            }
            let msg = '<span style="color: ' + colourCode + '">' + s.strengthLevel + '</span>';
            if (!retVal) {
                msg += "<br/>" + s.tips.join("<br/>");
            }
            newPasswordHelp.show();
            newPasswordHelp.html(msg);

            return retVal;
        }

        function validateNewPasswordRpt() {
            const newPasswordRptVal = newPasswordRpt.val();
            const newPasswordVal = newPassword.val();
            const newPasswordRptHelp = $("#new-password-rpt-help");
            let retVal;
            if (newPasswordRptVal.length === 0) {
                retVal = false;
                newPasswordRptHelp.show();
                newPasswordRptHelp.html("Please retype your new password!");
            } else if (newPasswordVal !== newPasswordRptVal) {
                retVal = false;
                newPasswordRptHelp.show();
                newPasswordRptHelp.html("New password does not match!");
            } else {
                retVal = true;
                newPasswordRptHelp.hide();
            }
            return retVal;
        }

        function validateForm() {
            console.log("Validating the form of changing password...");
            const oldPassCheck = validateOldPassword();
            const newPassCheck = validateNewPassword(true);
            const newPasswordRptCheck = validateNewPasswordRpt();
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
