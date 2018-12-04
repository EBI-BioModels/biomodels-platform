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
import com.rometools.rome.feed.synd.SyndContent
import com.rometools.rome.feed.synd.SyndContentImpl
import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndEntryImpl
import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.feed.synd.SyndFeedImpl
import com.rometools.rome.io.SyndFeedOutput
import grails.transaction.Transactional
import net.biomodels.jummp.deployment.biomodels.feeds.CustomSyndEntryImpl
import net.biomodels.jummp.deployment.biomodels.feeds.CustomSyndFeedImpl
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringEscapeUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import net.biomodels.jummp.model.Model

import java.text.SimpleDateFormat

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
     * Update the preview image and short description of a given model of the month entry
     */
    @Transactional(readOnly = false)
    ModelOfTheMonth updatePreviewImageAndShortDescription(Long id, byte[] previewImage, String shortDescription) {
        ModelOfTheMonth model = ModelOfTheMonth.findById(id)
        if (model) {
            model.previewImage = previewImage
            model.shortDescription = shortDescription
            ModelOfTheMonth updatedModel = model.save(flush: true)
            log.info "${model.id}: ${model.shortDescription}"
            if (!model.save(flush: true)) {
                log.debug("Errors at trying to save MoM: ${model.errors.allErrors.toString()}")
                return null
            } else {
                return model
            }
        } else {
            log.debug("Errors at trying to save MoM: ${model.errors.allErrors.toString()}")
            return null
        }
    }

    /**
     * Try to update the preview image and short description for entire model of the month entries if
     * they haven't been attached these information
     */
    @Transactional(readOnly = false)
    List<ModelOfTheMonth> updatePreviewImageAndShortDescription() {
        String prefixUrl = "https://www.ebi.ac.uk/biomodels/ModelMonth/"
        List<ModelOfTheMonth> modelOfTheMonths = ModelOfTheMonth.getAll()
        List<ModelOfTheMonth> results = []
        SimpleDateFormat dateFormat = new SimpleDateFormat('yyyy-MM')
        modelOfTheMonths.each { ModelOfTheMonth model ->
            Date publicationDate = model.publicationDate
            String momFolder = dateFormat.format(publicationDate) // this is the folder pattern of where is storing MoM entry
            String momFolderLink = "${prefixUrl}/${momFolder}"
            String imageFileName = "preview.png"
            String momImageLink = "${momFolderLink}/${imageFileName}"
            URL imageURL = new URL(momImageLink)
            int responseCode = imageURL.openConnection().getResponseCode()
            byte[] previewImage = []
            String shortDescription = ""
            if (responseCode == 200) {
                def InputStream is = new BufferedInputStream(imageURL.openStream())
                byte[] bytes = IOUtils.toByteArray(is)
                previewImage = bytes
            }
            String momShortDescriptionLink = "${momFolderLink}/briefdescrib"
            URL textURL = new URL(momShortDescriptionLink)
            responseCode = textURL.openConnection().getResponseCode()
            if (responseCode == 200) {
                def InputStream is = new BufferedInputStream(textURL.openStream())
                List<String> text = is.readLines()
                shortDescription = text.first()
            }
            results << updatePreviewImageAndShortDescription(model.id, previewImage, shortDescription)
        }
        results
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
        momEntries.sort()
        Collections.sort(momEntries, new Comparator<ModelOfTheMonthTransportCommand>() {
            int compare(ModelOfTheMonthTransportCommand model1,
                        ModelOfTheMonthTransportCommand model2) {
                return model2.publicationDate.compareTo(model1.publicationDate)
            }
        })

        String feedType = "rss_2.0"

        SyndEntry entry
        List entries = new ArrayList()
        for (model in momEntries) {
            entry = convertToSyndEntry(model, feedType)
            entries.add(entry)
        }

        SyndFeed feed = createFeed(feedType)
        feed.setEntries(entries)

        File writer = File.createTempFile("tmp", "xml")
        SyndFeedOutput output = new SyndFeedOutput()
        output.output(feed, writer)
        String result = writer.text
        return result
    }

    private SyndFeed createFeed(String feedType) {
        String title = "Models of The Month"
        String link = "${PREFIX_MOM_LINK}?all=yes"
        String description = """\
Every month, a scientist from the BioModels Database team selects a model to further investigate and writes a synopsis to explain that model in details."""
        SyndFeed feed = feedType == "rss_2.0" ? new CustomSyndFeedImpl() : new SyndFeedImpl()
        feed.setFeedType(feedType)
        feed.setTitle(title)
        feed.setLink(link)
        feed.setDescription(description)
        feed.setLanguage("en-GB")
        feed.setCopyright("Copyright 2005-2018, EMBL-EBI")
        feed.setManagingEditor("biomodels-developers@lists.sf.net (BioModels Team)")
        feed
    }

    private SyndEntry convertToSyndEntry(ModelOfTheMonthTransportCommand model, String feedType) {
        SyndEntry entry
        entry = feedType == "rss_2.0" ? new CustomSyndEntryImpl() : new SyndEntryImpl()
        entry.setTitle(StringEscapeUtils.escapeXml(model.title))
        entry.setPublishedDate(model.publicationDate)

        /* prepare the entry link */
        Calendar calendar = new GregorianCalendar()
        calendar.setTime(model.publicationDate)
        String year = calendar.get(Calendar.YEAR).toString()
        int month = calendar.get(Calendar.MONTH) + 1
        String strMonth = month < 10 ? '0'.concat(month.toString()) : month.toString()
        String uniqueModelMonth = "year=${year}&month=${strMonth}"
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

