<g:if test="${controller in ["publication", "submission", "model"] && operation in ["add", "edit", "update", "submit"]}">

    <%@ page import=" net.biomodels.jummp.model.PublicationLinkProvider" %>
    <%
        linkSourceTypes = PublicationLinkProvider.LinkType.values().collect { it.label }
    %>

    <div class="row">
            <div class="small-12 medium-3 large-3 columns" id="publicationLinkProviderBox">
                <label for="pubLinkProvider" class="required">Choose a publication source</label>
                <g:if test="${publication}">
                <g:select name="PubLinkProvider" id="pubLinkProvider"
                          from="${linkSourceTypes}"
                          value="${publication?.linkProvider?.linkType}"
                          noSelection="['NoPub':'- No publication available -']"/></g:if>
                <g:else>
                <g:select name="PubLinkProvider" id="pubLinkProvider"
                          from="${linkSourceTypes}"
                          noSelection="['NoPub':'- No publication available -']"/></g:else>
            </div>
            <div class="small-12 medium-6 large-6 columns" id="publicationLinkCol">
                <label for="publicationLink" class="required">Link <span id="lblPublicationLink" style="font-style: italic">[If this is
                PubMed
                identifier or DOI, please enter a valid
                value, then press on the <strong>Update</strong> button]</span></label>
                <g:textField class="input25" name="PublicationLink" id="publicationLink" value="${publication?.link}"
                             placeholder="Enter PubMed identifier, DOI or web link"/></div>
            <div class="small-12 medium-3 large-3 columns" id="freshPublicationBtnCol">
                <g:render template="/templates/publication/refreshPubMedDataButton"
                          plugin="jummp-plugin-web-application"
                          model="['publication': publication]"/>
            </div>
    </div>

    <script type="text/javascript">
        toastr.options = {
            "closeButton": false,
            "debug": false,
            "newestOnTop": false,
            "progressBar": true,
            "positionClass": "toast-top-right",
            "preventDuplicates": false,
            "onclick": null,
            "showDuration": "300",
            "hideDuration": "1000",
            "timeOut": "5000",
            "extendedTimeOut": "1000",
            "showEasing": "swing",
            "hideEasing": "linear",
            "showMethod": "fadeIn",
            "hideMethod": "fadeOut"
        }
        var validation = true;
        $(document).ready(function () {
            if ("${publication.id}" !== null && "${publication.id}" !== "") {
                $('#publicationLinkCol').show();
                $('#freshPublicationBtnCol').show();
            } else {
                $('#publicationLinkCol').hide();
                $('#freshPublicationBtnCol').hide();
            }
        });

        $(document).on('change', '#pubLinkProvider', {}, function(e) {
            let pubLinkProvider = $(this).val();
            let res = shouldWarnWhenUpdatingLinkProvider(pubLinkProvider);
            if (res) {
                let message =
                    "Please change the publication link in the Link box and click on the Update button to refresh the form";
                toastr.warning(message);
                showFlashMessages(message);
            }
            if (pubLinkProvider === "Publication without link" || pubLinkProvider === "NoPub")  {
                $('#publicationLinkCol').hide();
                $('#freshPublicationBtnCol').hide();
            } else {
                $('#publicationLinkCol').show();
                $('#freshPublicationBtnCol').show();
            }
            if (pubLinkProvider === "NoPub" && "${controller}" === "publication") {
                validation = false;
                let message = "You have to choose a publication source to complete your operation";
                toastr.warning(message);
                showFlashMessages(message);
            }
        });

        function shouldWarnWhenUpdatingPublicationLink(update) {
            return (update !== "${publication?.link}") && (update) ;
        }

        function shouldWarnWhenUpdatingLinkProvider(pubLinkProvider) {
            let Need2BeWarned = pubLinkProvider === "PubMed ID" || pubLinkProvider === "DOI";
            return Need2BeWarned;
        }

        $(document).on('blue focusout', '#publicationLink', {}, function(e) {
            let pubLink = $(this).val();
            let res = shouldWarnWhenUpdatingPublicationLink(pubLink);
            if (res) {
                let message = "Click on the Update button to refresh the publication details";
                toastr.warning(message);
                showFlashMessages(message);
            }
        });
    </script>
</g:if>
