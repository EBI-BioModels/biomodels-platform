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
        <meta name="layout" content="main" />
        <g:javascript contextPath="" src="useradministration.js"/>
     </head>
    <body>
        <div>
        <form id="edit-user-form" method="POST">
            <table>
                <thead></thead>
                <tbody>
                <tr>
                    <td><label for="edit-user-username"><g:message code="user.administration.ui.username"/>:</label></td>
                    <td><input type="hidden" id="edit-user-username" name="username" value="${user.username}"/>${user.username}</td>
                </tr>
                <tr>
                    <td><label for="edit-user-userrealname"><g:message code="user.administration.ui.realname"/>:</label></td>
                    <td><span><input type="text" id="edit-user-userrealname" name="userRealName" value="${user.person.userRealName}"/></span></td>
                </tr>
                <tr>
                    <td><label for="edit-user-email"><g:message code="user.administration.ui.email"/>:</label></td>
                    <td><span><input type="text" id="edit-user-email" name="email" value="${user.email}"/></span></td>
                </tr>
                <tr>
                    <td><label for="edit-user-institution"><g:message code="user.administration.ui.institution"/>:</label></td>
                    <td><span><input type="text" id="edit-user-institution" name="institution" value="${user.person.institution}"/></span></td>
                </tr>
                <tr>
                    <td><label for="edit-user-orcid"><g:message code="user.administration.ui.orcid"/>:</label></td>
                    <td><span><input type="text" id="edit-user-orcid" name="orcid" value="${user.person.orcid}"/></span></td>
                </tr>
                </tbody>
            </table>
            <div class="buttons">
                <input type="reset" value="${g.message(code: 'user.administration.cancel')}"/>
                <input type="submit" value="${g.message(code: 'user.administration.save')}"/>
            </div>
        </form>
        </div>
        <div id="user-role-management">
            <h1><g:message code="user.administration.userRole.ui.heading" args="[user.username]"/></h1>
            <div id="userRoles">
                <h3><g:message code="user.administration.userRole.ui.heading.usersRoles"/></h3>
                <input type="hidden" value="${user.id}"/>
                <input type="hidden" value="removeRole"/>
                <table>
                    <tbody>
                    <g:each var="role" in="${userRoles}">
                        <tr><td>${role.authority}</td><td><input type="hidden" value="${role.id}"/><a href="#" rel="#userRoles-${role.id}"><g:message code="user.administration.userRole.ui.removeRole"/></a></td></tr>
                    </g:each>
                    </tbody>
                </table>
            </div>
            <div id="availableRoles">
                <h3><g:message code="user.administration.userRole.ui.heading.availableRoles"/></h3>
                <input type="hidden" value="${user.id}"/>
                <input type="hidden" value="addRole"/>
                <table>
                    <tbody>
            <%
                for (def role in roles) {
                    if (userRoles.find { it.id == role.id }) {
                        continue
                    }
            %>
                    <tr><td>${role.authority}</td><td><input type="hidden" value="${role.id}"/><a href="#" rel="#availableRoles-${role.id}"><g:message code="user.administration.userRole.ui.addRole"/></a></td></tr>
            <%
                }
            %>
                    </tbody>
                </table>
            </div>
        </div>
        <g:javascript>
            $(function() {
                $.jummp.userAdministration.editUser();
            });
        </g:javascript>
    </body>
</html>
<content tag="title">
	<g:message code="user.administration.ui.heading.user"/>
</content>
