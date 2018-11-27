package net.biomodels.jummp.webapp.rest.model.show

import net.biomodels.jummp.plugins.security.PersonTransportCommand

class PublicationAuthor {
    String userRealName
    String institution
    String orcid

    PublicationAuthor(PersonTransportCommand personTC) {
        userRealName = personTC.userRealName
        institution = personTC.institution
        orcid = personTC.orcid
    }
}
