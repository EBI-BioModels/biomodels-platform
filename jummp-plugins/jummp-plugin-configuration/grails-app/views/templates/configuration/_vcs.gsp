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
                <td class="name" style="width: 25%;">
                    <label for="subversion">Subversion:</label></td>
                <td class="value ${hasErrors(bean: vcs, field: 'vcs', 'errors')}">
                    <input type="radio" disabled="true" name="vcs" id="subversion"
                           value="svn" ${!vcs || vcs.vcs == "svn" ? 'checked="checked"' : ''} title="Subversion"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name">
                    <label for="git">Git:</label></td>
                <td class="value ${hasErrors(bean: vcs, field: 'vcs', 'errors')}">
                    <input type="radio" name="vcs" id="git"
                           value="git" ${vcs?.vcs == "git" ? 'checked="checked"' : ''} title="Git"/>
                </td>
            </tr>
            <tr class="prop">
                <td class="name">
                    <label for="workingDirectory"
                           title="Local path to the directory where the checkout is kept. Required in git">Working Directory:</label></td>
                <td class="value ${hasErrors(bean: vcs, field: 'workingDirectory', 'errors')}">
                    <input type="text" name="workingDirectory" id="workingDirectory"
                           value="${vcs?.workingDirectory}"
                           title="Local path to the directory where the checkout is kept. Required in git"
                           style="width: 500px;"
                    />
                </td>
            </tr>
            <tr class="prop">
                <td class="name">
                    <label for="exchangeDirectory">Exchange Directory (Local path to the directory where retrieved files from VCS are saved):</label></td>
                <td class="value ${hasErrors(bean: vcs, field: 'exchangeDirectory', 'errors')}">
                    <input type="text" name="exchangeDirectory" id="exchangeDirectory"
                           value="${vcs?.exchangeDirectory}"
                           style="width: 500px;"
                    />
                </td>
            </tr>
        </tbody>
    </table>
</div>
