<%@
    page import="net.biomodels.jummp.core.model.ModelState"
%>

<%
    def totalCount
    if (matches) {
        totalCount = matches
    }
    else {
        totalCount = modelsAvailable
    }
    def imagePath = "/images"
    def resultOptions = net.biomodels.jummp.webapp.Preferences.getOptions("numResults")
    resultOptions = resultOptions.reverse()
%>
<div class="content">
    <g:if test="${models}">
        <div class="row">
            <div class="small-12 medium-12 large-12 columns">
                <div id="inline-list">
                <g:if test="${action == "list"}">
                    <sec:ifLoggedIn>
                        <a href="${createLink(controller: "search", action: "archive")}">Browse Archived Models</a>
                    </sec:ifLoggedIn>
                </g:if>
                <g:else>
                    <g:if test="${params.flashMessage}">
                        <div class="alert warning">
                            <span class="closebtn" onclick="this.parentElement.style.display='none';">&times;</span>
                            <h5>${params.flashMessage}</h5>
                        </div>
                    </g:if>
                    <span>Search terms: </span><span id="searchString" style="font-weight: bolder"></span>
                </g:else>
                <ul class="float-right" style="margin-right: 14px">
                    <g:each in="${resultOptions}">
                        <li>
                            <g:if test="${it == length}">
                                ${it}
                            </g:if>
                            <g:else>
                                <a href="${createLink(controller: 'search', action: action,
                                    params: [query: query,  sortDir: sortDirection,
                                             sortBy: sortBy, offset: 0, numResults: it])}">
                                    ${it}
                                </a>
                            </g:else>
                        </li>
                    </g:each>
                    <li>Page size </li>
                </ul>
                </div>
            </div>
        </div>
        <div class="row grid_18 omega" id="search-results">
            <section>
                <div class="modelList">
                    <div class="column row">
                        <h3>Found: ${totalCount} ${totalCount > 1 ? 'models' : 'model'}</h3>
                    </div>
                    <div class="column row">
                    <g:each status="i" in="${models}" var="model">
                    <div class="column row modelPlaceHolder">
                        <div class="small-12 medium-12 large-12 columns">
                            <%
                                def modelUrl = createLink(controller: 'model', id: model.publicationId ?: model.submissionId, action: 'show')
                                def description = model.description ?: ""
                                int maxNumChar = 255
                                boolean haveMoreDetails = description.length() > maxNumChar
                                def descriptionShown = description
                                def moreDetails = ""
                                if (haveMoreDetails) {
                                    moreDetails = "<a href=${modelUrl}>... See more</a>"
                                    descriptionShown = description.substring(1,maxNumChar) + moreDetails
                                }
                                // TODO: deal with HTML elements
                                descriptionShown = description
                            %>
                            %{--<input class="export-selection" type="checkbox" value="${model.submissionId}">--}%
                            <h4>
                                <a href="${modelUrl}">${model.name}</a><br/>
                                <span style="font-size: small; margin: -25px 0;">
                                ID: ${model.publicationId ?: model.submissionId} |
                                Format: ${model.format.name} |
                                Submitter: ${model.submitter} |
                                Uploaded date: ${model.submissionDate.format('yyyy/MM/dd')} |
                                Last modified date: ${model.lastModifiedDate.format('yyyy/MM/dd')}
                                </span>
                            </h4>
                            %{--<span id="modelDescription"></span>
                            <p style="font-size: 90%; margin-bottom: 0.5%">${descriptionShown}</p>--}%
                        </div>
                    </div>
                    </g:each>
                    </div>
                    <g:javascript>
                        // reduce font-size of model's notes (i.e. model description)
                        $('[class*="dc:"]').css("font-size", "90%");
                        // show the query string on local search box and string query division at the top of main content division
                        $(document).ready(function() {
                            $('#local-searchbox').val("${query}");
                            $('#searchString').text("${query}");
                        });
                    </g:javascript>
                </div>
            </section>
        </div>

        <%
            int currentPage = 1
            if (offset != 0) {
                currentPage = Math.ceil((double) (offset + 1) / (double) length)
            }
            int modelStart = 1 + (currentPage - 1)*length
            int modelEnd = length < models.size() ? length : models.size()
            modelEnd += modelStart - 1
            int numPages = Math.ceil((double) totalCount / (double) length)
            int stepPagination = 5 // the number of pages would be displayed
            if (numPages < stepPagination) {
                stepPagination = numPages
            }
            int rightPage = currentPage + stepPagination
            if (currentPage + stepPagination > numPages) {
                rightPage = numPages + 1
            }
            int leftPage = currentPage
        %>
        <div class="row" style="background-color: #00aaaa; margin-top: 3px">
        <div class="dataTables_info">
            Showing ${modelStart} to ${modelEnd} of ${totalCount} models
        </div>
        <div class="dataTables_paginate">
            <g:if test="${currentPage != 1 && numPages > stepPagination}">
                <a href="${createLink(controller: 'search', action: action,
                    params: [query: query, sortDir: sortDirection, sortBy: sortBy, offset: 0, numResults: length])}">First</a>
            </g:if>
            <g:else>
                First
            </g:else>
            <g:if test="${currentPage == 1 || numPages <= stepPagination}">
                <g:img dir="${imagePath}/pagination" absolute="true" contextPath="" file="arrow-previous-disable.gif" alt="Previous"/>
            </g:if>
            <g:else>
                <a href="${createLink(controller: 'search', action: action,
                    params: [query: query, sortDir: sortDirection, sortBy: sortBy, offset: modelStart-length-1, numResults: length])}">
                    <g:img dir="${imagePath}/pagination" absolute="true"  contextPath="" file="arrow-previous.gif" alt="Previous"/>
                </a>
            </g:else>
            <g:if test="${currentPage + stepPagination >= numPages}">
                <%
                    // regulate the leftPage when jumping to the last page
                    rightPage = numPages + 1
                    leftPage = rightPage - stepPagination
                %>
            </g:if>
            <g:each var="i" in="${ (leftPage..<rightPage) }">
                <span class="pageNumbers">
                    <g:if test="${currentPage == i}">
                        ${i}
                    </g:if>
                    <g:else>
                        <a href="${createLink(controller: 'search', action: action,
                            params: [query: query,  sortDir: sortDirection, sortBy: sortBy, offset: (i - 1)*length, numResults: length])}">
                            ${i}
                        </a>
                    </g:else>
                </span>
            </g:each>
            <g:if test="${modelEnd == totalCount || numPages <= stepPagination}">
                <g:img dir="${imagePath}/pagination" absolute="true"  contextPath="" file="arrow-next-disable.gif" alt="Next"/>
            </g:if>
            <g:else>
                <a href="${createLink(controller: 'search', action: action,
                    params: [query: query,  sortDir: sortDirection, sortBy: sortBy, offset: modelStart+length-1, numResults: length])}">
                    <g:img dir="${imagePath}/pagination" absolute="true"  contextPath="" file="arrow-next.gif" alt="Next"/>
                </a>
            </g:else>
            <g:if test="${currentPage != numPages && numPages > stepPagination}">
                <a href="${createLink(controller: 'search', action: action,
                    params: [query: query, sortDir: sortDirection, sortBy: sortBy, offset: length*(numPages-1), numResults: length])}">Last</a>
            </g:if>
            <g:else>
                Last
            </g:else>
        </div>
        </div>
    </g:if>
    <g:else>
        <g:if test="${matches != null}">
            <p>No available models matched your query. Please try logging in to access more models, or another search query.</p>
        </g:if>
        <g:else>
            <p>No available models matched your query. Please try logging in to access more models, or another search query.</p>
        </g:else>
        <div class="alert info">
            <span class="closebtn" onclick="this.parentElement.style.display='none';">&times;</span>
            <h5>Please also check the syntax of your search terms.</h5>
        </div>
    </g:else>
</div>
