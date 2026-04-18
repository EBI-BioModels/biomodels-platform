<div id="journalsChart" class="div-center-content"></div>
<g:javascript>
    // Limit to the top 40 journals by model count to avoid over-crowding the bubble chart
    var allJournals = ${journals};
    allJournals.sort(function(a, b) { return b.value - a.value; });
    var dataset = {
        'children': allJournals.slice(0, 40)
    };

    var color = d3
        .scaleOrdinal(d3.schemeCategory20c);

    var bubble = d3
        .pack()
        .size([diameter, diameter])
        .padding(1.5);

    var svg = d3
        .select('#journalsChart')
        .append('svg')
        .attr('viewBox','0 0 ' + (diameter) + ' ' + diameter);

    var root = d3
        .hierarchy(dataset)
        .sum(function(d) { return d.value; })
        .sort(function(a, b) { return b.value - a.value; });

    bubble(root);

    var node = svg
        .selectAll('.node')
        .data(root.children)
        .enter()
        .append('g').attr('class', 'node')
        .attr('transform', function(d) { return 'translate(' + d.x + ' ' + d.y + ')'; })
        .append('g').attr('class', 'graph');

    node
        .append("circle")
        .attr("r", function(d) { return d.r; })
        .style("fill", function(d) {
            return color(d.data.name);
        });

    node
        .append("title")
        .text(function(d) { return d.data.name + ": " + d.data.value; });

    node
        .append("text")
        .attr("dy", ".3em")
        .style("text-anchor", "middle")
        .text(function(d) { return d.data.name.substring(0, d.r / 3); })
        .style("fill", "#ffffff")
        .style("font-size", function(d) {
            //return d.r/5;
            return Math.min(2 * d.r, (2 * d.r - 8) / this.getComputedTextLength() * 14) + "px";
        });

    node
        .on("mouseover", function(d) {
                $('#item-on-focus').html(d.data.name + ": " + d.data.value + " models");
                $('#item-on-focus').css("color", "#000000");
            });
    node
        .on("mouseout", function(d) {
            $('#item-on-focus').css("color", "#e2e1e1");
        });
</g:javascript>

