/**
 * Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 **/

package net.biomodels.jummp.core.adapters
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.core.model.PublicationLinkProviderTransportCommand
/**
 * @short Adapter class for the PublicationLinkProvider domain class
 *
 * @author Raza Ali <raza.ali@ebi.ac.uk>
 */
public class PublicationLinkProviderAdapter extends DomainAdapter {
    PublicationLinkProvider linkProvider

    PublicationLinkProviderTransportCommand toCommandObject() {
        final String publicationLinkLabel = linkProvider.linkType.label
        return new PublicationLinkProviderTransportCommand(linkType: publicationLinkLabel,
                pattern: linkProvider.pattern, identifiersPrefix: linkProvider.identifiersPrefix)
    }

    static PublicationLinkProvider fromCommandObject(PublicationLinkProviderTransportCommand cmd) {
        PublicationLinkProvider.LinkType pubLinkProviderType =
                PublicationLinkProvider.LinkType.findLinkTypeByLabel(cmd.linkType)
        if (pubLinkProviderType) {
            def result = PublicationLinkProvider.findByLinkTypeAndPatternAndIdentifiersPrefix(
                    pubLinkProviderType, cmd.pattern, cmd.identifiersPrefix)
            return result
        }
        return null
    }
}
