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

/**
 * @short Event class for intercepted methods whose execution may be logged.
 *
 * This event is triggered by the PostLoggingAdvice whenever a method got intercepted.
 * Interested parties may listen to this Event and log it. It contains all important
 * information such as who executed the method, when, with what arguments and what
 * were the returned results.
 *
 * @see LoggingEventType
 * @see PostLoggingAdvice
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
class LoggingEvent extends JummpEvent {
    /**
     * The name of the user who executed a method.
     */
    String user
    /**
     * The return value of the executed method
     */
    Object returnValue
    /**
     * The arguments passed into the executed method.
     */
    Object[] arguments
    /**
     * The type of the logging event.
     */
    LoggingEventType type

    /**
     * Constructor for a LoggingEvent raised when an intercepted method was executed.
     * @param source The Method object which was intercepted.
     * @param user The name of the user who executed the method
     * @param returnValue The return value of the executed method
     * @param args The arguments passed into the executed method
     * @param type The type of the executed method
     */
    LoggingEvent(Object source, String user, Object returnValue, Object[] args, LoggingEventType type) {
        super(source)
        this.user = user
        this.returnValue = returnValue
        this.arguments = args
        this.type = type
    }
}
