<style type="text/css">
.tooltip2 {
    background: #eee;
    box-shadow: 0 0 5px #999999;
    color: #333;
    display: none;
    font-size: 20px;
    left: 100px;
    /*padding: 10px;*/
    position: absolute;
    text-align: center;
    bottom: 1px;
    width: 250px;
    z-index: 10;
}
    .biggerText {
        font-size: 18px;
    }
</style>

<div id="modellingApproachesChart" class="div-center-content"></div>
<g:javascript>
    var data = ${approaches};
    // console.log(data);
    var color = d3.scaleOrdinal(d3.schemeCategory20);
    var svg = d3.select("div#modellingApproachesChart")
        .append("svg")
        .attr("width", width)
        .attr("height", height)
        .append("g")
        .attr("transform", "translate("+width/2+","+height/2+")");

    var arc = d3.arc()
        .outerRadius(radius - 10)
        .innerRadius(radius - donutWidth);

    var labelArc = d3.arc()
        .outerRadius(radius - 40)
        .innerRadius(radius - 40);

    var pie = d3.pie()
        .sort(null)
        .value(function (d) {
            return d["count"]; });

    var g = svg.selectAll(".arc")
        .data(pie(data))
        .enter()
        .append("g")
        .attr("class", "arc");

    var path = g.append("path")
        .attr("d", arc)
        .style("fill", function (d) {
            return color(d.data["label"]);
        });

    var legendSize = 15;
    var legendSpacing = 8; // 2
    var legend = svg.selectAll(".legend")
        .data(color.domain())
        .enter()
        .append("g")
        .attr("transform",function(d,i){
            var legendH = color.domain().length*(legendSize+legendSpacing);//total height of legends
            var legendY = i*(legendSize+legendSpacing) - legendH/2;//
            var legendX = -(legendSize + 90); // add 60 to move legend left
            return "translate("+legendX+","+legendY+")";
        });
    legend.append("rect")
        .attr("width",legendSize)
        .attr("height",legendSize)
        .attr("fill",color)
        .attr("stroke",color);
    legend.append("text")
        .text(function(d){console.log(d); return d;})
        .attr('x', legendSize + legendSpacing)
        .attr('y', legendSize - legendSpacing + 6); // 2 -- remove

    g.append("text")
        .attr("transform", function (d) {
            return "translate(" + labelArc.centroid(d) + ")";
        })
        .attr("text-anchor", "middle")
        .attr("fill", "white")
        .style("font-size", "1.5em")
        .text(function (d) {
            return d.data["count"];
        });

    var tooltip2 = d3.select("div#modellingApproachesChart")
        .append("div")
        .attr("class", "tooltip2");
    tooltip2.append("div").attr("class", "label");
    tooltip2.append("div").attr("class", "count");
    path.on("mouseover", function(d) {
        tooltip2.select(".label").html(d.data["label"]);
        tooltip2.select(".count").html(d.data["count"]);
        tooltip2.style("display", "block");
        $('#item-on-focus').html(d.data["label"] + ": " + d.data["count"] + " models");
    });
    path.on("mouseout", function(d) {
        tooltip2.style("display", "none");
    });
    path.on("click", function (d) {
        var coords = d3.mouse(this);
        var label = d["data"]["label"];
        var count = d["data"]["count"];
        var serverURL = "${grailsApplication.config.grails.serverURL}";
        var fixedSearchURL = "/search?domain=biomodels&query=*%3A*+AND+modellingapproach%3A";
        var prefixSearchURL = serverURL + fixedSearchURL;
        var modellingapproach = '\"' + label + '\"';
        var queryURL = prefixSearchURL + modellingapproach;
        window.open(queryURL, '_blank');
    });
    /*window.addEventListener('resize', function (event) {
        var innerWidth = $('.orbit').width;
        var innerHeight = $('.orbit').height;
        $("#modellingApproachesChart").width(innerWidth * 0.9);
        $("#modellingApproachesChart").height(innerHeight);
    });*/
</g:javascript>
