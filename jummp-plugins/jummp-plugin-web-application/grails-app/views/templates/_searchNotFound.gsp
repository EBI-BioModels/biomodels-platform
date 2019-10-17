<div class="row">
    <div class="small-12 medium-2 large-2 columns">
        <img src="https://cdn1.iconfinder.com/data/icons/ui-colored-3-of-3/100/UI_3_-38-512.png"
             width="50%"
             class="float-center"></div>
    <div class="small-12 medium-10 large-10 columns">
        <h3 style="border-bottom: 2px solid #007c82;">Sorry, we couldn't find any matches.</h3>
        <p>You might need to:</p>
        <ul>
            <g:if test="${action == "search"}">
                <li>Check the syntax of your query.</li>
                <li><a href="${g.createLink(controller: "login", action: "auth")}">Log in</a> with your username
                and password if you're trying to access an unpublished model.</li></g:if>
                <li><a onclick="window.history.back()">Go back to the previous page.</a></li></ul>
        <script>
            $('#local-searchbox').val("${params.query}");
            $('#clearsearch').hide();
        </script>
    </div>
</div>
