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
 *
 * Additional permission under GNU Affero GPL version 3 section 7
 *
 * If you modify Jummp, or any covered work, by linking or combining it with
 * LibPharmml (or a modified version of that library), containing parts
 * covered by the terms of Apache License v2.0, the licensors of this
 * Program grant you additional permission to convey the resulting work.
 * {Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of LibPharmml used as well as
 * that of the covered work.}
 **/





package net.biomodels.jummp.plugins.pharmml

import eu.ddmore.libpharmml.dom.maths.Equation

class PharmMlTagLib {
    static namespace = "pharmml"

    def pharmMlRenderingService

    def pharmMlMetadataRenderingService

    def decideTabs = { attrs ->
        boolean haveTabsToDisplay = true
        def topics = ["iv", "fd", "md", "td", "est", "sim"]
        topics.inject(haveTabsToDisplay) { display, t ->
            display && attrs."${t}"
        }
        if (!haveTabsToDisplay) {
            return
        }
        //holds information about which PharmML-specific tabs should be shown
        Map<String, String> tabsMap = new HashMap<>(topics.size(), 1.0)
        if (attrs.iv || attrs.fd || attrs.md) {
            final String MDEF_TAB = "modelDefinition"
            tabsMap["mdef"] = MDEF_TAB
            out << "<li><a href='#${MDEF_TAB}'>Model Definition</a></li>\n"
        }
        if (attrs.td) {
            final String TD_TAB = "trialDesign"
            tabsMap["td"] = TD_TAB
            out << "<li><a href='#${TD_TAB}'>Trial Design</a></li>\n"
        }
        if (attrs.est) {
            final String EST_TAB = "estimationSteps"
            tabsMap["est"] = EST_TAB
            out << "<li><a href='#${EST_TAB}'>Estimation Steps</a></li>\n"
        }
        if (attrs.sim) {
            final String SIM_TAB = "simulationSteps"
            tabsMap["sim"] = SIM_TAB
            out << "<li><a href='#${SIM_TAB}'>Simulation Steps</a></li>\n"
        }
        pageScope.tabsMap = tabsMap
    }

    def handleModelDefinitionTab = { attrs ->
        if (!pageScope.tabsMap["mdef"]) {
            return
        }
        out << "<div id='${pageScope.tabsMap["mdef"]}'>"
        if (attrs.iv) {
            pharmMlRenderingService.renderIndependentVariable(attrs.iv, attrs.version, out)
        }
        if (attrs.fd) {
            pharmMlRenderingService.renderFunctionDefinitions(attrs.fd, attrs.version, out)
        }
        if (attrs.sm) {
            pharmMlRenderingService.renderStructuralModel(attrs.sm, attrs.iv, attrs.version, out)
        }
        if (attrs.vm) {
            pharmMlRenderingService.renderVariabilityModel(attrs.vm, attrs.version, out)
        }
        // resolve references to covariates from the parameter model
        Map<String, Equation> continuousCovariateTransformations = [:]
        if (attrs.cm) {
            pharmMlRenderingService.renderCovariateModel(attrs.cm,
                    continuousCovariateTransformations, attrs.version, out)
        }
        if (attrs.pm) {
            pharmMlRenderingService.renderParameterModel(attrs.pm, attrs.cm,
                        continuousCovariateTransformations, attrs.version, out)
        }
        if (attrs.om) {
            pharmMlRenderingService.renderObservationModel(attrs.om, attrs.cm, attrs.version, out)
        }
        out << "</div>\n"
    }

    def handleTrialDesignTab = { attrs ->
        if (!pageScope.tabsMap["td"]) {
            return
        }
       	String link=g.createLink(controller: 'model',
       							 action: 'download',
       							 id: attrs.rev.identifier()).replace("%3A",".");
        out << "<div id='${pageScope.tabsMap["td"]}'>"
        if (attrs.ts) {
            pharmMlRenderingService.renderTrialDesignStructure(attrs.ts, attrs.version, out)
        }
        if (attrs.td) {
            pharmMlRenderingService.renderIndividualDosing(attrs.td, attrs.version, out, attrs.rev, link)
        }
        if (attrs.tp) {
            pharmMlRenderingService.renderPopulation(attrs.tp, attrs.version, out, attrs.rev, link)
        }
        out << "</div>\n"
    }

    def handleModellingStepsTabs = { attrs ->
        if (!attrs.estimation && !attrs.simulation && !pageScope.tabsMap) {
            return
        }
        if (!attrs.independentVariable) {
            // the default independent variable is assumed to be time.
            attrs.independentVariable = "time"
        }

        if (attrs.estimation) {
            out << "<div id='${pageScope.tabsMap["est"]}'>"
            String link=g.createLink(controller: 'model', 
            						 action: 'download',
            						 id: attrs.rev.identifier()).replace("%3A",".");
            pharmMlRenderingService.renderEstimationSteps(attrs.estimation, attrs.version, out, attrs.rev, link)
            //only consider step dependencies here when there are no simulation steps
            if (!pageScope.tabsMap["sim"]) {
                pharmMlRenderingService.renderStepDependencies(attrs.deps, attrs.version, out)
            }
            out << "</div>"
        }
        if (attrs.simulation) {
            out << "<div id='${pageScope.tabsMap["sim"]}'>"
            pharmMlRenderingService.renderSimulationSteps(attrs.simulation,
                        attrs.independentVariable, attrs.version, out)
            pharmMlRenderingService.renderStepDependencies(attrs.deps, attrs.version, out)
            out << "</div>"
        }
    }
    /*TODO rename this to simpleParams and get rid of the other one*/
    /**
     * @attr simpleParameters REQUIRED the list of simple parameters to render.
     * @attr version REQUIRED the version of PharmML used to encode this model.
     * @attr transfMap OPTIONAL contains the tranformation for each continuous covariate.
     */
    def simpleParamsClosure = { attrs ->
        if (!attrs.simpleParameters) {
            return
        }
        def transf = attrs.transfMap ?: [:]
        pharmMlRenderingService.renderSimpleParameters(attrs.simpleParameters, transf,
                attrs.version, out)
    }

    /**
     * Renders the continuous covariates from a covariate model.
     *
     * @attr covariates REQUIRED the list of continuous covariates to render.
     * @attr version REQUIRED the version of PharmML used to encode the model.
     * @attr blkId REQUIRED the block identifier of the covariate model.
     * @attr transf OPTIONAL the transformations for continuous covariates.
     */
    def covariates = { attrs ->
        if (!attrs.covariates) {
            return
        }
        pharmMlRenderingService.renderCovariates(attrs.covariates, attrs.blkId, attrs.transf,
                    attrs.version, out)
    }

    def commonVariables = { attrs ->
        if (!(attrs.vars)) {
            return
        }
        pharmMlRenderingService.renderCommonVariables(attrs.vars, attrs.iv, attrs.version, out)
    }

    def initialConditions = { attrs ->
        if (!attrs.initialConditions) {
            return
        }
        pharmMlRenderingService.renderInitialConditions(attrs.initialConditions, attrs.version, out)
    }

    /**
     * Renders model-level annotations on the 'Overview' tab of the model display.
     *
     * @attr revision REQUIRED the revision for which to display the annotatations.
     */
    def renderGenericAnnotations = { attrs ->
        pharmMlMetadataRenderingService.renderGenericAnnotations(attrs.revision, out)
    }
}

