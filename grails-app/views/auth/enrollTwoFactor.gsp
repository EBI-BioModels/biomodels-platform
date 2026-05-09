<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${session['branding.style']}/main" />
    <title>Set Up Two-Factor Authentication | BioModels</title>
    <style>
        .fheader { text-align: center; }
        .textcode { background: lightcoral; }
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
        .otp-field input:focus { box-shadow: 0 1px 0 rgba(0, 0, 0, 0.1); }
        .otp-field input::-webkit-inner-spin-button,
        .otp-field input::-webkit-outer-spin-button { display: none; }
        .mfa-box {
            background-color: lightgrey;
            border: 3px solid #ED6B21;
            padding: 20px;
            margin-top: 20px;
        }
    </style>
</head>
<body>
<div class="row">
    <div class="small-12 medium-6 large-6 columns large-centered medium-centered mfa-box">
        <div class="fheader">
            <h2 style="text-align: center; color: #072C55;">Two-Factor Authentication Required</h2>
            <p>
                BioModels now requires two-factor authentication for all accounts.<br/>
                We have sent a 6-digit verification code to <strong>${user.email}</strong>.<br/>
                Enter it below to complete sign-in and activate 2FA on your account.
            </p>
            <p><em>The code expires after 15 minutes.</em></p>
        </div>

        <g:if test='${flash.message}'>
            <div class='login_message'>${flash.message}</div>
        </g:if>

        <form autocomplete='off'>
            <div class="otp-field">
                <label for='digit1'><input type='text' class="textcode" id='digit1' maxlength="1" data-next="2" oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
                <label for='digit2'><input type='text' class="textcode" id='digit2' maxlength="1" data-next="3" oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
                <label for='digit3'><input type='text' class="textcode" id='digit3' maxlength="1" data-next="4" oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
                <label for='digit4'><input type='text' class="textcode" id='digit4' maxlength="1" data-next="5" oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
                <label for='digit5'><input type='text' class="textcode" id='digit5' maxlength="1" data-next="6" oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
                <label for='digit6'><input type='text' class="textcode" id='digit6' maxlength="1" data-next="1" oninput="this.value=this.value.replace(/[^0-9]/g,'');"/></label>
            </div>
            <p style="text-align: center; margin-top: 16px;">
                <input type='button' id="verify" value='Verify &amp; Continue' class="button"/>
            </p>
            <p style="text-align: center">
                Didn't receive the code?
                <a href="${g.createLink(uri: "/auth/request-new-verification-code")}" target="_blank">Send again</a>
            </p>
        </form>
    </div>
</div>
<g:javascript type='text/javascript'>
    const $textCode = $(".textcode");

    $textCode.on("focus", function() { $(this).select(); });

    $textCode.on("keydown", function(e) {
        if (e.key === "Backspace" && $(this).val() === "") {
            const idx = parseInt(this.id.replace("digit", ""));
            if (idx > 1) $("#digit" + (idx - 1)).focus();
        }
    });

    $textCode.on("input", function() {
        if (this.value.length === this.maxLength) {
            const next = $(this).data('next');
            const idx = parseInt(this.id.replace("digit", ""));
            if (next > idx) $("#digit" + next).focus();
        }
    });

    $("#verify").on("click", function() {
        let otp = "";
        $textCode.each(function(i, obj) { otp += $(obj).val(); });
        const URL = "${createLink(controller: 'auth', action: 'verifyOTP')}";
        fetch(URL, {
            method: 'POST',
            headers: {
                'Accept': 'application/json; charset=utf-8',
                'Content-Type': 'application/json; charset=utf-8'
            },
            body: JSON.stringify({ 'otp': otp })
        }).then(response => {
            if (!response.ok) throw new Error('Network response failed');
            return response.json();
        }).then(data => {
            if (data["matched"]) {
                showNotification("<h3 style='color: green'><b>Verification successful!</b></h3><p>Redirecting you now…</p>");
                setTimeout(() => { window.location = data["postUrl"]; }, 2000);
            } else if (data["message"] === "forbidden") {
                window.location = data["postUrl"];
            } else {
                showNotification("<h3 style='color: darkred'><b>Verification failed</b></h3><p>" + data["cause"] + "</p>");
            }
        }).catch(error => { console.error('Error: ', error); });
    });
</g:javascript>
</body>
</html>
