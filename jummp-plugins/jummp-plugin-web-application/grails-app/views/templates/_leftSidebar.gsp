<%@
    page import="grails.converters.JSON"
    contentType="text/html;charset=UTF-8"
    expressionCodec="html"
%>
<%
    // create the list of indices using for the javascript at the bottom
    def listOfFacets = []
    facets.eachWithIndex {value, index ->
        listOfFacets.add(index)
    }
    def specialCharacters = "([:+\\(\\)\\[\\]\\{\\}\\|\\*\\&\"\\?\'\\!\\^])"
    def FACETS_WRAPPED_DOUBLE_QUOTE = ["curationstatus", "modelformat", "disease", "modellingapproach", "modelflag"]
%>
<g:if test="${models}">
<div class="sidebar">
    <h4 style="color: forestgreen; font-weight: bold">Filter results</h4>
    <g:each in="${facets}" var="facet" status="i">
        <div id="facetList${i}">
        <h5 style="padding-top: 5px">${facet.label}</h5>
        <% String idFacet = facet.label.replace(' ', '') %>
        <input type="text" id="txtSearch${idFacet}" placeholder="Find your ${facet.label}"
               class="searchEachFacet search" />
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
                        isAsked = query?.contains("${facet.id}:${escapedFacetValue}")
                    }
                %>
                <g:if test="${isAsked}">
                    <input type="checkbox" id="facetValue_${fv.value}" checked
                           value="${fv.value}" title="${fv.value}"
                           onchange="${jsMethod}($(this), '${facet.id}' ,'${escapedFacetValue}')">
                    <span class="facetLabel" onclick="${jsMethod}($(this), '${facet.id}' ,'${escapedFacetValue}')">
                        <g:render template="/templates/singleFacetShow" model="[fv: fv, method: jsMethod]" /></span>
                </g:if>
                <g:else>
                    <%
                        String newQuery = ""
                        def newParams = [:]
                        if (actionName.equalsIgnoreCase("search")) {
                            newQuery = query? "${query} AND ${selectedFacet}" : "${selectedFacet}"
                            if (params.domain) {
                                newParams['domain'] = params.domain
                            } else {
                                newParams['domain'] = 'biomodels'
                            }
                        } else {
                            newQuery = selectedFacet
                        }

                        if (query) {
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
                    <input type="checkbox" id="choosenFacetValue"
                           value="${fv.value}" title="${fv.value}"
                           onchange="${jsMethod}($(this), '${facet.id}' ,'${escapedFacetValue}')">
                    <g:link controller="search" action="${actionName}" params="${newParams}" class="facetLabel">
                        <span class="facetLabel">
                            <g:render template="/templates/singleFacetShow" model="[fv: fv, method: jsMethod]" />
                        </span>
                    </g:link>
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
</div>
</g:if>
<g:else>
    <p></p>
</g:else>

<script src="//cdnjs.cloudflare.com/ajax/libs/list.js/1.5.0/list.min.js"></script>

<g:javascript>
    let options = {
        valueNames: ['facetLabel'] // add css classes associated with the elements that you want to search in
    };
    let facetList = [];
    for (let i = 0; i< ${listOfFacets.size()}; i++) {
        facetList[i] = new List('facetList'+i, options);
    }

    function runFacetSearch(e, facetGroupId, facetValue) {
        let newSearchURI = "${grailsApplication.config.grails.serverURL}/search?query="
        let isNeededDQ = "${FACETS_WRAPPED_DOUBLE_QUOTE}".indexOf(facetGroupId) > -1
        if (isNeededDQ) {
            facetValue = '"' + facetValue + '"';
        }
        let lastQueryString = " AND " + facetGroupId + ":" + facetValue;
        let currentQuery = "${query}";
        if (e[0].checked) {
            currentQuery += lastQueryString;
        } else {
            // remove the search term out the query string, update newSearchURI
            currentQuery = currentQuery.replace(lastQueryString, "");
        }
        let otherParams = "domain=${params.domain}";
        if ("${params.offset}") {
            otherParams += "&offset=${params.offset}";
        }
        if ("${params.numResults}") {
            otherParams += "&numResults=${params.numResults}";
        }
        if ("${params.sort}") {
            otherParams += "&sort=${params.sort}";
        }
        newSearchURI += encodeURIComponent(currentQuery) + "&" + otherParams;
        window.location.href = newSearchURI;
    }

    function runFacetList(e, facetGroupId, facetValue) {
        let currentQueryString = "${query?.replaceAll('"', '\\\\"')}";
        facetValue = escapeSpecialLuceneCharacters(facetValue);
        /* the above utility function is defined in common.js which is already included in the footer section */
        let latestQueryString = facetGroupId + ":" + facetValue;
        if (e[0].checked) {
            // choose and click on a single facet
            if (currentQueryString === "") {
                currentQueryString = latestQueryString;
            } else {
                if (currentQueryString.indexOf(facetGroupId) >= 0) {
                    // another facet in the same group has been chosen
                    // we provide a single solution for now: replace the previous selection
                    // deal with 'OR' operator later on
                    currentQueryString = latestQueryString;
                } else {
                    // deal with the multiple criteria (AND operator) from different facet groups later on
                    //currentQueryString += " AND " + latestQueryString;
                }
            }
        } else {
            // remove the newly selected search term out the query string
            // why don't we need to check empty of the currentQueryString?
            currentQueryString = currentQueryString.replace(latestQueryString, "");
        }
        let otherParams = "";
        if ("${params.offset}") {
            otherParams += "&amp;offset=${params.offset}";
        }
        if ("${params.numResults}") {
            otherParams += "&amp;numResults=${params.numResults}";
        }
        if ("${params.sort}") {
            otherParams += "&amp;sort=${params.sort}";
        }
        let newSearchURL = "${grailsApplication.config.grails.serverURL}/models"
        currentQueryString = encodeURIComponent(currentQueryString);
        let newParams = "?";
        if (currentQueryString) {
            newParams += "query=" + currentQueryString + otherParams;
        } else {
            // eliminate the first HTML-encoded '&' (&amp;)
            newParams += otherParams.substr(5);
        }
        window.location.href = newSearchURL + newParams;
    }
</g:javascript>
