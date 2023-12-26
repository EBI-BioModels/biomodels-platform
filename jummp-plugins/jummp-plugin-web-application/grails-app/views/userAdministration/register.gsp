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
        <title><g:message code="user.administration.ui.heading.register"/></title>
        <meta name="layout" content="${theme}/main" />
        <g:render template="/templates/head" plugin="jummp-plugin-web-application" />
        <g:javascript contextPath="" src="useradministration.js"/>
    </head>
    <body>
        <g:render template="/templates/initRegistration" plugin="jummp-plugin-web-application" />
        <div id="userAdministrationRegister" class="row">
            <div class="medium-6 medium-centered large-6 large-centered columns">
            <form id="registerForm">
                <div class="row column register-form">
                    <g:render template="/templates/newAccountRegistrationForm"
                              plugin="jummp-plugin-web-application" model="[user: null]" />
                    <input type="reset" class="button" value="${g.message(code: 'user.administration.cancel')}"/>
                    <input type="submit" class="button" value="${g.message(code: 'user.administration.register')}"/>
                </div>
            </form>
            </div>
        </div>
        <g:javascript>
            $(function() {
                $.jummp.userAdministration.register();
            });
        </g:javascript>
    </body>
</html>
<content tag="title">
	<g:message code="user.administration.ui.heading.register"/>
</content>
