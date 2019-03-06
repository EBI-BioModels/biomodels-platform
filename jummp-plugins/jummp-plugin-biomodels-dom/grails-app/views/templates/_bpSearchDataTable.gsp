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
                data: 'fields.reaction',
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
                        if(href.includes(",")) {
                            var formattedArray = [];
                            var commaSeparatedLinks = href.split(",");
                            commaSeparatedLinks.forEach(function(subHref) {
                                formattedArray.push(generatePublicationLink(subHref));
                            });
                            formattedData = formattedArray.join(", ");
                        } else{
                            formattedData = generatePublicationLink(href);
                        }

                    }
                    return formattedData;
                }
            },
            {
                data: 'fields.rate',
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

            }
        ];

        function generatePublicationLink(href) {
            href = href.replace(/\\/g, "");
            var linkData = href.split('|');
            href = "<a target='_blank' href='" + linkData[0] + "'>" + linkData[1] + "</a>";
            return href;
        }

        // Function called for showing the data pagination stats
        function infoCallback(settings, start, end, max, total, pre) {
            return (!isNaN(total))
                ? "Showing " + start + " to " + end + " of " + total + " entries"
                + ((total !== max) ? " (filtered from " + max + " total entries)" : "")
                : "Showing " + start + " to " + (start + this.api().data().length - 1) + " entries";
        }

        // Preprocess custom params before calling EbiSearch WS
        function preProcessEbiSearchParams(urlParams) {
            var data = {};
            data.query = encodeURIComponent(urlParams.search.value);
            data.size = urlParams.length;
            data.start = urlParams.start;
            data.sort = "";
            data.format = "json";

            // Sorting
            urlParams.order.forEach(function (obj) {
                var column = urlParams.columns[obj.column];
                var columnName = column.data.replace("fields.", "").replace("_RAW","");
                if(columnName === "model") {
                    var sortDirectionArg = obj.dir;
                    var columnOrder;

                    if (sortDirectionArg === "desc") {
                        columnOrder = "descending";
                    }
                    else if (sortDirectionArg === "asc") {
                        columnOrder = "ascending";
                    }
                    if (data.sort && columnOrder && columnName) {
                        data.sort += ',';
                    }
                    data.sort += columnName + ':' + columnOrder;
                }else {
                    // Default sort
                    data.sort += "model:ascending";
                }
            });
            return data;
        }

        ajaxConfig = {
            "url": "${grailsApplication.config.grails.serverURL}/parameterSearch/search",
            "dataSrc": "entries",
            "data": preProcessEbiSearchParams
        };


        // Table configuration
        var table = $('#table_id').DataTable(
            {
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

    });
</script>
