/**
 * Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
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

import grails.converters.JSON
import net.biomodels.jummp.core.model.FlagTransportCommand
import net.biomodels.jummp.core.model.ModelState
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand as RFTC
import net.biomodels.jummp.model.PublicationLinkProvider
import net.biomodels.jummp.statistic.RecentlyPublishedModel

import java.text.DateFormatSymbols
import java.text.SimpleDateFormat

/**
 * @short General purpose helper for rendering BioModels pages.
 *
 * @author <a href="mailto:mihai.glont@ebi.ac.uk">Mihai Glonț</a>
 * @author <a href="mailto:tung.nguyen@ebi.ac.uk">Tung Nguyen</a>
 */
class BioModelsTagLib {
    static defaultEncodeAs = [taglib:'none']
    static namespace = 'biomd'
    /**
     * Declare dependency injections
     */
    def grailsApplication
    def decorationService
    def modelOfTheMonthService
    def tagService
    def p2mService

    /**
     * <p>Displays the Model of the Month (MoM) entry for the given model.
     *
     * <p>modelId REQUIRED the id of the model for which to render
     * the {@link ModelOfTheMonth} entry.
     */
    def renderModelOfMonth = { attrs ->
        Long id = attrs.modelId
        if (!id) {
            return
        }
        def entries = modelOfTheMonthService.fetchEntriesForModel id
        if (entries) {
            out << render(collection: entries, template: '/templates/modelOfTheMonth',
                plugin: 'jummp-plugin-biomodels-dom')
        }
    }

    def renderModelFlags = { attrs ->
        def base64Flags = attrs.flags?.collect { FlagTransportCommand cmd ->
            [
                img: Base64.encoder.encodeToString(cmd.icon),
                label: cmd.label,
                description: cmd.description
            ]
        }
        out << render(collection: base64Flags, template: '/templates/modelFlags',
                plugin: 'jummp-plugin-biomodels-dom', var: "flag")
    }

    /**
     * Rendering CurationNotes tab for the curated models
     */
    def renderCurationNotesTab = { attrs ->
        out << "<div id='Curation' class='row'>"
        def modelId =  attrs.model
        Map requiredParams = ["model": modelId]
        if (attrs.curationNotes != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss")
            def base64CurationNotes = attrs.curationNotes?.collect { CurationNotesTransportCommand cmd ->
                [
                    model: cmd.model,
                    submitter: cmd.submitter,
                    lastModifier: cmd.lastModifier,
                    dateAdded: dateFormat.format(cmd.dateAdded),
                    lastModified: dateFormat.format(cmd.lastModified),
                    comment: cmd.comment ?: "",
                    curationImage: cmd.curationImage ? Base64.encoder.encodeToString(cmd.curationImage) : null
                ]
            }
            requiredParams.put("cnId", attrs.curationNotes.id)
            // use class 'row' specifically designed by EBI Visual Framework to gain responsive design performance
            out << render(collection: base64CurationNotes, template: '/templates/curationNotes',
                    plugin: 'jummp-plugin-biomodels-dom', var: 'curaRec')
        } else {
            out << "<h3>The simulation result for this model is not present</h3>"
        }
        boolean hasCuratorRole = attrs.hasCuratorRole
        if (hasCuratorRole) {
            def btnLabel = attrs.curationNotes ? "Edit" : "Add"
            def actionName = "show"
            def href = g.link(controller: "curationNotes",
                action: actionName, class: "button",
                params: requiredParams) {
                btnLabel
            }
            out << render(template: '/templates/curationNotesAddOrUpdateButton',
                plugin: 'jummp-plugin-biomodels-dom', model: ['href': href])
        }
        out << "</div>" // for id = Curation
    }

    def renderModellingApproaches = { attrs ->
        out << render(collection:  attrs.modellingApproaches,
            template: '/templates/modellingApproach',
            plugin: 'jummp-plugin-biomodels-dom', var: 'modellingApproach')
    }

    def renderOriginalModels = { attrs ->
        out << render(collection: attrs.sources,
            template: '/templates/originalModel',
            plugin: 'jummp-plugin-biomodels-dom', var: 'source')
    }

    /**
     * Renders Submission/Update feature widget for the home page
     */
    def renderHomePageFeaturesDataSubmission = {
        def total = decorationService.fetchStatisticsTotalSubmissions()
        def totalSubmissions = String.format("%,d", total)
        out << render(template: "/templates/biomodels/homePage/hp-features-data-submission",
                    model: [totalSubmissions: totalSubmissions])
    }

    /**
     * Renders Model Browse feature widget for the home page
     */
    def renderHomePageFeaturesModelBrowse = {
        Long total = decorationService.fetchStatisticsTotalIndividualModels()
        def totalIndividualModels = String.format("%,d", total)
        total = decorationService.fetchStatisticsCuratedModels()
        def totalCuratedModels = String.format("%,d", total)
        total = decorationService.fetchStatisticsNonCuratedModels()
        def totalNonCuratedModels = String.format("%,d", total)
        total = decorationService.fetchStatisticsTotalAutoGeneratedModels()
        def totalAutoGeneratedModels = String.format("%,d", total)
        total = decorationService.fetchStatisticsTotalGOClasses()
        def totalGOClasses = String.format("%,d", total)
        out << render(template: "/templates/biomodels/homePage/hp-features-model-browse",
                    model: [totalIndividualModels: totalIndividualModels,
                            totalAutoGeneratedModels: totalAutoGeneratedModels,
                            totalCuratedModels: totalCuratedModels,
                            totalNonCuratedModels: totalNonCuratedModels,
                            totalGOClasses: totalGOClasses])
    }

    /**
     * Renders Parameters Search feature widget for the home page
     */
    def renderHomePageFeaturesParametersSearch = {
        Long total = decorationService.fetchStatisticsTotalParametersEntries()
        def totalRecords = String.format("%,d", total)
        out << render(template: "/templates/biomodels/homePage/hp-features-parameters-search",
            model: [totalRecords: totalRecords])
    }

    def renderHomePageStatisticsModellingApproaches = {
        def fetchedApproaches = decorationService.fetchStatisticsModellingApproaches()
        def approaches = fetchedApproaches.collect { entry ->
            [label: entry.key, count: entry.value]
        } as JSON
        out << render(template: "/templates/biomodels/homePage/hp-statistics-modelling-approaches-d3",
            model: [approaches: approaches])
    }

    def renderHomePageStatisticsOrganisms = {
        def organisms = decorationService.fetchStatisticsOrganisms() as JSON
        out << render(template: "/templates/biomodels/homePage/hp-statistics-organisms-d3",
            model: [organisms: organisms])
    }

    def renderHomePageStatisticsJournals = {
        def journalsMap = decorationService.fetchStatisticsJournals()
        def journals = journalsMap.collect { entry ->
            [name: entry.key, value: entry.value]
        } as JSON
        out << render(template: "/templates/biomodels/homePage/hp-statistics-journals-d3",
            model: [journals: journals])
    }

    def renderRecentlyAccessedModels = {
        Map<String, String> models = decorationService.fetchRecentlyAccessedModels()
        out << render(template: "/templates/biomodels/homePage/hp-recently-accessed-models-widget",
            model: [models: models])
    }

    def renderRecentlyPublishedModels = {
        Map<String, RecentlyPublishedModel> models = decorationService.fetchRecentlyPublishedModels()
        String serverURL = grailsApplication.config.grails.serverURL
        out << render(template: "/templates/biomodels/homePage/hp-recently-published-models-widget",
                      model: [models: models, serverURL: serverURL])
    }

    def renderNewsWidget = {
        Map<String, String> newsItems = decorationService.fetchDataNewsWidget()
        String serverURL = grailsApplication.config.grails.serverURL
        out << render(template: "/templates/biomodels/homePage/hp-news-widget",
            model: [newsItems: newsItems, serverURL: serverURL])
    }

    def renderTheLatestMoMEntryWidget = {
        Map<String, String> momEntry = decorationService.fetchMomEntry()
        if (momEntry) {
            out << render(template: "/templates/biomodels/homePage/theLatestMomEntryWidget", model: momEntry)
        } else {
            out << render(template: "/templates/biomodels/homePage/theEmptyMoMEntry")
        }
    }

    def renderConvertedFiles = { attrs ->
        List<RFTC> convertedFilesTC = attrs.convertedFilesTC
        out << "<ul>"
        out << render(plugin: "jummp-plugin-web-application",
            template: "/templates/model/convert/convertedFileShow",
            collection: convertedFilesTC, var: "fileTC")
        out << "</ul>"
    }

    def renderAllMoMEntriesPage = {
        Map sortedEntries = modelOfTheMonthService.buildAllEntries()
        DateFormatSymbols dfs = new java.text.DateFormatSymbols()
        out << render(template: "/templates/momIntroAllEntriesPage", plugin: "jummp-plugin-biomodels-dom",
            model: ['years': sortedEntries.keySet()])
        sortedEntries.each { String year, Set values ->
            out << render(template: "/templates/momYearTitleInAllEntriesPage", plugin: "jummp-plugin-biomodels-dom",
                model:['year': year])
            out << "<ul>"
            values.each { ModelOfTheMonthTransportCommand cmd ->
                int month = cmd.publicationMonth
                String monthName = dfs.months[month]
                String links = cmd.associatedModelMap.values().collect { String id ->
                    '<a href="' + g.createLink(controller: "model", action: "show", id: id) + '" target="_blank">' + id + '</a>'
                }.join(", ")
                String momLink = cmd.formattedURL.substring(8) // remove 'content/'
                Map entryMap = new LinkedHashMap()
                entryMap = ["monthName": monthName,
                       "models": cmd.associatedModelMap,
                       "links": links, "momLink": momLink,
                       "title": cmd.title,
                       "formattedURL": cmd.formattedURL,
                       "authors": cmd.authors]
                out << render(model: ['entry': entryMap],
                    template: "/templates/momEntryInAllEntriesPage", plugin: "jummp-plugin-biomodels-dom")
            }
            out << "</ul>"
        }
    }

    def displayDisclaimer = { attrs ->
        def revision = attrs.revision
        boolean isPublic = revision.state == ModelState.PUBLISHED
        boolean published = revision.model?.firstPublished != null
        String manualLabel = PublicationLinkProvider.LinkType.MANUAL_LABEL
        String linkType = revision.model.publication?.linkProvider?.linkType
        boolean manualPubEntry = linkType == manualLabel
        boolean withoutPublication = revision.model?.publication == null
        if (isPublic && published && (manualPubEntry || withoutPublication)) {
            out << render(template: "/templates/metadataSeparator", plugin: "jummp-plugin-biomodels-dom")
            String message = ""
            if (withoutPublication) {
                message = "This model has been pre-published upon author's request without reference publication."
            } else {
                message = "This model has been published without a web link to the reference publication."
            }
            out << render(template: "/templates/displayDisclaimer", plugin: "jummp-plugin-biomodels-dom",
                model: ['message': message])
        }
    }

    def showTags = { attrs ->
        Set<TagTransportCommand> bmTags = attrs.bmTags
        out << render(template: "/templates/showTags", plugin: "jummp-plugin-biomodels-dom", model: ['bmTags': bmTags])
    }

    def showEditableTags = { attrs ->
        Set<TagTransportCommand> bmTags = attrs.bmTags
        Set<Integer> tagIdSet = bmTags.collect { it.id }
        Set<TagTransportCommand> allTags = tagService.all.toSet()
        Set<TagTransportCommand> unTags = allTags.findAll { !tagIdSet.contains(it.id) }
        out << render(template: "/templates/showEditableTags", plugin: "jummp-plugin-biomodels-dom", model: ['tags': bmTags, 'unTags': unTags])
    }

    def insertSeparator = {
        out << render(template: "/templates/metadataSeparator", plugin: "jummp-plugin-biomodels-dom")
    }

    def insertSectionSeparator = {
        out << render(template: "/templates/sectionSeparator", plugin: "jummp-plugin-biomodels-dom")
    }

    def renderGridViewForPath2ModelsCategory = { attrs ->
        Set categories = attrs.categories
        int size = categories?.size()
        int nCol = 6
        int nElePerCol = (int) Math.ceil((double)size / 6)
        Map<Integer, Set> result = [:]
        int i = 0
        int bagId = 1
        while (i < size && bagId <= nCol) {
            Set elements = new TreeSet<AutoGeneratedCategory>(p2mService.abcOrderCategoryComparator())
            int j = 0
            while (j < nElePerCol && categories[i+j]) {
                elements.add(categories[i+j])
                j++
            }
            result.put(bagId, elements)
            i += nElePerCol
            bagId++
        }

        result.each {
            out << render(template: "/templates/p2mBrowsePageEachColumn",
                plugin: "jummp-plugin-biomodels-dom",
                model: [group: it.value])
        }

    }

    def renderPdgsmmDiseasesInTwoColumnsLayout = { attrs ->
        Map m = attrs.categories
        Map leftColumn = new TreeMap<String, TreeSet>()
        Map rightColumn = new TreeMap<String, TreeSet>()
        int nbElePerCol = m.size() / 2
        m.eachWithIndex { e, int i ->
            if (i <= nbElePerCol) {
                leftColumn.put(e.key, e.value)
            } else {
                rightColumn.put(e.key, e.value)
            }
        }
        out << '<div class="row">'
        out << render(template: "/templates/pdgsmmDiseaseColumn", plugin: "jummp-plugin-biomodels-dom",
            model: [diseases: leftColumn])
        out << render(template: "/templates/pdgsmmDiseaseColumn", plugin: "jummp-plugin-biomodels-dom",
            model: [diseases: rightColumn])
        out << '</div>'
    }
}
