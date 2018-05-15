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
    <link rel="stylesheet" href="${resource(dir: 'css', file: 'select.dataTables.min.css')}" type="text/css">
    <style>
    path {
        stroke: #000;
        stroke-width: .5;
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

    .highlight {
        fill: green !important;
    }

    #vis p#intro {
        text-align: center;
        margin: 1em 0;
    }
        .container {
            width: 100%;
        }
        .container #vis {
            width: 48%;
            float: left;
        }
        .container #model_data_wrapper {
            width: 50%;
            padding: 40px 10px;
            float: right;
            clear: inherit;
        }
        .container #model_data {
            visibility: hidden;
        }

    th.dt-center, td.dt-center { text-align: center; }

        #model_data thead {
            background-color: #008080;
            color: white;
        }
    button:disabled,
    button[disabled]{
        border: 1px solid #999999;
        background-color: #cccccc;
        color: #666666;
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
<g:javascript src="dataTables.select.min.js"/>

<g:javascript>
    var json = $.parseJSON('${classifiedModels.toString().replace('\'', '\\\'')}');

    var totalModels = 0;
    for (var i = 0; i < json.length; i++) {
        totalModels += json[i].count;
    }

    json = {
        name: "GO",
        children: json,
        count: totalModels,
        x: -30
    };

    var width = $("#vis").width(),
        height = width,
        radius = width / 2,
        x = d3.scale.linear().range([0, 2 * Math.PI]),
        y = d3.scale.pow().exponent(1.3).domain([0, 1]).range([0, radius]),
        padding = 5,
        duration = 1000;

    var table;
    var color = d3.scale.category20c();
    function fillTableData(d) {
        var models = recursiveGetData(d);
        if ($.fn.dataTable.isDataTable('#model_data')) {
            table = $('#model_data').DataTable();
            table.clear()
            .rows.add(models)
            .draw()
        } else {
            table = $('#model_data').DataTable({
                "searching": true,
                "lengthChange": false,
                "pageLength": 14,
                "columns": [
                    {
                        "orderable": false,
                        'checkboxes': true,
                        "targets":   0,
                        "data": "modelId",
                        'className': 'dt-body-center',
                        'title': '<input name="select_all" value="1" id="example-select-all" type="checkbox">',
                        'render': function (data, type, full, meta) {
                           return '<input type="checkbox" name="model_id[]" value="' + $('<div/>').text(data).html() + '">';
                        }
                    },
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
                    {"title": "Date Update", "data": "updateDate", "width": "14%", "className": "dt-center", "targets": "_all"}
                ],
                "dom": 'Bfrtip',
                "buttons": [
                    'copy', 'csv', 'excel', 'pdf', 'print',
                     {
                        text: 'Download',
                        className : 'download-button',
                        action: function ( e, dt, node, config ) {
                            var rows = table.rows({ 'search': 'applied' }).nodes();
                            modelDownload = [];
                            $('input[type="checkbox"]:checked', rows).each(function() {
                                modelDownload.push($(this).attr('value'))
                            });
                            if (modelDownload.length === 0) {
                                alert("You have to select at least one model to download");
                                return
                            }
                            var url = '/jummp-biomodels/search/download?models=' + modelDownload.join(",");
                            var win = window.open(url, '_blank');
                            win.focus();
                        }
                    }
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
                "order": [[ 1, 'asc' ]],
                "data": models
            });
        }

        $("#model_data").css('visibility', 'inherit');
        $("#model_data").css('min-height', $("#vis").width() + "px");
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
        .value(function(d) { return d.count + 30 });

    var arc = d3.svg.arc()
        .startAngle(function(d) {
            return Math.max(0, Math.min(2 * Math.PI, x(d.x)));
        })
        .endAngle(function(d) {
            return Math.max(0, Math.min(2 * Math.PI, x(d.x + d.dx)));
        })
        .innerRadius(function(d) {
            return Math.max(0, d.y ? y(d.y) : d.y);
        })
        .outerRadius(function(d) {
            return Math.max(0, y(d.y + d.dy));
        });

    var nodes = partition.nodes(json);

    var path = vis.selectAll("path").data(nodes);
    path.enter().append("path")
        .attr("id", function(d, i) { return "path-" + i; })
        .attr("d", arc)
        .attr("fill-rule", "evenodd")
        .style("fill", function(d) { return color((d.children ? d : d.parent).name); })
        .on("click", click);

    var text = vis.selectAll("text").data(nodes);
    text.enter().append("text")
        .attr("id", function(d, i) {
          return "text-" + i;
        })
        .style("fill-opacity", 1)
        .style("fill", function(d) {
        return brightness(d3.rgb(color((d.children ? d : d.parent).name))) < 125 ? "#eee" : "#000";
    })
        .attr("text-anchor", function(d) {
        return x(d.x + d.dx / 2) > Math.PI ? "end" : "start";
    })
        .attr("dy", ".5em")
        .attr("transform", function(d) {
            var dx = d.x;
            var angle = x(dx + d.dx / 2) * 180 / Math.PI - 90;
            return "rotate(" + angle + ")translate(" + (y(d.y) + padding) + ")rotate(" + (angle > 90 ? -180 : 0) + ")";
        })
        .text(function(d) {
          return d.name + "#(" + d.count + ")";
        })
        .call(wrap, 120)
        .on("click", click);

    function wrap(text, width) {
        text.each(function () {
            var dw = width;
            var text = d3.select(this),
                words = text.text().split(/\s+/).reverse(),
                word,
                line = [],
                lineNumber = 0,
                x = 0,
                y = 0,
                dy = 0; //parseFloat(text.attr("dy")),

            var tspan = text.text(null)
                        .append("tspan")
                        .attr("x", x)
                        .attr("y", y)
                        .attr("dy", dy + "em");
            while (word = words.pop()) {
                word = word.replace("#", " ");
                line.push(word);
                tspan.text(line.join(" "));
                if (tspan.node().getComputedTextLength() > dw) {
                    line.pop();
                    tspan.text(line.join(" "));
                    line = [word];
                    tspan = text.append("tspan")
                                .attr("x", x)
                                .attr("y", y)
                                .attr("dy", ++lineNumber + "em")
                                .text(word);
                }
            }
        });
    }

    fillTableData(nodes[0]);

    function toggleDownloadButton(rows) {
        if ($('input[type="checkbox"]:checked', rows).length > 0 ) {
            $('.download-button').prop("disabled", false);
        } else {
            $('.download-button').prop("disabled", true);
        }
    }

    $('.download-button').prop("disabled", true);

    $('#example-select-all').on('click', function(){
       // Get all rows with search applied
       var rows = table.rows({ 'search': 'applied' }).nodes();
       // Check/uncheck checkboxes for all rows in the table
       $('input[type="checkbox"]', rows).prop('checked', this.checked);
       toggleDownloadButton(rows)
    });

    var rows = table.rows({ 'search': 'applied' }).nodes();
    $('input[type="checkbox"]', rows).change(function() {
        toggleDownloadButton(rows)
    });

    var lastClick = d3.select('#path-0');
    lastClick.classed("highlight", true);

    function endall(transition, callback) {
        if (typeof callback !== "function") throw new Error("Wrong callback in endall");
        if (transition.size() === 0) { callback() }
        var n = 0;
        transition
            .each(function() { ++n; })
            .each("end", function() { if (!--n) callback.apply(this, arguments); });
    }

    function click(d) {
        path.transition()
            .duration(duration)
            .attrTween("d", arcTween(d))
            .call(endall, function() {
                fillTableData(d);
                var rows = table.rows({ 'search': 'applied' }).nodes();
                $('input[type="checkbox"]', rows).change(function() {
                    toggleDownloadButton(rows)
                });
             });

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
                var dx = d.x;
                var angle = x(dx + d.dx / 2) * 180 / Math.PI - 90,
                    rotate = angle + (multiline ? -.5 : 0);
                return "rotate(" + rotate + ")translate(" + (y(d.y) + padding) + ")rotate(" + (angle > 90 ? -180 : 0) + ")";
            };
        })
            .style("fill-opacity", function(e) {
                return isParentOf(d, e) ? 1 : 1e-6;
            })
            .each("end", function(e) {
            d3.select(this).style("visibility", isParentOf(d, e) ? null : "hidden");
        });
        var ref = d3.select(this);
        if (this.id.indexOf('text') > -1) {
            ref = d3.select('#path-' + this.id.split("-")[1])
        }
        lastClick.classed("highlight", false);
        lastClick = ref;
        ref.classed("highlight", true);
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
