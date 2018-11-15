<%
    List modelFiles = repoFiles.findAll { it.mainFile }
    List additionalFiles = repoFiles.findAll { !it.mainFile }
    mfMap = [:]
    mfMap["repoFiles"] = modelFiles
    afMap = [:]
    afMap["repoFiles"] = additionalFiles
%>
<style>
    .forCode {
        resize: both;
        overflow: auto;
        height: 80vh;
    }
    .forPdf {
        height: 80vh;
    }
    /* this class class is being used in sub templates */
    .type-of-file-heading {
        font-size: 26px;
    }
    .pad-bottom {
        padding-bottom: 20px;
    }
    .pad-left {
        padding-left: 20px;
    }
    .pad-right {
        padding-right: 20px;
    }
</style>
<table class="responsive-card-table stack">
    <thead>
        <tr>
            <th width="30%">Name</th>
            <th>Description</th>
            <th width="10%">Size</th>
            <th width="20%">Actions</th>
        </tr>
    </thead>
    <tbody>
        <g:render template="/templates/biomodels/modelDisplay/renderModelFiles" model="${mfMap}"/>
        <g:if test="${afMap["repoFiles"]}">
            <g:render template="/templates/biomodels/modelDisplay/renderAdditionalFiles" model="${afMap}"/>
        </g:if>
    </tbody>
</table>

<!-- this following is served the modal form for Preview box -->
<div class="large reveal" id="filePreviewBox" data-reveal>
    <h3 id="boxTitle"
        style="border-bottom: 1px solid grey"></h3>
    <button class="close-button" data-close aria-label="Close modal" type="button">
        <span aria-hidden="true">&times;</span>
    </button>
    <div class="contact-panel forCode" id="previewContentContainer" data-toggler=".is-active">
    </div>
</div>

<script type="text/javascript">
    var formats = ["text", "txt", "xml", "pdf", "jpg", "jpeg", "gif", "png",
        "bmp", "svg", "doc", "docx", "xls", "xlsx", "ppt", "pptx"];
    $('[id^="previewButton"]').on('click', function (e) {
        e.preventDefault();
        var filename = $(this).attr("data-file-name");
        var mimeType = $(this).attr("data-file-mime-type");
        var downloadLink = $(this).attr("data-download-link");
        var showPreview = $(this).attr("data-preview");
        $.ajax({
            url: downloadLink + "&preview=" + showPreview + "&inline=true",
            dataType: "text",
            success: function(data) {
                $('#boxTitle').html(filename);
                if (mimeType != null) {
                    var fileExtention = "";
                    var imageType = false;
                    var pdfType = false;
                    var mdlType = false;
                    var xmlType = false;
                    var csvType = false;
                    // var msDocument = false;
                    var content = [];
                    for (var index in formats) {
                        var format = formats[index];
                        if (mimeType.indexOf(format) != -1) {
                            if (format === "jpg" || format === "jpeg" || format === "gif" ||
                                format === "png" || format === "bmp" || format === "svg") {
                                imageType = true;
                            } else if (format === "txt" || format === "text" || format === "xml") {
                                if (filename.indexOf('.mdl') !== -1) {
                                    mdlType = true;
                                    fileExtention = "mdl";
                                }
                                if (filename.indexOf('.xml') !== -1) {
                                    xmlType = true;
                                    fileExtention = "Xml";
                                }
                                if (filename.indexOf('.csv') !== -1) {
                                    csvType = true;
                                }
                            } else if (format === "pdf") {
                                pdfType = true;
                            } /* don't support Microsoft Document for now
                            else if (format === "doc" || format === "docx"
                                || format === "xls" || format === "xlsx"
                                || format === "ppt" || format === "pptx") {
                                msDocument = true;
                            }*/
                            content.push("<div id='notificationgoeshere' class='pad-left pad-bottom' style='font-size: 18px'></div>");
                            content.push("<div id='filegoeshere' class='pad-right pad-bottom");
                            if (!mdlType && !xmlType) {
                                content.push(" pad-left");
                            }
                            content.push("'></div>");
                        }
                    }
                    // create a place holder where the content is put down
                    $('#previewContentContainer').html(content.join(""));
                    if (mdlType || xmlType) {
                        var brush;
                        if (fileExtention == "mdl") {
                            brush = new SyntaxHighlighter.brushes.mdl();
                        } else {
                            brush = new SyntaxHighlighter.brushes.Xml();
                        }
                        brush.init({ toolbar: false });
                        var html = brush.getHtml(data);
                        $('#filegoeshere').html(html);
                        $('#previewContentContainer').removeClass("forPdf").addClass("forCode");
                        addPreviewNotification(showPreview, downloadLink);
                        //$(".syntaxhighlighter").css({'max-height': (screen.height * 0.45)+'px'});
                    } else if (imageType) {
                        var img = $("<img style='width: 100%;' />").attr('src', downloadLink+"&inline=true")
                            .load(function() {
                                if (!this.complete
                                    || typeof this.naturalWidth === "undefined"
                                    || this.naturalWidth === 0) {
                                    $('#filegoeshere').text("Image could not be loaded")
                                } else {
                                    $('#filegoeshere').append(img);
                                }
                            });
                        $('#previewContentContainer').removeClass("forPdf").addClass("forCode");
                        addPreviewNotification(showPreview, downloadLink);
                    } else if (pdfType) {
                        var h = $('#previewContentContainer').height()*0.98;
                        var cont = [];
                        cont.push("<iframe width='100%' height='" + h + "px' src='");
                        cont.push(downloadLink+"&inline=true' />");
                        var frame = $(cont.join(""));
                        $('#filegoeshere').append(frame);
                        $('#previewContentContainer').removeClass("forCode").addClass("forPdf");
                        addPreviewNotification(showPreview, downloadLink);
                    } else if (filename.indexOf('.csv') === -1 && (mimeType.indexOf("txt") !== -1 || mimeType.indexOf("text") !== -1)) {
                        data = data.replace(/(\r\n|\n|\r)/gm, '<br/>');
                        $("#filegoeshere").html(data);
                        $('#previewContentContainer').removeClass("forPdf").addClass("forCode");
                        addPreviewNotification(showPreview, downloadLink);
                    } else if (csvType) {
                        var plottingData = getCSVData(data);
                        var handsontable = $("<div id='handsontable' class='hot handsontable htRowHeaders htColumnHeaders'></div>");
                        $('#filegoeshere').append(handsontable);
                        $('#handsontable').handsontable({
                            data: plottingData,
                            stretchH: 'all',
                            readOnly: true,
                            colHeaders: true, filters: true, columnSorting: true
                        });
                        $('#previewContentContainer').removeClass("forPdf").addClass("forCode");
                        addPreviewNotification(showPreview, downloadLink);
                    } else {
                        $("#notificationgoeshere").show();
                        var message = "<h3>Files of this type cannot be displayed here. Please <a href='";
                        message += downloadLink;
                        message += "'>download</a> the file to your device to view it.</h3>"
                        $("#notificationgoeshere").html(message);
                    }
                }
            }, 
            error: function (jqXHR, errorThrown) {
                $("#notificationgoeshere").show();
                $("#notificationgoeshere").html("Error: ", jqXHR.responseText + " " + errorThrown + JSON.stringify(jqXHR));
            }
        });
    });

    function addPreviewNotification(showNotification, downloadLink) {
        if (showNotification === "true") {
            $("#notificationgoeshere").html("As this is a large file, only a part of it is loaded below. " +
                "<a id='loadFileCompletely' href='" + downloadLink + "'>Click here</a> " +
                "to download the file to your device. Please be warned that this may be slow.");
        }
        else {
            $("#notificationgoeshere").hide();
        }
    }

    function getCSVData(data) {
        var lines = data.match(/[^\r\n]+/g);
        var data = [];
        for (var id = 0; id < lines.length; id++) {
            var line = lines[id];
            var fields = line.split(",");
            data.push(fields);
        }
        return data;
    }
</script>
