<%@ page import="grails.converters.JSON" %>


<div id="errors">
</div>
<table  id="table_id" class="display">
    <thead>
    <th>Entity</th>
    <th>Reaction</th>
    <th>Model</th>
    <th>Organism</th>
    <th>Publication</th>
    <th>Rate</th>
    <th>Parameters</th>
    <th>Entity SBO Link</th>
    <th>Reaction SBO Link</th>
    <th>Initial Concentration/<br/>Amount</th>
    <th>External Links</th>
    </thead>
</table>
<div class="pull-element-left">
    <hr/>
    <i>
        <strong>Legends</strong><br/>
        <span class="legend-green-block">
        </span>
        <span>
            : Variable used inside SBML models</span>
    </i>
</div>
<script>
    $(document).ready(function () {
        const DOWNLOADING_LABEL = "Downloading now...";
        const DOWNLOAD_LABEL = "Download";
        const DEFAULT_QUERY = "*:*";
        var isDirectionBack = false;
        var columnConfig = [
            {
                data: 'fields.entity_show',
                orderable: false
            },
            {
                data: 'fields.reaction_show',
                width: "40%",
                orderable: false
            },
            {
                data: 'fields.model',
                render: function (rawdata, type, row) {
                    var formattedData;
                    if (rawdata !== undefined && rawdata.length !== 0) {
                        formattedData = "<a target='_blank' href='https://www.ebi.ac.uk/biomodels/" + rawdata + "'>" + rawdata + "</a>";
                    }
                    return formattedData
                }
            },
            {
                data: 'fields.organism',
                width: "40%",
                orderable: false
            },
            {
                data: 'fields.publication',
                orderable: false,
                render: function (href, type, row) {
                    if (href !== undefined && href.length !== 0) {
                        var formattedData;
                        if (href.includes(",")) {
                            var formattedArray = [];
                            var commaSeparatedLinks = href.split(",");
                            commaSeparatedLinks.forEach(function (subHref) {
                                formattedArray.push(generatePublicationLink(subHref));
                            });
                            formattedData = formattedArray.join(", ");
                        } else {
                            formattedData = generatePublicationLink(href);
                        }
                    }
                    return formattedData;
                }
            },
            {
                data: 'fields.rate_show',
                orderable: false
            },
            {
                data: 'fields.parameters',
                orderable: false
            },
            {
                data: 'fields.entity_sbo_term_link',
                orderable: false
            },
            {
                data: 'fields.reaction_sbo_term_link',
                orderable: false
            },
            {
                data: 'fields.initial_data',
                orderable: false
            },
            {
                data: 'fields.external_links_show',
                orderable: false
            }
        ];

        // Global state variable
        var pageState = {
            rootURL: "${createLink(action: 'index')}",
            command: ${command as JSON},
            dataTable: {},

            isInitialState: function () {
                return this.dataTable.query === undefined;
            },

            createSearchUrl: function () {
                var url = this.rootURL;
                var paramsArray = [];
                for (var key in this.dataTable) {
                    if (this.dataTable.hasOwnProperty(key)) {
                        var value = this.dataTable[key];
                        if (value !== undefined) {
                            var encoded = encodeURIComponent(value);
                            paramsArray.push(key + '=' + encoded);
                        }
                    }
                }
                if (paramsArray.length > 0) {
                    url = url + '?' + paramsArray.join("&");
                }
                return url;
            }
        };

        // This method is to set the URL in browser once a table operation is executed
        function setBrowserUrl() {
            window.history.pushState(pageState.dataTable, 'Title', pageState.createSearchUrl());
        }

        // This event is triggered when browser back button is clicked
        window.onpopstate = function (event) {
            if (event.state === undefined || event.state === null) {
                return;
            }
            isDirectionBack = true;
            pageState.dataTable = event.state;
            updateTable(table);
            isDirectionBack = false;
        };

        function generatePublicationLink(href) {
            href = href.replace(/\\/g, "");
            var linkData = href.split('|');
            href = "<a target='_blank' href='" + linkData[0] + "'>" + linkData[1] + "</a>";
            return href;
        }

        // Function called for showing the data pagination stats
        function infoCallback(settings, start, end, max, total, pre) {
            return (!isNaN(total))
                ? "Showing " + start + " to " + end
                + " of " + total + " entries"
                + ((total !== max) ? " (filtered from " + max + " total entries)" : "")
                : "Showing " + start + " to " + (start + this.api().data().length - 1) + " entries";
        }

        // Function to update table as per the state
        function updateTable(table) {
            $('.dataTables_filter input').val(decodeURI(pageState.dataTable.query));

            var page = Math.floor(pageState.dataTable.start / pageState.dataTable.size);
            var size = pageState.dataTable.size;
            table.page.len(size);
            $('#searchButton').trigger("click");
            table.page(page).draw('page');
        }

        function downloadFile(query) {
            if (query) {
                var base = "${g.createLink(controller: "parameterSearch", action: "export", absolute: true)}";
                var uri = base + '?query=' + encodeURIComponent(query);
                $.jummp.openPage(uri);
            } else {
                alert("undefined query " + query);
            }
        }

        // Function to add Search and Clear button
        function addActionButtons() {
            if ($("#searchButton").length === 0) {
                var input = $('.dataTables_filter input').unbind(),
                    self = $("#table_id").dataTable().api(),
                    $downloadButton = $('<button id="downloadButton" class="button">')
                        .text(DOWNLOAD_LABEL)
                        .click(function () {
                            if(pageState.dataTable.hasOwnProperty("query") &&
                                pageState.dataTable.query !== "") {
                                var buttonSelector = $("#downloadButton");
                                buttonSelector
                                    .text(DOWNLOADING_LABEL)
                                    .prop("disabled", true);
                                try {
                                    downloadFile(pageState.dataTable.query);
                                } catch (e) {
                                    alert("Something went wrong. Please try again later: ", e);
                                }
                                buttonSelector
                                    .text(DOWNLOAD_LABEL)
                                    .prop("disabled", false);
                            }
                        }),
                    $searchButton = $('<button id="searchButton" class="button icon icon-functional">')
                        .text('Search')
                        .click(function () {
                            self.search(input.val()).draw();
                            pageState.dataTable.query = input.val();
                        }),
                    $clearButton = $('<button id="clearButton" class="button">')
                        .text('Clear')
                        .click(function () {
                            resetTable();
                            if (downloadButton.textContent === DOWNLOAD_LABEL) {
                                $("#downloadButton").prop("disabled", false);
                            }
                            if (!isDirectionBack) setBrowserUrl();
                        });

                $('.dataTables_filter').append($downloadButton, '&nbsp;', $searchButton, '&nbsp;', $clearButton);
            }
        }

        // Function to add Search and Clear button
        function displayAsyncMessage(message) {
            $('<p style="color:red">'
                + message + '</p>')
                .appendTo('#errors');
        }

        function resetTable() {
            pageState.dataTable.start = 0;
            pageState.dataTable.size = 10;
            pageState.dataTable.query = DEFAULT_QUERY;
            pageState.dataTable.sort = "";
            updateTable(table);
        }

        // Ajax configuration
        ajaxConfig = {
            "url": "${g.createLink(controller: "parameterSearch", action: "search", absolute: true)}",
            "dataSrc": function (data) {
                addActionButtons();
                var downloadButton = $("#downloadButton");
                if (data.entries.length === 0) {
                    downloadButton.prop("disabled", true);
                } else {
                    if (downloadButton.text() === DOWNLOAD_LABEL) {
                        downloadButton.prop("disabled", false);
                    }
                }
                return data.entries
            },
            "data": preProcessEbiSearchParams,
            "error": function (xhr, error, code) {
                addActionButtons();
                displayAsyncMessage(xhr.responseJSON.message);
                $("#downloadButton").prop("disabled", true);
            }
        };

        $.fn.dataTable.ext.errMode = 'throw';

        // Table configuration
        var table = $('#table_id').DataTable(
            {
                initComplete: function () {
                    updateTable(table);
                },
                columns: columnConfig,
                "processing": false,
                "serverSide": true,
                "infoCallback": infoCallback,
                "ajax": ajaxConfig,

                language: {
                    paginate: {
                        previous: '<',
                        next: '>'
                    },
                    aria: {
                        paginate: {
                            previous: 'Previous',
                            next: 'Next'
                        }
                    }
                }
            });

        // Function to preapare sort parameters
        function prepareSortParams(dataTableArg, sort) {
            $("#errors").empty();
            if (sort === undefined || sort === "" || sort === null) {
                sort = "";
                dataTableArg.order.forEach(function (obj) {
                    var column = dataTableArg.columns[obj.column];
                    var columnName = column.data.replace("fields.", "").replace("_RAW", "");
                    if (columnName === "model") {
                        var sortDirectionArg = obj.dir;
                        var columnOrder;

                        if (sortDirectionArg === "desc") {
                            columnOrder = "descending";
                        } else if (sortDirectionArg === "asc") {
                            columnOrder = "ascending";
                        }
                        if (sort && columnOrder && columnName) {
                            sort += ',';
                        }
                        sort += columnName + ':' + columnOrder;
                    } else {
                        // Default sort
                        sort += "model:ascending";
                    }
                });
            }
            return sort;
        }

        // Preprocess custom params before calling EbiSearch WS
        function preProcessEbiSearchParams(dataTableArg) {
            var data = {};
            var query, start, size, sort;
            if (pageState.isInitialState()) {
                // populate data object from pageState.command
                var command = pageState.command;
                query = decodeURI(command.query);
                start = Number(command.start);
                size = Number(command.size);
                sort = command.sort;

                $('.dataTables_filter input').val(query);
            } else {
                // populate data object from dataTableArg and set pageState.dataTable to dataTableArg
                if (dataTableArg.search.value === "") {
                    query = decodeURI($('.dataTables_filter input').val());
                } else {
                    query = decodeURI(dataTableArg.search.value);
                }
                start = dataTableArg.start;
                size = dataTableArg.length;
            }

            // Sorting
            sort = prepareSortParams(dataTableArg, sort);

            pageState.dataTable.query = query === "" || query === DEFAULT_QUERY ? DEFAULT_QUERY : query;
            pageState.dataTable.start = start;
            pageState.dataTable.size = size;
            pageState.dataTable.sort = sort;

            data.query = query;
            data.size = size;
            data.start = start;
            data.sort = sort;
            data.format = "json";

            if (isDirectionBack === false) {
                setBrowserUrl();
            }
            return data;
        }
    });
</script>
