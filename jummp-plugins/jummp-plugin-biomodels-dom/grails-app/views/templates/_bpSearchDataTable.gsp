<%@ page import="grails.converters.JSON" %>


<div id="errors">
</div>
<span>
    <span class="pull_element_right" >
        <input id="curated_id" type="radio" name="curation" value="curated"><label for="curated_id">Curated</label>
        <input id="non_curated_id" type="radio" name="curation" value="non-curated"><label for="non_curated_id" >Non-Curated</label>
    </span>
</span>
<table data-stripe-classes="[]" id="table_id" class="display">
    <thead>
    <tr>
    <th data-class-name="large-2 medium-2 small-2 align-top line-height-100 small">Entity</th>
    <th data-class-name="large-7 medium-7 small-7 align-top line-height-100 small">Reaction</th>
    <th data-class-name="large-3 medium-3 small-3 line-height-150 small word-break">External Links</th>
    </tr>
    </thead>
</table>
<script>
    $(document).ready(function () {
        const FIELD_SEPARATOR = ';';
        const DOWNLOADING_LABEL = "Downloading now...";
        const DOWNLOAD_LABEL = "Download";
        const DEFAULT_QUERY = "*:*";
        var isDirectionBack = false;
        var columnConfig = [
            {
                data: 'fields.entity_show',
                orderable: false,
                render: function (entity, type, row) {
                    return formatEntity(entity, row);
                }
            },
            {
                data: 'fields.reaction_show',
                orderable: false,
                render: function (data, type, row) {
                    return formatReaction(data, row);
                }
            },
            {
                data: 'fields.external_links_show',
                orderable: false,
                render: function (data, type, row) {
                    return formatExternalLinks(data, type, row);
                }
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

        function formatSboTerms(termArray) {
            let result = '';
            if (termArray !== undefined && termArray.length > 0) {
                result = '<span class="size-100">'
                    + '<abbr title="Systems Biology Ontology">SBO</abbr> term:</span>'
                    + '<span class="size-75 grey">' + termArray + '</span>';
            }
            return result;
        }

        function formatEntity(entityHTML, row) {
            let result = '';
            let entityRow = '';
            const entity = row.fields.entity_id;
            const initialValue = row.fields.initial_data;
            entityRow += '<span class="green size-150">' + entity;

            if (initialValue !== undefined && initialValue !== '') {
                entityRow += '</span>' + '&nbsp;=&nbsp;' + initialValue;
            }
            result = asEntityRow(entityRow);

            const sboTerms = row.fields.entity_sbo_term_link;
            if (sboTerms.length > 0) {
                result += asEntityRow(formatSboTerms(sboTerms));
            }

            let urls = row.fields.entity_accession_url;
            if (urls.length > 0) {
                let tagsIcon = ebiFontIcon("common", "icon-tags", "padding-right-small");
                result += asEntityRow(tagsIcon + urls);
            }

            return result;
        }

        function asEntityRow(text) {
            return asDiv(text, "line-height-150 padding-top-small padding-bottom-small");
        }

        function asReactionRow(text) {
            return asDiv(text, "padding-top-medium padding-bottom-medium");
        }

        function formatModel(accession) {
            let formattedData;
            if (accession === undefined || accession.length === 0) {
                return null;
            }
            formattedData = "<a target='_blank' href='https://www.ebi.ac.uk/biomodels/" + accession + "'>" + accession + "</a>";
            return formattedData
        }

        function generatePublicationLink(href) {
            href = href.replace(/\\/g, "");
            let linkData = href.split('|');
            href = "<a target='_blank' href='" + linkData[0] + "'>" + linkData[1] + "</a>";
            return href;
        }

        function formatReaction(data, row) {
            const reactionIcon = ebiFontIcon("common", "icon-flask", 'margin-right-medium', 'reaction (using entity IDs from the model)');
            const reaction = "<span class='green size-125'>" + row.fields.reaction_original_RAW + "</span>";
            const sbo = formatSboTerms(row.fields.reaction_sbo_term_link);
            const resolvedReaction = row.fields.reaction;
            let out = asReactionRow(reactionIcon + reaction);
            out += asReactionRow(sbo);
            const tagsIcon = ebiFontIcon("common", "icon-tags", "margin-right-medium", 'reaction (using cross reference information where applicable)');
            out += asReactionRow(tagsIcon + resolvedReaction);

            const rateIcon = ebiFontIcon("common", "icon-tachometer-alt", 'margin-right-medium', 'rate');
            const rate = "<span class='blue size-100'>" + row.fields.rate_original_RAW + "</span>";
            out += asReactionRow(rateIcon + rate);

            const paramsIcon = ebiFontIcon("common", "icon-sliders-h", 'margin-right-medium', 'parameters');
            const params = "<span class='grey'>" + row.fields.parameters + "</span>";
            out += asReactionRow(paramsIcon + params);

            // TODO include modifiers such as catalysts and inhibitors using icon-plug
            return asDiv(out, "box-shadow");
        }

        function formatPublication(href) {
            if (href === undefined || href.length === 0) {
                return null;
            }
            var formattedData;
            if (href.includes(FIELD_SEPARATOR)) {
                var formattedArray = [];
                var separatedLinks = href.split(FIELD_SEPARATOR);
                separatedLinks.forEach(function (subHref) {
                    formattedArray.push(generatePublicationLink(subHref));
                });
                formattedData = formattedArray.join(FIELD_SEPARATOR+' ');
            } else {
                formattedData = generatePublicationLink(href);
            }

            return formattedData;
        }

        function asDiv(text, styles = '') {
            if (undefined === text || text === '') {
                return "";
            }
            return "<div class='" + styles + "'>" + text + "</div>";
        }

        function ebiFontIcon(fontSet, letter, styles = 'margin-right-medium', title = '') {
            const fontClass = "icon-" + fontSet.toLocaleLowerCase();
            return '<i title="' + title + '" class="' + styles + ' icon ' + fontClass + ' ' + letter + '"></i>';
        }

        function textAndEbiFontIcon(text, fontSet, letter, fontStyles, fontTitle = '') {
            const icon = ebiFontIcon(fontSet, letter, fontStyles, fontTitle);
            const content = icon + text;
            return asDiv(content);
        }

        function formatExternalLinks(links, type, row) {
            let output = "<div>";

            const modelAccession = row.fields.model;
            const m = textAndEbiFontIcon(formatModel(modelAccession), "common", "icon-unreviewed-data", 'margin-right-medium', 'model');
            output += m;

            const pub = row.fields.publication;
            const p = textAndEbiFontIcon(formatPublication(pub), "common", "icon-publication", 'margin-right-medium', 'manuscript');
            output += p;

            const org = row.fields.organism;
            const o = textAndEbiFontIcon(org, "conceptual", "icon-dna", 'margin-right-medium', 'organism');
            output += o;

            if (links !== undefined && links !== "") {
                output += textAndEbiFontIcon(links, "common", "icon-external-systems", 'margin-right-medium', 'cross references');
            }

            return output + "</div>";
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
            var page = Math.floor(pageState.dataTable.start / pageState.dataTable.size);
            var size = pageState.dataTable.size;
            table.page.len(size);
            $('#searchButton').trigger("click");
            table.page(page).draw('page');
        }

        function downloadFile(query, is_curated) {
            if (query) {
                var base = "${g.createLink(controller: "parameterSearch", action: "export", absolute: true)}";
                var uri = base + '?query=' + encodeURIComponent(query) + '&is_curated='+is_curated;
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
                                    downloadFile(pageState.dataTable.query, pageState.dataTable.is_curated);
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
            var query, start, size, sort, is_curated;

            if (pageState.isInitialState()) {
                // populate data object from pageState.command
                var command = pageState.command;
                query = command.query;
                start = Number(command.start);
                size = Number(command.size);
                sort = command.sort;
                is_curated = command.is_curated;
                $('.dataTables_filter input').val(query);
            } else {
                // populate data object from dataTableArg and set pageState.dataTable to dataTableArg
                if (dataTableArg.search.value === "") {
                    query = encodeURIComponent($('.dataTables_filter input').val());
                } else {
                    query = dataTableArg.search.value;
                }
                start = dataTableArg.start;
                size = dataTableArg.length;
            }

            // Setting radioboxes
            if (is_curated===undefined) {
                is_curated = $('input[name="curation"]:checked')[0].value === "curated";
            } else if(is_curated === true) {
                $("#curated_id").prop("checked",true);
            }else if (is_curated === false) {
                $("#non_curated_id").prop("checked",true);
            }

            // Sorting
            sort = prepareSortParams(dataTableArg, sort);

            pageState.dataTable.query = query;
            pageState.dataTable.start = start;
            pageState.dataTable.size = size;
            pageState.dataTable.sort = sort;
            pageState.dataTable.is_curated = is_curated;
            data.query = query;
            data.size = size;
            data.start = start;
            data.sort = sort;
            data.format = "json";
            data.is_curated = is_curated;
            if (isDirectionBack === false) {
                setBrowserUrl();
            }
            return data;
        }
    });
</script>
