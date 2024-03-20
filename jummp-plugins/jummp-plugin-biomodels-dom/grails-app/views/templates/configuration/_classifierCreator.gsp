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
    <table class="formtable">
        <tbody>
        <tr class="prop">
            <td class="name" width="25%">
                <label for="dlName">${message(code: 'modelclassifier.dllmodel.details.name')}:</label>
            </td>
            <td class="value ${hasErrors(bean: classifierCreator, field: 'dlName', 'errors')}">
                <input type="text" name="dlName" id="dlName"
                       style="width: 350px;"
                />
            </td>
        </tr>
        <tr class="prop">
            <td class="name">
                <label for="totalEpoch">${message(code: 'modelclassifier.dllmodel.creator.totalEpoch')}:</label>
            </td>
            <td class="value ${hasErrors(bean: classifierCreator, field: 'totalEpoch', 'errors')}">
                <input type="number" name="totalEpoch" id="totalEpoch"
                       value="1000"
                       style="width: 350px;"
                />
            </td>
        </tr>
        <tr class="prop">
            <td class="name">
                <label for="valPerEpoch">${message(code: 'modelclassifier.dllmodel.creator.valPerEpoch')}:</label>
            </td>
            <td class="value ${hasErrors(bean: classifierCreator, field: 'valPerEpoch', 'errors')}">
                <input type="number" name="valPerEpoch" id="valPerEpoch"
                       value="2" style="width: 350px;"/>
            </td>
        </tr>
        <tr class="prop">
            <td class="name">
                <label for="batchSize">${message(code: 'modelclassifier.dllmodel.creator.batchSize')}:</label>
            </td>
            <td class="value ${hasErrors(bean: classifierCreator, field: 'batchSize', 'errors')}">
                <input type="number" name="batchSize" id="batchSize"
                       value="32" style="width: 350px;"/>
            </td>
        </tr>
        <tr class="prop">
            <td class="name">
                <label for="hiddenLayer">${message(code: 'modelclassifier.dllmodel.creator.hiddenLayer')}:</label>
            </td>
            <td class="value ${hasErrors(bean: classifierCreator, field: 'hiddenLayer', 'errors')}">
                <input type="text" name="hiddenLayer" id="hiddenLayer" value="Auto" style="width: 350px;"/>
            </td>
        </tr>
        </tbody>
    </table>
</div>
