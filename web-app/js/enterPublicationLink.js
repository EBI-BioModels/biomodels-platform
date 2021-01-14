/**
 * Created by Tung (tnguyen@ebi.ac.uk) on 09/03/2016.
 */
$(document).ready(function () {
    if ($('#publicationLink').val() === "") {
        $('#publicationLink').hide();
    }
    $('#pubLinkProvider').on('change', function() {
        if (this.value === 'Publication without link') {
            $('#publicationLink').hide();
            $('#refreshPubLinkBtn').hide();
            $('#publicationForm').show();
            hideNow();
        } else if (this.value !== 'NoPub') {
            $('#publicationLink').show();
            $('#refreshPubLinkBtn').show();
            $('#publicationForm').show();
            hideNow();
        } else {
            $('#publicationLink').val("");
            $('#publicationLink').hide();
            $('#refreshPubLinkBtn').hide();
            $('#publicationForm').hide();
            let warningMessage = "We gently remind you to update the publication details as soon as they are available to increase the chances of your model getting cited.";
            showNotification(warningMessage);
        }
    });
});
