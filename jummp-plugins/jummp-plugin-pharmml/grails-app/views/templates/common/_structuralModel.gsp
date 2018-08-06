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
<h3>Structural Model <span class='italic'>${name}</span></h3>
<g:if test="${simpleParameters}">
    <p class='bold'>Parameters</p>
    <pharmml:simpleParamsClosure simpleParameters="${simpleParameters}" version="${version}"/>
</g:if>
<g:if test="${variableDefinitions}">
    <p class='bold'>Variable definitions</p>
    <div>
        <pharmml:commonVariables vars="${variableDefinitions}" iv="${independentVariable}"
                version="${version}" />
    </div>
</g:if>
<g:if test="${pkMacroList}">
    <p class='bold'>PK Macros</p>
    <div>
        <pharmml:pkMacros vars="${pkMacroList}" version="${version}" />
    </div>
</g:if>
<g:if test="${error}">
    <p>${error}</p>
</g:if>
