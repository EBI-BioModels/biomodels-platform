<style>
    .verified {
        position: relative;
    }
    .verified::after {
        position: absolute;
        content: "";
        background-image: url("${grailsApplication.config.grails.serverURL}/images/verified.png");
        background-size: 20px 25px;
        display: inline-block;
        width: 20px;
        height: 25px;
    }
    .actual_category {
        padding: 2px;
        height: inherit;
    }
    .easy-autocomplete-container {
        width: 400px;
    }
    .easy-autocomplete-container ul li div {
        text-align: left;
    }
    .easy-autocomplete-container ul {
        margin: 0;
    }
    .wrapper-autocomplete {
        position: relative;
    }
    .switch-btn {
        position: absolute;
        right: 3px;
        background-image: url("${grailsApplication.config.grails.serverURL}/images/hamburger.png");
        background-size: 26px 26px;
        display: inline-block;
        width: 26px;
        height: 26px;
        cursor: pointer;
        top: 3px;
    }

    .switch-btn.search {
        background-image: url("${grailsApplication.config.grails.serverURL}/images/search.png") !important;
    }

    #model_data_filter {
        width: 500px;
    }

    #model_data_filter > label {
        width: 200px;
        display: inline-block;
    }

    #ground_truth_filter, #ground_truth_search {
        width: 200px;
        float: right;
        margin-left: 10px;
    }

    .dt-buttons {
        width: 100%;
    }

    .has_change {
        border-color: green !important;
    }
</style>
<table id="model_data"></table>
<g:javascript src="datatable/jquery.dataTables.min.js" contextPath=""/>
<g:javascript src="datatable/dataTables.buttons.min.js" contextPath=""/>
<g:javascript src="datatable/buttons.flash.min.js" contextPath=""/>
<g:javascript src="datatable/jszip.min.js" contextPath=""/>
<g:javascript src="datatable/pdfmake.min.js" contextPath=""/>
<g:javascript src="datatable/vfs_fonts.js" contextPath=""/>
<g:javascript src="datatable/buttons.html5.min.js" contextPath=""/>
<g:javascript src="datatable/buttons.print.min.js" contextPath=""/>
<g:javascript src="datatable/dataTables.select.min.js" contextPath=""/>
<g:javascript src="jquery.easy-autocomplete.min.js"/>
<script>
    var models = [];
    <g:each var="item" in="${models}">
        models.push({
            name: "${item['name']}",
            updateDate: "${item['updateDate']}",
            category: "${item['class']}",
            submissionId: "${item['model'].submissionId}",
            publicationId: "${item['model'].publicationId}",
            realCategory: "${item['realClass']}",
            categoryName: "${item['class_name']}",
            groundTruth: parseInt("${item['groundTruth']}"),
            hasChange: 0
        });
    </g:each>

    function changeVal(event, row) {
        var element = $(event.target);
        var original_val = element.parent().siblings('input[name="originalRealCategory[]"]');
        if (original_val.length === 0) {
            original_val = element.siblings('input[name="originalRealCategory[]"]');
        }
        original_val = original_val.val();
        if (element.val() !== original_val) {
            element.addClass("has_change");
            table.cell(row, 6).data(1);
        } else {
            element.removeClass("has_change");
            table.cell(row, 6).data(0);
        }
    }

    function possibleOntology(event) {
        var element = $(event.target);
        var inputElement = element.siblings(".actual_category");
        if (inputElement.length === 0) {
            inputElement = element.siblings(".easy-autocomplete").children(".actual_category");
        }
        if (!element.hasClass("search")) {
            console.log("Search mode");
            element.addClass("search");
            inputElement.data("at", "false");
            searchOntologyMode(inputElement);
        } else {
            element.removeClass("search");
            console.log("Possible category mode");
            var submission_id = element.data("model");
            var options = {
                url: $.jummp.createLink("classifierConfigure", "getPossibleCategories?submission_id=" + submission_id),
                getValue: function(val) {
                    return val.ontology + " - " + val.name;
                },
                list: {
                    match: {
                        enabled: true
                    },
                    onSelectItemEvent: function() {
                        var data = inputElement.getSelectedItemData();
                        inputElement.val(data.ontology).trigger('change');;
                    }
                }
            };

            inputElement.easyAutocomplete(options);
            inputElement.data("at", "true");
            inputElement.select();
        }
    }

    function searchOntologyMode(element) {
        if (element.data("at") !== "true") {
            var options = {
                url: $.jummp.createLink("classifierConfigure", "searchCategories"),
                ajaxSettings: {
                    dataType: "json",
                    method: "GET",
                    data: {}
                },
                getValue: function(element) {
                    return element.ontology + " - " + element.name;
                },
                requestDelay: 400,
                preparePostData: function(data) {
                    data.keyword = element.val();
                    return data;
                },
                list: {
                    match: {
                        enabled: true
                    },
                    onSelectItemEvent: function() {
                        var data = element.getSelectedItemData();
                        element.val(data.ontology).trigger('change');
                    }
                }
            };

            element.easyAutocomplete(options);
            element.data("at", "true");
            element.select();
        }
    }

    function searchOntology(event) {
        var element = $(event.target);
        searchOntologyMode(element);
    }
    var table = $('#model_data').DataTable({
        "pageLength": 20,
        "columns": [
            {
                "title": "Model Id",
                "data": "submissionId",
                "render": function(data, type, row, meta){
                    if (type === 'display') {
                        var url = '<a target="_blank" ';
                        if (row.groundTruth === 1) {
                            url += 'class="verified" ';
                        }
                        return url + 'href="https://wwwdev.ebi.ac.uk/biomodels/' + data + '">' + data + '</a>';
                    }
                    return data
                }
            },
            {"title": "Model Name", "data": "name"},
            {"title": "Category Predicted",
                "data": "category",
                "className": "dt-center",
                "render": function (data, type, row, meta) {
                    if (type === 'display') {
                        return '<span title="' + row.categoryName + '">' + data + '</span>'
                    }
                    return data
                }
            },
            {
                "title": "Actual Category",
                "data": "realCategory",
                "className": "dt-center",
                "render": function(data, type, row, meta){
                    if (type === 'display') {
                        var dom = '<div class="wrapper-autocomplete">';
                        dom += '<div class="easy-autocomplete">';
                        dom += '<input name="realCategory[]" class="actual_category" onclick="searchOntology(event)" ' +
                            'value="' + data + '" onchange="changeVal(event, ' + meta.row + ')"/>';
                        dom += '<button data-model="' + row.submissionId + '" onclick="possibleOntology(event)" ' +
                            'type="button" class="switch-btn search"></button>';
                        dom += '<input type="hidden" name="originalRealCategory[]" value="'+ data +'"/>';
                        dom += '<input type="hidden" name="modelSubmissionId[]" value="'+ row.submissionId +'"/>';
                        dom += '</div>';
                        dom += '</div>';
                        return dom;
                    }
                    return data
                }
            },
            {"title": "Date Update", "data": "updateDate", "width": "14%", "className": "dt-center", "targets": "_all"},
            {"title": "groundTruth", "data": "groundTruth", "visible": false},
            {"title": "hasChange", "data": "hasChange", "visible": false}
        ],
        "dom": 'Brtip',
        "buttons": [
            {
                extend: 'copy',
                exportOptions: { orthogonal: 'export' }
            },
            {
                extend: 'csv',
                exportOptions: { orthogonal: 'export' }
            },
            {
                extend: 'excel',
                exportOptions: { orthogonal: 'export' }
            },
            {
                extend: 'pdf',
                exportOptions: { orthogonal: 'export' }
            },
            {
                extend: 'print',
                exportOptions: { orthogonal: 'export' }
            }
        ],
        "language": {
            "paginate": {
                "previous": '<i class="fa fa-angle-left"></i>',
                "next": '<i class="fa fa-angle-right"></i>'
            }
        },
        "order": [[ 1, 'asc' ]],
        "data": models,
        "initComplete": function () {
            $(".dt-buttons").append('<input id="ground_truth_search" type="text" placeholder="Search..."/>');
            $(".dt-buttons").append('<select id="ground_truth_filter">\n' +
                '    <option value="all" selected>All models</option>\n' +
                '    <option value="other">Only other category</option>\n' +
                '    <option value="new">Only new models</option>\n' +
                '    <option value="changes">Your changes</option>\n' +
                '</select>');
        }
    });

    $(document).ready(function () {
        $("#ground_truth_filter").change(function () {
            switch ($(this).val()) {
                case "all":
                    table.search( '' ).columns().search( '' ).draw();
                    break;
                case "new":
                    table.search( '' ).columns().search( '' ).column(5).search(0, false, false).draw();
                    break;
                case "changes":
                    table.search( '' ).columns().search( '' ).column(6).search(1, false, false).draw();
                    break;
                case "other":
                    table.search( '' ).columns().search( '' ).column(2).search("other", false, false).draw();
                    break;
            }
        });
        $('#cancelButton').click(function() {
            $("input").trigger('change');
        });
        $("#ground_truth_search").keyup(function () {
            var keyword = $(this).val();
            table.search(keyword).draw();
        });
    });
</script>
