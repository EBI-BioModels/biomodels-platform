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
        <title>Login | BioModels</title>
        <g:render template="/usermanagement/head"/>
    </head>
    <body>
        <g:render template="/templates/initRegistration"
                  plugin="jummp-plugin-web-application" />
        <div id="login" class="row">
            <div class="small-12 medium-6 medium-centered large-4 large-centered columns">
                <form action="${postUrl}" method="post" id="loginForm" class="cssform" autocomplete="on">
                    <div class="row column log-in-form">
                        <h3 class="text-center">Log in to your account</h3>
                        <label><g:message code="login.form.label"/>
                            <input type='text' name='j_username' id='username' placeholder="Username">
                        </label>
                        <label><g:message code="login.form.password"/>
                            <input type='password' name='j_password' id='password' placeholder="Password" autocomplete="on"/>
                            <span id="toggle-password"
                                  class="fa fa-fw fa-eye field-icon toggle-password"></span>
                        </label>
                        <label for='j_previousURL'>
                            <input type='text' name='j_previousURL' id='j_previousURL'  value="${j_previousURL}"
                                   style="display: none"/></label>
                        <label for="j_deviceInfo">
                            <input type='text' name='j_deviceInfo' id='j_deviceInfo' value=""
                                   style="display: none"/>
                        </label>
                        <p><button type="button" class="button expanded" id="btnLogIn">Log In</button></p>
                        <p class="text-center">
                            <a href="${grailsApplication.config.grails.serverURL}/forgotpassword">Forgot your password?</a></p>
                        <g:if test="${grailsApplication.config.jummp.security.anonymousRegistration}">
                        <p class="text-center">
                            <a href="${grailsApplication.config.grails.serverURL}/registration">Register</a></p>
                        </g:if>
                    </div>
                </form>

            </div>
        </div>

        <g:render template="/usermanagement/common-scripts"/>

        <script type='text/javascript'>
            const loginInput = $("#loginForm input");
            const loginForm = $("#loginForm");
            const loginUsername = $("#username");
            const loginPassword = $("#password");
            loginInput.focus(function() {
                if ($(this).data("reset") === undefined) {
                $(this).val("");
                $(this).data("reset", true);
                }
            });
            function submitLogin() {
                const deviceInfo = $("#j_deviceInfo");
                $(deviceInfo).val(localStorage.getItem(loginUsername.val()));
                loginForm.submit();
                checkCompromisedPasswordOnServerSide(loginUsername.val(), loginPassword.val());
            }
            loginInput.on("keyup", function(event) {
                // magic value 13 is entered
                if (event.which === 13) {
                    submitLogin();
                }
            });
            const loginSubmit = $("#btnLogIn");
            loginSubmit.on("click", function() {
                submitLogin();
            });
            loginPassword.on("focus", function() {
                clearNotification();
                hideNow();
            });
            loginPassword.on("blur", function() {
                // the function below was defined in the common-script.gsp template
                verifyCompromisedPassword($(this).val());
            });
        </script>
    <g:render template="/usermanagement/foot"/>
    </body>
</html>
<content tag="login">
    selected
</content>
<content tag="title">
    Login
</content>
<content tag="contexthelp">
    login
</content>
