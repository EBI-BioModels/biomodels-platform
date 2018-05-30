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
        <meta name="layout" content="${session['branding.style']}/main" />
        <g:javascript contextPath="" src="useradministration.js"/>
    </head>
    <body>
        <div id="userAdministrationRegister" class="row">
            <div class="medium-6 medium-centered large-6 large-centered columns">
            <form id="registerForm">
                <div class="row column register-form">
                    <label for="register-form-username" class="required">
                        <g:message code="user.administration.ui.username"/></label>
                    <input type="text" id="register-form-username" name="username"
                           placeholder="Choose an username" />

                    <label for="register-form-name" class="required">
                        <g:message code="user.administration.ui.realname"/></label>
                    <input type="text" id="register-form-name" name="email"
                           placeholder="Enter your real name"/>

                    <label for="register-form-email" class="required">
                        <g:message code="user.administration.ui.email"/></label>
                    <input type="text" id="register-form-email" name="email"
                           placeholder="Enter your email address"/>

                    <label for="register-form-institution" class="label-floating-left">
                        <g:message code="user.administration.ui.institution"/></label>
                    <input type="text" id="register-form-institution" name="institution"
                           placeholder="Enter an institution name where you are working with"/>

                    <label for="register-form-orcid"  class="label-floating-left">
                        <g:message code="user.administration.ui.orcid"/></label>
                    <input type="text" id="register-form-orcid" name="orcid"
                           placeholder="For example, 0000-0002-2876-6046"/>
                    <p>
                    <input type="reset" class="button" value="${g.message(code: 'user.administration.cancel')}"/>
                    <input type="submit" class="button" value="${g.message(code: 'user.administration.register')}"/></p>
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
