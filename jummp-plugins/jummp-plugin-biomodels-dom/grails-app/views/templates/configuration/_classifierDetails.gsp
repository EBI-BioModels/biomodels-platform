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



<style>
body {
    background: #f5f5f5;
}

.classifiers {
    list-style: none;
    margin: 0;
    text-shadow: 0 0 1px rgba(255, 255, 255, 0.004);
    font-size: 100%;
    font-weight: 400;
    max-width: 1200px;
    padding: 10px 0 30px 0;
    box-sizing: border-box;
}

.classifier-item {
    display: block;
    margin-bottom: 10px;
    padding: 20px 0;
    border-radius: 2px;
    background: white;
    box-shadow: 0 2px 1px rgba(170, 170, 170, 0.25);
}

.classifier-name {
    font-weight: 400;
}

.list .classifier-item {
    position: relative;
    padding: 0;
    font-size: 14px;
    line-height: 50px;
}
.list .classifier-item .pull-right {
    position: absolute;
    right: 0;
    top: 0;
}
@media screen and (max-width: 800px) {
    .list .classifier-item .stage:not(.active) {
        display: none;
    }
}
@media screen and (max-width: 700px) {
    .list .classifier-item .classifier-progress-bg {
        display: none;
    }
}
@media screen and (max-width: 600px) {
    .list .classifier-item .pull-right {
        position: static;
        line-height: 20px;
        padding-bottom: 10px;
    }
}
.list .classifier-country,
.list .classifier-progress,
.list .classifier-completes,
.list .classifier-end-date {
    color: #A1A1A4;
}
.list .classifier-country,
.list .classifier-completes,
.list .classifier-end-date,
.list .classifier-name,
.list .classifier-stage {
    margin: 0 10px;
}
.list .classifier-country {
    margin-right: 0;
}
.list .classifier-end-date,
.list .classifier-completes,
.list .classifier-country,
.list .classifier-name {
    vertical-align: middle;
}
.list .classifier-end-date {
    display: inline-block;
    width: 145px;
    white-space: nowrap;
    overflow: hidden;
}

.classifier-stage .stage {
    display: inline-block;
    vertical-align: middle;
    width: 16px;
    height: 16px;
    overflow: hidden;
    border-radius: 50%;
    padding: 0;
    margin: 0 2px;
    background: #f2f2f2;
    text-indent: -9999px;
    color: transparent;
    line-height: 16px;
}
.classifier-stage .stage.active {
    background: #A1A1A4;
}

.list .list-only {
    display: auto;
}

.classifier-progress-label {
    vertical-align: middle;
    margin: 0 10px;
    color: #8DC63F;
}

.classifier-progress-labels span {
    display: inline-block;
    width: 70px;
}

.classifier-progress-bg {
    display: inline-block;
    vertical-align: middle;
    position: relative;
    width: 100px;
    height: 4px;
    border-radius: 2px;
    overflow: hidden;
    background: #eee;
}

.classifier-progress-fg {
    position: absolute;
    top: 0;
    bottom: 0;
    height: 100%;
    left: 0;
    margin: 0;
    background: #8DC63F;
}

.pretty.p-switch .state::before {
    top: 4px !important;
}

.pretty .state label::after, .pretty .state label::before {
    top: 5px !important;
}
.pretty .state label{
    color: #A1A1A4 !important;
}
.pretty.p-switch .state label {
    text-indent: 3em !important;
}
.x-close {
    position: relative;
    display: inline-block;
    width: 30px;
    height: 30px;
}
.x-close:hover::before, .x-close:hover::after {
    background: #1ebcc5;
}
.x-close::before, .x-close::after {
    content: '';
    position: absolute;
    height: 2px;
    width: 75%;
    top: 88%;
    right: 10px;
    margin-top: -1px;
    background: #A1A1A4;
}
.x-close::before {
    -webkit-transform: rotate(45deg);
    -moz-transform: rotate(45deg);
    -ms-transform: rotate(45deg);
    -o-transform: rotate(45deg);
    transform: rotate(45deg);
}
.x-close::after {
    -webkit-transform: rotate(-45deg);
    -moz-transform: rotate(-45deg);
    -ms-transform: rotate(-45deg);
    -o-transform: rotate(-45deg);
    transform: rotate(-45deg);
}
.x-close.hairline::before, .x-close.hairline::after {
    height: 1px;
}
.delete-deep-model {
    cursor: pointer;
}

.dlmodel-progress {
    position: relative;
    height: 4px;
    display: inline-block;
    width: 55%;
    background-color: #eee;
    border-radius: 2px;
    background-clip: padding-box;
    margin: auto 0px auto 10px;
    overflow: hidden; }
.dlmodel-progress .determinate {
    position: absolute;
    top: 0;
    bottom: 0;
    background-color: #5cb85c;
    transition: width .3s linear; }

.btn {
    position: relative;

    display: inline-block;
    padding: 0;

    overflow: hidden;

    border-width: 0;
    outline: none;
    border-radius: 2px;

    background-color: #2ecc71;
    color: #ecf0f1;

    margin: auto 20px auto 25px;

    cursor: pointer;

    transition: background-color .3s;
}

.btn:hover, .btn:focus {
    background-color: #27ae60;
}

.btn > * {
    position: relative;
}

.btn span {
    display: block;
    padding: 10px;
}

.btn:before {
    content: "";

    position: absolute;
    top: 50%;
    left: 50%;

    display: inline-block;
    width: 0;
    padding-top: 0;

    border-radius: 100%;

    background-color: rgba(236, 240, 241, .3);

    -webkit-transform: translate(-50%, -50%);
    -moz-transform: translate(-50%, -50%);
    -ms-transform: translate(-50%, -50%);
    -o-transform: translate(-50%, -50%);
    transform: translate(-50%, -50%);
}

.btn:active:before {
    width: 120%;
    padding-top: 120%;

    transition: width .2s ease-out, padding-top .2s ease-out;
}

.axis path,
.axis line {
    fill: none;
    stroke: #000;
    shape-rendering: crispEdges;
}


.line {
    fill: none;
    stroke: steelblue;
    stroke-width: 1.5px;
}
.classifier-item input {
    margin: auto;
    height: 2.2rem;
    border-radius: 2px;
}
</style>

<ul class="classifiers list">
    <li class="classifier-item">

        <span class="classifier-country list-only">
            ${message(code: 'modelclassifier.dllmodel.details.name')}
        </span>

        <span class="classifier-name">
            ${classifierCreator.dlname}
        </span>

        <span class="classifier-country list-only">
            ${message(code: 'modelclassifier.dllmodel.details.progress')}
        </span>

        <span>
            <span class="dlmodel-progress">
                <span class="determinate" id="dlmodel-progress-val" style="width: 70%"></span>
            </span>
            <span class="classifier-progress-label" id="dlmodel-val">
                0%
            </span>
        </span>

        <div class="pull-right">
            <span class="classifier-end-date ended" id="dlmodel-date">

            </span>
            <span>
                <button class="btn" type="submit" id="dlmodel-now">
                    <span>${message(code: 'modelclassifier.dllmodel.details.retrain')}</span>
                </button>
            </span>
        </div>
    </li>
    <li class="classifier-item">
        <input type="hidden" name="dlname" id="dlname" value="${classifierCreator.dlname}"/>
        <table>
            <tbody>
            <tr>
                <td class="name">
                    <label for="totalEpoch">${message(code: 'modelclassifier.dllmodel.creator.totalEpoch')}:</label>
                </td>
                <td class="value ${hasErrors(bean: classifierCreator, field: 'totalEpoch', 'errors')}">
                    <input type="number" name="totalEpoch" id="totalEpoch" value="${classifierCreator.totalEpoch}"/>
                </td>
                <td class="name">
                    <label for="valPerEpoch">${message(code: 'modelclassifier.dllmodel.creator.valPerEpoch')}:</label>
                </td>
                <td class="value ${hasErrors(bean: classifierCreator, field: 'valPerEpoch', 'errors')}">
                    <input type="number" name="valPerEpoch" id="valPerEpoch" value="${classifierCreator.valPerEpoch}"/>
                </td>
            </tr>
            <tr>
                <td class="name">
                    <label for="batchSize">${message(code: 'modelclassifier.dllmodel.creator.batchSize')}:</label>
                </td>
                <td class="value ${hasErrors(bean: classifierCreator, field: 'batchSize', 'errors')}">
                    <input type="number" name="batchSize" id="batchSize" value="${classifierCreator.batchSize}"/>
                </td>
                <td class="name">
                    <label for="hiddenLayer">${message(code: 'modelclassifier.dllmodel.creator.hiddenLayer')}:</label>
                </td>
                <td class="value ${hasErrors(bean: classifierCreator, field: 'hiddenLayer', 'errors')}">
                    <input type="text" name="hiddenLayer" id="hiddenLayer" value="${classifierCreator.hiddenLayer}"/>
                </td>
            </tr>
            </tbody>
        </table>
    </li>
    <li class="classifier-item" id="model-train-chart"></li>
</ul>
<script src="https://d3js.org/d3.v3.min.js"></script>
<g:javascript>
    var svg;

    $('#configurationForm').submit(function (e) {
            if (!confirm("${message(code: 'modelclassifier.dllmodel.retrain.confirm')}")) {
                return;
            }
        });

    function getTrainPercent() {
      $.ajax({
            dataType:'json',
            type: "GET",
            url: $.jummp.createLink("classifierConfigure", "trainModelStatus"),
            data: {
                  model_name: encodeURIComponent("${classifierCreator.dlname}"),
                  time: Date.now()
            },
            error: function(jqXHR) {
                toastr.error(jqXHR.responseJSON.message);
            },
            success: function(data) {
                $("#dlmodel-progress-val").css("width", data.percent + "%");
                $("#dlmodel-val").html(data.percent + "%");
                setTimeout(getTrainPercent, 3000);
            }
        });
    }

    function getTrainLog() {
        $.ajax({
            dataType:'json',
            type: "GET",
            url: $.jummp.createLink("classifierConfigure", "trainModelLogs"),
            data: {
                  model_name: encodeURIComponent("${classifierCreator.dlname}"),
                  time: Date.now()
            },
            error: function(jqXHR) {
                toastr.error(jqXHR.responseJSON.message);
            },
            success: function(data) {
                if (data.length !== 0) {
                    generateLineChart(data, "#model-train-chart");
                    setTimeout(getTrainLog, 3000);
                }
            }
        });
    }

    function generateLineChart(data, destination) {
        function prepend(value, array) {
            var newArray = array.slice();
            newArray.unshift(value);
            return newArray;
        }

        if (data[0]["epoch"] != "0") {
            data = prepend({"loss": data[0]["loss"], "accuracy": data[0]["accuracy"], "epoch": "0"}, data);
        }

        var margin = {top: 20, right: 80, bottom: 30, left: 50},
            width = $(destination).width() - margin.left - margin.right,
            height = 500 - margin.top - margin.bottom;

        var x = d3.scale.linear()
            .range([0, width]);

        var y = d3.scale.linear()
            .range([height, 0]);

        var color = d3.scale.category10();

        var xAxis = d3.svg.axis()
            .scale(x)
            .orient("bottom");

        var yAxis = d3.svg.axis()
            .scale(y)
            .orient("left");

        var line = d3.svg.line()
            .interpolate("basis")
            .x(function(d) { return x(d.epoch); })
            .y(function(d) { return y(d.percent); });

        color.domain(d3.keys(data[0]).filter(function(key) { return key !== "epoch"; }));

        var cities = color.domain().map(function(name) {
            return {
                name: name,
                values: data.map(function(d) {
                    return {epoch: parseInt(d.epoch), percent: parseFloat(d[name])};
                })
            };
        });

        x.domain(d3.extent(data, function(d) { return parseInt(d.epoch); }));

        y.domain([
            d3.min(cities, function(c) { return 0; }),
            d3.max(cities, function(c) { return 1; })
        ]);

        if (!svg) {
            svg = d3.select(destination).append("svg")
                .attr("width", width + margin.left + margin.right)
                .attr("height", height + margin.top + margin.bottom)
            .append("g")
                .attr("transform", "translate(" + margin.left + "," + margin.top + ")");
            svg.append("g")
              .attr("class", "x axis")
              .attr("transform", "translate(0," + height + ")")
              .call(xAxis)
              .append("text")
              .attr("x", width)
              .attr("y", -6)
              .style("text-anchor", "end")
              .text("Epoch");

            svg.append("g")
                  .attr("class", "y axis")
                  .call(yAxis);
                // .append("text")
                //   .attr("transform", "rotate(-90)")
                //   .attr("y", 6)
                //   .attr("dy", ".71em")
                //   .style("text-anchor", "end")
                //   .text("Percent");
            var city = svg.selectAll(".city")
                .data(cities)
                .enter().append("g")
                .attr("class", "city");
            city.append("path")
                  .attr("class", "line")
                  .attr("d", function(d) { return line(d.values); })
                  .style("stroke", function(d) { return color(d.name); });

            city.append("text")
                  .datum(function(d) { return {name: d.name, value: d.values[d.values.length - 1]}; })
                  .attr("transform", function(d) { return "translate(" + x(d.value.epoch) + "," + y(d.value.percent) + ")"; })
                  .attr("x", 3)
                  .attr("dy", ".35em")
                  .text(function(d) { return d.name; });
        }
        svg.selectAll(".city")
                .data(cities)
                .enter()
                .select(".line")   // change the line
                .attr("d", function(d) { return line(d.values); })
                  .style("stroke", function(d) { return color(d.name); });
        svg.select(".x.axis") // change the x axis
            .call(xAxis);
        svg.select(".y.axis") // change the y axis
            .call(yAxis);
    }

    $(document).ready(function() {
        getTrainPercent();
        getTrainLog();
    });
</g:javascript>
