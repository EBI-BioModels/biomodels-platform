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
    def specialCharacters = "([:+\\(\\)\\[\\]\\{\\}\\|\\*\\&\"\\?\'\\!\\^])"
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
                        String escapedFacetValue = fv.value.replaceAll("${specialCharacters}", '\\\\$1')
                        boolean isAsked = query.contains("${facet.id}:${escapedFacetValue}")
                        String fvSearchURL = "${grailsApplication.config.grails.serverURL}/search?query=${query}%20and%20${facet.id}%3A${escapedFacetValue}"
                    %>
                    <g:if test="${isAsked}">
                        <input type="checkbox" value="${fv.value}" checked 
				onchange="runFacetSearch($(this), '${facet.id}' ,'${escapedFacetValue}')">
                        <span class="facetLabel">${fv.label} (${fv.count})</span>
                    </g:if>
                    <g:else>
                        <input type="checkbox" value="${fv.value}" id="choosenFacetValue" 
				onchange="runFacetSearch($(this), '${facet.id}' ,'${escapedFacetValue}')">
                        <a href="${fvSearchURL}" class="facetLabel">
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

    function runFacetSearch(e, facetGroupId, facetValue) {
        var currentSearchURI = window.location.href;
        var newSearchURI = "${grailsApplication.config.grails.serverURL}/search?query="
        var entireQueryString = currentSearchURI.substring(currentSearchURI.search("=") + 1);
	var lastQueryString = "%20and%20" + facetGroupId + "%3A" + facetValue;
        if (e[0].checked) {
            entireQueryString += lastQueryString;
        } else {
            // remove the search term out the query string, update newSearchURI
            entireQueryString = entireQueryString.replace(lastQueryString, "");
        }
        newSearchURI += entireQueryString;
        window.location.href = newSearchURI;
    }
</g:javascript>
