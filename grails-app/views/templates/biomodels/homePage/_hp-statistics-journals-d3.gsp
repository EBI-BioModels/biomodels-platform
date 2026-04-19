<div id="journalsChart" class="div-center-content"></div>
<g:javascript>
    // Show top 40 journals; aggregate the remainder into a single "Others" bubble
    const allJournals = ${journals};
    allJournals.sort(function(a, b) { return b.value - a.value; });
    const topJournals = allJournals.slice(0, 40);
    const rest = allJournals.slice(40);
    if (rest.length > 0) {
        const othersCount = rest.reduce(function(sum, d) { return sum + d.value; }, 0);
        topJournals.push({ name: 'Others (' + rest.length + ')', value: othersCount });
    }
    const dataset = {
        'children': topJournals
    };

    const color = d3
        .scaleOrdinal(d3.schemeCategory20c);

    const bubble = d3
        .pack()
        .size([diameter, diameter])
        .padding(1.5);

    const svg = d3
        .select('#journalsChart')
        .append('svg')
        .attr('viewBox','0 0 ' + (diameter) + ' ' + diameter);

    const root = d3
        .hierarchy(dataset)
        .sum(function(d) { return d.value; })
        .sort(function(a, b) { return b.value - a.value; });

    bubble(root);

    let node = svg
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
            // return d.r/5;
            return Math.min(2 * d.r, (2 * d.r - 8) / this.getComputedTextLength() * 14) + "px";
        });
    const $itemOnFocus = $('#item-on-focus');
    node
        .on("mouseover", function(d) {
                $itemOnFocus.html(d.data.name + ": " + d.data.value + " models");
                $itemOnFocus.css("color", "#000000");
            });
    node
        .on("mouseout", function() {
            $itemOnFocus.css("color", "#e2e1e1");
        });
</g:javascript>

