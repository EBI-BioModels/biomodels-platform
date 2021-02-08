const DELIMITER = "|";
function addAuthor() {
    if ($('#newAuthorName').val()) {
        var userRealName = $('#newAuthorName').val();
        var orcid = $('#newAuthorOrcid').val() || "";
        var institution = $('#newAuthorInstitution').val() || "";
        if (authorList.filter(function(v) {
            return v["userRealName"] === userRealName &&
                   v["orcid"] === orcid &&
                   v["institution"] === institution;
            })[0]) {
            showNotification("The author named " + userRealName + " already exists. Please change it or enter an another name.");
        } else {
            // add the new author to the authors list
            var newAuthor = {userRealName: userRealName, institution: institution, orcid: orcid};
            authorList.push(newAuthor);
            // display/add it to the option element
            var id = userRealName + DELIMITER + orcid + DELIMITER + institution;
            $('#authorList').attr('size', 4);
            $('#authorList')
                .append($('<option>', {
                    value : id,
                    "data-person-id": "undefined",
                    "data-person-realname": userRealName,
                    "data-person-orcid": orcid,
                    "data-person-institution": institution
                }).text(userRealName));
            showNotification("The author has been added in the author list.");
            // update the temporary hidden element playing as transporter
            updateData();
        }
        // clean up the text boxes
        $('#newAuthorName').val("");
        $('#newAuthorOrcid').val("");
        $('#newAuthorInstitution').val("");
    } else {
        showNotification("Please enter a name.");
    }
}
function deleteAuthor() {
    var userRealName = $('#newAuthorName').val();
    var orcid = $('#newAuthorOrcid').val() || "";
    var institution = $('#newAuthorInstitution').val() || "";
    var deletedAuthor = authorList.filter(function(v) {
        return v["userRealName"] === userRealName &&
               v["orcid"] === orcid &&
               v["institution"] === institution;
    })[0];
    if (deletedAuthor) {
        for (index in authorList)
            if (authorList[index].userRealName === userRealName &&
                authorList[index].orcid === orcid &&
                authorList[index].institution === institution) {
                authorList.splice(index, 1);
            }
        showNotification("The author has been deleted.")
        updateData();
        $("#authorList option:selected").remove();
        $('#newAuthorName').val("");
        $('#newAuthorOrcid').val("");
        $('#newAuthorInstitution').val("");
    } else {
        showNotification("Please select an author before deleting it.")
    }
}
function updateAuthor() {
    let selectedIndex = $('#authorList').prop('selectedIndex');
    if ($('#newAuthorName').val()) {
        let personId = $("#authorList option:selected").attr("data-person-id");
        let userRealName = $('#newAuthorName').val();
        let orcid = $('#newAuthorOrcid').val() || "";
        let institution = $('#newAuthorInstitution').val() || "";
        let position;
        let updatedAuthor = authorList.filter(function(v,index) {
            position = index;
            return v["userRealName"] == userRealName &&
                v["orcid"] == orcid &&
                v["institution"] == institution;
        })[0];
        if (updatedAuthor) {
            showNotification("No changes need to be saved.");
        } else if (selectedIndex < 0) {
            showNotification("Cannot update a nonexistent author (i.e. " + userRealName + ")");
        } else {
            // create a new author before updating
            let newAuthor = {userRealName: userRealName, institution: institution, orcid: orcid};
            if (personId !== "undefined") {
                newAuthor["id"] = parseInt(personId);
            }
            // display/add it to the option element
            let pId = personId === "undefined" ? "" : personId + DELIMITER
            let id = pId + userRealName + DELIMITER + orcid + DELIMITER + institution;
            let position = selectedIndex;
            authorList[position] = newAuthor;
            let selected = $("#authorList option:selected");
            $('#authorList option:selected')
                .after($('<option>', {
                    value : id,
                    "data-person-id": personId,
                    "data-person-realname": userRealName,
                    "data-person-orcid": orcid,
                    "data-person-institution": institution
                }).text(userRealName));
            $(selected).remove();
            showNotification("The author has been updated.");
            // update the temporary hidden element playing as transporter
            updateData();
        }
        // clean up the text boxes
        $('#newAuthorName').val("");
        $('#newAuthorOrcid').val("");
        $('#newAuthorInstitution').val("");
    } else {
        showNotification("Please select a name before updating it.");
    }
}
/**
 * Update the content of a temporary HTML element containing the author list on client side will
 * be sent back to controller.
 * This content is formed at a JSON string that could be parsed by JsonSlurper on server side.
 */
function updateTempDataDivElement() {
    let input = "<textarea name='authorListContainer' style='width: 200%; height: 40px'>"
    const authors = authorMap.authors.length ? JSON.stringify(authorMap) : "";
    input += authors + "</textarea>";
    document.getElementById("authorListTemp").innerHTML = input;
}
function updateData() {
    // update the author map
    authorMap.authors = [];
    $.each(authorList, function(index, entry) {
        let userRealName = entry["userRealName"];
        let institution = entry["institution"] || "";
        let orcid = entry["orcid"] || "";
        let obj = {'userRealName': userRealName, 'institution': institution, 'orcid': orcid};
        if (entry["id"] !== "undefined") {
            obj["id"] = entry["id"];
        }
        authorMap.authors.push(obj);
    });
    updateTempDataDivElement();
}

$(document).ready(function () {
    if (authorMap.authors.length !== 0) {
        updateData();
    }
});
$(document).on("change", "#authorList", function() {
    let selected = $("option:selected", this);
    let personRealName = $(selected).attr("data-person-realname");
    let personOrcid = $(selected).attr("data-person-orcid");
    let personInstitution = $(selected).attr("data-person-institution");
    if (personRealName) {
        $("#newAuthorName").val(personRealName);
    }
    if (personOrcid == "") {
        $("#newAuthorOrcid").val("");
    } else {
        $("#newAuthorOrcid").val(personOrcid);
    }
    if (personInstitution == "") {
        $("#newAuthorInstitution").val("");
    } else {
        $("#newAuthorInstitution").val(personInstitution);
    }
});

$(document).on("click", '#addButton', function() {
    addAuthor();
});
$(document).on("click", '#deleteButton', function() {
    deleteAuthor();
});
$(document).on("click", '#updateButton', function() {
    updateAuthor();
});
$(document).on("click", "#continueButton", function() {
    updateData();
});

/* The following function is used for re-ordering authors */
$(document).on("click", '.re-ordering-author', function() {
    let $op = $('#authorList option:selected'),
        $this = $(this);
    if ($op.length){
        ($this.attr("data-name") === 'Up') ? $op.first().prev().before($op) : $op.last().next().after($op);
    }
});
function backAway(){
    // if it was the first page
    if(history.length === 1){
        window.location = "https://www.ebi.ac.uk/biomodels";//"${serverUrl}";
    } else {
        history.back();
    }
}
