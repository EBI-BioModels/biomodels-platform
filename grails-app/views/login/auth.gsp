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
        <title>Login</title>
    </head>
    <body>
        <div id="login" class="row">
            <div class="small-12 medium-6 medium-centered large-4 large-centered columns">
                <form action='${postUrl}' method='POST' id='loginForm' class='cssform' autocomplete='on'>
                    <div class="row column log-in-form">
                        <p>${flash.message}</p>
                        <h3 class="text-center">Log in to your account</h3>
                        <label><g:message code="login.form.label"/>
                            <input type='text' name='j_username' id='username' placeholder="Username">
                        </label>
                        <label><g:message code="login.form.password"/>
                            <input type='password' name='j_password' id='password' placeholder="Password"/>
                        </label>
                        %{--<input id="show-password" type="checkbox"><label for="show-password">Show password</label>--}%
                        <p><button type="submit" class="button expanded">Log In</button></p>
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
        <script type='text/javascript'>
            // TODO: move out of HTML page
            $("#loginForm input").focus(function() {
                if ($(this).data("reset") === undefined) {
                $(this).val("");
                $(this).data("reset", true);
                }
            });
            $("#loginForm input").keyup(function(event) {
            // magic value 13 is enter
            if (event.which == 13) {
                $("#loginForm").submit();
                }
            });
            $("#login div.loginButton button").click(function() {
                $("#loginForm").submit();
            });
        </script>
    </body>
</html>
<content tag="title">
    Login
</content>
<content tag="contexthelp">
    login
</content>
