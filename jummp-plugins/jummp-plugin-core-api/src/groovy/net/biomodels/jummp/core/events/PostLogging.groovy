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


package net.biomodels.jummp.core.events

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * @short Annotation for defining that a method execution should be logged.
 *
 * If a method of a spring-managed bean is annotated with this Annotation
 * it's execution will be intercepted after-return and execute the
 * PostLoggingAdvice, which will broadcast a LoggingEvent. By using this
 * Annotation it is possible to specify that the method execution may be
 * logged.
 * @see PostLoggingAdvice
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@interface PostLogging {
    /**
     *
     * @return The LoggingEventType of the annotated method
     */
    LoggingEventType value()
}
