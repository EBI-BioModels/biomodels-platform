function displayModelComponents(id, typeName, table_id, columnConfig, url) {

    function prepareParams(dataTableArg) {
        var data = {};
        data.id = id;
        data.limit = dataTableArg.length;
        data.skip = dataTableArg.start;
        data.typeName = typeName;
        return data;
    }

    function customizeData(data) {
        var json = jQuery.parseJSON(data);
        console.log(json);
        var totalKey = typeName + 'RecordsTotal';
        json.recordsTotal = json.components[totalKey];
        json.recordsFiltered = json.components[totalKey];
        json.data = json.components[typeName];
        return JSON.stringify(json); // return JSON string
    }

    function getAjaxConfig() {
        ajaxConfig = {
            "url": url,
            "data": prepareParams,
            "dataFilter": customizeData,
            "dataSrc": function (data) {
                return data.data
            },
            "error": function (xhr, error, code) {
                console.log(error);
            }
        };
        return ajaxConfig;
    }

    // Function called for showing the data pagination stats
    function infoCallback(settings, start, end, max, total, pre) {
        return (!isNaN(total))
            ? "Showing " + start + " to " + end
            + " of " + total + " components"
            + ((total !== max) ? " (filtered from " + max + " total components)" : "")
            : "Showing " + start + " to " + (start + this.api().data().length - 1) + " components";
    }


    $(table_id).DataTable(
        {
            "ordering": false,
            "searching": false,
            "columns": columnConfig,
            "processing": false,
            "serverSide": true,
            "infoCallback": infoCallback,
            "ajax": getAjaxConfig(),

            "language": {
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
}
