<!--
 common-script.gsp is as the placeholder to define scripts where they need to use
 Grails tag-libs or server side variables.
 -->
<%@ page import="net.biomodels.jummp.utils.MathUtils" contentType="text/html;charset=UTF-8" %>

<g:javascript>
function verifyCompromisedPassword(password) {
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
            if (['editPassword', 'reset'].includes("${actionName}")) {
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

function checkCompromisedPasswordOnServerSide(password) {
    const URL = "${createLink(controller: 'usermanagement', action: 'checkCompromisedPasswordOnServerSide')}";
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
            console.log("Sent the verification compromised password to the server side.");
        } else {
            console.log("Cannot verify compromised password to the server side.");
        }
    }).catch(error => {
        console.error('Error: ', error);
    });
}

function checkPasswordStrength(password) {
    // source: https://martech.zone/javascript-password-strength/
    // Initialize variables
    let strength = 0;
    let strengthLevel;
    let tips = [];

    // Check password length
    let tip = "Make the password longer.";
    if (password.length < 10) {
        tips.push(tip);
    } else {
        strength += 1;
        // tips.splice( $.inArray(tip, tips), 1);
        tips = jQuery.grep(tips, function(value) { return value !== tip; });
    }

    // Check for mixed case
    tip = "Use both lowercase and uppercase letters.";
    if (password.match(/[a-z]/) && password.match(/[A-Z]/)) {
        strength += 1;
        // tips.splice( $.inArray(tip, tips), 1);
        tips = jQuery.grep(tips, function(value) { return value !== tip; });
    } else {
        tips.push(tip);
    }

    // Check for numbers
    tip = "Include at least one number.";
    if (password.match(/\d/)) {
        strength += 1;
        // tips.splice( $.inArray(tip, tips), 1);
        tips = jQuery.grep(tips, function(value) { return value !== tip; });
    } else {
        tips.push(tip);
    }

    // Check for special characters
    tip = "Include at least one special character.";
    if (password.match(/[^a-zA-Z\d]/)) {
        strength += 1;
        // tips.splice( $.inArray(tip, tips), 1);
        tips = jQuery.grep(tips, function(value) { return value !== tip; });
    } else {
        tips.push(tip);
    }

    // Return results
    if (strength < 2) {
         strengthLevel = "Easy to guess.";
    } else if (strength === 2) {
        strengthLevel = "Medium difficulty." ;
    } else if (strength === 3) {
        strengthLevel =  "Difficult.";
    } else {
        strengthLevel = "Extremely difficult.";
    }
    return { strength: strength, strengthLevel: strengthLevel, tips: tips };
}

function validateNewPassword(newPassword, newPasswordHelp, showWarning = false) {
    const newPasswordVal = newPassword.val();
   // the function below was defined in the common-script template in jummp-plugin-web-app
    verifyCompromisedPassword(newPasswordVal);
    let s = checkPasswordStrength(newPasswordVal);
    let retVal = s.tips.length === 0;
    let colourCode;
    switch (s.strengthLevel) {
        case "${MathUtils.PWD_HARD_LEVEL.EASY.label}":
            colourCode = "red";
            if (showWarning) {
                toastr.error(s.strengthLevel);
            }
            break;
        case "${MathUtils.PWD_HARD_LEVEL.MEDIUM.label}":
            colourCode = "orange";
            if (showWarning) {
                toastr.warning(s.strengthLevel);
            }
            break;
        case "${MathUtils.PWD_HARD_LEVEL.HARD.label}":
            colourCode = "cornflowerblue";
            if (showWarning) {
                toastr.info(s.strengthLevel);
            }
            break;
        case "${MathUtils.PWD_HARD_LEVEL.X_HARD.label}":
            colourCode = "green";
            if (showWarning) {
                toastr.success(s.strengthLevel);
            }
            break;
    }
    let msg = '<span style="color: ' + colourCode + '">' + s.strengthLevel + '</span>';
    if (!retVal) {
        msg += "<br/>" + s.tips.join("<br/>");
    }
    newPasswordHelp.show();
    newPasswordHelp.html(msg);

    return retVal;
}

function validateNewPasswordRpt(newPassword, newPasswordRpt, newPasswordRptHelp) {
    const newPasswordRptVal = newPasswordRpt.val();
    const newPasswordVal = newPassword.val();
    let retVal;
    if (newPasswordRptVal.length === 0) {
        retVal = false;
        newPasswordRptHelp.show();
        newPasswordRptHelp.html("Please retype your new password!");
    } else if (newPasswordVal !== newPasswordRptVal) {
        retVal = false;
        newPasswordRptHelp.show();
        newPasswordRptHelp.html("New password does not match!");
    } else {
        retVal = true;
        newPasswordRptHelp.hide();
    }
    return retVal;
}
</g:javascript>