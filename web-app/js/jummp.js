/*global $: false, window: false
 */
// TODO: create proper links
$.jummp = {};

$.jummp.message = function (message, warning) {
    "use strict";
    if (!message) {
        return;
    }
    var classes, button, divCode;
    classes = "message";
    if (warning) {
        classes += " warning";
    }
    button = $("<div><button class=\"close\"></button></div>");
    $("button", button).click(function () {
        $(this).parent().parent().remove();
    });
    divCode = $("<div class=\"" + classes + "\"><p>" + message + "</p></div>");
    button.appendTo(divCode);
    $("#infoBox").append(divCode);
};

$.jummp.infoMessage = function (message) {
    "use strict";
    this.message(message, false);
};

$.jummp.warningMessage = function (message) {
    "use strict";
    this.message(message, true);
};

/**
 * Same as g.createLinkNoAction.
 * @param controller The name of the grails controller
 * @param id The optional id
 */
$.jummp.createLinkNoAction = function (controller, id) {
    "use strict";
    var path = controller;
    if (id !== undefined) {
         path += "/" + id;
    }
    return $.jummp.createURI(path);
};


/**
 * Same as g.createLink.
 * @param controller The name of the grails controller
 * @param action The optional action
 * @param id The optional id
 */
$.jummp.createLink = function (controller, action, id) {
    "use strict";
    var path = controller;
    if (action !== undefined) {
        path += "/" + action;
        if (id !== undefined) {
            path += "/" + id;
        }
    }
    return $.jummp.createURI(path);
};

/**
 * Creates a URI to be used in a href or src HTML attribute.
 * @param path The path
 */
$.jummp.createURI = function (path) {
    "use strict";
    return $.serverUrl + "/" + path;
};

$(function () {
    "use strict";
    $("#loginLogout button.logout").click(function () {
        window.location.pathname = $.jummp.createURI("logout");
    });
    $("#loginLogout button.login").click(function () {
        window.location.pathname = $.jummp.createLink("login", "auth");
    });
});

/**
 * Redirects the browser to the given location.
 * @param location the page to which to navigate.
 */
$.jummp.openPage = function(location) {
    window.location.href = location;
}
