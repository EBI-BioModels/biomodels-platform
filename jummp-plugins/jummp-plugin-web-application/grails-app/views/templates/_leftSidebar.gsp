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
<g:javascript>
    function escapeSpecialLuceneCharacters(facet_value) {
        facet_value = facet_value.replace(/\+/g, '\\+');
        facet_value = facet_value.replace(/\?/g, '\\?');
        facet_value = facet_value.replace(/\*/g, '\\*');
        facet_value = facet_value.replace(/\(/g, '\\(');
        facet_value = facet_value.replace(/\)/g, '\\)');
        facet_value = facet_value.replace(/\[/g, '\\[');
        facet_value = facet_value.replace(/\]/g, '\\]');
        facet_value = facet_value.replace(/\{/g, '\\{');
        facet_value = facet_value.replace(/\}/g, '\\}');
        facet_value = facet_value.replace(/\:/g, '\\:');
        facet_value = facet_value.replace(/\//g, '\\/');
        return facet_value;
    }
</g:javascript>
<g:if test="${models}">
    <g:if test="${actionName == 'search'}">
        <h4>Filter your results</h4>
        <g:each in="${facets}" var="facet" status="i">
            <div id="facetList${i}">
            <h5 style="padding-top: 5px">${facet.label}</h5>
            <% String idFacet = facet.label.replace(' ', '') %>
            <input type="text" id="txtSearch${idFacet}" placeholder="Find your ${facet.label}" class="searchEachFacet search" />
            <div class="facetContainer" id="facet${idFacet}">
                <ul id="${idFacet}" class="list">
                <g:each in="${facet.facetValues}" var="fv">
                    <li>
                    <%
                        String escapedFacetValue = fv.value.replaceAll("${specialCharacters}", '\\\\$1')
                        boolean isAsked = query.contains("${facet.id}:${escapedFacetValue}")
                        String newQuery = "${query} and ${facet.id}:${escapedFacetValue}"
                    %>
                    <g:if test="${isAsked}">
                        <input type="checkbox" id="facetValue_${fv.value}" value="${fv.value}" checked title="${fv.value}"
				            onchange="runFacetSearch($(this), '${facet.id}' ,'${escapedFacetValue}')">
                        <span class="facetLabel">${fv.label} (${fv.count})</span>
                    </g:if>
                    <g:else>
                        <input type="checkbox" value="${fv.value}" id="choosenFacetValue" title="${fv.value}"
				            onchange="runFacetSearch($(this), '${facet.id}' ,'${escapedFacetValue}')">
                        <g:link controller="search" action="search" params="${[query: newQuery]}" class="facetLabel">
                            <span class="facetLabel">${fv.label} (${fv.count})</span></g:link>
                    </g:else>
                    </li>
                </g:each>
                </ul>
                <g:javascript>
                    var nbFV = ${facet.facetValues.size()};
                    if (nbFV < 5) {
                        $("#facet${idFacet}").outerHeight(nbFV*29 + 20);
                        $("#facet${idFacet}").css("overflow-y", "hidden");
                        $("#txtSearch${idFacet}").hide();
                    }
                </g:javascript>
            </div>
            </div> <!-- facetList -->
        </g:each>
    </g:if>
    <g:elseif test="${actionName == 'list'}">
        <input id="filterModel" name="query" hidden/>
        <h4>Filter your models</h4>
        <g:each in="${facets}" var="facet" status="i">
            <div id="facetList${i}">
                <h5 style="padding-top: 5px">${facet.label}</h5>
                <% String idFacet = facet.label.replace(' ', '') %>
                <input type="text" id="txtSearch${idFacet}" placeholder="Find your ${facet.label}" class="searchEachFacet search" />
                <div class="facetContainer" id="facet${idFacet}">
                    <ul id="${idFacet}" class="list">
                        <g:each in="${facet.facetValues}" var="fv">
                            <li>
                                <%
                                    String escapedFacetValue = fv.value.replaceAll("${specialCharacters}", '\\\\$1')
                                    boolean isAsked = params.query?.contains("${facet.id}:${escapedFacetValue}")
                                    String newQuery = params.query
                				    if (query) { // rather: params.query
                                        newQuery += " or ${facet.id}:${escapedFacetValue}"
				                    } else {
                                        newQuery = "${facet.id}:${escapedFacetValue}"
				                    }
                                %>
                                <g:if test="${isAsked}">
                                    <input type="checkbox" id="facetValue_${fv.value}" value="${fv.value}" checked title="${fv.value}"
                                           onchange="runFacetList($(this), '${facet.id}' ,'${escapedFacetValue}')">
                                    <span class="facetLabel">${fv.label} (${fv.count})</span>
                                </g:if>
                                <g:else>
                                    <input type="checkbox" value="${fv.value}" id="choosenFacetValue" title="${fv.value}"
                                           onchange="runFacetList($(this), '${facet.id}' ,'${escapedFacetValue}')">
                                    <g:link controller="search" action="list" params="${[query: newQuery]}" class="facetLabel">
                                        <span class="facetLabel">${fv.label} (${fv.count})</span></g:link>
                                </g:else>
                            </li>
                        </g:each>
                    </ul>
                </div>
            </div>
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
        facetValue = escapeSpecialLuceneCharacters(facetValue);
	    var lastQueryString = "+and+" + encodeURIComponent(facetGroupId + ":" + facetValue);
        if (e[0].checked) {
            if ("${params.sort}") {
                entireQueryString = entireQueryString.replace("&sort=${params.sort}", "");
                entireQueryString += lastQueryString + "&sort=${params.sort}";
            } else {
                entireQueryString += lastQueryString;
            }
        } else {
            // remove the search term out the query string, update newSearchURI
            entireQueryString = entireQueryString.replace(lastQueryString, "");
        }
        newSearchURI += entireQueryString;
        window.location.href = newSearchURI;
    }

    function runFacetList(e, facetGroupId, facetValue) {
        var entireQueryString = $("#filterModel").val();
        facetValue = escapeSpecialLuceneCharacters(facetValue);
	    var lastQueryString = encodeURIComponent(facetGroupId + ":" + facetValue);
        if (e[0].checked) {
            if (entireQueryString == "") {
                entireQueryString = lastQueryString;
            } else {
                entireQueryString += "+or+" + lastQueryString;
            }
        } else {
            // remove the search term out the query string
            entireQueryString = entireQueryString.replace(lastQueryString, "");
        }
        $("#filterModel").val(entireQueryString);
        var newSearchURI = "${grailsApplication.config.grails.serverURL}/models"
        if (entireQueryString != "") {
            newSearchURI += "?query=" + entireQueryString;
        }
        window.location.href = newSearchURI;
    }
</g:javascript>
