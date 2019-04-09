<div class="row">
    <div class="small-12 medium-6 medium-centered large-6 large-centered columns">
        <g:form action="update" method="POST">
            <div class="row">
                <div class="small-12 columns">
                    <input id="id" name="id" value="${tag?.id}" hidden>
                </div>
            </div>
            <div class="row">
                <div class="small-3 columns">
                    <label for="name" class="right inline">Tag Name</label>
                </div>
                <div class="small-9 columns">
                    <input type="text" id="name" name="name"
                           placeholder="Enter A Tag Name" value="${tag?.name}">
                </div>
            </div>
            <div class="row">
                <div class="small-3 columns">
                    <label for="userCreated" class="right inline">Created By</label>
                </div>
                <div class="small-9 columns">
                    <g:textField type="text" id="userCreated" name="userCreated"
                                 placeholder="Enter A User Name" value="${tag?.userCreated?.username}"/>
                </div>
            </div>
            <div class="row">
                <div class="small-3 columns">
                    <label for="dateCreated" class="right inline">Created On</label>
                </div>
                <div class="small-9 columns">
                    <g:textField type="text" id="dateCreated" name="dateCreated"
                                 placeholder="Enter A Date" value="${dateFormat.format(tag?.dateCreated)}"/>
                </div>
            </div>
            <div class="row">
                <div class="small-3 columns">
                    <label for="dateModified" class="right inline">Modified On</label>
                </div>
                <div class="small-9 columns">
                    <g:textField type="text" id="dateModified" name="dateModified"
                                 placeholder="Enter A Date" value="${dateFormat.format(tag?.dateModified)}"/>
                </div>
            </div>
            <div class="row">
                <div class="columns text-center">
                    <button type="button" class="button" name="btnBack" id="btnBack">Back</button>
                    <button type="button" class="button" name="btnReset" id="btnReset">Reset</button>
                    <button type="submit" class="button" name="btnSave" id="btnSave">Save</button>
                </div>
            </div>
        </g:form>
    </div>
</div>
<g:javascript>
    $('#btnBack').click(function () {
        window.location.href = "${g.createLink(controller: "tag", action: "index")}";
    });
    $('#btnReset').click(function (event) {
        event.preventDefault();
        $('#name').val("${tag?.name}");
        $('#userCreated').val("${tag.userCreated?.username}");
        $('#dateCreated').val("${dateFormat.format(tag?.dateCreated)}");
        $('#dateModified').val("${dateFormat.format(tag?.dateModified)}");
    });
</g:javascript>
