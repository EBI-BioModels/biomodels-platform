<%--
 Copyright (C) 2010-2022 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
     </head>
    <body>
    	<div class="content">
    	<div class="view view-dom-id-9c00a92f557689f996511ded36a88594">
    	<div class="view-content">
        <div class="row">
            <div class="small-12 medium-6 medium-centered large-4 large-centered columns">
			<g:form action="editUser">
                <h2>Update user information</h2>
                <g:render template="userInforInput" model="[user: user]"/>
				<h2>Notifications</h2>
				<table class="responsive-table">
					<thead>
						<th>Notification Type</th>
						<th>Web Notification</th>
						<th>Email Notification</th>
					</thead>
					<tbody>
						<g:each status="i" in="${notificationPermissions}" var="perm">
							<tr><td class='tableLabels'><label>${perm.notificationType.toString()}</label></td>
							<td>
								<g:if test="${perm.sendNotification}">
									<input type="checkbox" name="sendNotification${perm.notificationType.id}" checked/>
								</g:if>
								<g:else>
									<input type="checkbox" name="sendNotification${perm.notificationType.id}"/>
								</g:else>
							</td>
							<td>
								<g:if test="${perm.sendMail}">
									<input type="checkbox" name="sendMail${perm.notificationType.id}" checked/>
								</g:if>
								<g:else>
									<input type="checkbox" name="sendMail${perm.notificationType.id}"/>
								</g:else>
							</td></tr>
						</g:each>
					</tbody>
				</table>
				<div class="buttons">
                    <input type="submit" class="button" value="${g.message(code: 'user.administration.edit.save')}"/>
				</div>
			</g:form>
            </div>
        </div>
        </div>
        </div>
        </div>
        <g:javascript>
            // define the current user variables for later usages in common.js
            var currentUsername = "${user.username}";
            var currentEmail = "${user.email}";
            var currentRealName = "${user.person.userRealName}";
            var currentOrcid = "${user.person.orcid}";
            var actionName = "${params.action}";
        </g:javascript>
   </body>
</html>
<content tag="title">
	<g:message code="user.administration.ui.heading.user"/>
</content>
