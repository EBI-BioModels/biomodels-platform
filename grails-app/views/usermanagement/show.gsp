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
        <title>${titlePage}</title>
        <meta name="layout" content="${session['branding.style']}/main" />
		<g:render template="/templates/initRegistration" plugin="jummp-plugin-web-application" />
		<style>
	#manage2FA {
		justify-content: center;
		align-items: center;
		height: 10vh;
	}
	#manage2FA [type=checkbox] {
		height: 0;
		width: 0;
		visibility: hidden;
	}

	#manage2FA label {
		cursor: pointer;
		text-indent: -9999px;
		width: 80px;
		height: 45px;
		background: grey;
		display: block;
		border-radius: 100px;
		position: relative;
	}

	#manage2FA label:after {
		content: '';
		position: absolute;
		top: 5px;
		left: 5px;
		right: 5px;
		width: 35px;
		height: 35px;
		background: #fff;
		border-radius: 100px;
		transition: 0.3s;
	}

	#manage2FA input:checked + label {
		background: #007c82;
	}

	#manage2FA input:checked + label:after {
		left: calc(100% - 5px) !important;
		transform: translateX(-100%) !important;
	}

	#manage2FA label:active:after {
		width: 130px;
	}
		</style>
     </head>
    <body>
    	<div class="content">
    		<div class="view view-dom-id-9c00a92f557689f996511ded36a88594">
    		<div class="view-content">
        <div class="row">
            <div class="small-6 columns">
            <h2>User information</h2>
			<table class="responsive-table">
				<tbody>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.username"/></label></td>
					<td>${user.username}</td>
				</tr>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.password"/></label></td>
					<td>**********</td>
				</tr>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.realname"/></label></td>
					<td>${user.person.userRealName}</td>
				</tr>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.email"/></label></td>
					<td>${user.email}</td>
				</tr>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.institution"/></label></td>
					<td>${user.person.institution}</td>
				</tr>
				<tr>
					<td class='tableLabels'><label><g:message code="user.administration.ui.orcid"/></label></td>
					<td>${user.person.orcid}</td>
				</tr>
				</tbody>
			</table>

			<div class="row">
				<div class="columns small-12 medium-10 large-10"><h2>Two-Factor Authentication</h2></div>
				<div class="columns small-12 medium-2 large-2">
					<p id="2fa-status" style="font-weight: bold; color: green;">
						<g:if test="${enabled2FA}">
							Enabled
						</g:if>
						<g:else>
							Disabled
						</g:else>
					</p></div>
			</div>
				<div class="row">
				<div id="manage2FA"  class="columns small-12 medium-3 large-3" style="margin-top: -30px">
<g:if test="${enabled2FA}">
	<input type="checkbox" id="switch-2fa" name="btnToggle2FA" checked/>
</g:if>
					<g:else>
	<input type="checkbox" id="switch-2fa" name="btnToggle2FA"/>

					</g:else>
					<label for="switch-2fa">Toggle</label></div>

				<div class="columns small-12 medium-9 large-9">
						<div class="row" id="otp-verification-code-block">
							<div class="columns small-12 medium-3 large-3">
								<label for="txt-otp-verification-code"
									   class="text-right middle" style="margin-top: -15px">
									<a style="cursor: pointer" id="request-confirmation-code"
									   href="${createLink(uri: "/auth/request-new-verification-code")}">Request a
									confirmation
									code</a></label>
							</div>
							<div class="columns small-12 medium-9 large-9">
								<div class="input-group">
									<input type="text" class="input-group-field" id="txt-otp-verification-code"
										   placeholder="Verification code">
									<div class="input-group-button">
										<input type="button" id="btn-confirm" class="button" value="Confirm"/></div></div>
							</div>
						</div>
				</div>


				</div>

            </div>
            <div class="small-6 columns">
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
								<img width="20px" height="auto" title="Receiving notifications on the website"
                                     src="${grailsApplication.config.grails.serverURL}/images/Accept.png"/>
							</g:if>
							<g:else>
								<img width="20px" height="auto" title="Not receiving notifications on the website"
                                     src="${grailsApplication.config.grails.serverURL}/images/close.png"/>
							</g:else>
						</td>
						<td>
							<g:if test="${perm.sendMail}">
								<img width="20px" height="auto" title="Receiving notifications by email"
                                     src="${grailsApplication.config.grails.serverURL}/images/Accept.png"/>
							</g:if>
							<g:else>
								<img width="20px" height="auto" title="Not receiving notifications by email"
                                     src="${grailsApplication.config.grails.serverURL}/images/close.png"/>
							</g:else>
						</td></tr>
					</g:each>
				</tbody>
			</table>
            </div>
            <div class="buttons medium-12 columns">
                <a href='<g:createLink action="edit"/>' class="button">Edit User</a>
                <a href='<g:createLink action="editPassword" />' class="button">Change Password</a>
            </div>
        </div>
        </div>
        </div>
        </div>
<g:javascript>
	const otpVCB = $("#otp-verification-code-block");
	const otpEle = $("#txt-otp-verification-code");
	const switch2FA = $("#switch-2fa");
    const requestCC = $("#request-confirmation-code");
	$(document).ready(function() {
		otpVCB.hide();
	});

	switch2FA.on("click", function() {
		const checked = $(this).is(":checked");
		console.log(checked);
        otpVCB.show();
	});

    requestCC.on("click", function() {

    });

    $("#btn-confirm").on("click", function() {
		const URL = "${createLink(controller: "auth", action: "toggle2FA")}";
		fetch(URL, {
			method: 'POST',
			headers: {
				'Accept': 'application/json; charset=utf-8',
				'Content-Type': 'application/json; charset=utf-8'
			},
			body: JSON.stringify({
				'checked': switch2FA.is(":checked"),
				'username': "${user.username}",
				'otp': otpEle.val()
			})
		}).then(response => {
			if (!response.ok) {
				throw new Error('Network response failed!!!');
			}
			return response.json();
		}).then(data => {
			console.log(data);
            if (data["status"]) {
                showNotification(data["message"]);
            }
		}).catch(error => {
			console.error('Error: ', error);
		});
    });
</g:javascript>
</body>
</html>
<content tag="myprofile">
	${user.person.userRealName}'s Profile
</content>
<content tag="contexthelp">
	profile
</content>
