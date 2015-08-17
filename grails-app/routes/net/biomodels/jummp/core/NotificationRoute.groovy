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

package net.biomodels.jummp.core

import org.apache.camel.builder.RouteBuilder

class NotificationRoute extends RouteBuilder {

    @Override
    void configure() {
        from("seda:model.publish").to("bean:notificationService?method=modelPublished")
        from("seda:model.readAccessGranted").to("bean:notificationService?method=readAccessGranted")
        from("seda:model.writeAccessGranted").to("bean:notificationService?method=writeAccessGranted")
        from("seda:model.delete").to("bean:notificationService?method=delete")
        from("seda:model.update").to("bean:notificationService?method=update")
    }
}
