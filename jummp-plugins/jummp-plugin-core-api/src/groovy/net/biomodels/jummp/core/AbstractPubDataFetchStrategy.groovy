package net.biomodels.jummp.core

import net.biomodels.jummp.core.adapters.PublicationLinkProviderAdapter as PLPA
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand as PLPTC
import net.biomodels.jummp.core.model.PublicationTransportCommand
import net.biomodels.jummp.core.model.PublicationTransportCommand as PubTC
import net.biomodels.jummp.model.PublicationLinkProvider as PubLP

class AbstractPubDataFetchStrategy implements PubDataFetchStrategy {

    @Override
    PubTC fetchPublicationData(String id) throws JummpException {
        return null
    }

    @Override
    PublicationTransportCommand fetchPublicationData(String id, String pubLinkProvider) throws JummpException {
        return null
    }

    @Override
    PublicationTransportCommand fetchPublicationData(String id, PubLP.LinkType pubLinkProvider) throws JummpException {
        return null
    }

    @Override
    PLPTC createLinkProviderInstance() {
        return null
    }

    @Override
    PublicationLinkProviderTransportCommand createLinkProviderInstance(String pubLinkProvider) {
        return null
    }

    @Override
    PublicationLinkProviderTransportCommand createLinkProviderInstance(PubLP.LinkType pubLinkProvider) {
        return null
    }
}
