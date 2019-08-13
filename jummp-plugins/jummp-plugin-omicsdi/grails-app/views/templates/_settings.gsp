<%--
 Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>






<div id="OmicsDISchemaXMLeditor" class="editor">
    <div class="row">
        <div class="columns small-12 medium-6 large-6">
        <h3>How to export OmicsDI XML file(s)</h3>
        <input type="radio" name="howToExportFile" value="1"
               checked> All models of the whole database will be exported in an XML file.<br>
        <input type="radio" name="howToExportFile" value="2"> Multiple models will be accommodated in an XML file.
        <div id="nbEntries" class="small-12 medium-6">
            <label>How many models are exported in each XML file?
                <input type="number" id="numberEntriesOnEachFile"
                       width="10%"/></label>
        </div></div>

        <div class="columns small-12 medium-6 large-6">
        <h3>How to exclude models based on tags</h3>

        <p>We can exclude multiple models from the export by indicating tags associated with those excluded models</p>
        <select class="js-data-example-ajax" id="tagsExcluded"
                name="tagsExcluded"></select>
        </div></div>

    <div class="row">
        <div class="columns small-12 medium-6 large-6">
        <h3>How to launch the job</h3>
        <button class="button" type="button" id="btnSaveSettings">Save changes of OmicsDI Export Settings</button>
        <button class="button" type="button" id="btnExport">Export OmicsDI entries via JummpIndexer</button>
        </div></div>
</div>
<script type="text/javascript">
    $(document).ready(function () {
        $('#nbEntries').hide();
    });
    let element = $('input[name="howToExportFile"]');
    let res = element.filter(function () {
        return this.checked;
    });
    // when select option 2, need to enter the number of file
    let option = 1;
    let nbEntriesPerFile = '';
    $('input[name="howToExportFile"]').click(function () {
        option = $(this).val();
        if (option === "2") {
            $('#numberEntriesOnEachFile').prop('required', true);
            $('#nbEntries').show();
        } else {
            $('#numberEntriesOnEachFile').val('');
            $('#numberEntriesOnEachFile').removeAttr('required');
            $('#nbEntries').hide();
        }
    });
    $('.js-data-example-ajax').select2({
        placeholder: "Enter list of tags excluded from export",
        multiple: true,
        tags: true, scrollAfterSelect: true,
        ajax: {
            url: "${createLink(controller: "omicsdi", action: "fetchAllTags")}",
            dataType: 'json',
            data: function(term) {
                return term;
            },
            processResults: function(data, page) {
                return {
                    results: $.map(data,
                        function (item) {
                            return {
                                text: item.name,
                                id: item.id
                            }
                        })
                    }
                }
        }
    });
    $('#btnExport').click(function () {
        let data = $('.js-data-example-ajax').select2('data');
        let tags = $.map(data,
            function (item) {
                return [
                    item.text
                ]
            });
        if (option === "2") {
            var nbFiles = $('#numberEntriesOnEachFile').val();
            if (nbFiles === '') {
                showNotification("Please enter a positive integer number.");
                $('#numberEntriesOnEachFile').focus();
                return false;
            } else {
                nbEntriesPerFile = nbFiles;
            }
        }
        $.ajax({
            type: "post",
            dataType: "JSON",
            cache: false,
            url: $.jummp.createLink("Omicsdi", "exportOmicsdiEntriesWithIndexer"),
            data: {
                howToExportFile: option,
                numberEntriesOnEachFile: $('#numberEntriesOnEachFile').val(),
                tags: tags
            },
            success: function (response) {
                let message = response[0];
                if (message.trim()) {
                    message = message.trim();
                    showNotification(message);
                }
            }
        });
    });
</script>
