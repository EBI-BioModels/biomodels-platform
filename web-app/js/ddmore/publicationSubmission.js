const DELIMITER = "|";
function addAuthor() {
    if ($('#newAuthorName').val()) {
        var userRealName = $('#newAuthorName').val();
        var orcid = $('#newAuthorOrcid').val() || "";
        var institution = $('#newAuthorInstitution').val() || "";
        if (authorList.filter(function(v) {
            return v["userRealName"] == userRealName &&
                   v["orcid"] == orcid &&
                   v["institution"] == institution;
            })[0]) {
            showNotification("The author named " + userRealName + " already exists. Please change it or enter an another name.");
        } else {
            // add the new author to the authors list
            var newAuthor = {userRealName: userRealName, institution: institution, orcid: orcid};
            authorList.push(newAuthor);
            // display/add it to the option element
            var id = userRealName + DELIMITER + orcid + DELIMITER + institution;
            $('#authorList').attr('size', authorList.length);
            $('#authorList')
                .append($('<option>', { value : id })
                    .text(userRealName));
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
        return v["userRealName"] == userRealName &&
               v["orcid"] == orcid &&
               v["institution"] == institution;
    })[0];
    if (deletedAuthor) {
        for (index in authorList)
            if (authorList[index].userRealName == userRealName &&
                authorList[index].orcid == orcid &&
                authorList[index].institution == institution) {
                authorList.splice(index, 1);
            }
        showNotification("The author has been deleted.")
        updateData();
        $("#authorList option:selected").remove();
        $('#authorList').attr('size', authorList.length);
        $('#newAuthorName').val("");
        $('#newAuthorOrcid').val("");
        $('#newAuthorInstitution').val("");
    } else {
        showNotification("Please select an author before deleting it.")
    }
}
function updateAuthor() {
    if ($('#newAuthorName').val()) {
        var userRealName = $('#newAuthorName').val();
        var orcid = $('#newAuthorOrcid').val() || "";
        var institution = $('#newAuthorInstitution').val() || "";
        var position;
        var updatedAuthor = authorList.filter(function(v,index) {
            position = index;
            return v["userRealName"] == userRealName &&
                v["orcid"] == orcid &&
                v["institution"] == institution;
        })[0];
        if (updatedAuthor) {
            showNotification("No changes need to be saved.");
        } else {
            // create a new author before updating
            var newAuthor = {userRealName: userRealName, institution: institution, orcid: orcid};
            //authorList.push(newAuthor);
            // display/add it to the option element
            var id = userRealName + DELIMITER + orcid + DELIMITER + institution;
            authorList[position] = newAuthor;
            $("#authorList option:selected").remove();
            $('#authorList')
                .append($('<option>', { value : id })
                    .text(userRealName));
            showNotification("The author has been updated.");
            // update the temporary hidden element playing as transporter
            updateData();
        }
        // clean up the text boxes
        $('#newAuthorName').val("");
        $('#newAuthorOrcid').val("");
        $('#newAuthorInstitution').val("");
        console.log(authorList);
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
    var input = "<input name='authorListContainer' value='";
    input += JSON.stringify(authorMap) + "' style='width: 98%; height: 40px'/>";
    document.getElementById("authorListTemp").innerHTML = input;
}
function updateData() {
    // update the author map
    authorMap.authors = [];
    $.each(authorList, function(index, entry) {
        var userRealName = entry["userRealName"];
        var institution = entry["institution"] || "";
        var orcid = entry["orcid"] || "";
        authorMap.authors.push({'userRealName': userRealName, 'institution': institution, 'orcid': orcid});
    });
    updateTempDataDivElement();
}

$(document).ready(function () {
    updateData();
    /* Basic initialisations */
    $("#synopsis").width("94%");
    $("#affiliation").width("94%");
    $("#authorList").width("97%");

    $("#authorList").change(function() {
        var value = $(this).val();
        var authorDetail = value.split(DELIMITER);
        if (authorDetail[0]) {
            $("#newAuthorName").val(authorDetail[0]);
        }
        if (authorDetail[1] == "") {
            $("#newAuthorOrcid").val("");
        } else {
            $("#newAuthorOrcid").val(authorDetail[1]);
        }
        if (authorDetail[2] == "") {
            $("#newAuthorInstitution").val("");
        } else {
            $("#newAuthorInstitution").val(authorDetail[2]);
        }
    });
    $('#addButton').click(function() {
        addAuthor();
    });
    $('#deleteButton').click(function() {
        deleteAuthor();
    });
    $('#updateButton').click(function() {
        updateAuthor();
    });
    $("#continueButton").click(function() {
        updateData();
    });
});
