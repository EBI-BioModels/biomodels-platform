<%@
    page import="grails.converters.JSON"
    contentType="text/html;charset=UTF-8"
    expressionCodec="none"
%>
<%
    // create the list of indices using for the javascript at the bottom
    def listOfFacets = []
    facets.eachWithIndex {value, index ->
        listOfFacets.add(index)
    }
%>

<g:if test="${models}">
    <g:if test="${actionName == 'search'}">
        <h4>Filter your results</h4>
        <g:each in="${facets}" var="facet" status="i">
            <div id="facetList${i}">
            <h5 style="padding-top: 5px">${facet.label}</h5>
            <input type="text" placeholder="Find your ${facet.label}" class="searchEachFacet search" />
            <div class="facetContainer" id="${facet.label}">
                <ul id="${facet.label.replace(' ', '')}" class="list">
                <g:each in="${facet.facetValues}" var="fv">
                    <li>
                    <%
                        boolean isAsked = query.contains("${facet.id}:${fv.value}")
                    %>
                    <g:if test="${isAsked}">
                        <input type="checkbox" value="${fv.value}" checked onchange="runFacetSearch($(this), '${facet.id}' ,'${fv.value}')">
                        <span class="facetLabel">${fv.label} (${fv.count})</span>
                    </g:if>
                    <g:else>
                        <input type="checkbox" value="${fv.value}" id="choosenFacetValue" onchange="runFacetSearch($(this), '${facet.id}' ,'${fv.value}')">
                        <a href="${grailsApplication.config.grails.serverURL}/search?query=${query} and ${facet.id}:${fv.value}" class="facetLabel">
                            <span class="facetLabel">${fv.label} (${fv.count})</span></a>
                    </g:else></li>
                </g:each>
                </ul>
            </div>
            </div> <!-- facetList -->
        </g:each>
    </g:if>
    <g:elseif test="${actionName == 'list'}">
        <h3>Filter your models</h3>
        <g:each in="${facets}" var="facet">
            <h4>${facet}</h4>
            <p>Will be realised soon!!!</p>
        </g:each>
    </g:elseif>
</g:if>
<g:else>
    <p></p>
</g:else>

<script src="http://listjs.com/assets/javascripts/list.min.js"></script>

<g:javascript>
    var options = {
        valueNames: ['facetLabel'] // add css classes associated with the elements that you want to search in
    };
    var facetList = [];
    $.each(${listOfFacets}, function(index, element) {
        facetList[index] = new List('facetList'+index, options);
    });

    function runFacetSearch(e, facetGroupLabel, facetValue) {
        var baseURI = e[0].baseURI;
        var newSearchURI = decodeURI(baseURI)
        if (e[0].checked) {
            newSearchURI += " and " + facetGroupLabel + ":" + facetValue;
            console.log(newSearchURI);

        } else {
            // remove the term out the query string, update newSearchURI
            newSearchURI = newSearchURI.replace(" and " + facetGroupLabel + ":" + facetValue, "");
        }
        window.location.href = newSearchURI;
    }
</g:javascript>
