package net.biomodels.jummp.webapp.rest.model.show

import net.biomodels.jummp.core.model.PublicationTransportCommand

class Publication {
    String journal
    String title
    String affiliation
    String synopsis
    Integer year
    String month
    Integer day
    String volume
    String issue
    String pages
    String link
    List<PublicationAuthor> authors

    Publication(PublicationTransportCommand publicationTC) {
        journal = publicationTC.journal
        title = publicationTC.title
        affiliation = publicationTC.affiliation
        synopsis = publicationTC.synopsis
        year = publicationTC.year
        month = publicationTC.month
        day = publicationTC.day
        volume = publicationTC.volume
        issue = publicationTC.issue
        pages = publicationTC.pages
        link = publicationTC.link
        authors = new ArrayList<PublicationAuthor>()
    }
}
