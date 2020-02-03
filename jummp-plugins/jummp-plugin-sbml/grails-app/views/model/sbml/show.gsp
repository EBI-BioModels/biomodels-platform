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

                    <table  id="table_id1">
                    </table>

                    <div class="header"><span>Reactions</span>
                    </div>

                    <table  id="table_id2">
                    </table>

                </div>

            </div>
        </div>

    </div>

    <script>

        var columnConfig1 = [
            {
                title: 'Species',
                data: 'species',
                orderable: false,
                render: function (data, type, full, meta) {
                    return "<span style=\"color:green;\">"
                        + full.speciesId
                        + "</span><br/><span>"
                        + full.resolvedAccessionUrlsShow
                        + "</span>"
                }
            },
            {
                title: 'Initial Data',
                data: 'initialData',
                orderable: false
            }];

        var columnConfig2 = [
            {
                title: 'Reactions',
                data: 'reactions',
                orderable: false,
                render: function (data, type, full, meta) {
                    return "<span style=\"color:green;\">"
                        + full.unResolvedReaction
                        + "</span><br/><span>"
                        + full.resolvedReaction
                        + "</span>"
                }
            },
            {
                title: 'Rate',
                data: 'rates',
                orderable: false,
                render: function (data, type, full, meta) {
                    return "<span style=\"color:green;\">"
                        + full.unResolvedRate
                        + "</span><br/><span>"
                        + full.resolvedRate
                        + "</span>"
                }
            },
            {
                title: 'Parameters',
                data: 'parameters',
                orderable: false
            }];

        (function(){
            var url = "${g.createLink(controller: "sbml", action: "fetchComponents", absolute: true)}";
            displayModelComponents("${revision.modelIdentifier()}",
                "${revision.revisionNumber}",
                "species",
                "#table_id1",
                columnConfig1,
                url
            );
            displayModelComponents("${revision.modelIdentifier()}",
                "${revision.revisionNumber}",
                "reactions",
                "#table_id2",
                columnConfig2,
                url
            );
            $("#Components").hide();
            $(".header").click(function () {

                $header = $(this);
                //getting the next element
                $content = $header.next();
                //open up the content needed - toggle the slide- if visible, slide up, if not slidedown.
                $content.slideToggle(500, function () {
                });

            });
        }());
    </script>

</content>

