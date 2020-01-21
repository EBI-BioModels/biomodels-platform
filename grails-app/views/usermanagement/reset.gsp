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
        <title>Reset Password</title>
        <style>
        	.verysecure {
        		visibility:hidden;
        	}
        </style>
        <link rel="stylesheet" href="${resource(dir: 'css', file: 'jstree.css')}" />
    </head>
     <body>
        <div class="row">
            <div class="small-12 medium-6 medium-centered large-4 large-centered columns">
            <h3 class="text-center">Reset your password</h3>
            <g:form name="resetForm" action="newPassword" class="log-in-form" useToken="true">
                <label for="username">
                    <span class="required"><g:message code="user.signup.ui.username"/></span>
                    <g:textField name="username"/>
                </label>
                <label for="newPassword">
                    <span class="required"><g:message code="user.administration.updatePassword.newPassword"/></span>
                    <g:passwordField name="newPassword"/>
                </label>
                <label for="newPasswordRpt">
                    <span class="required"><g:message code="user.administration.updatePassword.newPasswordRpt"/></span>
                    <g:passwordField name="newPasswordRpt"/>
                </label>
                <div class="buttons">
                    <input type="submit" class="button" value="Reset Password"/>
                </div>
                <input class="verysecure" name="hashCode" value="${hashCode}"/>
            </g:form>
            </div>
        </div>
        </body>
</html>
<content tag="title">
	<g:message code="user.resetpassword.ui.heading"/>
</content>
