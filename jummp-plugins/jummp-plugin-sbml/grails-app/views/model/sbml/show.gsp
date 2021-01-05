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











<meta name="layout" content="${session['branding.style']}/modelDisplay"/>
<content tag="genericAnnotations">
    <anno:renderGenericAnnotations annotations="${genericAnnotations}"/>
</content>
<content tag="modelspecifictabs">
    <sbml:decideTabs/>
</content>
<content tag="modelspecifictabscontent">
    <div id="Components" class="row">
        <div class="small-12 columns">
            <div class="row">
                <div class="pull-element-right round-border small-12 medium-4 large-2 columns">
                    <strong>Legends</strong><br/>
                    <span class="legend-green-block">
                    </span>
                    <span>
                        : Variable used inside SBML models</span>
                </div>

                <div class="small-12 medium-4 large-2 columns">
                <button id="expand-all"><i class="icon icon-common icon-plus"></i> Expand All</button><br/>
                <button id="collapse-all"><i class="icon icon-common icon-collapse"></i> Collapse All</button>
            </div>
        </div>
        <br/>

            <div class="container row">
                <div class="small-12 medium-4 large-8 columns">
                    <div class="header"><span>Species</span>
                    </div>


                    <div class="content">
                        <g:if test="${components.species?.size() > 0}">
                            <table>
                                <th>Species</th>
                                <th>Initial Concentration/Amount</th>
                                <g:each var="a" in="${components.species}">
                                    <tr style="text-align: center">
                                        <td>${a.value.speciesAnnotationShow}</td>
                                        <td>${a.value.initialData}</td>
                                    </tr>
                                </g:each>
                            </table>
                        </g:if>
                        <g:else>
                            No records to display
                        </g:else>
                    </div>

                    <div class="header"><span>Reactions</span>

                    </div>

                    <div class="content small-12 medium-4 large-8 columns">
                        <g:if test="${components.reactions?.size() > 0}">
                            <table style="width: 100%">
                                <th>Reactions</th>
                                <th>Rate</th>
                                <th>Parameters</th>
                                <g:each var="a" in="${components.reactions}">
                                    <tr style="text-align: center">
                                        <td>${a.value.reaction}</td>
                                        <td>${a.value.rate}</td>
                                        <td>${a.value.parameters}</td>
                                    </tr>
                                </g:each>
                            </table>
                        </g:if>
                        <g:else>
                            No records to display
                        </g:else>

                    </div>

                </div>

            </div>
        </div>

    </div>

    <g:javascript>
        $(".header").click(function () {

            $header = $(this);
            //getting the next element
            $content = $header.next();
            //open up the content needed - toggle the slide- if visible, slide up, if not slidedown.
            $content.slideToggle(500, function () {
            });

        });
    </g:javascript>

</content>

