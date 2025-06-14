<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 11/06/2025
  Time: 20:47
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>OTP Verification | BioModels</title>
    <style>
        .fheader {
            text-align: center;
        }
        .textcode {
            background: lightcoral;
        }

        .otp-field {
            flex-direction: row;
            column-gap: 10px;
            display: flex;
            align-items: center;
            justify-content: center;
        }

        .otp-field input {
            height: 45px;
            width: 42px;
            border-radius: 6px;
            outline: none;
            font-size: 1.125rem;
            text-align: center;
            border: 1px solid #ddd;
        }
        .otp-field input:focus {
            box-shadow: 0 1px 0 rgba(0, 0, 0, 0.1);
        }
        .otp-field input::-webkit-inner-spin-button,
        .otp-field input::-webkit-outer-spin-button {
            display: none;
        }
    </style>
</head>

<body>
<div class="row">
    <div class="small-12 medium-6 large-6 columns large-centered medium-centered"
         style="background-color: lightgrey; width: 500px; border: 3px solid green; padding: 20px; margin: auto">
%{--        <div class='fheader'><g:message code="securitytoken.header"/></div>--}%
        <div class='fheader'>
        <h2 class="green" style="text-align: center">Two-Factor Authentication<br/>(aka. OTP Verification)</h2>
        <p>Please enter the One-Time Passcode (OTP) sent to your registered email to complete your verification.<br/>
            <em>The code expires after 15
            minutes and will no longer work if it was already entered for this account.</em>
            <br/>
        Didn't receive code? <a href="${g.createLink(uri: "/auth/request-new-verification-code")}"
                                target="_blank">Request again!</a></p></div>

        <g:if test='${flash.message}'>
            <div class='login_message'>${flash.message}</div>
        </g:if>

        <form action='${postUrl}' method='POST' id='stepTwoLoginForm' autocomplete='off'>
            <div class="otp-field">
                <label for='digit1'>
                    <input type='text' class="textcode" id='digit1' maxlength="1" data-next="2"
                           oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
                <label for='digit2'>
                    <input type='text' class="textcode" id='digit2' maxlength="1" data-next="3"
                           oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
                <label for='digit3'>
                    <input type='text' class="textcode" id='digit3' maxlength="1" data-next="4"
                           oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
                <label for='digit4'>
                    <input type='text' class="textcode" id='digit4' maxlength="1" data-next="5"
                           oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
                <label for='digit5'>
                    <input type='text' class="textcode" id='digit5' maxlength="1" data-next="6"
                           oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
                <label for='digit6'>
                    <input type='text' class="textcode" id='digit6' maxlength="1" data-next="1"
                           oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
            </div>
            <p style="text-align: center">
%{--                <input type='submit' id="submit" value='${message(code: "securitytoken.button")}'/>--}%
                <input type='button' id="verify" value='Verify' class="button"/></p>
                %{--<g:if test="${!trustDevice}">--}%
                <div id="div-trust-device"><label for="chkTrustDevice">
                    <input type="checkbox" id="chkTrustDevice"/> Trust this device for 30 days
                </label>
                </div>
                %{--</g:if>--}%
        </form>
    </div>
</div>
<g:javascript type='text/javascript'>
    (function() {
        const username = "${user.username}";
        const deviceInfo =  localStorage.getItem(username);
        if (deviceInfo) {
            $("#div-trust-device").remove();
        }
    })();

    // https://codepen.io/tnguyenv/pen/JodvWZy
    $(".textcode").on("change keyup", function() {
        if (this.value.length === this.maxLength) {
            let next = $(this).data('next');
            $('#digit' + next).focus();
        }
    });
    $("#verify").on("click", function() {
        let otp = "";
        $(".textcode").each(function(i, obj) {
            otp += $(obj).val();
        });
        const URL = "${createLink(controller: 'auth', action: 'verifyOTP')}";
        fetch(URL, {
            method: 'POST',
            headers: {
                'Accept': 'application/json; charset=utf-8',
                'Content-Type': 'application/json; charset=utf-8'
            },
            body: JSON.stringify({'otp': otp})
        }).then(response => {
            if (!response.ok) {
                throw new Error('Network response failed!!!');
            }
            return response.json();
        }).then(data => {
            let msg = "";
            if (data["matched"]) {
                msg = "<h3 style='color: green'><b>OTP verification passed!</b></h3>";
                msg += "<p>You can close this page now. Otherwise, you will be redirected after 3 seconds.<p>"
                showNotification(msg);
                setTimeout(() => {
                    window.location = data["postUrl"];
                }, 3000);
            } else if (data["message"] === "forbidden") {
                window.location = data["postUrl"];
            } else if (!data["matched"]) {
                msg = "<h3 style='color: darkred'><b>OTP verification failed!</b></h3>";
                msg += "<p>Mismatched One-Time Passcode (OTP). It could be expired. Try again or request a new OTP.<p>"
                showNotification(msg);
            } else {
                clearNotification();
                hideNow();
            }
        }).catch(error => {
            console.error('Error: ', error);
        });
    });

    $("#chkTrustDevice").on("change", function () {
        console.log("Checkbox `Trust this device` has been changed!");
        let isChecked = $(this).is(':checked');

        if (isChecked) {
            getIP().
            then((data) => {
                const ipaddr = data;
                const type = deviceType();
                const userAgent = navigator.userAgent;
                let device = new Device(ipaddr, type, userAgent);
                console.log(device.toString());
                localStorage.setItem("${user.username}", device.toString());
            }).
            then(() => {
                // console.log("Do nothing");
            })
        }
    });

    let cachedIP = null;

    async function getIP() {
        if (cachedIP) {
            return cachedIP;
        }
        const response = await fetch('https://api.ipify.org?format=json');
        const data = await response.json();
        cachedIP = data["ip"];
        return cachedIP;
    }

    function deviceType() {
        const ua = navigator.userAgent;
        if (/(tablet|ipad|playbook|silk)|(android(?!.*mobi))/i.test(ua)) {
            return "tablet";
        }
        else if (/Mobile|Android|iP(hone|od)|IEMobile|BlackBerry|Kindle|Silk-Accelerated|(hpw|web)OS|Opera M(obi|ini)/.test(ua)) {
            return "mobile";
        }
        return "desktop";
    }

    class Device {
        ipAddress
        type
        userAgent
        constructor(ipAddress, type, userAgent) {
            this.ipAddress = ipAddress;
            this.type = type;
            this.userAgent = userAgent;
        }

        toString() {
            return this.ipAddress+"|"+this.type+"|"+this.userAgent;
        }
    }

</g:javascript>
</body>
</html>