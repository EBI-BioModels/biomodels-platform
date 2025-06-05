<g:javascript>
function verifyCompromisedPassword(password) {
    console.log("${actionName}");
    const URL = "${createLink(controller: 'usermanagement', action: 'verifyCompromisedPassword')}";
    fetch(URL, {
        method: 'POST',
        headers: {
            'Accept': 'application/json; charset=utf-8',
            'Content-Type': 'application/json; charset=utf-8'
        },
        body: JSON.stringify({'password': password})
    }).then(response => {
        if (!response.ok) {
            throw new Error('Network response failed!!!');
        }
        return response.json();
    }).then(data => {
        if (data["result"]) {
            let msg = "<h4 style='color: red'><b>Compromised Password Alert!</b></h4>";
            if ("${actionName}" === 'edit') {
                msg += "This password is known to cybercriminals far and wide! " +
                "It has been publicly exposed in one or more data breaches.";
            } else if ("${actionName}" === 'auth') {
                msg += "This password has appeared in one or more data breaches which puts your account at " +
                "high risk of compromise. You should change your password immediately."
            }
            showNotification(msg);
        } else {
            clearNotification();
            hideNow();
        }
    }).catch(error => {
        console.error('Error: ', error);
    });
}
</g:javascript>