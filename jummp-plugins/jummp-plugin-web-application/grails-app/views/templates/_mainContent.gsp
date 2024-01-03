<%@ page import="net.biomodels.jummp.core.model.ModelState" %>
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
    if (!params.sort) {
        params.sort = "relevance-desc"
    }
    def domain = params.domain
    if (!domain) {
        domain = "biomodels"
    }
    String queryString = query?.replaceAll('([^\\\\])"', '$1\\\\"')
%>
<div class="content">
    <g:if test="${models}">
        <div id="inline-list" class="row" style="margin-top: 10px;">
            <div class="small-12 medium-12 large-6 columns" id="sorting">
                <!-- Show Sort by box on the search page only for now-->
                <g:if test="${action == "search"}">
                    <g:render template="/templates/sorting" />
                </g:if>
            </div>
            <div class="small-12 medium-12 large-6 columns">
                <g:render template="/templates/pageSize"
                          model="[resultOptions: resultOptions, length: length,
                                  action: action, query: query, domain: domain]"/>
            </div>
        </div>
        <div class="row">
                <g:if test="${action == "list"}">

                </g:if>
                <g:else>
                    <g:if test="${params.flashMessage}">
                        <div class="alert warning">
                            <span class="closebtn" onclick="this.parentElement.style.display='none';">&times;</span>
                            <h5>${params.flashMessage}</h5>
                        </div>
                    </g:if>
                    <span id="flashMessage"></span>
                    <span style="font-size: 85%">Search terms: </span>
                    <span id="searchString" style="font-weight: bolder; font-size: 85%"></span>
                    <span id="resetSearch" style="margin-left: 1em; font-size: 85%"></span>
                </g:else>
        </div>
        <div class="row grid_18 omega" id="search-results">
            <section>
                <div class="modelList">
                    <div class="column row">
                        <g:render template="/templates/resultHeader"
                                  model="[totalCount: totalCount, action: action]"/>
                    </div>
                    <div class="column row">
                    <g:each status="i" in="${models}" var="model">
                    <div class="column row modelPlaceHolder">
                    <%
                        def id = model.publicationId ?: model.submissionId
                        def modelUrl = createLink(controller: 'model', id: id, action: 'show')
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
                        <div class="small-11 medium-11 large-11 columns">
                            <h4>
                                <a href="${modelUrl}" target="_blank">${model.name}</a>
                                <br/>
                                <span style="font-size: small; margin: -25px 0;">
                                ID: ${id}
                                <g:if test="${model.state == ModelState.PUBLISHED}">|
                                Format: ${model.format.name} |
                                Submitter: ${model.submitter} |
                                Uploaded date: ${model.submissionDate.format('dd/MM/yyyy')} |
                                Last modified date: ${model.lastModifiedDate.format('dd/MM/yyyy')}
                                </g:if>
                                <g:if test="${model.publication}"> | Published in: ${model.publication.year}</g:if>
                                </span>
                            </h4>
                        </div>
                        <div class="small-1 medium-1 large-1 columns" id="download">
                            <g:if test="${model.state == ModelState.PUBLISHED}">
                                <g:if test="${action == 'search'}">
                                    <input id="chkDownload" type="checkbox" value="${id}"
                                           style="float: right; margin-top: 10px">
                                </g:if>
                                <g:else>
                                    <span class="icon icon-functional" data-icon="U"
                                          title="This is a public model"
                                          style="float: right; margin-top: 10px"></span>
                                </g:else>
                            </g:if>
                            <g:else>
                                <span class="icon icon-common icon-lock"
                                      title="This is a private model"
                                      style="float: right; margin-top: 10px"></span>
                            </g:else>
                        </div>
                    </div>
                    </g:each>
                    </div>
                    <g:javascript>
                        function flashHtmlMessageBuilder(message) {
                            return "<div class='alert info'><span class='closebtn'>&times;</span> " +
                                                "<h5 style='color: #ffffff'>" + message + "</h5> </div>"
                        }

                        function showFlashMessage(message) {
                            var shouldShown = typeof $('.alert').val() === "undefined" || $('.alert').val() === "";
                            if (shouldShown) {
                                $(message).insertBefore('#flashMessage');
                            }
                            $('.closetbn').on("click", function() {
                                $(this).slideUp();
                            });
                            $('.alert').on("click", function() {
                                $(this).slideUp();
                            });
                        }

                        // reduce font-size of model's notes (i.e. model description)
                        $('[class*="dc:"]').css("font-size", "90%");
                        // show the query string on local search box and string query division
                        // at the top of main content division
                        $(document).ready(function() {
                            var query = "${queryString}";
                            $('#local-searchbox').val(query);
                            $('#searchString').text(query);
                            if ("${params.sort}") {
                                $('div#sorting > label > select').val("${params.sort}");
                            }
                        });

                        if (${action == 'search'}) {
                            $('div#sorting > label > select').change(function() {
                                var selectedValue = $(this).val();
                                var url = "${createLink(controller: 'search', action: "${action}",
                            params: [query: "${query}"])}";
                                if ("${params.offset}") {
                                    url += "&offset=${params.offset}";
                                }
                                if ("${params.numResults}") {
                                    url += "&numResults=${params.numResults}";
                                }
                                url += "&sort=" + selectedValue;
                                window.location.href = url;
                            });
                            var selectedModels = [];
                            $('div#download > input').click(function() {
                                var isChecked = $(this).is(':checked');
                                var checkedValue = $(this).val();
                                if (isChecked)
                                    selectedModels.push(checkedValue);
                                else {
                                    var index = selectedModels.indexOf(checkedValue);
                                    if (index > -1) {
                                        selectedModels.splice(index, 1);
                                    }
                                }
                            });
                            $('#checkAll').on("click", function() {
                                selectedModels = [];
                                var operation = $(this).text();
                                if (operation === "Select all") {
                                    var downloadCheckbox = $('#download > input');
                                    if (downloadCheckbox.length > 0) {
                                        console.log(downloadCheckbox.length);
                                        downloadCheckbox.prop('checked', true);
                                        $(this).text("Deselect all");
                                        downloadCheckbox.each(function() {
                                            console.log($(this).val());
                                            selectedModels.push($(this).val());
                                        });
                                    } else {
                                        var htmlMessage = flashHtmlMessageBuilder("${g.message(code: "jummp.search.download.model.unavailable")}");
                                        showFlashMessage(htmlMessage);
                                    }
                                } else {
                                    $('#download > input').prop('checked', false);
                                    $(this).text("Select all");
                                }
                            });
                            var link = "";
                            $('#btnDownload').on("click", function() {
                                if (typeof selectedModels !== undefined && selectedModels.length > 0) {
                                    link = "${g.createLink(controller: "search", action: "download", params: ['models': ''])}";
                                    link += selectedModels.join();
                                    // if the browser sees the response type of 'link' to be binary, then it will download
                                    // the file rather than trying to display it as plain text. The response type is set in
                                    // the controller method
                                    window.location = link;
                                } else {
                                    var htmlMessage = flashHtmlMessageBuilder("${g.message(code: "jummp.search.download.model.checkOne")}");
                                    showFlashMessage(htmlMessage);
                                }
                            });
                            // show all models ~ reset the current search ==> start a new search
                            var query = "${queryString}";
                            if (query !== "*:*") {
                                var url = "${createLink(controller: 'search', action: "${action}",
                                                    params: [query: "*:*"])}";
                                $('#resetSearch').html('<a href="' + url + '" title="Clear the current search">Reset</a>');
                            }
                        }
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
            <%
                Map pagedParams = [:]
            %>
            <g:if test="${currentPage != 1 && numPages > stepPagination}">
                <%
                    pagedParams = [offset: 0, numResults: length, sort: params.sort]
                    if (query) {
                        pagedParams["query"] = query
                        if (action.equalsIgnoreCase("search")) {
                            pagedParams["domain"] = params.domain
                        }
                    }
                %>
                <a href="${createLink(controller: 'search', action: action, params: pagedParams)}">First</a>
            </g:if>
            <g:else>
                First
            </g:else>
            <g:if test="${currentPage == 1 || numPages <= stepPagination}">
                <g:img dir="${imagePath}/pagination" absolute="true" contextPath=""
                       file="arrow-previous-disable.gif" alt="Previous"/>
            </g:if>
            <g:else>
                <%
                    pagedParams = [offset: modelStart - length - 1, numResults: length, sort: params.sort]
                    if (query) {
                        pagedParams["query"] = query
                        if (action.equalsIgnoreCase("search")) {
                            pagedParams["domain"] = params.domain
                        }
                    }
                %>
                <a href="${createLink(controller: 'search', action: action, params: pagedParams)}">
                    <g:img dir="${imagePath}/pagination" absolute="true"  contextPath=""
                           file="arrow-previous.gif" alt="Previous"/>
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
                        <%
                            pagedParams = [offset: (i - 1) * length, numResults: length, sort: params.sort]
                            if (query) {
                                pagedParams["query"] = query
                                if (action.equalsIgnoreCase("search")) {
                                    pagedParams["domain"] = params.domain
                                }
                            }
                        %>
                        <a href="${createLink(controller: 'search', action: action, params: pagedParams)}">
                            ${i}
                        </a>
                    </g:else>
                </span>
            </g:each>
            <g:if test="${modelEnd == totalCount || numPages <= stepPagination}">
                <g:img dir="${imagePath}/pagination" absolute="true"  contextPath=""
                       file="arrow-next-disable.gif" alt="Next"/>
            </g:if>
            <g:else>
                <%
                    pagedParams = [offset: modelStart + length - 1, numResults: length, sort: params.sort]
                    if (query) {
                        pagedParams["query"] = query
                        if (action.equalsIgnoreCase("search")) {
                            pagedParams["domain"] = params.domain
                        }
                    }
                %>
                <a href="${createLink(controller: 'search', action: action, params: pagedParams)}">
                    <g:img dir="${imagePath}/pagination" absolute="true"  contextPath=""
                           file="arrow-next.gif" alt="Next"/>
                </a>
            </g:else>
            <g:if test="${currentPage != numPages && numPages > stepPagination}">
                <%
                    pagedParams = [offset: length * (numPages - 1), numResults: length, sort: params.sort]
                    if (query) {
                        pagedParams["query"] = query
                        if (action.equalsIgnoreCase("search")) {
                            pagedParams["domain"] = params.domain
                        }
                    }
                %>
                <a href="${createLink(controller: 'search', action: action, params: pagedParams)}">Last</a>
            </g:if>
            <g:else>
                Last
            </g:else>
        </div>
        </div>
    </g:if>
    <g:else>
        <g:render template="/templates/searchNotFound" model="[params: params]"/>
    </g:else>
</div>
