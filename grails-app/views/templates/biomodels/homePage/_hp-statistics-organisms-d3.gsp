<div id="organismsChart" class="div-center-content"></div>
<g:javascript>
/* global diameter */
{
    var dataset = ${organisms};
    var color = d3.scaleOrdinal(d3.schemeCategory20);
    var bubble = d3.pack(dataset).size([diameter, diameter]).padding(1.5);

    var svg = d3.select("#organismsChart")
        .append("svg")
        .attr("viewBox", "0 0 " + diameter + " " + diameter)
        .attr("class", "bubble");

    var nodes = d3.hierarchy(dataset)
        .sum(function(d) {
            return d.NormalisedCount; });

    var node = svg.selectAll(".node")
        .data(bubble(nodes).descendants())
        .enter()
        .filter(function(d){
        return  !d.children
        })
        .append("g")
        .attr("class", "node")
        .attr("transform", function(d) {
        return "translate(" + d.x + "," + d.y + ")";
    });

    node.append("title")
        .text(function(d) {
            return d.data.Name + ": " + d.data.Count;
    });

    node.append("circle")
        .attr("r", function(d) {
        return d.r;
    })
        .style("fill", function(d,i) {
        return color(i);
    });

    node.append("text")
        .attr("dy", ".2em")
        .style("text-anchor", "middle")
        .text(function(d) {return d.data.Name.substring(0, d.r / 3);})
        .attr("font-family", "sans-serif")
        .attr("font-size", function(d) {return d.r/5;})
        .attr("fill", "white");

    node.append("text")
        .attr("dy", "1.3em")
        .style("text-anchor", "middle")
        .text(function(d) { return d.data.Count;})
        .attr("font-family",  "Gill Sans", "Gill Sans MT")
        .attr("font-size", function(d) { return d.r/5;})
        .attr("fill", "white");

    d3.select(self.frameElement)
        .style("height", diameter + "px");
    node.on('click', function(d) {
        var label = d["data"]["Name"];
        var value = d["data"]["Count"];
        var taxonomy = d["data"]["Taxonomy"];
        var serverURL = "${grailsApplication.config.grails.serverURL}";
        var fixedSearchURL = "/search?domain=biomodels&query=*%3A*+AND+TAXONOMY%3A";
        var prefixSearchURL = serverURL + fixedSearchURL;
        var queryURL = prefixSearchURL + taxonomy;
        window.open(queryURL, '_blank');
    });
    node.on("mouseover", function(d) {
        var label = d["data"]["Name"];
        var value = d["data"]["Count"];
        var taxonomy = d["data"]["Taxonomy"];
        $('#item-on-focus').html(label + ": " + value + " models");
        $('#item-on-focus').css("color", "#000000");
    });
    node.on("mouseout", function(d) {
        $('#item-on-focus').css("color", "#e2e1e1");
    });
}
</g:javascript>

