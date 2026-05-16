/**
 * Copyright (C) 2010-2024 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * Spring Framework (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Spring Framework used as well as
 * that of the covered work.}
 **/


package net.biomodels.jummp.core.events

import java.lang.reflect.Method
import org.springframework.aop.AfterReturningAdvice
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.security.core.context.SecurityContextHolder

/**
 * @short Advice for logging events post method execution.
 *
 * The advice is used as an advisor for methods annotated by the
 * PostLogging Annotation. It's main purpose is to create a LoggingEvent
 * broadcast to all interested parties. The LoggingEvent informs about
 * which method was executed with which arguments and the return value.
 * Additionally from the Annotation the LoggingEventType is retrieved and
 * from the SpringSecurityContext the user who accessed the method.
 *
 * @see LoggingEventType
 * @see PostLogging
 * @see LoggingEvent
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
class PostLoggingAdvice implements AfterReturningAdvice, ApplicationContextAware {
    /**
     * The application context needed for publishing events
     */
    private ApplicationContext ctx

    void setApplicationContext(ApplicationContext applicationContext) {
        ctx = applicationContext
    }

    void afterReturning(Object returnValue, Method method, Object[] args, Object target) throws Throwable {
        PostLogging annotation = (PostLogging) method.getAnnotation(PostLogging)
        LoggingEvent event = new LoggingEvent(method,
            SecurityContextHolder.context.authentication.principal.toString(),
            returnValue, args, annotation.value())
        ctx.publishEvent(event)
    }
}
