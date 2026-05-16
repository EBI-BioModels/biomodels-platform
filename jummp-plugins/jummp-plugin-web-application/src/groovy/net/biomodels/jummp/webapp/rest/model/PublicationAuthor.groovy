package net.biomodels.jummp.webapp.rest.model

import net.biomodels.jummp.core.user.PersonTransportCommand

class PublicationAuthor {
    String name
    String institution
    String orcid

    PublicationAuthor(PersonTransportCommand personTC) {
        name = personTC.userRealName
        institution = personTC.institution
        orcid = personTC.orcid
    }
}
