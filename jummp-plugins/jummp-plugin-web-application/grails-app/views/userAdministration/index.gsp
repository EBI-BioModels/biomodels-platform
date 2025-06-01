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
        <meta name="layout" content="${theme}/main" />
        <g:javascript contextPath="" src="useradministration.js"/>
        <g:javascript contextPath="" src="jquery/jquery.dataTables.min.js"/>
        <g:javascript contextPath="" src="jquery/dataTables.responsive.min.js"/>
        <link rel="stylesheet"
              href="${resource(contextPath: "${serverURL}",
                  dir: '/css', file: 'jquery.dataTables.min.css')}" />
    </head>
    <body>
        <div class="content">
            <div class="view view-dom-id-9c00a92f557689f996511ded36a88594">
                <a style="float:right;margin-top:5px" title="Add new user" href="${g.createLink(action: "register")}">
                    <span>
                        Add new user
                        <img style="width:20px;float:none;margin-left:5px"
                             src="${serverURL}/images/user_add.png" alt="Add a new user"/>
                    </span>
                </a>
                <div class="view-content row">
                    <table id="userTable" class="display responsive nowrap table" style="width: 100%">
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
                /*
                select { width: 100% } defined in ebi-global.css breaks the label
                and the number are on different line. Removing this attribute after
                loading the entire data table resolved the issue.
                */
                const ele = $('select[name="userTable_length"]');
                ele.css('width', 'auto');
            });
        </g:javascript>
    </body>
</html>
<content tag="title">
    User Administration
</content>
