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
        "bmp", "svg", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "cc3d", "zip"];
    $('[id^="previewButton"]').on('click', function (e) {
        e.preventDefault();
        const filename = $(this).attr("data-file-name");
        const mimeType = $(this).attr("data-file-mime-type");
        const downloadLink = $(this).attr("data-download-link");
        const showPreview = $(this).attr("data-preview");

        $.ajax({
            url: downloadLink + "&preview=" + showPreview + "&inline=true",
            dataType: "text",
            success: function(data) {
                $('#boxTitle').html(filename);
                if (mimeType != null) {
                    let fileExtension = "";
                    let imageType = false;
                    let pdfType = false;
                    let mdlType = false;
                    let xmlType = false;
                    let csvType = false;
                    // var msDocument = false;
                    const content = [];
                    for (let index in formats) {
                        let format = formats[index];
                        if (mimeType.indexOf(format) != -1) {
                            if (format === "jpg" || format === "jpeg" || format === "gif" ||
                                format === "png" || format === "bmp" || format === "svg") {
                                imageType = true;
                            } else if (format === "txt" || format === "text" || format === "xml" || format === "cc3d") {
                                if (filename.indexOf('.mdl') !== -1) {
                                    mdlType = true;
                                    fileExtension = "mdl";
                                }
                                if (filename.indexOf('.xml') !== -1 || filename.indexOf('.cc3d') !== -1 ) {
                                    xmlType = true;
                                    fileExtension = "Xml";
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
                    // create a placeholder where the content is put down
                    $('#previewContentContainer').html(content.join(""));
                    if (data === "BIG_FILE") {
                        //alert("File " + filename + " is too big to be served now. Please contact us if you really need to download this file.");
                        // $('#previewContentContainer').html("");
                        addPreviewNotification(showPreview, downloadLink, true);
                        return;
                    } else if (mdlType || xmlType) {
                        let brush;
                        if (fileExtension == "mdl") {
                            brush = new SyntaxHighlighter.brushes.mdl();
                        } else {
                            brush = new SyntaxHighlighter.brushes.Xml();
                        }
                        brush.init({ toolbar: false });
                        const html = brush.getHtml(data);
                        $('#filegoeshere').html(html);
                        $('#previewContentContainer').removeClass("forPdf").addClass("forCode");
                        addPreviewNotification(showPreview, downloadLink);
                        //$(".syntaxhighlighter").css({'max-height': (screen.height * 0.45)+'px'});
                    } else if (imageType) {
                        const img = $("<img style='width: 100%;' />").attr('src', downloadLink + "&inline=true")
                            .load(function () {
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
                        const h = $('#previewContentContainer').height() * 0.98;
                        let cont = [];
                        cont.push("<iframe width='100%' height='" + h + "px' src='");
                        cont.push(downloadLink+"&inline=true' />");
                        const frame = $(cont.join(""));
                        $('#filegoeshere').append(frame);
                        $('#previewContentContainer').removeClass("forCode").addClass("forPdf");
                        addPreviewNotification(showPreview, downloadLink);
                    } else if (filename.indexOf('.csv') === -1 && (mimeType.indexOf("txt") !== -1 || mimeType.indexOf("text") !== -1)) {
                        data = data.replace(/(\r\n|\n|\r)/gm, '<br/>');
                        $("#filegoeshere").html(data);
                        $('#previewContentContainer').removeClass("forPdf").addClass("forCode");
                        addPreviewNotification(showPreview, downloadLink);
                    } else if (csvType) {
                        const plottingData = getCSVData(data);
                        const handsontable = $("<div id='handsontable' class='hot handsontable htRowHeaders htColumnHeaders'></div>");
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
                        let message = "<h3>Files of this type cannot be displayed here. Please <a href='";
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

    function addPreviewNotification(showNotification, downloadLink, bigFile = false) {
        if (showNotification && !bigFile) {
            $("#notificationgoeshere").html("<h4 style='color: darkorange'>As this is a large file, only a part of it is loaded below. " +
                "<a id='loadFileCompletely' href='" + downloadLink + "'>Click here</a> " +
                "to download the file to your device. Please be aware that downloading it may be slow.</h4");
        } else if (bigFile) {
            $("#notificationgoeshere").html("<h4 style='color: darkorange'>The file is too large for serving now. " +
                "Please contact us if you actually need to download it.</h4>");
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
