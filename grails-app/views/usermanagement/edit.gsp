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
<%@ page import="grails.converters.JSON;" %>
<html>
    <head>
        <title><g:message code="user.administration.ui.heading.user"/></title>
        <meta name="layout" content="${session['branding.style']}/main" />
     </head>
    <body>
    	<div class="content">
    	<div class="view view-dom-id-9c00a92f557689f996511ded36a88594">
    	<div class="view-content">
        <form action="editUser" id="edit-user-form">
        <div class="row">
            <div class="small-12 medium-6 large-6 columns">
                <h2>Update user information</h2>
                <g:render template="userInforInput" model="[user: user]"/>
            </div>
            <div class="small-12 medium-6 large-6 columns">
                <h2>Notifications</h2>
				<table class="responsive-table">
					<thead>
						<th>Notification Type</th>
						<th>Web Notification</th>
						<th>Email Notification</th>
					</thead>
					<tbody>
						<g:each status="i" in="${notificationPermissions}" var="perm">
							<tr><td class='tableLabels'><label>${perm.notificationType.text()}</label></td>
							<td>
								<g:if test="${perm.sendNotification}">
									<input type="checkbox" id="notify-${perm.notificationType.slug()}"
                                           name="sendNotification${perm.notificationType.id}" checked/>
								</g:if>
								<g:else>
									<input type="checkbox" id="notify-${perm.notificationType.slug()}"
                                           name="sendNotification${perm.notificationType.id}"/>
								</g:else>
							</td>
							<td>
								<g:if test="${perm.sendMail}">
									<input type="checkbox" id="email-${perm.notificationType.slug()}"
                                           name="sendMail${perm.notificationType.id}" checked/>
								</g:if>
								<g:else>
									<input type="checkbox" id="email-${perm.notificationType.slug()}"
                                           name="sendMail${perm.notificationType.id}"/>
								</g:else>
							</td></tr>
						</g:each>
                        <input type="text" name="options" id="options" placeholder="store all options" style="display: inline"/>
					</tbody>
				</table>
            </div>
        </div>
        <div class="row">
            <div class="columns">
            <div class="buttons">
                <input type="button" id="btn-save" class="button" value="${g.message(code: 'user.administration.edit.save')}"/>
            </div></div>
        </div>
        </form>
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
            let options = {};
            if (${notificationPermissions != null}) {
                options = ${notificationPermissions.collect {
                    ["id": it.notificationType.id, "slug": it.notificationType.slug(),
                     "text": it.notificationType.text(), "notify": 1, "email": 0]
                } as JSON }
            }

            console.log("options are loaded from DB: ", options);

            $('#btn-save').on("click", function() {
                jQuery.each(options, (index, opt) => {
                    console.log(opt);
                    const chkNotify = $("#notify-"+opt["slug"]).is(":checked");
                    const chkEmail = $("#email-"+opt["slug"]).is(":checked");
                    console.log(chkNotify, chkEmail);
                    options[index]['notify'] = chkNotify ? 1 : 0;
                    options[index]['email'] = chkEmail ? 1 : 0;
                });
                console.log("options are updated from Web: ", options);
                const URL = "${serverURL}/user/update";
                const data = {
                    "username": $('#username').val(),
                    "userRealName": $("#userRealName").val(),
                    "email": $("#email").val(),
                    "institution": $("#institution").val(),
                    "orcid": $("#orcid").val(),
                    "options": JSON.stringify(options)
                }
                fetch(URL, {
                    method: "POST",
                    headers: {
                        "Accept": "application/json; charset=utf-8",
                        "Content-Type": "application/json; charset=utf-8"
                    },
                    body: JSON.stringify(data)
                }).then((response) => {
                    if (!response.ok) {
                        throw new Error('Network response failed or Internal Server Error!!!');
                    }
                    return response.json();
                }).then((data) => {
                    // console.log(data);
                    if (data) {
                        const flashDiv = $(".flashNotificationDiv");
                        flashDiv.html(data["message"]);
                        flashDiv.show();
                    }
                }).catch(error => {
                    console.error(error);
                    const flashDiv = $(".flashNotificationDiv");
                    flashDiv.html(error);
                    flashDiv.show();
                });
            });
        </g:javascript>
   </body>
</html>
<content tag="title">
	<g:message code="user.administration.ui.heading.user"/>
</content>
