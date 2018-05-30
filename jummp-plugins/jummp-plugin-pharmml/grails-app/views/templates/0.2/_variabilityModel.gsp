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
<h3>Variability Model</h3>

<table class="views-table cols-2">
    <thead>
        <tr>
            <th>Level</th>
            <th>Type</th>
        </tr>
    </thead>
    <tbody>
        <g:each var="m" in="${variabilityModels}">
            <tr>
                <td>
                    <g:render template="/templates/0.2/variabilityLevels" collection="${m.levels}"/>
                </td>
                <td>${m.type}</td>
            </tr>
        </g:each>
    </tbody>
</table>
