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
    String queryString = params.query?.replaceAll('"', '\\\\"')
%>
<div class="content">
    <g:if test="${models}">
        <div id="inline-list" class="row">
            <div class="small-12 medium-12 large-6 columns" id="sorting">
                <!-- Show Sort by box on the search page only for now-->
                <g:if test="${action == "search"}">
                <label style="display: inline-block; float: left; padding-right: 4px; width: 100%">Sort by
                    <select name="sortBy"
                            style="display: inline-block; width: 50%; font-size: 85%;
                            height: 35px !important; margin: 0 0 0.125em;">
                        <option value="relevance-desc">Relevance</option>
                        <option value="id-asc">Model ID: A to Z</option>
                        <option value="id-desc">Model ID: Z to A</option>
                        <option value="name-asc">Model Name: A to Z</option>
                        <option value="name-desc">Model Name: Z to A</option>
                    </select>
                </label>
                </g:if>
            </div>
            <div class="small-12 medium-12 large-6 columns">
                <ul>
                    <g:each in="${resultOptions}">
                        <li>
                            <g:if test="${it == length}">
                                ${it}
                            </g:if>
                            <g:else>
                                <a href="${createLink(controller: 'search', action: action,
                                    params: [query: query, offset: 0, numResults: it, sort: params.sort])}">
                                    ${it}
                                </a>
                            </g:else>
                        </li>
                    </g:each>
                    <li>Page size </li>
                </ul>
            </div>
        </div>
        <div class="row">
                <g:if test="${action == "list"}">
                    <sec:ifLoggedIn>
                        <a href="${createLink(controller: "search", action: "archive")}">
                            Browse Archived Models</a>
                    </sec:ifLoggedIn>
                </g:if>
                <g:else>
                    <g:if test="${params.flashMessage}">
                        <div class="alert warning">
                            <span class="closebtn" onclick="this.parentElement.style.display='none';">&times;</span>
                            <h5>${params.flashMessage}</h5>
                        </div>
                    </g:if>
                    <span id="flashMessage"></span>
                    <span>Search terms: </span><span id="searchString" style="font-weight: bolder"></span>
                </g:else>
        </div>
        <div class="row grid_18 omega" id="search-results">
            <section>
                <div class="modelList">
                    <div class="column row">
                        <h3>
                            Found: ${totalCount} ${totalCount > 1 ? 'models' : 'model'}
                            <span style="float: right"><a id="btnDownload">Download</a></span>
                            <span style="float: right"><a id="checkAll">Select all</a> |&nbsp;</span>
                        </h3>
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
                                <a href="${modelUrl}">${model.name}</a>
                                <br/>
                                <span style="font-size: small; margin: -25px 0;">
                                ID: ${id} |
                                Format: ${model.format.name} |
                                Submitter: ${model.submitter} |
                                Uploaded date: ${model.submissionDate.format('dd/MM/yyyy')} |
                                Last modified date: ${model.lastModifiedDate.format('dd/MM/yyyy')}
                                </span>
                            </h4>
                        </div>
                        <div class="small-1 medium-1 large-1 columns" id="download">
                            <input id="chkDownload" type="checkbox" value="${id}" style="float: right; margin-top: 5px">
                        </div>
                    </div>
                    </g:each>
                    </div>
                    <g:javascript>
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
                            console.log(selectedModels);
                        });
                        $('#checkAll').click(function() {
                            selectedModels = [];
                            var operation = $(this).text();
                            if (operation === "Select all") {
                                $('#download > input').prop('checked', true);
                                $(this).text("Deselect all");
                                $('#download > input').each(function() {
                                    selectedModels.push($(this).val());
                                });
                            } else {
                                $('#download > input').prop('checked', false);
                                $(this).text("Select all");
                            }
                            console.log(selectedModels);
                        });
                        $('#btnDownload').click(function() {
                            if (typeof selectedModels != undefined && selectedModels.length > 0) {
                                console.log("Downloading...")
                            } else {
                                var strHtml ="<div class='alert info'><span class='closebtn'>&times;</span> " +
                                    "<h5 style='color: #ffffff'>Please select at least one model.</h5> </div>";
                                var shouldShown = typeof $('.alert').val() === "undefined" || $('.alert').val() === "";
                                console.log(shouldShown);
                                if (shouldShown) {
                                    $(strHtml).insertBefore('#flashMessage');
                                }
                                $('.closetbn').click(function() {
                                    $(this).slideUp();
                                })
                                $('.alert').click(function() {
                                    $(this).slideUp();
                                })
                            }
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
                    params: [query: query, offset: 0, numResults: length, sort: params.sort])}">First</a>
            </g:if>
            <g:else>
                First
            </g:else>
            <g:if test="${currentPage == 1 || numPages <= stepPagination}">
                <g:img dir="${imagePath}/pagination" absolute="true" contextPath=""
                       file="arrow-previous-disable.gif" alt="Previous"/>
            </g:if>
            <g:else>
                <a href="${createLink(controller: 'search', action: action,
                    params: [query: query, offset: modelStart - length - 1, numResults: length, sort: params.sort])}">
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
                        <a href="${createLink(controller: 'search', action: action,
                            params: [query: query, offset: (i - 1) * length, numResults: length, sort: params.sort])}">
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
                <a href="${createLink(controller: 'search', action: action,
                    params: [query: query, offset: modelStart + length - 1, numResults: length, sort: params.sort])}">
                    <g:img dir="${imagePath}/pagination" absolute="true"  contextPath=""
                           file="arrow-next.gif" alt="Next"/>
                </a>
            </g:else>
            <g:if test="${currentPage != numPages && numPages > stepPagination}">
                <a href="${createLink(controller: 'search', action: action,
                    params: [query: query, offset: length * (numPages - 1), numResults: length, sort: params.sort])}">Last</a>
            </g:if>
            <g:else>
                Last
            </g:else>
        </div>
        </div>
    </g:if>
    <g:else>
        <g:if test="${matches != null}">
            <p>No available models matched your query. Please try logging in to
            access more models, or another search query.</p>
        </g:if>
        <g:else>
            <p>No available models matched your query. Please try logging in to
            access more models, or another search query.</p>
        </g:else>
        <div class="alert info">
            <span class="closebtn" onclick="this.parentElement.style.display='none';">&times;</span>
            <h5 style="color: #ffffff">Please also check the syntax of your search terms.</h5>
        </div>
    </g:else>
</div>
