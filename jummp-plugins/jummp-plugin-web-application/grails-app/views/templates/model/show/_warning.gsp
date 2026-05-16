<g:if test="${hasAdminRole}">
<p id="new-look-and-feel-switcher"
   style="color: #ffff00 !important; background-color: rgb(0, 124, 130); text-align: center; cursor: pointer; padding: 0.5em
    0 0.5em 0" data-new-look="${newLook}">
    <a>${announcement}</a></p>
    <script>
        $("#new-look-and-feel-switcher").on("click", function () {
            const newLook = $(this).attr("data-new-look");
            // console.log(newLook);
            if (newLook === "true") {
                console.log("Call a service to get back the old interface and refresh the page");
            } else {
                console.log("Call a service to apply for the new look and feel, then refresh the page");
            }
            const URL = $.jummp.createLink("jummp", "switchLookAndFeelForModelDisplay");
            const body = {
                "newLook": newLook
            }
            fetch(URL, {
                method: 'POST',
                headers: {
                    'Accept': 'application/json; charset=utf-8',
                    'Content-Type': 'application/json; charset=utf-8'
                },
                body: JSON.stringify(body)
            }).then(response => {
                if (!response.ok) {
                    throw new Error('Network response failed!!!');
                }
                return response.json();
            }).then(data => {
                //console.log(data);
                window.location.reload();
            }).catch(error => {
                console.error('Error: ', error);
            });
        });
    </script>
</g:if>
<g:if test="${revision.model.deleted}">
    <div class='PermanentMessage'>
        This is an archived model.
    </div>
</g:if>
<g:if test="${oldVersion}">
    <div class='PermanentMessage'>
        You are viewing a version of a model that has been updated.
        To access the latest version, and a more detailed display please
        go <a href="${createLink(controller: "model", action: "show", id:
        revision.modelIdentifier())}">here</a>.
    </div>
</g:if>

