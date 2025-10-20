<%--
 Copyright (C) 2010-2025 EMBL-European Bioinformatics Institute (EMBL-EBI),
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











<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <meta name="layout" content="${session['branding.style']}/main" />
        <title>Account Not Found | BioModels</title>
        <g:render template="/usermanagement/head"/>

    </head>
     <body>
        <div class="row">
            <div class="small-12 medium-12 large-6 large-centered columns">
            <h3 style="color: red; font-weight: bold">Your account not found</h3>
                <p>This account is not found. Please contact us for further support if you believe this is a mistake.</p>
            </div>
        </div>

        <g:render template="/usermanagement/common-scripts"/>
        <g:render template="/usermanagement/foot"/>
     </body>
</html>
<content tag="title">
	Account Not Found
</content>
