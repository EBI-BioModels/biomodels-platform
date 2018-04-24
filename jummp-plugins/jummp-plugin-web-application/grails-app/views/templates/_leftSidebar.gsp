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
    def FACETS_WRAPPED_DOUBLE_QUOTE = ["curationstatus", "modelformat", "disease", "modellingapproach", "modelflag"]
    String queryString = params.query?.replaceAll('"', '\\\\"')
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
    <h4>Filter your results</h4>
    <g:if test="${actionName == 'list'}">
        <input id="filterModel" name="query" hidden/>
    </g:if>
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
                    String escapedFacetValue = fv.value?.replaceAll("${specialCharacters}", '\\\\$1')
                    boolean isAsked
                    String selectedFacet
                    String jsMethod = "runFacetList"
                    if (actionName.equalsIgnoreCase('search')) {
                        selectedFacet = "${facet.id}:${fv.value}"
                        boolean isNeededDQ = facet.id in FACETS_WRAPPED_DOUBLE_QUOTE
                        if (isNeededDQ) {
                            selectedFacet = "${facet.id}:\"${fv.value}\""
                        }
                        isAsked = query.contains(selectedFacet)
                        jsMethod = "runFacetSearch"
                    } else {
                        selectedFacet = "${facet.id}:${fv.value}"
                        isAsked = params.query?.contains("${facet.id}:${escapedFacetValue}")
                    }

                %>
                <g:if test="${isAsked}">
                    <input type="checkbox" id="facetValue_${fv.value}"
                           value="${fv.value}" checked title="${fv.value}"
                           onchange="${jsMethod}($(this), '${facet.id}' ,'${escapedFacetValue}')">
                    <span class="facetLabel" onclick="${jsMethod}($(this), '${facet.id}' ,'${escapedFacetValue}')">
                        ${fv.label} (${fv.count})</span>
                </g:if>
                <g:else>
                    <%
                        String newQuery = query ? "${query} AND ${selectedFacet}" : "${selectedFacet}"
                        def newParams = [:]
                        if (params.query) {
                            newParams["query"] = newQuery
                        } else {
                            newParams["query"] = "${selectedFacet}"
                        }
                        if (params.offset) {
                            newParams["offset"] = params.offset
                        }
                        if (params.numResults) {
                            newParams["numResults"] = params.numResults
                        }
                        if (params.sort) {
                            newParams["sort"] = params.sort
                        }
                    %>
                    <input type="checkbox" value="${fv.value}" id="choosenFacetValue" title="${fv.value}"
                        onchange="${jsMethod}($(this), '${facet.id}' ,'${escapedFacetValue}')">
                    <g:link controller="search" action="${actionName}" params="${newParams}" class="facetLabel">
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
<g:else>
    <p></p>
</g:else>

<script src="//cdnjs.cloudflare.com/ajax/libs/list.js/1.5.0/list.min.js"></script>

<g:javascript>
    var options = {
        valueNames: ['facetLabel'] // add css classes associated with the elements that you want to search in
    };
    var facetList = [];
    $.each(${listOfFacets}, function(index, element) {
        facetList[index] = new List('facetList'+index, options);
    });

    function runFacetSearch(e, facetGroupId, facetValue) {
        var newSearchURI = "${grailsApplication.config.grails.serverURL}/search?query="
        var isNeededDQ = "${FACETS_WRAPPED_DOUBLE_QUOTE}".indexOf(facetGroupId) > -1
        if (isNeededDQ) {
            facetValue = '"' + facetValue + '"';
        }
	    var lastQueryString = " AND " + facetGroupId + ":" + facetValue;
	    var currentQuery = "${queryString}";
        if (e[0].checked) {
            currentQuery += lastQueryString;
        } else {
            // remove the search term out the query string, update newSearchURI
            currentQuery = currentQuery.replace(lastQueryString, "")
        }
        var otherParams = "";
        if ("${params.offset}") {
            otherParams += "&offset=${params.offset}";
        }
        if ("${params.numResults}") {
            otherParams += "&numResults=${params.numResults}";
        }
        if ("${params.sort}") {
            otherParams += "&sort=${params.sort}";
        }
        newSearchURI += encodeURIComponent(currentQuery) + otherParams;
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
