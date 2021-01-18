package net.biomodels.jummp.core

import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC

interface PubDataFetchStrategy {
    PubTC fetchPublicationData(String id) throws JummpException

    PLPTC createLinkProviderInstance()
}
