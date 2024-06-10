package net.biomodels.jummp.webapp.rest.model

import net.biomodels.jummp.core.model.PublicationTransportCommand

class Publication {
    String source
    String accession
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
        source = publicationTC.linkProvider.linkType
        accession = publicationTC.link
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
        link = publicationTC.linkProvider.identifiersPrefix ?
            publicationTC.linkProvider.identifiersPrefix + publicationTC.link : publicationTC.link
        authors = new ArrayList<PublicationAuthor>()
    }
}
