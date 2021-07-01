package net.biomodels.jummp.core

import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.model.PublicationLinkProvider.LinkType

interface PubDataFetchStrategy {
    PubTC fetchPublicationData(String id) throws JummpException
    PubTC fetchPublicationData(String id, String pubLinkProvider) throws JummpException
    PubTC fetchPublicationData(String id, LinkType pubLinkProvider) throws JummpException

    PLPTC createLinkProviderInstance()

    PLPTC createLinkProviderInstance(String pubLinkProvider)

    PLPTC createLinkProviderInstance(LinkType pubLinkProvider)
}
