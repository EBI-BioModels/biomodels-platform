<table id="table_id" class="display">
    <thead>
    <th>Entity</th>
    <th>Entity Link</th>
    <th>Entity Id</th>
    <th>Reaction</th>
    <th>Model</th>
    <th>Publication</th>
    <th>Rate</th>
    <th>Parameters</th>
    <th>Entity SBO Link</th>
    <th>Reaction SBO Link</th>
    </thead>
</table>

<script>
    $(document).ready(function () {
        var columnConfig = [
            {
                data: 'fields.entity_RAW'
            },
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
                    if (rawdata !== undefined && rawdata.length !== 0) {
                        var data = rawdata;
                        var formattedData = "<a target='_blank' href='https://www.ebi.ac.uk/biomodels/" + data + "'>" + data + "</a>";
                    }
                    return formattedData
                }
            },
            {
                data: 'fields.publication',
                orderable: false,
                render: function (data, type, row) {
                    if (data !== undefined && data.length !== 0) {
                        data = data.replace(/\\/g, "");
                        var lastIndexofSlash = "http://identifiers.org/".lastIndexOf("/") + 1;
                        data = "<a target='_blank' href='" + data + "'>" + data.substring(lastIndexofSlash, data.length) + "</a>"
                    }
                    return data
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
            }
        ];

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

            // Sorting
            urlParams.order.forEach(function (obj) {
                var column = urlParams.columns[obj.column];
                var columnName = column.data.replace("fields.", "").replace("_RAW","");
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
