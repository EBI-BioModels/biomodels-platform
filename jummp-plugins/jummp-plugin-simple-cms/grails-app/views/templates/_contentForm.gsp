<style>
    .ck-editor__editable {
        min-height: 300px;
    }
    .ck {
        padding-bottom: 10px;
    }
    #contentBody {
        border: 1px solid lightgrey;
        margin-bottom: 10px;
    }
</style>
<form class="form-horizontal">
    <div class="row">
        <div class="columns small-12 medium-6 large-6">
            <label>Title <span style="color: red">(*)</span>
                <input type="text" placeholder="Type a title of this content"
                       id="title" name="title" value="${content?.title}">
            </label>
        </div>
        <div class="columns small-12 medium-6 large-6">
            <label>Alias URI or Slug <span style="color: red">(*)</span>
                <input type="text" placeholder="Customise the slug for this content"
                       id="aliasURI" name="aliasURI" value="${content?.aliasURI}">
            </label>
            <label>Alias URI or Slug of the parent node <span style="color: red">(*)</span>
                <input type="text" placeholder="The alias URI or Slug of the parent node, for example: news, model-of-the-year,..."
                       id="parentAliasURI" name="parentAliasURI" value="${content?.parentAliasURI}">
                <div id="suggestion-box"></div>
            </label>
        </div>
    </div>

    <div class="row">
        <div class="columns small-12 medium-12 large-12">
        <label for="description" class="col-sm-2 control-label">Description <span style="color: red">(*)</span>
            <input type="text" class="form-control" id="description" name="description"
                   placeholder="Type a short description about this content"
                   value="${content?.description}"></label>
        </div>
    </div>

    <div class="row">
        <div class="small-12 medium-3 large-3 columns">
            <label>Initially created by <small style="color: red">required</small>
                <input type="text" id="createdBy" name="createdBy" required
                       placeholder="the user who had created this content initially"
                       value="${content?.createdBy}" readonly>
            </label>
        </div>
        <div class="small-12 medium-3 large-3 columns">
            <label>Date created <small style="color: red">required</small>
                <input type="text" id="createdOn" required
                       placeholder="enter the date when the content was created"
                       value="${dateFormat.format(content?.createdOn)}">
            </label>
        </div>

        <div class="small-12 medium-3 large-3 columns">
            <label>Last changed by <small style="color: red">required</small>
                <input type="text" id="lastChangedBy" required
                       placeholder="the last modifier who is updating this content"
                       value="${content?.lastChangedBy}" readonly>
            </label>
        </div>
        <div class="small-12 medium-3 large-3 columns">
            <label>Last changed <small style="color: red">required</small>
                <input type="text" id="lastChangedOn" required
                       placeholder="enter the latest date when the content has been updated"
                       value="${dateFormat.format(content?.lastChangedOn)}">
            </label>
        </div>
    </div>

    <div class="row">
        <div class="columns small-12 medium-12 large-12">
            <label for="contentBody" class="col-sm-2 control-label">Body <span style="color: red">(*)</span></label>
            <!-- The toolbar will be rendered in this container. -->
            <div id="toolbar-container"></div>
            <div id="contentBody">${content?.content}</div>
            <!-- Below is used with Classic Editor or CKEDITOR 4 -->
            <!--
            <label for="contentBody" class="col-sm-2 control-label">Body <span style="color: red">(*)</span>
            <textarea id="contentBody" name="contentBody" aria-multiline="true" rows="20"
                style="white-space: pre-wrap">${content?.content}</textarea></label>
            -->
        </div>
    </div>

    <div class="row">
        <% def dashboardLink = createLink(uri: grailsApplication.config.grails.serverURL + "/cms/content")%>
        <div class="columns small-12 medium-12 large-12">
            <button type="button" id="btnDashboard" class="button btn-default"
                    onclick='$.jummp.openPage("${dashboardLink}")'>Dashboard</button>
            <button type="reset" class="button btn-default">Reset</button>
            <g:if test="${actionName == 'edit'}">
                <button type="button" class="button btn-default"
                        onclick="redirectToShow(${id})">Show</button>
                <button type="button" class="button" id="btnSave">Save</button>
            </g:if>
            <g:else>
                <button type="button" class="button" id="btnCreate">Create</button>
            </g:else>
        </div>
    </div>
</form>
<!-- Optional theme -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/toastr.js/2.1.4/toastr.min.js"></script>
<script src="https://cdn.ckeditor.com/ckeditor5/40.1.0/super-build/ckeditor.js"></script>
<script>
    /* Below is the object to be used in the further processes */
    let editor;

    // This sample still does not showcase all CKEditor&nbsp;5 features (!)
    // Visit https://ckeditor.com/docs/ckeditor5/latest/features/index.html to browse all the features.
    CKEDITOR.ClassicEditor.create(document.getElementById("contentBody"), {
        // https://ckeditor.com/docs/ckeditor5/latest/features/toolbar/toolbar.html#extended-toolbar-configuration-format
        toolbar: {
            items: [
                'exportPDF','exportWord', '|',
                'findAndReplace', 'selectAll', '|',
                'heading', '|',
                'bold', 'italic', 'strikethrough', 'underline', 'code', 'subscript', 'superscript', 'removeFormat', '|',
                'bulletedList', 'numberedList', 'todoList', '|',
                'outdent', 'indent', '|',
                'undo', 'redo',
                '-',
                'fontSize', 'fontFamily', 'fontColor', 'fontBackgroundColor', 'highlight', '|',
                'alignment', '|',
                'link', 'insertImage', 'blockQuote', 'insertTable', 'mediaEmbed', 'codeBlock', 'htmlEmbed', '|',
                'specialCharacters', 'horizontalLine', 'pageBreak', '|',
                'textPartLanguage', '|',
                'sourceEditing'
            ],
            shouldNotGroupWhenFull: true
        },
        // Changing the language of the editor interface requires loading the language file using the <script> tag.
        // language: 'es',
        list: {
            properties: {
                styles: true,
                startIndex: true,
                reversed: true
            }
        },
        // https://ckeditor.com/docs/ckeditor5/latest/features/headings.html#configuration
        heading: {
            options: [
                { model: 'paragraph', title: 'Paragraph', class: 'ck-heading_paragraph' },
                { model: 'heading1', view: 'h1', title: 'Heading 1', class: 'ck-heading_heading1' },
                { model: 'heading2', view: 'h2', title: 'Heading 2', class: 'ck-heading_heading2' },
                { model: 'heading3', view: 'h3', title: 'Heading 3', class: 'ck-heading_heading3' },
                { model: 'heading4', view: 'h4', title: 'Heading 4', class: 'ck-heading_heading4' },
                { model: 'heading5', view: 'h5', title: 'Heading 5', class: 'ck-heading_heading5' },
                { model: 'heading6', view: 'h6', title: 'Heading 6', class: 'ck-heading_heading6' }
            ]
        },
        // https://ckeditor.com/docs/ckeditor5/latest/features/editor-placeholder.html#using-the-editor-configuration
        placeholder: 'Welcome to CKEditor&nbsp;5!',
        // https://ckeditor.com/docs/ckeditor5/latest/features/font.html#configuring-the-font-family-feature
        fontFamily: {
            options: [
                'default',
                'Arial, Helvetica, sans-serif',
                'Courier New, Courier, monospace',
                'Georgia, serif',
                'Lucida Sans Unicode, Lucida Grande, sans-serif',
                'Tahoma, Geneva, sans-serif',
                'Times New Roman, Times, serif',
                'Trebuchet MS, Helvetica, sans-serif',
                'Verdana, Geneva, sans-serif'
            ],
            supportAllValues: true
        },
        // https://ckeditor.com/docs/ckeditor5/latest/features/font.html#configuring-the-font-size-feature
        fontSize: {
            options: [ 10, 12, 14, 'default', 18, 20, 22 ],
            supportAllValues: true
        },
        // Be careful with the setting below. It instructs CKEditor to accept ALL HTML markup.
        // https://ckeditor.com/docs/ckeditor5/latest/features/general-html-support.html#enabling-all-html-features
        htmlSupport: {
            allow: [
                {
                    name: /.*/,
                    attributes: true,
                    classes: true,
                    styles: true
                }
            ]
        },
        // Be careful with enabling previews
        // https://ckeditor.com/docs/ckeditor5/latest/features/html-embed.html#content-previews
        htmlEmbed: {
            showPreviews: true
        },
        // https://ckeditor.com/docs/ckeditor5/latest/features/link.html#custom-link-attributes-decorators
        link: {
            decorators: {
                addTargetToExternalLinks: true,
                defaultProtocol: 'https://',
                toggleDownloadable: {
                    mode: 'manual',
                    label: 'Downloadable',
                    attributes: {
                        download: 'file'
                    }
                }
            }
        },
        // https://ckeditor.com/docs/ckeditor5/latest/features/mentions.html#configuration
        mention: {
            feeds: [
                {
                    marker: '@',
                    feed: [
                        '@apple', '@bears', '@brownie', '@cake', '@cake', '@candy', '@canes', '@chocolate', '@cookie', '@cotton', '@cream',
                        '@cupcake', '@danish', '@donut', '@dragée', '@fruitcake', '@gingerbread', '@gummi', '@ice', '@jelly-o',
                        '@liquorice', '@macaroon', '@marzipan', '@oat', '@pie', '@plum', '@pudding', '@sesame', '@snaps', '@soufflé',
                        '@sugar', '@sweet', '@topping', '@wafer'
                    ],
                    minimumCharacters: 1
                }
            ]
        },
        // The "super-build" contains more premium features that require additional configuration, disable them below.
        // Do not turn them on unless you read the documentation and know how to configure them and setup the editor.
        removePlugins: [
            // These two are commercial, but you can try them out without registering to a trial.
            // 'ExportPdf',
            // 'ExportWord',
            'AIAssistant',
            'CKBox',
            'CKFinder',
            'EasyImage',
            // This sample uses the Base64UploadAdapter to handle image uploads as it requires no configuration.
            // https://ckeditor.com/docs/ckeditor5/latest/features/images/image-upload/base64-upload-adapter.html
            // Storing images as Base64 is usually a very bad idea.
            // Replace it on production website with other solutions:
            // https://ckeditor.com/docs/ckeditor5/latest/features/images/image-upload/image-upload.html
            // 'Base64UploadAdapter',
            'RealTimeCollaborativeComments',
            'RealTimeCollaborativeTrackChanges',
            'RealTimeCollaborativeRevisionHistory',
            'PresenceList',
            'Comments',
            'TrackChanges',
            'TrackChangesData',
            'RevisionHistory',
            'Pagination',
            'WProofreader',
            // Careful, with the Mathtype plugin CKEditor will not load when loading this sample
            // from a local file system (file://) - load this site via HTTP server if you enable MathType.
            'MathType',
            // The following features are part of the Productivity Pack and require additional license.
            'SlashCommand',
            'Template',
            'DocumentOutline',
            'FormatPainter',
            'TableOfContents',
            'PasteFromOfficeEnhanced'
        ]
    }).then(newEditor => {
        editor = newEditor;
    }).catch(error => {
        console.error(error);
    });
</script>
<g:javascript>
    $(document).ready(function() {
        // AJAX call for autocomplete
        searchAutocomplete();
    });

    $('#btnSave, #btnCreate').on("click", function (event) {
        "use strict";
        event.preventDefault();
        const data = buildCmsContentTransportCommand();
        $.ajax({
            type: "POST",
            url: $.jummp.createLink("cmsContent", "save"),
            cache: true,
            processData: true,
            dataType: "json",
            data: data,
            beforeSend: function () {
                let msg = "Saving updates and modifications into the database...";
                toastr.clear();
                toastr.info(msg);
            }
        }).done(function (data, txtStatus, jqXHR) {
            let message = data.message;
            let status = data.status;
            toastr.clear();
            if (status === "Succeeded") {
                toastr.success(message);
                if ("${actionName}" === "create") {
                    toastr.warning("Your content has been created successfully. Please wait 3s before redirecting...");
                    setTimeout(function() {
                        redirectToShow(data.id);
                    }, 3000);
                }
            } else if (status === "Failed") {
                toastr.error(message);
            }
            /*let statusCode = data.status
            if (statusCode === 200) {
                toastr.success(msg);
            } else if (statusCode === 400) {
                toastr.error(msg);
            } else if (statusCode === 422) {
                toastr.warn(msg);
            } else {
                toastr.error("Cannot determine the reason for the unexpected error.");
            }*/
        }).fail(function (jqXHR, status, errorThrown) {
            let msg = jqXHR.statusText;
            toastr.clear();
            toastr.error(msg);
        });
    });

    function buildCmsContentTransportCommand(format) {
        let id = -1;
        if ("${actionName}" === "edit") {
            id = "${content.id}";
        }
        const title = $('#title').val();
        const aliasURI = $('#aliasURI').val();
        const description = $('#description').val();
        const content = editor.getData(format);
        const createdBy = $('#createdBy').val();
        const createdOn = $('#createdOn').val();
        const lastChangedBy = $('#lastChangedBy').val();
        const lastChangedOn = $('#lastChangedOn').val();
        const parentAliasURI = $('#parentAliasURI').val();

        const cmsContentTC = {
            'id': id,
            'title': title,
            'aliasURI': aliasURI,
            'description': description,
            'content': content,
            'createdBy': createdBy,
            'createdOn': createdOn,
            'lastChangedBy': lastChangedBy,
            'lastChangedOn': lastChangedOn,
            'parentAliasURI': parentAliasURI
        }
        return cmsContentTC;
    }

    $('#title').on("blur", function() {
        const title = $(this).val();
        if (title) {
            const data = { 'title': title };
            let message = "";
            $.ajax({
                type: "POST",
                url: $.jummp.createLink("cmsContent", "generateSlug"),
                cache: true,
                processData: true,
                dataType: "json",
                data: data,
            }).done(function (data, txtStatus, jqXHR) {
                let slug = data.slug;
                $('#aliasURI').val(slug);
            }).fail(function (jqXHR, status, errorThrown) {
                message = jqXHR.statusText;
                console.log(message);
            });
        }
    });

    $('#createdOn').datepicker({
        dateFormat: 'yy-mm-dd',
        onSelect: function(datetext) {
            datetext = datetext + getTimeStamp();
            $('#datepicker').val(datetext);
            $(this).val(datetext);
        }
    });

    $('#lastChangedOn').datepicker({
        dateFormat: 'yy-mm-dd',
        onSelect: function(datetext) {
            datetext = datetext + getTimeStamp();
            $('#datepicker').val(datetext);
            $(this).val(datetext);
        }
    });

    function redirectToShow(id) {
        window.location.href = "${createLink(uri: "/cms/content/show/")}" + id;
    }

    function searchAutocomplete() {
        $("#parentAliasURI").on("keyup", function(){
            const postURL = "${createLink(controller: "cmsContent", action: "searchAliasURIForEditorForm")}";
            $.ajax({
                type: "POST",
                url: postURL,
                data: {
                    searchTerm: $(this).val(),
                    column: 1 // or 2
                },
                beforeSend: function(){
                    $("#parentAliasURI").css("background", "#FFF url(${serverURL}/images/loading.gif) no-repeat 225px");
                },
                success: function(data) {
                    const posts = data["posts"];
                    if (posts.length > 0) {
                        $("#suggestion-box").show();
                        $("#suggestion-box").html(data["htmlBasedStringOfPosts"]);
                    }
                    $("#parentAliasURI").css("background", "#ffffff"); //"#87cefa"
                },
                error: (err) => {
                    const message = err["message"];
                }
            });
        });
    }

    // To select a parent node found: to display
    function selectFoundPost(val) {
        $("#parentAliasURI").val(val);
        $("#suggestion-box").hide();
    }

</g:javascript>
