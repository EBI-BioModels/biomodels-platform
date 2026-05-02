<%--
 Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
        <title>${title}</title>
        <style>
        	.very-secure {
        		visibility:hidden;
        	}
        </style>
    <g:render template="head"/>
    </head>
    <body>
        <g:render template="/templates/initRegistration" plugin="jummp-plugin-web-application" />
        <div id="register" class="row">
            <div class="small-12 medium-8 medium-centered large-6 large-centered columns">
                <g:form name="registerForm" action="signUp" onkeypress="return event.keyCode !== 13;" useToken="true"
                        onsubmit="return validateForm()" >
                    <div class="row column register-form">
                        <g:render template="/templates/newAccountRegistrationForm"
                                  plugin="jummp-plugin-web-application" model="[user: null]" />
                        <label class="required" for="captcha"><g:message code="user.signup.ui.captcha"/></label>
                        <img style="margin-top:0;float:none" src="${createLink(controller: 'simpleCaptcha', action: 'captcha')}"/>
                        <br/>
                        <g:textField name="captcha" required="true"/>

                        <p><input type="submit" class="button" value="${g.message(code: 'user.signup.register')}"/>
                        <input type="reset" class="button" id="resetFormButton" value="${g.message(code: 'user.signup.reset')}"/>
                        </p>
                    </div>
                    <label class="very-secure">You shouldn't see me.</label>
                    <label>
                        <input class="very-secure" name="securityfeature" value=""/>
                    </label>
                </g:form>
            </div>
        </div>
<g:javascript>
    /* global toastr, validateAllowedCharacters */
    const usernameEle = $("#username");
    const emailEle    = $("#email");
    const realNameEle = $("#userRealName");

    function validateEmail(val) {
        if (!val) return "Email address is required.";
        if (!/^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$/.test(val)) {
            return "Please enter a valid email address.";
        }
        return "";
    }

    emailEle.on("blur", function() {
        const msg = validateEmail($(this).val().trim());
        if (msg) toastr.error(msg);
    });

    realNameEle.on("blur", function() {
        if (!$(this).val().trim()) toastr.error("Real name is required.");
    });

    function validateForm() {
        let valid = true;

        const usernameMsg = validateAllowedCharacters(usernameEle.val().trim());
        if (usernameMsg) {
            toastr.error(usernameMsg);
            valid = false;
        }

        const emailMsg = validateEmail(emailEle.val().trim());
        if (emailMsg) {
            toastr.error(emailMsg);
            valid = false;
        }

        if (!realNameEle.val().trim()) {
            toastr.error("Real name is required.");
            valid = false;
        }

        return valid;
    }
</g:javascript>
    </body>
</html>
<content tag="register">
    selected
</content>
<content tag="title">
    <g:message code="user.signup.ui.heading.register"/>
</content>
