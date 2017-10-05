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
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
        <title><g:message code="Regeneration Complete"/></title>
    </head>
    <body>
        <div>
            <h2>Regenerating indices</h2>
            <p><strong>Notes</strong>: This process will take a long time and please think thoroughly before clicking the button.</p>
            <button class="button"
                onclick="<g:remoteFunction controller="search"
                                           action="regenIndices"
                                           name="regenIndices" update="report"
                                           asynchronous="false"/>" >
                    Click here to regenerate indices</button>
            <div id="report">

            </div>
        </div>
    </body>

