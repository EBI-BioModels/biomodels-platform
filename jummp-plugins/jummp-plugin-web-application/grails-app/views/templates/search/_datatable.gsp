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
	<div class="view view-dom-id-9c00a92f557689f996511ded36a88594">
        <div class="view-content">
            <div id="inline-list">
                <g:if test="${action == "list"}">
                    <sec:ifLoggedIn>
                        <a href="${createLink(controller: "search", action: "archive")}">Browse Archived Models</a>
                    </sec:ifLoggedIn>
                </g:if>
                <g:if test="${models}">
                    <ul>
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
                    </ul>
                </g:if>
            </div>
            <g:if test="${models}">
	 			<table id="modelTable">
    	    	<thead>
                <tr>
                    <g:render template="/templates/search/tableheader" model="[action: action, 'sortColumn': 'name', 'msgCode':'model.list.name']"/>
                    <g:render template="/templates/search/tableheader" model="[action: action, 'sortColumn': 'format', 'msgCode':'model.list.format']"/>
                    <g:render template="/templates/search/tableheader" model="[action: action, 'sortColumn': 'submitter', 'msgCode':'model.list.submitter']"/>
                    <g:render template="/templates/search/tableheader" model="[action: action, 'sortColumn': 'submitted', 'msgCode':'model.list.submissionDate']"/>
                    <g:render template="/templates/search/tableheader" model="[action: action, 'sortColumn': 'modified', 'msgCode':'model.list.modifiedDate']"/>
                    <th>Certification</th>
                    <sec:ifLoggedIn>
                        <th>Status</th>
    				</sec:ifLoggedIn>
                </tr>
                </thead>
                <tbody>
                	<g:each status="i" in="${models}" var="model">
                		<tr class="${ (i % 2) == 0 ? 'even' : 'odd'}">
                			<td>
                				<a href="${createLink(controller: 'model', id: model.publicationId ?: model.submissionId, action: 'show')}">
                					${model.name}
                				</a>
                			</td>
                			<td>${model.format.name}</td>
                			<td>${model.submitter}</td>
                			<td>${model.submissionDate.format('yyyy/MM/dd')}</td>
                			<td>${model.lastModifiedDate.format('yyyy/MM/dd')}</td>
                            <td style="text-align: center;">
                                    <jummp:renderStarLevels flag="${model.flagLevel}"/></td>
                            <sec:ifLoggedIn>
								<td style="text-align: center;">
									<g:if test="${model.state == ModelState.PUBLISHED}">
										<img style="width:14px" title="Published" alt="public model" src="${grailsApplication.config.grails.serverURL}/images/unlock.png"/>
									</g:if>
									<g:else>
										<img style="width:12px" title="Unpublished" alt="unpublished model" src="${grailsApplication.config.grails.serverURL}/images/lock.png"/>
										<g:if test="${model.submitterUsername.toString() == sec.username().toString() }">
											<img style="width:14px" title="You own this model" alt="own model" src="${grailsApplication.config.grails.serverURL}/images/meetus.png"/>
										</g:if>
										<g:else>
											<img style="width:14px" title="This model is shared with you" alt="shared model" src="${grailsApplication.config.grails.serverURL}/images/Face_to_Face.png"/>
										</g:else>
									</g:else>
								</td>
                			</sec:ifLoggedIn>
                		</tr>
                	</g:each>
                </tbody>
                <tfoot>
                	<tr>
                	</tr>
                </tfoot>
                </table>
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
    </div>
</div>
