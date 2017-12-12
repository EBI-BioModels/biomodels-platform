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
        <title>Register</title>
        <style>
        	.verysecure {
        		visibility:hidden;
        	}
        </style>
        <link rel="stylesheet" href="${resource(dir: 'css', file: 'jstree.css')}" />
    </head>
    <body>
        <div id="register" class="row">
            <div class="small-12 medium-6 medium-centered large-4 large-centered columns">
                <g:form name="registerForm" action="signUp" onkeypress="return event.keyCode != 13;">
                    <div class="row column register-form">
                        <label class="required" for="username"><g:message code="user.signup.ui.username"/></label>
                        <g:textField name="username" placeholder="Choose an username" required="true" />

                        <label class="required" for="email"><g:message code="user.signup.ui.email"/></label>
                        <g:textField name="email" placeholder="Enter your email address" required="true" />

                        <label class="required" for="userRealName"><g:message code="user.signup.ui.realname"/></label>
                        <g:textField name="userRealName" placeholder="Enter your real name" required="true" />

                        <label for="institution"><g:message code="user.signup.ui.institution"/></label>
                        <g:textField name="institution" placeholder="Enter an institution name where you are working now"/>

                        <label for="orcid"><g:message code="user.signup.ui.orcid"/></label>
                        <g:textField name="orcid" placeholder="For example, 0000-0002-2876-6046"/>

                        <label class="required" for="captcha"><g:message code="user.signup.ui.captcha"/></label>
                        <img style="margin-top:0;float:none" src="${createLink(controller: 'simpleCaptcha', action: 'captcha')}"/>
                        <br/>
                        <g:textField name="captcha" required="true"/>

                        <p><input type="submit" class="button" value="${g.message(code: 'user.signup.register')}"/>
                        <input type="reset" class="button" id="resetFormButton" value="${g.message(code: 'user.signup.reset')}"/>
                        </p>
                    </div>
                    <label class="verysecure">You shouldn't see me.</label>
                    <input class="verysecure" name="securityfeature" value=""/>
                </g:form>
            </div>
        </div>
        <script type="text/javascript">
            var orcidRegExp = /^\d{4}-\d{4}-\d{4}-\d{3}(?:\d|X)$/gi;
            $("#registerForm #resetFormButton").click(function() {
                $('#registerForm')[0].reset();
            });
            $('#registerForm input').on("change input", function() {
                hideNow();
            });
            $('input[name=username]').blur(function() {
                var username = $(this).val();
                var message = "";
                $.ajax({
                    dataType: "json",
                    cache: false,
                    data: {
                        query: username,
                        column:  1
                    },
                    url: $.jummp.createLink("usermanagement", "lookupUser"),
                    success: function(response) {
                        username = response[0];
                        if (username.trim()) {
                            message = "A user with this username " + username.trim() + " already exists. Please try another one."
                            showNotification(message);
                        }
                    }
                });

            });
            $('input[name=email]').blur(function() {
                var email = $(this).val();
                var message = "";
                $.ajax({
                    dataType: "json",
                    cache: false,
                    data: {
                        query: email,
                        column:  2
                    },
                    url: $.jummp.createLink("usermanagement", "lookupUser"),
                    success: function(response) {
                        message = response[0];
                        if (message.trim()) {
                            message = "A user with this email address " + message.trim() + " already exists in our database. Please use a different one."
                            showNotification(message);
                        }
                    }
                });
            });
            $('input[name=orcid]').change(function() {
                var orcid = $(this).val();
                var message = "";
                if (orcid.match(orcidRegExp)) {
                    // look it up in the database
                    $.ajax({
                        dataType: "json",
                        cache: false,
                        data: {
                            query: orcid,
                            column: 3
                        },
                        url: $.jummp.createLink("usermanagement", "lookupUser"),
                        success: function(response) {
                            message = response[0];
                            if (message.trim()) {
                                message = "Someone with this ORCID is already registered in our database.";
                                showNotification(message);
                            }
                        }
                    });
                } else {
                    message = "<g:message code="net.biomodels.jummp.webapp.RegistrationCommand.orcid.validator.error"/>";
                }
                if (message.trim() !== "") {
                    showNotification(message);
                }
            });
        </script>
    </body>
</html>
<content tag="title">
    <g:message code="user.signup.ui.heading.register"/>
</content>
