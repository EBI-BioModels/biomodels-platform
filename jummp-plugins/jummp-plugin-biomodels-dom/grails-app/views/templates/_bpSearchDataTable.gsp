<%@ page import="grails.converters.JSON" %>


<div id="errors">
</div>

<g:render template="/templates/parameterSearch/searchTips"
          model="['tips': [
              'BIOMD*292',
              'Cyclin',
              'organism:Homo Sapiens',
              'publication:16838084',
              'modifiers:cyclin OR reactants:cyclin OR products:cyclin'
          ]]" />

<span class="pull_element_right">
    <input id="curated_id" type="radio" name="curation" value="curated"><label for="curated_id" class="size-75 line-height-75">Curated</label>
    <input id="non_curated_id" type="radio" name="curation" value="non-curated"><label for="non_curated_id" class="size-75 line-height-75">Non-Curated</label>
</span>

<table data-stripe-classes="[]" id="table_id" class="display">
    <thead>
    <tr>
    <th data-class-name="large-2 medium-2 small-2 align-top line-height-100 small word-break">Entity</th>
    <th data-class-name="large-7 medium-7 small-7 align-top line-height-100 small">Reaction</th>
    <th data-class-name="large-3 medium-3 small-3 line-height-150 small word-break"
        title="model, manuscript, organism, cross references, etc.">More information</th>
    </tr>
    </thead>
</table>
<script>
    $(document).ready(function () {
        const FIELD_SEPARATOR = ';';
        const DOWNLOADING_LABEL = "Downloading now...";
        const DOWNLOAD_LABEL = "Download";
        const DEFAULT_QUERY = "*:*";
        let isDirectionBack = false;

        // Global state variable
        const pageState = {
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
                result = "${bp.renderSboTerms(termArray: termArray)}";
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
            formattedData = "<a target='_blank' title='Access the model containing this reaction'  href='https://www.ebi.ac.uk/biomodels/" + accession + "'>" + accession + "</a>";
            return formattedData
        }

        function createXrefHyperlink(href, title = '') {
            href = href.replace(/\\/g, "");
            let linkData = href.split('|');
            if (linkData.length === 1) return href;

            href = "<a title='" + title + "' target='_blank' href='" + linkData[0] + "'>" + linkData[1] + "</a>";
            return href;
        }

        function constructReactionLegend(row) {
            const reactants = row.fields.reactants_RAW;
            const products = row.fields.products_RAW;
            const modifiers = row.fields.modifiers_RAW;

            let table = "<table>\n" +
                "  <thead><tr>\n" +
                "    <th class='text-center word-break'>role</th>\n" +
                "    <th class='word-break'>id</th>\n" +
                "    <th class='word-break'>name</th>\n" +
                "    <th class='text-center word-break'>references</th>\n" +
                "    </tr>\n" +
                "  </thead>\n" +
                "  <tbody>";
            table += asReactionLegendRows(reactants, "Reactant", "icon-download");
            table += asReactionLegendRows(products, "Product", "icon-upload");
            table += asReactionLegendRows(modifiers, "Modifier", "icon-cog");
            table += "</tbody></table>";

            return asDiv(table, "margin-top-small margin-bottom-small");
        }

        function createOptionalLegendCell(contents, classes = '') {
            const suffix = "</td>";
            let prefix = "<td";
            if (classes !== undefined && classes.length > 0) {
                prefix += " class='" + classes + "'";
            }
            prefix += ">";
            let result = prefix;
            if (contents !== undefined && contents.length !== 0) {
                result += contents;
            }
            return result + suffix;
        }

        // show reactants, products and modifiers as a table
        function asReactionLegendRows(entries, type, icon) {
            let out = "";
            let rows = undefined;
            // modifiers are arrays
            if (Array.isArray(entries)) {
                rows = entries;
            } else {
                // products and reactants are CSV strings with rows delimited by '£'
                const rowDelimiter = "£";
                rows = entries.split(rowDelimiter);
            }
            const count = rows.length;
            if (count === 0) return out;

            const typeIcon = ebiFontIcon("common", icon, 'margin-right-medium', type);
            out += "<tr class='size-75'>"
                + "<td>"
                + '<span>' + typeIcon + type + "</span>"
                + "</td>";
            rows.forEach(function (row, idx, allRows) {
                // row structure is <entity_id>$<entity_name>$<entity_cross_references>
                const cellDelimiter = "$";
                const cells = row.split(cellDelimiter);
                const id = cells[0];
                const name = cells[1];
                const refs = formatLegendXref(cells[2]);
                let thisRow = "<td>"
                    + id + "</td>";
                thisRow += createOptionalLegendCell(name, "word-wrap");
                thisRow += createOptionalLegendCell(refs, "word-wrap");
                thisRow += "</tr>";

                if (idx !== count - 1) {
                    thisRow += "<tr class='size-75'>"
                        + "<td>"
                        + '<span>' + typeIcon + type + "</span>"
                        + "</td>";
                }
                out += thisRow;
            });
            return out;
        }

        function formatLegendXref(xrefCsv) {
            const XREF_DELIMITER = "; ";
            const refs = xrefCsv.split(XREF_DELIMITER);
            let result = "";
            const count = refs.length;
            if (0 === count) return result;

            refs.forEach(function (ref, idx, ignored) {
                if (ref !== undefined && ref.length > 0) {
                    const refAnchor = createXrefHyperlink(ref);
                    if (refAnchor.length > 0) {
                        result += refAnchor;
                        if (idx !== count - 1) {
                            result += XREF_DELIMITER;
                        }
                    }
                }
            });
            return result;
        }

        function formatReaction(data, row) {
            const reactionIcon = ebiFontIcon("common", "icon-flask", 'margin-right-medium', 'reaction (using entity IDs from the model)');
            const reaction = "<span class='green size-125'>" + row.fields.reaction_original_RAW + "</span>";
            const sbo = formatSboTerms(row.fields.reaction_sbo_term_link);
            const reactionLegend = constructReactionLegend(row);
            let out = asReactionRow(reactionIcon + reaction);
            out += asReactionRow(sbo);
            out += asReactionRow(reactionLegend);
            const rateIcon = ebiFontIcon("common", "icon-tachometer-alt", 'margin-right-small', 'rate');
            const rate = "Rate: <span class='blue size-100'>" + row.fields.rate_original_RAW + "</span>";
            out += asReactionRow(rateIcon + rate);

            const paramsIcon = ebiFontIcon("common", "icon-sliders-h", 'margin-right-small', 'parameters');
            const params = "Parameters: <span class='grey'>" + row.fields.parameters + "</span>";
            out += asReactionRow(paramsIcon + params);
            return asDiv(out, "box-shadow");
        }

        function formatPublication(href) {
            if (href === undefined || href.length === 0) {
                return undefined;
            }
            var formattedData;
            if (href.includes(FIELD_SEPARATOR)) {
                var formattedArray = [];
                var separatedLinks = href.split(FIELD_SEPARATOR);
                separatedLinks.forEach(function (subHref) {
                    formattedArray.push(createXrefHyperlink(subHref, "Access the associated manuscript"));
                });
                formattedData = formattedArray.join(FIELD_SEPARATOR + ' ');
            } else {
                formattedData = createXrefHyperlink(href);
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
            if (pub !== undefined && pub.length !== 0) {
                output += textAndEbiFontIcon(formatPublication(pub), "common", "icon-publication", 'margin-right-medium', 'manuscript');
            }

            const org = row.fields.organism;
            if (org !== undefined && org.length !== 0) {
                output += textAndEbiFontIcon(org, "conceptual", "icon-dna", 'margin-right-medium', 'organism');
            }

            if (links !== undefined && links !== "") {
                output += textAndEbiFontIcon(links, "common", "icon-external-systems", 'margin-right-medium', 'cross references');
            }

            return output + "</div>";
        }

        // Function called for showing the data pagination stats
        function infoCallback(settings, start, end, max, total) {
            let text = "Showing ";
            return (!isNaN(total))
                ? text + start + " to " + end
                + " of " + total + " entries"
                + ((total !== max) ? " (filtered from " + max + " total entries)" : "")
                : text + start + " to " + (start + this.api().data().length - 1) + " entries";
        }

        // Function to update table as per the state
        function updateTable(table) {
            $('.dataTables_filter input').val(pageState.dataTable.query);
            const page = Math.floor(pageState.dataTable.start / pageState.dataTable.size);
            const size = pageState.dataTable.size;
            table.page.len(size);
            $('#searchButton').trigger("click");
            table.page(page).draw('page');
        }

        function downloadFile(query, is_curated) {
            if (query) {
                var base = "${g.createLink(controller: "parameterSearch", action: "export", absolute: true)}";
                var uri = base + '?query=' + encodeURIComponent(query) + '&is_curated=' + is_curated;
                $.jummp.openPage(uri);
            } else {
                alert("undefined query " + query);
            }
        }

        function performSearch(query) {
            const dataTable = $("#table_id").dataTable().api();
            dataTable.search(query).draw();
            pageState.dataTable.query = query;
        }

        // allow the performSearch function to be called from outside of this block
        window.search = function(query) {
            performSearch(query);
        };

        // Function to add Search and Clear button
        function addActionButtons() {
            if ($("#searchButton").length === 0) {
                // DataTables offers 'search as you type' by default
                // replace with a dedicated search button when the page loads
                var input = $('.dataTables_filter input').unbind(),
                    $downloadButton = $('<button id="downloadButton" class="button">')
                        .text(DOWNLOAD_LABEL)
                        .click(function (message) {
                            if(pageState.dataTable.hasOwnProperty("query") &&
                                pageState.dataTable.query !== "") {
                                var buttonSelector = $("#downloadButton");
                                buttonSelector
                                    .text(DOWNLOADING_LABEL)
                                    .prop("disabled", true);
                                try {
                                    downloadFile(pageState.dataTable.query, pageState.dataTable.is_curated);
                                } catch (e) {
                                    alert("Something went wrong. Please try again later: " + e);
                                }
                                buttonSelector
                                    .text(DOWNLOAD_LABEL)
                                    .prop("disabled", false);
                            }
                        }),
                    $searchButton = $('<button id="searchButton" class="button icon icon-functional">')
                        .text('Search')
                        .click(function () {
                            const query = input.val();
                            performSearch(query);
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

                $('.dataTables_filter').append($searchButton, '&nbsp;', $downloadButton, '&nbsp;', $clearButton);
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
        const ajaxConfig = {
            "url": "${g.createLink(controller: "parameterSearch", action: "search", absolute: true)}",
            "dataSrc": function (data) {
                addActionButtons();
                const downloadButton = $("#downloadButton");
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

        const columnConfig = [
            {
                data: 'fields.entity_show',
                orderable: false,
                render: function (entity, type, row) {
                    return formatEntity(row);
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
                // this has been renamed to More Information
                // TODO: make this field name consistent with More information
                data: 'fields.external_links_show',
                orderable: false,
                render: function (data, type, row) {
                    console.log("EL: " + data);
                    return formatExternalLinks(data, type, row);
                }
            }
        ];
        // Table configuration
        const table = $('#table_id').DataTable({
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

        // Function to prepare sort parameters
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
            let data = {};
            let query, start, size, sort, is_curated;

            if (pageState.isInitialState()) {
                // populate data object from pageState.command
                let command = pageState.command;
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

            // Setting radio boxes
            if (is_curated === undefined) {
                is_curated = $('input[name="curation"]:checked')[0].value === "curated";
            } else if (is_curated === true) {
                $("#curated_id").prop("checked", true);
            } else if (is_curated === false) {
                $("#non_curated_id").prop("checked", true);
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
