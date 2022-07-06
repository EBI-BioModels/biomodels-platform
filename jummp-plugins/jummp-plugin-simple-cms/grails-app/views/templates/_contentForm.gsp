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
        <div class="columns small-12 medium-12 large-12">
            <button type="button" id="btnDashboard" class="button btn-default">Dashboard</button>
            <button type="reset" class="button btn-default">Reset</button>
            <g:if test="${actionName == 'edit'}">
                <button type="button" class="button" id="btnSave">Save</button>
            </g:if>
            <g:else>
                <button type="button" class="button" id="btnCreate">Create</button>
            </g:else>
        </div>
    </div>
</form>
<!-- Optional theme -->
<script src="https://cdn.ckeditor.com/ckeditor5/34.2.0/decoupled-document/ckeditor.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/toastr.js/2.1.4/toastr.min.js"></script>

<g:javascript>
    %{--$('#btnDashboard').on("click", function () {
        window.location.href = "${createLink(controller: "jummp", action: "cms")}"
    });--}%

    let editor;
    $(document).ready(function () {
        DecoupledEditor
        .create(document.querySelector('#contentBody'))
        .then( newEditor => {
            editor = newEditor;
            const toolbarContainer = document.querySelector('#toolbar-container');
            toolbarContainer.appendChild(newEditor.ui.view.toolbar.element);
        })
        .catch( error => {
            console.error(error);
        });
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
            } else if (status == "Failed") {
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

        const cmsContentTC = {
            'id': id,
            'title': title,
            'aliasURI': aliasURI,
            'description': description,
            'content': content,
            'createdBy': createdBy,
            'createdOn': createdOn,
            'lastChangedBy': lastChangedBy,
            'lastChangedOn': lastChangedOn
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
                let slug = data.slug
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
</g:javascript>
