<%@ page import="net.biomodels.jummp.core.model.ModelState"%>
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
            <div class="small-12 large-12 columns">
                <div id="inline-list">
                <g:if test="${action == "list"}">
                    <sec:ifLoggedIn>
                        <a href="${createLink(controller: "search", action: "archive")}">Browse Archived Models</a>
                    </sec:ifLoggedIn>
                </g:if>

                <ul class="float-right">
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
        <div class="grid_18 omega" id="search-results">
            <section>
                <div class="result-category">
                    <h3>
                        <input type="checkbox" class="checkall" />
                        <a href="#">BioModels</a></h3>

                    <div class="column row result">
                    <g:each status="i" in="${models}" var="model">
                        <div class="result-detail">
                            <h4>
                                <input class="export-selection" type="checkbox" value="${model.submissionId}">
                                <a href="${createLink(controller: 'model', id: model.publicationId ?: model.submissionId, action: 'show')}">
                                ${model.name}
                            </a></h4>
                            <span class="fieldName"></span>
                            <p>${model.description}</p>
                        </div>
                        <div class="result-meta">
                            <div class="entry_actions_panel">
                                <div class="entry_actions">
                                    <p class="entry_actions_menu">dsdsadasdasd</p>
                                    <p class="entry-source">dsdsadasdasd</p>
                                </div>
                            </div>
                        </div>
                    %{--<span>${model.format.name}</span>
                    <span>${model.submitter}</span>
                    <span>${model.submissionDate.format('yyyy/MM/dd')}</span>
                    <span>${model.lastModifiedDate.format('yyyy/MM/dd')}</span>--}%
                    </g:each>
                    </div>

                </div>
            </section>
        </div>

        <div class="column row">
            <hr style="border-color: #00aaaa; border-width: 5px;"/>
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
    </g:if>
    <g:else>
        <g:if test="${matches != null}">
            <p>No available models matched your query. Please try logging in to access more models, or another search query.</p>
        </g:if>
        <g:else>
            <p>No available models matched your query. Please try logging in to access more models, or another search query.</p>
        </g:else>
    </g:else>
</div>
