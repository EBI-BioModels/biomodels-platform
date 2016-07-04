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
        <div>
            <g:form name="registerForm" action="signUp">
                <table>
                    <tbody>
                    <tr>
                        <td class='tableLabels'><label class="required" for="register-form-username"><g:message code="user.signup.ui.username"/>:</label></td>
                        <td><span><g:textField name="username"/></span></td>
                    </tr>
                    <tr>
                        <td class='tableLabels'><label class="required" for="register-form-name"><g:message code="user.signup.ui.realname"/>:</label></td>
                        <td><span><g:textField name="userRealName"/></span></td>
                    </tr>
                    <tr>
                        <td class='tableLabels'><label class="required" for="register-form-email"><g:message code="user.signup.ui.email"/>:</label></td>
                        <td><span><g:textField name="email"/></span></td>
                    </tr>
                    <tr>
                        <td class='tableLabels'><label for="register-form-institution"><g:message code="user.signup.ui.institution"/>:</label></td>
                        <td><span><g:textField name="institution"/></span></td>
                    </tr>
                    <tr>
                        <td class='tableLabels'><label for="register-form-orcid"><g:message code="user.signup.ui.orcid"/>:</label></td>
                        <td><span><g:textField name="orcid"/></span></td>
                    </tr>
                    <tr>
                        <td class='tableLabels'><label class="required" for="register-form-captcha"><g:message code="user.signup.ui.captcha"/>:</label></td>
                        <td>
                            <img style="margin-top:0;float:none" src="${createLink(controller: 'simpleCaptcha', action: 'captcha')}"/>
                            <br/>
                            <g:textField name="captcha"/>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <div class="buttons">
                    <input type="submit" value="${g.message(code: 'user.signup.register')}"/>
                </div>
                <label class="verysecure">You shouldnt see me</label>
                <input class="verysecure" name="securityfeature" value=""/>
                 
            </g:form>
        </div>
        </body>
</html>
<content tag="title">
	<g:message code="user.signup.ui.heading.register"/>
</content>
