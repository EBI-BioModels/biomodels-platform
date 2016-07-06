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











<%@ page import="net.biomodels.jummp.plugins.configuration.DatabaseCommand" %>


<div class="dialog">
    <table class="formtable">
        <tbody>
            <tr class="prop">
                <td class="name" style="width: 25%;"><label for="databaseType">DB Server Type</label></td>
                <td class="value ${hasErrors(bean: database, field: 'type', 'errors')}">
                    <g:select id="type.key" name='databaseType' value='${database?.type?.key}'
    noSelection="${['null':'Select One...']}"
    from='${net.biomodels.jummp.plugins.configuration.DatabaseType?.list()}'
    optionKey="key" optionValue="value" style="width: 250px;"></g:select>

                </td>
            </tr>
            <tr class="prop">
                <td class="name" width="25%"><label for="databaseServer">Server (e.g. localhost):</label></td>
                <td class="value ${hasErrors(bean: database, field: 'server', 'errors')}">
                    <input type="text" name="server" id="databaseServer"
                           value="${database ? database.server : 'localhost'}"
                           style="width: 350px;"
                    />
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="databasePort">Port (e.g. 3306):</label></td>
                <td class="value ${hasErrors(bean: database, field: 'port', 'errors')}">
                    <input type="text" name="port" id="databasePort"
                           value="${database ? database.port : '3306'}"
                           style="width: 100px;"
                    />
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="databaseName">Database (name of the database):</label></td>
                <td class="value ${hasErrors(bean: database, field: 'database', 'errors')}">
                    <input type="text" name="database" id="DatabaseName"
                           value="${database?.database}" style="width: 350px;"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="databaseUsername">Username:</label></td>
                <td class="value ${hasErrors(bean: database, field: 'username', 'errors')}">
                    <input type="text" name="username" id="databaseUsername"
                           value="${database?.username}" style="width: 350px;"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="databasePassword">Password:</label></td>
                <td class="value ${hasErrors(bean: mysql, field: 'password', 'errors')}">
                    <input type="password" name="password" id="databasePassword" style="width: 350px;"/>
                </td>
            </tr>
        </tbody>
    </table>
</div>
