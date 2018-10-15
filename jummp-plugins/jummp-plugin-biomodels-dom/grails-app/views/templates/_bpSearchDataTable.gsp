<table id="table_id" class="display">
    <thead>
    <th>Entity</th>
    <th>Entity Id</th>
    <th>Reaction</th>
    <th>Model</th>
    <th>Publication</th>
    <th>Rate</th>
    <th>Parameters</th>
    </thead>
</table>

<script>
    $(document).ready(function () {
        const BASE_URL = "https://wwwdev.ebi.ac.uk/ebisearch/ws/rest/biomodels_parameters?format=json&";
        const FIELDS = "fields=entity,entity_id,reaction,model,publication,rate,parameters&";

        var columnConfig = [
            {
                data: 'fields.entity',
                render: formatData
            },
            {
                data: 'fields.entity_id',
                orderable: false,
                render: formatData
            },

            {
                data: 'fields.reaction',
                width: "40%",
                orderable: false,
                render: formatData
            },
            {
                data: 'fields.model',
                render: function (data, type, row) {
                    if (data !== undefined && data.length !== 0) {
                        data = data[0].replace(/\\/g, "");
                        var lastIndexOfSlash = data.lastIndexOf("/");
                        data = "<a target='_blank' href='https://www.ebi.ac.uk/biomodels/" + data + "'>" + data.substring(lastIndexOfSlash, data.length) + "</a>"
                    }
                    return data
                }
            },
            {
                data: 'fields.publication',
                orderable: false,
                render: function (data, type, row) {
                    if (data !== undefined && data.length !== 0) {
                        data = data[0].replace(/\\/g, "");
                        var lastIndexofSlash = data.lastIndexOf("/");
                        lastIndexofSlash = data.lastIndexOf("/")+1;
                        data = "<a target='_blank' href='" + data + "'>" + data.substring(lastIndexofSlash, data.length) + "</a>"
                    }
                    return data
                }
            },
            {
                data: 'fields.rate',
                orderable: false,
                render: formatData
            },
            {
                data: 'fields.parameters',
                orderable: false,
                render: formatData
            }
        ];

        function infoCallback(settings, start, end, max, total, pre) {
            return (!isNaN(total))
                ? "Showing " + start + " to " + end + " of " + total + " entries"
                + ((total !== max) ? " (filtered from " + max + " total entries)" : "")
                : "Showing " + start + " to " + (start + this.api().data().length - 1) + " entries";
        }


        function preProcessEbiSearchParams(urlParams) {
            // Global Filtering
            var query;
            if (!(urlParams.search.value)) {
                query = "domain_source:biomodels_parameters";
                /*query = "BIOMD*";*/
            } else {
                query = urlParams.search.value;
            }
            urlParams.query = query;

            // Sorting
            urlParams.order.forEach(function (obj) {
                var column = urlParams.columns[obj.column];
                var columnName = column.data.replace("fields.", "");
                var sortDirection = obj.dir;
                urlParams.sortfield = columnName;
                if (sortDirection === "desc")
                    urlParams.order = "descending";
                else {
                    urlParams.order = "ascending";
                }
            });
            urlParams.size = urlParams.length;
            delete urlParams[search];
            delete urlParams[length];
            return urlParams;
        }

        function formatData(data, type, row) {
            if (data !== undefined && data.length !== 0) {
                data = data[0].replace(/\\/g, "");
            }
            return data
        }

        ajaxConfig = {
            "url": BASE_URL + FIELDS,
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
                        previous: '‹',
                        next: '.'
                    },
                    aria: {
                        paginate: {
                            previous: 'Previous',
                            next: '.'
                        }
                    }
                }
            });

    });
</script>
