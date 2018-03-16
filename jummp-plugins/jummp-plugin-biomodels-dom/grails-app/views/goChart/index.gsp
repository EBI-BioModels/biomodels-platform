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
    <title>Model GO Categories</title>
    <meta name="layout" content="${session['branding.style']}/main" />
    <link rel="stylesheet" href="${resource(dir: 'css', file: 'jquery.dataTables.min.css')}" type="text/css">
    <link rel="stylesheet" href="${resource(dir: 'css', file: 'buttons.dataTables.min.css')}" type="text/css">
    <style>
    path {
        stroke: #000;
        stroke-width: 1.5;
        cursor: pointer;
    }

    text {
        font: 11px sans-serif;
        cursor: pointer;
    }

    #vis h1 {
        text-align: center;
        margin: .5em 0;
    }

    #vis p#intro {
        text-align: center;
        margin: 1em 0;
    }
        .container {
            width: 100%;
        }
        .container #vis {
            width: 42%;
            float: left;
        }
        .container #model_data_wrapper {
            width: 55%;
            padding: 40px 10px;
            float: right;
            clear: inherit;
        }
        .container #model_data {
            visibility: hidden;
        }

        #model_data thead {
            background-color: #008080;
            color: white;
        }
    </style>
</head>
<body>
<div class="container">
    <div id="vis">&nbsp;</div>
    <table id="model_data" class="display">
    </table>
</div>
<div style="clear: both"></div>

<g:javascript src="d3.v3.js"/>
<g:javascript src="jquery.dataTables.min.js"/>
<g:javascript src="dataTables.buttons.min.js"/>
<g:javascript src="buttons.flash.min.js"/>
<g:javascript src="jszip.min.js"/>
<g:javascript src="pdfmake.min.js"/>
<g:javascript src="vfs_fonts.js"/>
<g:javascript src="buttons.html5.min.js"/>
<g:javascript src="buttons.print.min.js"/>

<g:javascript>
    $(document).ready(function() {

    } );
    var json = $.parseJSON('${classifiedModels.toString().replace('\'', '\\\'')}');

    var width = $("#vis").width(),
        height = width,
        radius = width / 2,
        x = d3.scale.linear().range([0, 2 * Math.PI]),
        y = d3.scale.pow().exponent(1.3).domain([0, 1]).range([0, radius]),
        padding = 5,
        duration = 1000;

    function hasMoreWord(d) {
        parts = d.name.split(" ");
        return parts.length > 1;
    }

    function fillTableData(d) {
        var models = recursiveGetData(d);
        if ($.fn.dataTable.isDataTable('#model_data')) {
            table = $('#model_data').DataTable();
            table.clear()
            .rows.add(models)
            .draw()
        } else {
            $('#model_data').DataTable({
                "searching": true,
                "lengthChange": false,
                "pageLength": 14,
                "columns": [
                    {
                        "title": "Model Id",
                        "data": "modelId",
                        "render": function(data, type, row, meta){
                            if(type === 'display'){
                                data = '<a target="_blank" href="https://wwwdev.ebi.ac.uk/biomodels/' + data + '">' + data + '</a>';
                            }

                            return data;
                         }
                    },
                    {"title": "Model Name", "data": "name"},
                    {"title": "Date Update", "data": "updateDate"}
                ],
                "dom": 'Bfrtip',
                "buttons": [
                    'copy', 'csv', 'excel', 'pdf', 'print'
                ],
                "language": {
                    "lengthMenu": '_MENU_ search',
                        "search": '<i class="fa fa-search"></i>',
                        "searchPlaceholder": "Search",
                        "paginate": {
                        "previous": '<i class="fa fa-angle-left"></i>',
                            "next": '<i class="fa fa-angle-right"></i>'
                    }
                },
                "data": models
            });
        }

        $("#model_data").css('visibility', 'inherit')
        $("#model_data").css('min-height', $("#vis").width() + "px")
    }

    function recursiveGetData(d) {
        if (Object.keys(d).indexOf("models") > -1) {
            return d.models
        }
        var result = [];
        for (var i = 0; i < d.children.length; i++) {
            result = result.concat(recursiveGetData(d.children[i]));
        }
        return result
    }


    var div = d3.select("#vis");

    div.select("img").remove();

    var vis = div.append("svg")
        .attr("width", width + padding * 2)
        .attr("height", height + padding * 2)
        .append("g")
        .attr("transform", "translate(" + [radius + padding, radius + padding] + ")");

    var partition = d3.layout.partition()
        .sort(null)
        .value(function(d) { return 5.8 - d.depth; });

    var arc = d3.svg.arc()
        .startAngle(function(d) { return Math.max(0, Math.min(2 * Math.PI, x(d.x))); })
        .endAngle(function(d) { return Math.max(0, Math.min(2 * Math.PI, x(d.x + d.dx))); })
        .innerRadius(function(d) { return Math.max(0, d.y ? y(d.y) : d.y); })
        .outerRadius(function(d) { return Math.max(0, y(d.y + d.dy)); });

    var nodes = partition.nodes({children: json});

    var path = vis.selectAll("path").data(nodes);
    path.enter().append("path")
        .attr("id", function(d, i) { return "path-" + i; })
        .attr("d", arc)
        .attr("fill-rule", "evenodd")
        .style("fill", colour)
        .on("click", click);

    var text = vis.selectAll("text").data(nodes);
    var textEnter = text.enter().append("text")
        .style("fill-opacity", 1)
        .style("fill", function(d) {
        return brightness(d3.rgb(colour(d))) < 125 ? "#eee" : "#000";
    })
        .attr("text-anchor", function(d) {
        return x(d.x + d.dx / 2) > Math.PI ? "end" : "start";
    })
        .attr("dy", ".2em")
        .attr("transform", function(d) {
        var multiline = (d.name || "").split(" ").length > 1,
            angle = x(d.x + d.dx / 2) * 180 / Math.PI - 90,
            rotate = angle + (multiline ? -.5 : 0);
        return "rotate(" + rotate + ")translate(" + (y(d.y) + padding) + ")rotate(" + (angle > 90 ? -180 : 0) + ")";
    })
        .on("click", click);
    textEnter.append("tspan")
        .attr("x", 0)
        .text(function(d) {
            if (d.depth) {
                if (hasMoreWord(d)) {
                    return d.name.split(" ")[0]
                }
                return d.name + " (" + d.count + ")"
            }
            return "";
        });
    textEnter.append("tspan")
        .attr("x", 0)
        .attr("dy", "1em")
        .text(function(d) {
            if (d.depth) {
                if (hasMoreWord(d)) {
                    return d.name.split(" ")[1] + " (" + d.count + ")"
                }
            }
            return "";
        });

    fillTableData(nodes[0])

    function click(d) {
        path.transition()
            .duration(duration)
            .attrTween("d", arcTween(d));

        // Somewhat of a hack as we rely on arcTween updating the scales.
        text.style("visibility", function(e) {
            return isParentOf(d, e) ? null : d3.select(this).style("visibility");
        })
            .transition()
            .duration(duration)
            .attrTween("text-anchor", function(d) {
            return function() {
                return x(d.x + d.dx / 2) > Math.PI ? "end" : "start";
            };
        })
            .attrTween("transform", function(d) {
            var multiline = (d.name || "").split(" ").length > 1;
            return function() {
                var angle = x(d.x + d.dx / 2) * 180 / Math.PI - 90,
                    rotate = angle + (multiline ? -.5 : 0);
                return "rotate(" + rotate + ")translate(" + (y(d.y) + padding) + ")rotate(" + (angle > 90 ? -180 : 0) + ")";
            };
        })
            .style("fill-opacity", function(e) { return isParentOf(d, e) ? 1 : 1e-6; })
            .each("end", function(e) {
            d3.select(this).style("visibility", isParentOf(d, e) ? null : "hidden");
        });
        fillTableData(d);
    }

    function isParentOf(p, c) {
        if (p === c) return true;
        if (p.children) {
            return p.children.some(function(d) {
                return isParentOf(d, c);
            });
        }
        return false;
    }

    function colour(d) {
        return '#008080'
    }

    // Interpolate the scales!
    function arcTween(d) {
        var my = maxY(d),
            xd = d3.interpolate(x.domain(), [d.x, d.x + d.dx]),
            yd = d3.interpolate(y.domain(), [d.y, my]),
            yr = d3.interpolate(y.range(), [d.y ? 20 : 0, radius]);
        return function(d) {
            return function(t) { x.domain(xd(t)); y.domain(yd(t)).range(yr(t)); return arc(d); };
        };
    }

    function maxY(d) {
        return d.children ? Math.max.apply(Math, d.children.map(maxY)) : d.y + d.dy;
    }

    // http://www.w3.org/WAI/ER/WD-AERT/#color-contrast
    function brightness(rgb) {
        return rgb.r * .299 + rgb.g * .587 + rgb.b * .114;
    }
</g:javascript>
%{--<script>--}%
    %{--if (top != self) top.location.replace(location);--}%
%{--</script>--}%

</body>

%{--<content tag="goChart">--}%
    %{--selected--}%
%{--</content>--}%

%{--<content tag="title">--}%
    %{--Model Gene Ontology Categories--}%
%{--</content>--}%
%{--<content tag="contexthelp">--}%
    %{--Model Gene Ontology Categories--}%
%{--</content>--}%
