<style type="text/css">
.tooltip {
    background: #eee;
    box-shadow: 0 0 5px #999999;
    color: #333;
    display: none;
    font-size: 13px;
    left: 160px;
    padding: 10px;
    position: absolute;
    text-align: center;
    top: 80px;
    width: 100px;
    z-index: 10;
}
</style>

<div id="curationStateChart" class="div-center-content">
</div>
<g:javascript>
    var dataset = [
        ["Manually curated", ${nbManuallyCurated}],
        ["Non-curated", ${nbNoncurated}]
    ];
    // console.log(dataset);
    var color = d3.scaleOrdinal(d3.schemeCategory20);
    var svg = d3.select("div#curationStateChart")
        .append("svg")
        .attr("width", width)
        .attr("height", height)
        .append("g")
        .attr("transform","translate("+width/2+","+height/2+")");

    var arc = d3.arc()
        .outerRadius(radius - 10)
        .innerRadius(radius - donutWidth);

    var pie = d3.pie()
        .value(function(d){return d[1];}); //sort the value to show from the 12 0'clock

    var gs = svg.selectAll(".arc")
        .data(pie(dataset))
        .enter()
        .append("g")
        .attr("class","arc"); //all arcs

    var paths = gs.append("path")
        .attr("d",arc)
        .style("fill",function(d){return color(d.data);});
    //add legend to the donut chart

    var legendSize = 15;
    var legendSpacing = 8; // 2
    var legend = svg.selectAll(".legend")
        .data(color.domain())
        .enter()
        .append("g")
        .attr("transform",function(d,i){
            var legendH = color.domain().length*(legendSize+legendSpacing);//total height of legends
            var legendY = i*(legendSize+legendSpacing) - legendH/2;//
            var legendX = -(legendSize + 60); // add 60 to move legend left
            return "translate("+legendX+","+legendY+")";
        });
    legend.append("rect")
        .attr("width",legendSize)
        .attr("height",legendSize)
        .attr("fill",color)
        .attr("stroke",color);
    legend.append("text")
        .text(function(d){return d[0];})
        .attr('x', legendSize + legendSpacing)
        .attr('y', legendSize - legendSpacing + 6); // 2 -- remove 6

    // add  tooltip to paths
    var tooltip = d3.select("#curationStateChart").append("div").attr("class","tooltip");
    tooltip.append("div").attr("class","name");
    tooltip.append("div").attr("class","count");
    tooltip.append("div").attr("class","percentage");

    var totalModels = d3.sum(dataset, d => d[1]);
    paths.on("mouseover", function(d) {
        // var total = d3.sum(dataset, d => d[1]);
        var percent = Math.round(1000 * d.data[1] / totalModels) / 10;
        tooltip.select(".name").html(d.data[0]);
        tooltip.select(".count").html(d.data[1]);
        tooltip.select(".percentage").html(percent + '%');
        tooltip.style("display", "block");
    });
    paths.on("mouseout", function(d){
        tooltip.style("display", "none");
    });
    paths.on("click", function(d) {
        // var coords = d3.mouse(this);
        var label = d["data"][0];
        var value = d["data"][1];
        var serverURL = "${grailsApplication.config.grails.serverURL}";
        var fixedSearchURL = "/search?domain=biomodels&query=*%3A*+AND+curationstatus%3A";
        var prefixSearchURL = serverURL + fixedSearchURL;
        var curationState = '\"' + label + '\"';
        var queryURL = prefixSearchURL + curationState;
        window.open(queryURL, '_blank');
    });
    /*window.addEventListener('resize', function (event) {
      $("#curationStateChart").width(window.innerWidth * 0.9);
      $("#curationStateChart").height(window.innerHeight);
    });*/
</g:javascript>
