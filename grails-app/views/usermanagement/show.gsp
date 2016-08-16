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
        <link rel="stylesheet" href="${resource(dir: 'css', file: 'jstree.css')}" /> 
     </head>
    <body>
    	<div class="content">
    		<div class="view view-dom-id-9c00a92f557689f996511ded36a88594">
    		<div class="view-content">
        <div>
			<table>
				<thead></thead>
				<tbody>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.username"/>:</label></td>
					<td>${user.username}</td>
				</tr>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.realname"/>:</label></td>
					<td>${user.person.userRealName}</td>
				</tr>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.email"/>:</label></td>
					<td>${user.email}</td>
				</tr>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.institution"/>:</label></td>
					<td>${user.person.institution}</td>
				</tr>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.orcid"/>:</label></td>
					<td>${user.person.orcid}</td>
				</tr>
				</tbody>
			</table>
			<h2>Notifications</h2>
			<table>
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
								<img width="20px" height="auto" title="Receiving notifications on the website" src="${grailsApplication.config.grails.serverURL}/images/Accept.png"/>
							</g:if>
							<g:else>
								<img width="20px" height="auto" title="Not receiving notifications on the website" src="${grailsApplication.config.grails.serverURL}/images/close.png"/>
							</g:else>
						</td>
						<td>
							<g:if test="${perm.sendMail}">
								<img width="20px" height="auto" title="Receiving notifications by email" src="${grailsApplication.config.grails.serverURL}/images/Accept.png"/>
							</g:if>
							<g:else>
								<img width="20px" height="auto" title="Not receiving notifications by email" src="${grailsApplication.config.grails.serverURL}/images/close.png"/>
							</g:else>
						</td></tr>
					</g:each>
				</tbody>
			</table>
			<ul id="optionsList">
				<li><a href='<g:createLink action="edit"/>'>Edit User</a></li>
				<li><a href='<g:createLink action="editPassword" />'>Change Password</a></li>
			</ul>
        </div>
        </div>
        </div>
        </div>
   </body>
</html>
<content tag="title">
	${user.person.userRealName}'s Profile
</content>
<content tag="contexthelp">
	profile
</content>
