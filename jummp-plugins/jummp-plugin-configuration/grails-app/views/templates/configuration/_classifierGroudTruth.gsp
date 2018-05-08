<div id="model_data"></div>
<g:javascript src="datatable/jquery.dataTables.min.js" contextPath=""/>
<g:javascript src="datatable/dataTables.buttons.min.js" contextPath=""/>
<g:javascript src="datatable/buttons.flash.min.js" contextPath=""/>
<g:javascript src="datatable/jszip.min.js" contextPath=""/>
<g:javascript src="datatable/pdfmake.min.js" contextPath=""/>
<g:javascript src="datatable/vfs_fonts.js" contextPath=""/>
<g:javascript src="datatable/buttons.html5.min.js" contextPath=""/>
<g:javascript src="datatable/buttons.print.min.js" contextPath=""/>
<g:javascript src="datatable/dataTables.select.min.js" contextPath=""/>
<g:javascript>
    var models = JSON.parse("${models}");
    $('#model_data').DataTable({
        "searching": true,
        "pageLength": 14,
        "columns": [
            {
                "title": "Model Id",
                "data": "modelId",
                "render": function(data, type, row, meta){
                    if(type === 'display'){
                        data = '<a target="_blank" href="https://wwwdev.ebi.ac.uk/biomodels/' + data + '">' + data + '</a>';
                    }

                    return data;
                }
            },
            {"title": "Model Name", "data": "name"},
            {"title": "Date Update", "data": "updateDate", "width": "14%", "className": "dt-center", "targets": "_all"}
        ],
        "dom": 'Bfrtip',
        "buttons": [
            'copy', 'csv', 'excel', 'pdf', 'print'
        ],
        "language": {
            "lengthMenu": '_MENU_ search',
            "search": '<i class="fa fa-search"></i>',
            "searchPlaceholder": "Search",
            "paginate": {
                "previous": '<i class="fa fa-angle-left"></i>',
                "next": '<i class="fa fa-angle-right"></i>'
            }
        },
        "order": [[ 1, 'asc' ]],
        "data": models
    });
</g:javascript>
