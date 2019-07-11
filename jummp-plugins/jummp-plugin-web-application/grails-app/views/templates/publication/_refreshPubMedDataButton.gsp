<g:if test="${publication?.linkProvider.linkType == "PubMed ID"}">
    <div class="small-12 medium-12 large-12 columns">
        <input type="button" class="button" id="refreshPublicationFromPubMed" name="refreshPublicationFromPubMed"
               value="Refresh publication details from Europe PMC" />
        <span id="howDoesItWork" style="font-size: large; color: #ff0055">How does it work?</span>
        <div
            id="explainFreshPublicationFromPubMed"><em>If you chose <strong>PubMed ID</strong> from Source
        dropdown box below and entered a valid value in <strong>Link</strong> text box for this PubMed publication
        record, you can click this button to fetch or reload a fresh copy of
        publication details from PubMed server without attempting to fill the form in manually. In other words,
        type a PubMed identifier in Link text box, then click the button to fetch all details of the
        publication into the form.</em></div>
    </div>
</g:if>

<g:javascript>
    $('#explainFreshPublicationFromPubMed').hide();
    $('#howDoesItWork').on("click", function () {
        if ($("div#explainFreshPublicationFromPubMed").is(":hidden")) {
            $("div#explainFreshPublicationFromPubMed").show("slow");
        } else {
            $("div#explainFreshPublicationFromPubMed").slideUp();
        }
    });
</g:javascript>
