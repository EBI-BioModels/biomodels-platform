<g:javascript>
    $(".toggle-password").on("click", function() {
        $(this).toggleClass("fa-eye fa-eye-slash");
        let input = $("#password");
        if (["editPassword", "reset"].includes("${actionName}")) {
            input = $("#newPassword");
        }
        if (input.attr("type") === "password") {
            input.attr("type", "text");
        } else {
            input.attr("type", "password");
        }
    });
</g:javascript>
