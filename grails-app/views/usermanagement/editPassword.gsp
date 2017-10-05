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




<%@ page contentType="text/html;charset=UTF-8" %>
<html>
    <head>
        <title><g:message code="user.administration.ui.heading.user"/></title>
        <meta name="layout" content="${session['branding.style']}/main" />
        <link rel="stylesheet" href="${resource(dir: 'css', file: 'jstree.css')}" />
     </head>
    <body>
        <div class="row">
            <div class="small-12 medium-6 medium-centered large-4 large-centered columns">
                <h3>Change your password</h3>
                <g:form action="updatePassword">
                    <div class="row column edit-password-form">
                        <label class="required"><g:message code="user.administration.updatePassword.oldPassword"/></label>
                        <g:passwordField name="oldPassword"/>

                        <label class="required"><g:message code="user.administration.updatePassword.newPassword"/></label>
                        <g:passwordField name="newPassword"/>

                        <label class="required"><g:message code="user.administration.updatePassword.newPasswordRpt"/></label>
                        <g:passwordField name="newPasswordRpt"/>
                        <p class="buttons">
                            <input type="submit" class="button" value="${g.message(code: 'user.administration.updatePassword.submit')}"/>
                        </p>
                    </div>
                </g:form>
            </div>
        </div>
   </body>
</html>
<content tag="title">
    <g:message code="user.administration.updatePassword.heading"/>
</content>
