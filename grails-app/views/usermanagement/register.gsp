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
        <meta name="layout" content="main"/>
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
            <div class="medium-6 medium-centered large-6 large-centered columns">
                <g:form name="registerForm" action="signUp">
                    <div class="row column register-form">
                        <label class="required" for="username"><g:message code="user.signup.ui.username"/></label>
                        <g:textField name="username" placeholder="Choose an username"/>

                        <label class="required" for="email"><g:message code="user.signup.ui.email"/></label>
                        <g:textField name="email" placeholder="Enter your email address"/>

                        <label class="required" for="userRealName"><g:message code="user.signup.ui.realname"/></label>
                        <g:textField name="userRealName" placeholder="Enter your real name"/>

                        <label for="institution"><g:message code="user.signup.ui.institution"/></label>
                        <g:textField name="institution" placeholder="Enter an institution name where you are working now"/>

                        <label for="orcid"><g:message code="user.signup.ui.orcid"/></label>
                        <g:textField name="orcid" placeholder="For example, 0000-0002-2876-6046"/>

                        <label class="required" for="captcha"><g:message code="user.signup.ui.captcha"/></label>
                        <img style="margin-top:0;float:none" src="${createLink(controller: 'simpleCaptcha', action: 'captcha')}"/>
                        <br/>
                        <g:textField name="captcha"/>

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
            $("#registerForm #resetFormButton").click(function() {
                $('#registerForm')[0].reset();
            });
        </script>
    </body>
</html>
<content tag="title">
    <g:message code="user.signup.ui.heading.register"/>
</content>
