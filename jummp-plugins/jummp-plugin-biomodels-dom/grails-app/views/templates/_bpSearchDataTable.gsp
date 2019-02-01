<%@ page import="grails.converters.JSON" %>
<table id="table_id" class="display">
    <thead>
    <th>Entity</th>
    <th>Entity Id</th>
    <th>Reaction</th>
    <th>Model</th>
    <th>Publication</th>
    <th>Rate</th>
    <th>Parameters</th>
    <th>Entity SBO Link</th>
    <th>Reaction SBO Link</th>
    <th>Initial Data</th>
    </thead>
</table>

<script>
    $(document).ready(function () {
        var isDirectionBack = false;
        var columnConfig = [

            {
                data: 'fields.entity_accession_url',
                orderable: false

            },
            {
                data: 'fields.entity_id',
                orderable: false
            },

            {
                data: 'fields.reaction_RAW',
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
                data: 'fields.rate_RAW',
                orderable: false
            },
            {
                data: 'fields.parameters_RAW',
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
                data: 'fields.initial_data_RAW',
                orderable: false

            }
        ];

        // Global state variable
        var pageState = {
            rootURL: "${createLinkTo(action: 'parameterSearch')}",
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
            if(event.state === undefined || event.state === null) {
                return;
            }
            isDirectionBack = true;
            pageState.dataTable = event.state;
            updateTable(table);
            isDirectionBack = false;

        };
        function generatePublicationLink(href) {
            href = href.replace(/\\/g, "");
            var lastIndexofUrlPrefix = "http://identifiers.org/".lastIndexOf("/") + 1;
            var urlSuffix = href.substring(lastIndexofUrlPrefix, href.length);
            var firstIndexOfUrlSuffix = urlSuffix.indexOf("/") + 1;
            href = "<a target='_blank' href='" + href + "'>" + urlSuffix.substring(firstIndexOfUrlSuffix, href.length) + "</a>";
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
            $('.dataTables_filter input').val(pageState.dataTable.query);
            $('#searchButton').trigger("click");

            var page = Math.floor(pageState.dataTable.start / pageState.dataTable.size);

            table.page.len(pageState.dataTable.size).draw('page');
            table.page(page).draw('page');

        }

        // Ajax configuration
        ajaxConfig = {
            "url": "${g.createLink(controller: "parameterSearch", action: "search", absolute: true)}",
            "dataSrc": "entries",
            "data": preProcessEbiSearchParams
        };


        // Table configuration
        var table = $('#table_id').DataTable(
            {
                initComplete: function () {
                    updateTable(table);
                    var input = $('.dataTables_filter input').unbind(),
                        self = this.api(),
                        $searchButton = $('<button id="searchButton" class="button icon icon-functional">')
                            .text('search')
                            .click(function () {
                                self.search(input.val()).draw();
                                pageState.dataTable.query = input.val();
                                if (!isDirectionBack) setBrowserUrl();
                            }),
                        $clearButton = $('<button id="clearButton" class="button">')
                            .text('clear')
                            .click(function () {
                                input.val('');
                                $searchButton.click();
                                pageState.dataTable = {};
                                if (!isDirectionBack) setBrowserUrl();
                            });
                    $('.dataTables_filter').append($searchButton, '&nbsp;', $clearButton);

                },
                columns: columnConfig,
                "processing": true,
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
                        }
                        else if (sortDirectionArg === "asc") {
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
                query = encodeURIComponent(dataTableArg.search.value);
                start = dataTableArg.start;
                size = dataTableArg.length;
            }


            // Sorting
            sort = prepareSortParams(dataTableArg, sort);

            pageState.dataTable.query = query;
            pageState.dataTable.start = start;
            pageState.dataTable.size = size;
            pageState.dataTable.sort = sort;

            data.query = query;
            data.size = size;
            data.start = start;
            data.sort = sort;

            if (isDirectionBack === false) {
                setBrowserUrl();
            }
            return data;
        }
    });
</script>
