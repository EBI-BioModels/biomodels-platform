/**
 * Determines the size unit from the given size of a file
 * @param bytes     a long integer indicating the file size
 * @param decimals  the places of decimals
 * @returns {string} a String, for example, 123.45 Bytes or 1.2 MB
 */
function formatBytes(bytes, decimals = 2) {
    // this helper function was introduced at https://stackoverflow.com/a/18650828/865603
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const dm = decimals < 0 ? 0 : decimals;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB'];

    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(dm)) + ' ' + sizes[i];
}

function extractErrorMessage(jqXHR) {
    let message = "";
    switch (jqXHR.status) {
        case 401:
            message = "401: You are not authorised. Please log in and try your operation again.";
            break;
        default:
            message = "There has been an unknown error. Please contact us to be helped!";
            break;
    }
    return message;
}

function showFlashMessages(messages) {
    // there is a specific division beneath the main menu to be designed to show all flash messages
    let msg = "";
    if ($.isArray(messages)) {
        if (messages.length) {
            msg = "<ul>";
            for (let i = 0; i < messages.length; i++) {
                msg += "<li>" + messages[i] + "</li>";
            }
            msg += "</ul>";
        }
    } else if (typeof messages === "string") {
        msg = messages;
    }
    if (msg) {
        $('.flashNotificationDiv').html(msg).show();
    }
}

function hideFlashMessages() {
    $('.flashNotificationDiv').html("").hide();
}


/**
 * Checks acceptable characters for the uploading file names
 *
 * @param filename  A String denoting the file name
 * @returns {boolean}   A logical value showing that the file name only contains the acceptable characters or not
 */
function checkAcceptableCharactersForFileName(filename) {
    let regexp = /^[\w\s\.\+\-]+\.[\w]+$/;
    let retVal = filename.search(regexp) === -1 ? false : true;
    return retVal;
}
