<%--
 Copyright (C) 2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

<div id="searchTips">
    <div style="font-weight: bold; font-size: small;" class="margin-bottom-medium">Example searches</div>
    <ul class="inline">
    <g:each in="${tips}" var="t">
        <li class="inline size-75 margin-right-medium">
            %{-- search function defined in /templates/bpSearchDataTable --}%
            <a href="javascript:search('${t}')" class="tag secondary-background">${t}</a>
        </li>
    </g:each>
    </ul>
</div>
