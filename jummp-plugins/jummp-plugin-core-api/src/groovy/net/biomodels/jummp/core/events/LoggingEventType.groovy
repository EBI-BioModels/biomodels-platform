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
 * @short Enum defining the type of event to be logged.
 *
 * This enum is used by the PostLoggingAdvice to define what kind of method
 * has been executed, that is whether it retrieved data, updated data, created
 * new data and so on.
 * @see PostLoggingAdvice
 * @author Martin Gräßlin <m.graesslin@dkfz-heidelberg.de>
 */
public enum LoggingEventType {
    RETRIEVAL, ///< Data is retrieved either from database or VCS
    CREATION, ///< New Data is created (e.g. new Model uploaded)
    DELETION, ///< Existing Data is deleted or marked as deleted
    UPDATE, ///< Existing Data is changed/updated
    SUBMIT_FOR_PUBLICATION ///< Existing Data is submitted for publication
}
