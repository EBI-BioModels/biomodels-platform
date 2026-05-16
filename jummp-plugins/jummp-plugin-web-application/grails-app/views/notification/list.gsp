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
    <head>
        <meta name="layout" content="${session['branding.style']}/main" />
        <title>My Notifications | BioModels</title>
    </head>
    <body activetab="search">
    	<div class="content">
    		<div class="view view-dom-id-9c00a92f557689f996511ded36a88594">
    		<div class="view-content">
    		<g:if test="${notifications.size() > 0}">
            	<g:if test="${partial && notifications.size() == 10}">
            		<p>You are viewing your ten most recent notifications. To access the complete list, <a href="${createLink(action: "list", params: [all: true])}">click here</a></p>
            	</g:if>
    			<table>
            	<thead>
    	    		<th>Sender</th>
    	    		<th>Title</th>
    	    		<th>Sent</th>
    	    		</thead>
    	    		<tbody>
    	    		<g:each status="i" in="${notifications}" var="msg">
                		<tr id='msgtitle-${msg.notification.id}' class="titleRow ${ (i % 2) == 0 ? 'even' : 'odd'}">
                			<td>${msg.notification.sender.person.userRealName}</td>
                			<g:if test="${msg.notificationSeen}">
                				<td class="msgTitle" data-msgid="${msg.notification.id}" data-seen='${msg.notificationSeen}'>
                			</g:if>
                			<g:else>
                				<td class="msgTitle unseenNotification" data-msgid="${msg.notification.id}" data-seen='${msg.notificationSeen}'>
                			</g:else>
               					${msg.notification.title}
               				</td>
               				<td>${msg.notification.dateCreated}</td>
               				<tr id='msgbody-${msg.notification.id}' style="display: none;" class="msgbody ${ (i % 2) == 0 ? 'even' : 'odd'}">
                				<td colspan="3">
                					${msg.notification.body}
                				</td>
                			</tr>
                		</tr>
                	</g:each>
              </tbody>
            </table>
            </g:if>
            <g:else>
               	You have received no notifications.
            </g:else>
         </div>
          </div>
         </div>
         <g:javascript>
         	$(function() {
         		$(".msgTitle").click(function() {
         			$(".msgbody").hide();
         			$(".msgbody").removeClass("selectedNotification");
         			$(".titleRow" ).removeClass("selectedNotification");
         			var showThis = $(this).data("msgid");
         			$("#msgbody-"+showThis).show();
         			$("#msgtitle-"+showThis).addClass("selectedNotification");
         			$("#msgbody-"+showThis).addClass("selectedNotification");
         			var seen = $(this).data("seen");
         			if (!seen) {
         				$( this ).removeClass( "unseenNotification" )
         				markAsRead('<g:createLink controller="notification" action="markAsRead"/>'+"?msg="+showThis,'<g:createLink controller="notification" action="unreadNotificationCount"/>');

         			}
         		});
         	});
         </g:javascript>
    </body>
    <content tag="mynotifications">
        selected
    </content>
    <content tag="title">
        <g:message code="user.notifications.heading"/>
    </content>
    <content tag="contexthelp">
       	notifications
    </content>
