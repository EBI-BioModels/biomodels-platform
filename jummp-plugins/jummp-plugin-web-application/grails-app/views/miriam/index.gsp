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
        <title><g:message code="miriam.title"/></title>
        <meta name="layout" content="${session['branding.style']}/main" />
        <g:javascript contextPath="" src="js/miriamadministration.js"/>
    </head>
    <body>
        <h1><g:message code="miriam.title"/></h1>
        <div id="miriam">
            <form method="POST">
                <table>
                    <tr>
                        <th><label for="miriam-update-miriam-url"><g:message code="miriam.update.ui.url"/></label></th>
                        <td><input type="text" id="miriam-update-miriam-url" name="miriamUrl" value="http://www.ebi.ac.uk/miriam/main/export/xml/"/></td>
                    </tr>
                    <tr>
                        <th><label for="miriam-update-force"><g:message code="miriam.update.ui.force"/></label></th>
                        <td><g:checkBox id="miriam-update-force" name="force"/></td>
                    </tr>
                </table>
                <div class="buttons">
                    <input type="reset" value="${g.message(code: 'miriam.button.cancel')}"/>
                    <input type="submit" value="${g.message(code: 'miriam.button.save')}"/>
                </div>
            </form>
        </div>
        <div id="miriam-update">
            <form method="POST">
            <g:message code="miriam.data.update"/>
            <div class="buttons">
                <input type="submit" value="${g.message(code: 'miriam.button.schedule')}"/>
            </div>
            </form>
        </div>
        <div id="miriam-model-update">
            <form method="POST">
            <g:message code="miriam.model.update"/>
            <div class="buttons">
                <input type="submit" value="${g.message(code: 'miriam.button.schedule')}"/>
            </div>
            </form>
        </div>
        <g:javascript>
$(function() {
    $.jummp.miriam.init();
});
        </g:javascript>
    </body>
</html>
