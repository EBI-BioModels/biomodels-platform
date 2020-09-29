/**
 * Some helper functions to work with our UI and keep our code cleaner
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

// Adds an entry to our debug area
function ui_add_log(message, color) {
    const d = new Date();

    const dateString = (('0' + d.getHours())).slice(-2) + ':' +
        (('0' + d.getMinutes())).slice(-2) + ':' +
        (('0' + d.getSeconds())).slice(-2);

    color = (typeof color === 'undefined' ? 'muted' : color);

    let template = $('#debug-template').text();
    template = template.replace('%%date%%', dateString);
    template = template.replace('%%message%%', message);
    template = template.replace('%%color%%', color);

    $('#debug').find('li.empty').fadeOut(); // remove the 'no messages yet'
    $('#debug').prepend(template);
}

// Creates a new file and add it to our list
function ui_multi_add_file(id, file) {
    let template = $('#files-template').text();
    template = template.replace('%%filename%%', file.name).replace('%%filesize%%', formatBytes(file.size));
    template = $(template);
    template.prop('id', 'uploaderFile' + id);
    template.data('file-id', id);

    $('#files').find('li.empty').fadeOut(); // remove the 'no files yet'
    $('#files').prepend(template);
}

// Changes the status messages on our list
function ui_multi_update_file_status(id, status, message) {
    $('#uploaderFile' + id).find('span').html(message).prop('class', 'status text-' + status);
}

// Updates a file progress, depending on the parameters it may animate it or change the color.
function ui_multi_update_file_progress(id, percent, color, active) {
    color = (typeof color === 'undefined' ? false : color);
    active = (typeof active === 'undefined' ? true : active);

    var bar = $('#uploaderFile' + id).find('div.progress-bar');

    bar.width(percent + '%').attr('aria-valuenow', percent);
    bar.toggleClass('progress-bar-striped progress-bar-animated', active);

    if (percent === 0) {
        bar.html('');
    } else {
        bar.html(percent + '%');
    }

    if (color !== false) {
        bar.removeClass('bg-success bg-info bg-warning bg-danger');
        bar.addClass('bg-' + color);
    }
}
