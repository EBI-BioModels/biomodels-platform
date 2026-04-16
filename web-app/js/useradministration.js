/*global $: false
 */
$.jummp.userAdministration = {};
$.jummp.userAdministration.changeUser = function (userId, field, target) {
    "use strict";
    const value = $("#" + field).prop("checked");
    $.ajax({
        url: "/biomodels/userAdministration/" + target + "/" + userId,
        dataType: "json",
        data: {
            value: value
        },
        cache: "false",
        success: function () {
            // redraw the dataTable to reset all changes
            $('#userTable').dataTable().fnDraw();
        }
    });
};

$(document).on('click', ".chk-feature", function (e) {
    if ($(this).prop("checked")) {
        $(this).attr("checked", true);
    } else {
        $(this).removeAttr("checked");
    }
});

$.jummp.userAdministration.loadUserList = function () {
    "use strict";
    const createUserChangeMarkup = function (id, target, enabled) {
        let html;
        const checkboxId = "user-change-" + id + "-" + target;
        html = '<input type="checkbox" id="' + checkboxId + '" ';
        if (enabled) {
            html += 'checked="checked" class="chk-feature"';
        }
        html += '/>&nbsp;<input type="button" class="button" value="update" ' +
            'onclick="$.jummp.userAdministration.changeUser(' + id + ', \'' + checkboxId + '\', \'' + target + '\')"/>';
        return html;
    };

    $('#userTable').dataTable({
        // TODO: in future it might be interesting to allow filtering
        responsive: true,
        bFilter: false,
        columnDefs: [{
            targets: 2, /* For real name column */
            width: "5%"
        }, {
            targets: 4, /* For institution column */
            width: "10%",
            visible: false
        }, {
            targets: 5, /* For ORCID Identifier column */
            width: "5%"
        }],
        aLengthMenu: [[5, 10, 15, 20, 25, 50, 100, -1], [5, 10, 15, 20, 25, 50, 100, "All"]],
        bProcessing: true,
        bServerSide: true,
        bJQueryUI: false,
        sPaginationType: "full_numbers",
        sAjaxSource: 'dataTableSource',
        "fnServerData": function (sSource, aoData, fnCallback) {
            $.ajax({
                "dataType": 'json',
                "type": "POST",
                "url": sSource,
                "data": aoData,
                "error": function () {
                    // clear the table
                    fnCallback({aaData: [], iTotalRecords: 0, iTotalDisplayRecords: 0});
                },
                "success": function (json) {
                    let rowData, id, i;
                    for (i = 0; i < json.aaData.length; i += 1) {
                        rowData = json.aaData[i];
                        id = rowData[0];
                        rowData[0] = "<a href=\"show/" + id + "\">" + id + "</a>";
                        rowData[6] = createUserChangeMarkup(id, 'enable', rowData[6]);
                        rowData[7] = createUserChangeMarkup(id, 'expireAccount', rowData[7]);
                        rowData[8] = createUserChangeMarkup(id, 'lockAccount', rowData[8]);
                        rowData[9] = createUserChangeMarkup(id, 'expirePassword', rowData[9]);
                    }
                    fnCallback(json);
                }
            });
        }
    });
    $('.dataTables_filter input[type="search"]').
        attr('placeholder','Type in username, email or real name...').
        css({'width':'250px','display':'inline-block'});
};

$.jummp.userAdministration.editUser = function () {
    "use strict";
    $("#user-role-management table tr a").on("click", function () {
        let link, id, container, userId, action;
        link = $(this);
        id = link.prev().val();
        container = link.parents("div")[0];
        userId = $($("input", container)[0]).val();
        action = $($("input", container)[1]).val();
        $.ajax({
            type: 'GET',
            url: "../" + action + "/" + id + "?userId=" + userId,
            dataType: 'json',
            cache: 'false',
            beforeSend: function(jqXHR) {

            },
            success: function (data) {
                if (data.error) {
                    $.jummp.errorMessage(data.error);
                } else if (data.success) {
                    var linkText, divInsertId, tableRow;
                    linkText = "";
                    divInsertId = "";
                    if (action === "addRole") {
                        linkText = "Remove Role from User";
                        divInsertId = "#userRoles";
                    } else if (action === "removeRole") {
                        linkText = "Add Role to User";
                        divInsertId = "#availableRoles";
                    }
                    tableRow = link.parents("tr");
                    $("a", tableRow).text(linkText);
                    tableRow.detach();
                    tableRow.appendTo($("table tbody", $(divInsertId)));
                }
            },
            error: (jqXHR) => {
                toastr.error("An error occured: " + jqXHR.status + " " + jqXHR.statusText);
            },
            complete: (jqXHR, status) => {
                toastr.success("The user update has completed " + status);
            }
        });
    });
    $("#edit-user-form").submit(function (event) {
        let msg = "";
        event.preventDefault();
        hideNow();
        $.ajax({
            type: 'GET',
            url: "../editUser",
            dataType: 'json',
            cache: 'false',
            data: {
                username: $("#edit-user-username").val(),
                userRealName: $("#edit-user-userrealname").val(),
                institution: $("#edit-user-institution").val(),
                orcid: $("#edit-user-orcid").val(),
                email: $("#edit-user-email").val()
            },
            success: function (data) {
                if (data.error) {
                    msg = "User could not be updated. Please check the values provided and try again";
                    toastr.error(msg);
                } else if (data.success) {
                    msg = "User details updated";
                    toastr.success(msg);
                }
            },
            error: (jqXHR) => {
                msg = "An error occurred: " + jqXHR.status + " " + jqXHR.statusText;
                toastr.error(msg);
            },
            complete: (jqXHR, status) => {
                msg = "The user update has completed " + status;
                toastr.info(msg);
            }
        });
    });
};

$.jummp.userAdministration.register = function () {
    "use strict";
    $("#registerForm").submit(function (event) {
        event.preventDefault();
        $.ajax({
            type: 'GET',
            url: "performRegistration",
            dataType: 'json',
            cache: 'false',
            data: {
                username: $("#register-form-username").val(),
                userRealName: $("#register-form-name").val(),
                institution: $("#register-form-institution").val(),
                orcid: $("#register-form-orcid").val(),
                email: $("#register-form-email").val()
            },
            success: function (data) {
                if (data.error) {
                	toastr.error("User could not be created. Please check values provided and try again")
                } else if (data.success) {
                	toastr.success("User created successfully")
                }
            }
        });
    });
};
