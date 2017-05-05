<%@
    page contentType="text/html;charset=UTF-8"
    expressionCodec = "none"
%>
<g:if test="${models}">
    <g:if test="${actionName == 'search'}">
        <div class="element">
            <h3>Make descriptive statistics</h3>
            <div id="globalchart">
                <svg width="375" height="375"></svg>
            </div>
            <script src="https://d3js.org/d3.v4.min.js"></script>
            <g:javascript>
                var svg = d3.select("svg"),
                    width = +svg.attr("width"),
                    height = +svg.attr("height"),
                    radius = Math.min(width, height) / 2,
                    g = svg.append("g").attr("transform", "translate(" + width / 2 + "," + height / 2 + ")");

                var color = d3.scaleOrdinal(["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"]);

                var pie = d3.pie()
                    .sort(null)
                    .value(function(d) { return d.total; });

                var path = d3.arc()
                    .outerRadius(radius - 10)
                    .innerRadius(0);

                var label = d3.arc()
                    .outerRadius(radius - 40)
                    .innerRadius(radius - 40);

                /*d3.csv("data.csv", function(d) {
                    d.population = +d.population;
                    return d;
                }, function(error, data) {
                    if (error) throw error;

                    var arc = g.selectAll(".arc")
                        .data(pie(data))
                        .enter().append("g")
                        .attr("class", "arc");

                    arc.append("path")
                        .attr("d", path)
                        .attr("fill", function(d) { return color(d.data.age); });

                    arc.append("text")
                        .attr("transform", function(d) { return "translate(" + label.centroid(d) + ")"; })
                        .attr("dy", "0.35em")
                        .text(function(d) { return d.data.age; });
                });*/

                var strData = "age,population\n<5,2704659\n5-13,4499890\n14-17,2159981\n18-24,3853788\n25-44,14106543\n45-64,8819342\n≥65,612463";
                var data = d3.csvParse(strData, function(d){
                   return {
                       age: d.age,
                       population: parseInt(d.population, 10)
                   }
                });
                console.log(data);
                var facetsData = {"facets": {label: "", total: 0}}
                facetsData.facets = [];
                var data0 = d3.entries(${facetStats});
                data0.forEach(function(d) {
                    // console.log(d.value["label"], d.value["total"]);
                    facetsData.facets.push({'label': d.value["label"] + "(" + d.value["total"] + ")", 'total': d.value["total"]})
                    // JSON.stringify()
                    // return {
                    //     d.label = d.value["label"],
                    //     d.total = d.value["total"]
                    // }
                });
                console.log(facetsData.facets);
                var data1 = d3.entries(facetsData.facets);
                console.log(data1);
                data = facetsData.facets;
                var arc = g.selectAll(".arc")
                        .data(pie(data))
                        .enter().append("g")
                        .attr("class", "arc");

                arc.append("path")
                    .attr("d", path)
                    .attr("fill", function(d) { return color(d.data.label); });

                arc.append("text")
                    .attr("transform", function(d) { return "translate(" + label.centroid(d) + ")"; })
                    .attr("dy", "0.35em")
                    .text(function(d) { return d.data.label; });
            </g:javascript>
        </div>
    </g:if>
    <g:elseif test="${actionName == 'list'}">
        <g:if test="${history}">
            <div class="element" id="sidebar-element-last-accessed-models">
                <h3><g:message code="model.history.title"/></h3>
                <ul>
                    <g:each in="${history}">
                        <li><a href="${createLink(controller: "model", action: "show", id: it.publicationId ?: it.submissionId)}">${it.name}</a><br/>
                            <g:message code="model.history.submitter"/>${it.submitter}</li>
                    </g:each>
                </ul>
            </div>
        </g:if>
    <%--  GoTree code, disabled until it is useful again.
    <div class="element">
        <h2>Gene Ontology Tree</h2>
        <h3>Browse models using GO Tree</h3>
        <p>This is a tree view of the models in this Database based on <a href="http://www.geneontology.org/">Gene Ontology</a>.</p>
        <p><g:link controller="gotree">link</g:link></p>
    </div> --%>
    </g:elseif>
</g:if>
<g:else>
    <p></p>
</g:else>
