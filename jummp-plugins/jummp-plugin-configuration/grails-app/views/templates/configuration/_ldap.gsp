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











<div class="dialog">
    <table class="formtable" width="100%">
        <tbody>
            <tr class="prop">
                <td class="name" width="25%"><label for="ldapServer">Server:</label></td>
                <td class="value ${hasErrors(bean: ldap, field: 'ldapServer', 'errors')}">
                    <input type="text" name="ldapServer" id="ldapServer"
                           value="${ldap?.ldapServer}" style="width: 350px"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="ldapManagerDn">Manager DN:</label></td>
                <td class="value ${hasErrors(bean: ldap, field: 'ldapManagerDn', 'errors')}">
                    <input type="text" name="ldapManagerDn" id="ldapManagerDn"
                           value="${ldap?.ldapManagerDn}" style="width: 350px"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="ldapManagerPassword">Manager Password:</label></td>
                <td class="value ${hasErrors(bean: ldap, field: 'ldapManagerPassword', 'errors')}">
                    <input type="text" name="ldapManagerPassword" id="ldapManagerPassword"
                           value="${ldap?.ldapManagerPassword}" style="width: 350px"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="ldapSearchBase">Search Base:</label></td>
                <td class="value ${hasErrors(bean: ldap, field: 'ldapSearchBase', 'errors')}">
                    <input type="text" name="ldapSearchBase" id="ldapSearchBase"
                           value="${ldap?.ldapSearchBase}" style="width: 350px"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="ldapSearchFilter">Search Filter:</label></td>
                <td class="value ${hasErrors(bean: ldap, field: 'ldapSearchFilter', 'errors')}">
                    <input type="text" name="ldapSearchFilter" id="ldapSearchFilter"
                           value="${ldap?.ldapSearchFilter}" style="width: 350px"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name"><label for="ldapSearchSubtree">Search Subtree:</label></td>
                <td class="value ${hasErrors(bean: ldap, field: 'ldapSearchSubtree', 'errors')}">
                    <g:checkBox name="ldapSearchSubtree" id="ldapSearchSubtree"
                                value="${ldap?.ldapSearchSubtree}"/>
                </td>
            </tr>
        </tbody>
    </table>
</div>
