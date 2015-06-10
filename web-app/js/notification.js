/*
 * The notification system depends on including notificationDiv template in the 
 * page (ensuring the presence of the notification div)
 */
function scheduleHide() {
    setTimeout(function() {
        $(".flashNotificationDiv").fadeOut("slow", function() {
            $(".flashNotificationDiv").hide();
        });
    }, 4000);
}

function hideNow() {
    $(".flashNotificationDiv").hide();
}

function showNotification(message) {
    $(".flashNotificationDiv").show()
    $(".flashNotificationDiv").text(message)
}

function pollForNotifications(url) {
    $.get( url, function(data) {
        if (data > 0) {
            $("#notificationLink").text(' (' + data + ')');
            $("#notificationLink").show();
        } else {
            $("#notificationLink").hide();
        }
    });
}

function markAsRead(url, updateCount) {
    $.get( url, function() {});
    setTimeout(function() {
        pollForNotifications(updateCount);
    },1000);
}
