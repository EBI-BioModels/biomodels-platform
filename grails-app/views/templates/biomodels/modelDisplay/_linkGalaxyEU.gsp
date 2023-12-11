<div id="galaxy-link-placeholder" style="padding-top:1em">
    <g:if test="${existed}">
        <g:render template="/templates/biomodels/modelDisplay/linkGalaxyEU_RenderLink" />
    </g:if>
    <g:elseif test="${canAddGalaxyLink}">
        <g:render template="/templates/biomodels/modelDisplay/linkGalaxyEU_AddButton" />
    </g:elseif>
    <g:else>
        <p>&nbsp;</p>
    </g:else>
</div>
<style>
#btn-remove-galaxy-link {
    /*position: relative;
    width: 50px;
    height: 50px;
    border-radius: 25px;
    border: 2px solid rgb(231, 50, 50);
    background-color: #fff;
    cursor: pointer;
    box-shadow: 0 0 10px #333;
    overflow: hidden;
    transition: .3s;*/
}
#btn-remove-galaxy-link:hover {
    background-color: rgb(245, 207, 207);
    transform: scale(1.2);
    box-shadow: 0 0 4px #111;
    transition: .3s;
}
</style>
<script>
    $("#galaxy-link-placeholder").on("click", function(event) {
        if (event.target.id === "btn-add-galaxy-link") {
            console.log("Adding...");
            const urlPost = "${createLink(controller: "model", action: "doAddOrRemoveGalaxyLink")}";

            $.ajax({
                url: urlPost,
                type: "POST",
                data: {
                    modelId: "${revision.modelIdentifier()}",
                    flag: "Yes"
                }
            }).done(function(response) {
                console.log("Text: " + response);
                $("#galaxy-link-placeholder").html(response);
                //console.log(data);
            }).fail(function(jqXHR) {
                // the method below is defined in helpers.js
                //const msg = extractErrorMessage(jqXHR);
                toastr.error(jqXHR);
            }).complete(function() {
                console.log("Completed");
            });
            return true;
        }
    });

    $("#galaxy-link-placeholder").on("click", "#btn-remove-galaxy-link", function(event) {
        if (event.target.id === "btn-remove-galaxy-link") {
            console.log("Removing...");
            const urlPost = $.jummp.createLink("model", "doAddOrRemoveGalaxyLink");
            $.ajax({
                url: urlPost,
                type: "POST",
                data: {
                    modelId: "${revision.modelIdentifier()}",
                    flag: "No"
                }
            }).done(function(response) {
                console.log("Text: " + response);
                $("#galaxy-link-placeholder").html(response);
            }).fail(function(jqXHR) {
                console.log(jqXHR);
            }).complete(function() {
                console.log("Completed");
            });
            return true;

        }
    });
</script>


