/**
 * Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 */

package net.biomodels.jummp.deployment.biomodels

import com.rometools.rome.feed.rss.Guid
import com.rometools.rome.feed.synd.*
import com.rometools.rome.io.SyndFeedOutput
import grails.transaction.Transactional
import net.biomodels.jummp.deployment.biomodels.feeds.CustomSyndEntryImpl
import net.biomodels.jummp.deployment.biomodels.feeds.CustomSyndFeedImpl
import net.biomodels.jummp.model.Model
import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

/**
 * @short Service responsible for retrieving BioModels ModelOfTheMonth entries.
 *
 * @author Mihai Glonț <mihai.glont@ebi.ac.uk>
 * @author Tung Nguyen <tung.nguyen@ebi.ac.uk>
 */
@Transactional(readOnly = true)
class ModelOfTheMonthService {
    /**
     * The class logger.
     */
    private static final Log log = LogFactory.getLog(ModelOfTheMonth.class)
    /**
     * Threshold for the verbosity of the logger.
     */
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()
    /**
     * Threshold for the verbosity of the logger.
     */
    private static final boolean IS_DEBUG_ENABLED = log.isDebugEnabled()

    private static final String PREFIX_MOM_LINK = "https://www.ebi.ac.uk/biomodels/content/model-of-the-month"

    def grailsApplication

    List fetchEntriesForModel(Long id) {
        List entries = ModelOfTheMonth.withCriteria {
            models {
                eq "id", id
            }
        }
        entries*.toCommandObject()
    }

    def list() {
        List<ModelOfTheMonth> entries = ModelOfTheMonth.getAll()
        List<ModelOfTheMonthTransportCommand>  entryCommands = new ArrayList<>()
        for (ModelOfTheMonth entry : entries) {
            entryCommands.add(entry.toCommandObject())
        }
        entryCommands
    }

    /**
     * Get the model of the month record by the identifier,
     * then convert it to transport command object
     *
     * @param id An integer denoting the identifier of the domain object
     * @return the corresponding transport command object of the domain object
     */
    ModelOfTheMonthTransportCommand get(int id) {
        ModelOfTheMonth m = ModelOfTheMonth.get(id)
        m?.toCommandObject()
    }

    /**
     * Create or update a record of Model of The Month
     * By passing an object bringing date of an entry of Model of The Month, this method
     * will try to look for in the database in order to determine to create a new record
     * or update the existing one that data are accordance with the command.
     *
     * @param   command The transport command object representing the data of the object in demand
     * @return  The latest record has been created or updated
     */
    @Transactional
    ModelOfTheMonth doCreateOrUpdate(ModelOfTheMonthTransportCommand command) {
        ModelOfTheMonth entry
        if (command?.id) {
            entry = ModelOfTheMonth.get(command?.id)
        } else {
            entry = new ModelOfTheMonth()
        }
        if (entry) {
            entry.publicationDate = command.publicationDate
            entry.lastUpdated = command.lastUpdated
        } else {
            entry.lastUpdated = new Date()
            entry.publicationDate = new Date()
        }
        // for the models associated with this entry
        Set<Model> models = new HashSet<>()
        command.associatedModelMap.each {
            Long id = it.key
            Model model = Model.get(id)
            models.add(model)
        }
        entry.models = models
        entry.authors = command.authors
        entry.title = command.title
        entry.shortDescription = command.shortDescription
        if (command.previewImage) {
            entry.previewImage = Base64.decoder.decode(command.previewImage)
        }
        if (entry.save(flush: true)) {
            log.debug("The entry (${entry.id}) of the model of the month ${command.getYearMonth()} has been saved successfully!")
        } else {
            log.error("""\
There are errors when trying to persist entry (${entry.id}) of the model of the month ${command.getYearMonth()} into the database: ${entry.errors.allErrors.inspect()}""")
            entry = null
        }
        entry
    }

    String createFeeds() {
        List<ModelOfTheMonthTransportCommand> momEntries = list()
        momEntries.sort { m1, m2 -> m2.publicationDate <=> m1.publicationDate }

        String feedType = "rss_2.0"

        SyndEntry entry
        List entries = new ArrayList()
        for (model in momEntries) {
            entry = convertToSyndEntry(model, feedType)
            entries.add(entry)
        }

        SyndFeed feed = createFeed(feedType)
        feed.setEntries(entries)

        SyndFeedOutput output = new SyndFeedOutput()
        String result = output.outputString(feed, true)
        return result
    }

    private SyndFeed createFeed(String feedType) {
        String title = "Models of The Month"
        String link = "${PREFIX_MOM_LINK}?all=yes"
        String description = """\
Every month, a scientist from the BioModels Database team selects a model to further investigate and writes a synopsis to explain that model in details."""
        SyndFeed feed = feedType == "rss_2.0" ? new CustomSyndFeedImpl() : new SyndFeedImpl()
        Date currentDate = new Date()
        String currentYear = currentDate.format('YYYY')
        feed.setFeedType(feedType)
        feed.setTitle(title)
        feed.setLink(link)
        feed.setDescription(description)
        feed.setLanguage("en-GB")
        feed.setCopyright("Copyright 2005-${currentYear}, EMBL-EBI")
        feed.setManagingEditor("biomodels-developers@lists.sf.net (BioModels Team)")
        final String iconUrl = "${grailsApplication.config.grails.serverURL}/images/biomodels/logo_small.png"
        final SyndImage image = new SyndImageImpl()
        image.setTitle(title)
        image.setUrl(iconUrl)
        feed.setImage(image)
        feed.setIcon(image)
        feed
    }

    private SyndEntry convertToSyndEntry(ModelOfTheMonthTransportCommand model, String feedType) {
        SyndEntry entry
        entry = feedType == "rss_2.0" ? new CustomSyndEntryImpl() : new SyndEntryImpl()
        entry.setTitle(StringEscapeUtils.escapeXml(model.title))
        entry.setPublishedDate(model.publicationDate)

        /* prepare the entry link */
        String year = model.publicationDate.format('YYYY')
        String month = model.publicationDate.format('MM')
        String uniqueModelMonth = "year=${year}&month=${month}"
        String link = "${PREFIX_MOM_LINK}?${uniqueModelMonth}"
        entry.setLink(link)
        Guid guid = new Guid()
        guid.setValue(uniqueModelMonth)
        entry.setUri(guid.value)

        /* prepare the entry description */
        SyndContent entryDescription
        entryDescription = new SyndContentImpl()
        entryDescription.setType("text/html")
        String escapedDescription = StringEscapeUtils.escapeXml(model.shortDescription)
        entryDescription.setValue(escapedDescription)
        entry.setDescription(entryDescription)
        entry
    }
}

