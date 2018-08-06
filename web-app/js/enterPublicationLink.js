/**
 * Created by Tung (tnguyen@ebi.ac.uk) on 09/03/2016.
 */
$(document).ready(function () {
    if ($('#publicationLink').val()=="") {
        $('#publicationLink').hide();
    }
    $('#pubLinkProvider').on('change', function() {
        if (this.value == 'PubMed ID' || this.value == 'DOI' || this.value == 'Other Link (URL)') {
            $('#publicationLink').show();
        } else {
            $('#publicationLink').val("");
            $('#publicationLink').hide();
        }
    });
});
