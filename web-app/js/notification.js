/*
 * The notification system depends on including notificationDiv template in the
 * page (ensuring the presence of the notification div)
 */
function scheduleHide() {
    setTimeout(function() {
        const flashNotificationDiv = $(".flashNotificationDiv");
        flashNotificationDiv.fadeOut("slow", function() {
            flashNotificationDiv.hide();
        });
    }, 4000);
}

function hideNow() {
    const flashNotificationDiv = $(".flashNotificationDiv");
    flashNotificationDiv.hide();
}

function showNotification(message) {
    const flashNotificationDiv = $(".flashNotificationDiv");
    flashNotificationDiv.html(message);
    flashNotificationDiv.show();
}

function pollForNotifications(url) {
    const notificationLink = $("#notificationLink");
    $.get( url, function(data) {
        if (data > 0) {
            notificationLink.text('My notifications (' + data + ')');
            notificationLink.show();
        }
    });
}

function clearNotification() {
    const flashNotificationDiv = $(".flashNotificationDiv");
    flashNotificationDiv.html("");
}

function markAsRead(url, updateCount) {
    $.get( url, function() {});
    setTimeout(function() {
        pollForNotifications(updateCount);
    },1000);
}
