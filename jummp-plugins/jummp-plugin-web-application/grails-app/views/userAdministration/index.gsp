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
        <title>User Administration</title>
        <meta name="layout" content="main" />
        <g:javascript contextPath="" src="useradministration.js"/>
        <g:javascript contextPath="" src="jquery/jquery.dataTables.min.js"/>
        <link rel="stylesheet" href="${resource(contextPath: "${grailsApplication.config.grails.serverURL}", dir: '/css', file: 'jquery.dataTables.min.css')}" />
    </head>
    <body>
    
    	<div class="content">
    		  <div class="view view-dom-id-9c00a92f557689f996511ded36a88594">
    		  <a style="float:right;margin-top:5px" title="Add new user" href="${g.createLink(action: "register")}">
	  	     		<span>
	  	     			Add new user
    		  		<img style="width:20px;float:none;margin-left:5px" src="http://www.ebi.ac.uk/web_guidelines/images/icons/EBI-Functional/Functional%20icons/user_add.png"/>
    		  		</span>
	  	      </a>
        <div class="view-content">
        <table id="userTable" class="views-table cols-4">
            <thead>
            <tr>
                <th><g:message code="user.administration.list.id"/></th>
                <th><g:message code="user.administration.list.username"/></th>
                <th><g:message code="user.administration.list.realname"/></th>
                <th><g:message code="user.administration.list.email"/></th>
                <th><g:message code="user.administration.list.institution"/></th>
                <th><g:message code="user.administration.list.orcid"/></th>
                <th><g:message code="user.administration.list.enabled"/></th>
                <th><g:message code="user.administration.list.accountExpired"/></th>
                <th><g:message code="user.administration.list.accountLocked"/></th>
                <th><g:message code="user.administration.list.passwordExpired"/></th>
            </tr>
            </thead>
            <tbody></tbody>
            <tfoot>
            <tr>
                <th><g:message code="user.administration.list.id"/></th>
                <th><g:message code="user.administration.list.username"/></th>
                <th><g:message code="user.administration.list.realname"/></th>
                <th><g:message code="user.administration.list.email"/></th>
                <th><g:message code="user.administration.list.institution"/></th>
                <th><g:message code="user.administration.list.orcid"/></th>
                <th><g:message code="user.administration.list.enabled"/></th>
                <th><g:message code="user.administration.list.accountExpired"/></th>
                <th><g:message code="user.administration.list.accountLocked"/></th>
                <th><g:message code="user.administration.list.passwordExpired"/></th>
            </tr>
            </tfoot>
        </table>
        </div>
        </div>
        </div>
        <g:javascript>
            $(function() {
                $.jummp.userAdministration.loadUserList();
            });
        </g:javascript>
    </body>
</html>
<content tag="title">
    User Administration
</content>
