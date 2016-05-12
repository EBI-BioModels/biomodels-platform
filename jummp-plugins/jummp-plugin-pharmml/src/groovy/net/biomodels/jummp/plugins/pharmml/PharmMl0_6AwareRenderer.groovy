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
 * Apache Tika, Apache Commons, LibPharmml, Perf4j (or a modified version of these
 * libraries), containing parts covered by the terms of Apache License v2.0,
 * the licensors of this Program grant you additional permission to convey the
 * resulting work.
 *{Corresponding Source for a non-source form of such a combination shall
 * include the source code for the parts of Apache Tika, Apache Commons,
 * LibPharmml, Perf4j used as well as that of the covered work.}**/

package net.biomodels.jummp.plugins.pharmml

import eu.ddmore.libpharmml.dom.commontypes.*
import eu.ddmore.libpharmml.dom.commontypes.Vector as CTVector
import eu.ddmore.libpharmml.dom.dataset.DataSet
import eu.ddmore.libpharmml.dom.maths.*
import eu.ddmore.libpharmml.dom.modeldefn.*
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.CompartmentMacro
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.MacroValue
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacro
import eu.ddmore.libpharmml.dom.modeldefn.pkmacro.PKMacroList
import eu.ddmore.libpharmml.dom.modellingsteps.*
import eu.ddmore.libpharmml.dom.trialdesign.IndividualDosing
import eu.ddmore.libpharmml.dom.trialdesign.Population
import eu.ddmore.libpharmml.dom.trialdesign.TrialDesign
import eu.ddmore.libpharmml.dom.trialdesign.TrialStructure
import eu.ddmore.libpharmml.dom.uncertml.BinomialDistribution
import eu.ddmore.libpharmml.dom.uncertml.BinomialDistributionType
import eu.ddmore.libpharmml.dom.uncertml.PoissonDistributionType
import grails.util.Holders
import net.biomodels.jummp.core.model.RevisionTransportCommand
import net.biomodels.jummp.plugins.pharmml.util.correlation.CorrelationMatrix
import net.biomodels.jummp.plugins.pharmml.util.correlation.PharmMl0_3AwareCorrelationProcessor
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.perf4j.aop.Profiled

import javax.xml.bind.JAXBElement

class PharmMl0_6AwareRenderer extends AbstractPharmMlRenderer {
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    private static final String IS_DEBUG_ENABLED = log.isDebugEnabled()
    private static final String IS_INFO_ENABLED = log.isInfoEnabled()
    /* Dependency injection for groovyPageRenderer */
    def groovyPageRenderer = Holders.applicationContext.getBean("groovyPageRenderer")

    /* lazy-loaded instance of this class.*/
    private static PharmMl0_6AwareRenderer instance = null

    /* Enforce the singleton pattern by keeping the constructor private. */

    private PharmMl0_6AwareRenderer() {}

    @Profiled(tag = "pharmMl0_6AwareRenderer.getInstance")
    public static PharmMl0_6AwareRenderer getInstance() {
        if (instance == null) {
            synchronized (PharmMl0_6AwareRenderer.class) {
                if (instance == null) {
                    if (IS_DEBUG_ENABLED) {
                        log.debug "Initialising the renderer for PharmML 0.6"
                    }
                    instance = new PharmMl0_6AwareRenderer()
                }
            }
        } else if (IS_DEBUG_ENABLED) {
            log.debug "Returning the already-initialised instance of the PharmML 0.6 renderer"
        }
        if (IS_INFO_ENABLED) {
            log.info "Returning the PharmML 0.6 renderer."
        }
        return instance
    }

    /**
     * @param independentVariable the independent variable to render
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderIndependentVariable")
    String renderIndependentVariable(String independentVariable) {
        if (IS_INFO_ENABLED) {
            log.info "Rendering independent variable $independentVariable"
        }
        return groovyPageRenderer.render(template: "/templates/common/independentVariable",
            model: [independentVariable: independentVariable])
    }

    /**
     * @param functionDefinitions the list of
     * {@link eu.ddmore.libpharmml.dom.commontypes.FunctionDefinition}s.
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderFunctionDefinitions")
    String renderFunctionDefinitions(List functionDefinitions) {
        def definitionList = []
        try {
            functionDefinitions.collect(definitionList) { d ->
                def rightHandSide
                if (d.definition.equation) {
                    rightHandSide = d.definition.equation
                } else if (d.definition.scalar) {
                    rightHandSide = d.definition.scalar
                } else if (d.definition.symbRef) {
                    rightHandSide = d.definition.symbRef
                }
                //should not be null by now
                assert !!rightHandSide
                return convertToMathML(d.symbId, d.functionArgument, rightHandSide)
            }
        } catch (Exception e) {
            log.error("Error while rendering function definitions ${functionDefs.inspect()}: ${e.message}", e)
            definitionList.add("Sorry, cannot render the function definitions.")
        } finally {
            return groovyPageRenderer.render(template: "/templates/common/functionDefinitions",
                model: [functionDefinitions: definitionList])
        }
    }

    /**
     * @param modelDefinition an instance of {@link eu.ddmore.libpharmml.dom.modeldefn.ModelDefinition}
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderModelDefinition")
    String renderModelDefinition(ModelDefinition modelDefinition) {}

    /**
     * @param covModel a list of
     * {@link eu.ddmore.libpharmml.dom.modeldefn.CovariateModel}s.
     * @param transfMap the transformations for continuous covariates.
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderCovariateModel")
    String renderCovariateModel(List covModel, Map transfMap) {
        def model = [:]
        def result = []
        try {
            covModel.each { cm ->
                def thisCovModel = [:]
                thisCovModel["blkId"] = cm.blkId
                if (cm.simpleParameter) {
                    thisCovModel["parameters"] = cm.simpleParameter
                }
                if (cm.covariate) {
                    thisCovModel["covariates"] = cm.covariate
                }
                result.add thisCovModel
            }
        } catch (Exception e) {
            model["error"] = "Sorry, something went wrong while rendering the covariates."
            log.error("Error rendering the covariates ${covModel.inspect()} ${covModel.properties} ${transfMap.inspect()}: ${e.message}", e)
        } finally {
            model["covariateModels"] = result
            model["version"] = "0.6"
            model["transfMap"] = transfMap
            return groovyPageRenderer.render(template: "/templates/0.2/covariateModel",
                model: model)
        }
    }

    /**
     * @param cov a a list of {@link eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition}s.
     * @param blkId the block identifier of the covariate model where @p cov are defined.
     * @param transfMap the transformations for continuous covariates.
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderCovariates")
    String renderCovariates(List<CovariateDefinition> cov, String blkId, Map transfMap) {
        def model = [:]
        def covariates = []
        try {
            cov.each { c ->
                def thisCov = [:]
                String symbol = c.symbId
                thisCov["symbId"] = symbol
                if (c.continuous) {
                    def cc = c.continuous
                    def ccMap = [:]
                    def transformations = cc.getListOfTransformation()
                    if (transformations) {
                        ccMap['transf'] = []
                        transformations.each { t ->
                            final EquationType TRANSF_EQ = t.equation
                            final String TRANSF = convertToMathML(t.transformedCovariate.symbId, TRANSF_EQ)
                            final String COV_KEY = "${blkId}_${t.transformedCovariate.symbId}"
                            transfMap[COV_KEY] = TRANSF_EQ
                            ccMap["transf"] << TRANSF
                        }
                        final def COV_DISTRIB = cc.abstractContinuousUnivariateDistribution
                        if (COV_DISTRIB) {
                            final String DISTRIB = distributionAssignment(symbol, COV_DISTRIB)
                            ccMap["dist"] = DISTRIB
                        }
                    }
                    thisCov["continuous"] = ccMap
                } else if (c.categorical) {
                    List cc = c.categorical.category
                    List categoryList = []
                    cc.each { cat ->
                        StringBuilder sb = new StringBuilder(cat.catId)
                        if (cat.name) {
                            sb.append("(").append(cat.name.value).append(")")
                        }
                        if (cat.probability) {
                            sb.append("&emdash;").append(scalarRhs(cat.probability))
                        }
                        categoryList.add(sb.toString())
                    }
                    String categories = categoryList.join("; ")
                    thisCov["categorical"] = categories
                }
                covariates.add(thisCov)
            }
        } catch (Exception e) {
            log.error("Error rendering the covariates ${covariates.inspect()}:${e.message}", e)
            model["error"] = "Sorry, something went wrong while rendering the covariates."
        } finally {
            model["covariates"] = covariates
            model["version"] = "0.6"
            model["transfMap"] = transfMap
            return groovyPageRenderer.render(template: "/templates/0.6/covariates", model: model)
        }
    }

    /**
     * @param variabilityModels a list of
     * {@link eu.ddmore.libpharmml.dom.modeldefn.VariabilityDefnBlock}s.
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderIndependentVariable")
    String renderVariabilityModel(List<VariabilityDefnBlock> variabilityModels) {
        def models = []
        variabilityModels.each { m ->
            def thisModel = [:]
            thisModel["blkId"] = m.blkId
            thisModel["name"] = m.name ?: "&nbsp;"
            thisModel["levels"] = formatVariabilityLevels(m.level)
            thisModel["type"] = m.type.value()
            models.add thisModel
        }
        return groovyPageRenderer.render(template: "/templates/0.2/variabilityModel",
            model: [variabilityModels: models])
    }

    List<String> formatVariabilityLevels(List variabilityLevels) {
        def result = []
        variabilityLevels.inject(result) { r, l ->
            StringBuilder sb = new StringBuilder()
            sb.append((l.name) ? l.name.value : l.symbId)
            if (l.parentLevel) {
                sb.append(", parent level: ").append(l.parentLevel.symbRef.symbIdRef)
            }
            result.add sb.toString()
        }
        return result
    }

    @Override
    protected StringBuilder rhs(Rhs r, StringBuilder text) {
        if (r.interpolation) {
            return text.append(renderInterpolation(r.interpolation))
        }
        return super.rhs(r, text)
    }

    protected String renderInterpolation(Interpolation i) {
        String algorithm = i?.algorithm
        String variable = i?.interpIndepVar?.symbRef?.symbIdRef
        if (!algorithm) {
            return "Missing interpolation algorithm"
        }
        if (!variable) {
            return "Missing interpolation independent variable"
        }
        return "$algorithm interpolation over $variable"
    }

    /**
     * Expects as arguments the parameter model as well as the covariate model.
     * The latter is necessary to display the transformations that are defined
     * for each individual parameter.
     *
     * @param parameterModel a list of
     * {@link eu.ddmore.libpharmml.dom.modeldefn.ParameterModel}s.
     * @param covariates a list of covariate models.
     * @param transfMap the transformations for the continuous covariates.
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderParameterModel")
    String renderParameterModel(List parameterModel,
                                List covariates, Map transfMap) {

        def result = new StringBuilder()
        result.append("<h3>Parameter Model</h3>")
        result.append("<span class=\"bold\">Parameters </span>")
        try {
            parameterModel.each { pm ->
                result.append("<div class='spaced-top-bottom'>")
                def simpleParameters = pm.commonParameterElement.value.findAll {
                    it instanceof SimpleParameter
                }
                def rv = pm.commonParameterElement.value.findAll {
                    it instanceof ParameterRandomVariable
                }
                def individualParameters = pm.commonParameterElement.value.findAll {
                    it instanceof IndividualParameter
                }
                result.append(simpleParams(simpleParameters, transfMap))

                Map<String, List<String>> paramRandomVariableMap = [:]
                String randoms = randomVariables(rv, paramRandomVariableMap)
                if (randoms) {
                    result.append(randoms)
                }
                StringBuilder individuals = individualParams(individualParameters, rv, covariates,
                    transfMap)
                if (individuals) {
                    result.append(individuals)
                }
                if (pm.correlation) {
                    def processor = new PharmMl0_3AwareCorrelationProcessor()
                    List<CorrelationMatrix> matrices = processor.convertToStringMatrix(
                        pm.correlation, paramRandomVariableMap)
                    if (matrices) {
                        displayCorrelationMatrices(matrices, result)
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error rendering the parameter model for ${parameterModel.inspect()} ${parameterModel.properties}: ${e.message}", e)
            result.append("Sorry, something went wrong while rendering the parameter model.")
        } finally {
            return result.append("</div>").toString()
        }
    }

    /**
     * @param structuralModels a list of
     * {@link eu.ddmore.libpharmml.dom.modeldefn.StructuralModel}s.
     * @param iv the independent variable of the model
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderStructuralModel")
    String renderStructuralModel(List<StructuralModel> structuralModels, String iv) {
        def model = [:]
        model["version"] = "0.6"
        try {
            structuralModels.each { sm ->
                String modelName = sm.name?.value ?: sm.blkId
                model["independentVariable"] = iv
                model["name"] = modelName
                if (sm.simpleParameter) {
                    model["simpleParameters"] = sm.simpleParameter
                }
                if (sm.commonVariable) {
                    model["variableDefinitions"] = sm.commonVariable
                }
                if(sm.getPKmacros()){
                    model["pkMacroList"] = sm.getPKmacros()
                }
            }
        } catch (Exception e) {
            log.error("Error while rendering structural model ${sm.inspect()} ${sm.properties}:${e.message}", e)
            model["error"] = "Sorry, something went wrong while displaying the structural model."
        } finally {
            return groovyPageRenderer.render(template: "/templates/common/structuralModel",
                model: model)
        }
    }

    String renderSimpleParameters(List simpleParameters, Map transf = [:]) {
        def params = []
        def model = [:]
        try {
            simpleParameters.collect(params) { p ->
                String thisParam
                if (p.assign) {
                    thisParam = convertToMathML(p.symbId, p.assign, transf)
                } else {
                    StringBuilder sb = new StringBuilder("<math display='inline'><mstyle>")
                    sb.append(op(p.symbId)).append("</mstyle></math>")
                    thisParam = sb.toString()
                }
                return thisParam
            }
        } catch (Exception e) {
            model["error"] = "Cannot display simple parameters."
            log.error("Error encountered while rendering simple params ${simpleParameters.inspect()}: ${e.message}", e)
        } finally {
            model["simpleParameters"] = params
            return groovyPageRenderer.render(template: "/templates/simpleParameters",
                model: model)
        }
    }

    String convertVariableDefinitionToMathML(VariableDefinition vd) {
        if (vd.assign) {
            return convertToMathML(vd.symbId, vd.assign)
        } else {
            StringBuilder sb = new StringBuilder()
            sb.append("<math display='inline'><mstyle>")
            sb.append(op(vd.symbId))
            sb.append("</mstyle></math>")
            return sb.toString()
        }
    }

    String renderCommonVariables(List vars, String iv) {
        def model = [:]
        // manually set this because we need it for rendering initial conditions
        model["version"] = "0.6"
        def initialConditions = [:]
        def variableList = []
        try {
            vars.each { v ->
                switch (v.value) {
                    case DerivativeVariable:
                        if (v.value.initialCondition) {
                            initialConditions << [(v.value.symbId): v.value.initialCondition]
                        }
                        def dv = convertToMathML(v.value, iv)
                        variableList.add(dv)
                        break
                    case VariableDefinition:
                        String vd = convertVariableDefinitionToMathML(v.value)
                        variableList.add vd
                        break
                    case FunctionDefinition:
                        def fd = v.value
                        variableList.add(convertToMathML(fd.symbId,
                            fd.functionArgument, fd))
                        break
                    case FunctionParameter:
                        variableList.add(v.value.symbId)
                        break
                    default:
                        variableList.add(v.value.symbId)
                        break
                }
            }
            model["variableDefinitions"] = variableList
            model["initialConditions"] = initialConditions
        } catch (Exception e) {
            log.error("Error while displaying common variables - arguments ${vars.properties} ${iv}: ${e.message}", e)
            model["error"] = "Sorry, ran into issues while trying to display variable definitions."
        } finally {
            return groovyPageRenderer.render(template: "/templates/commonVariables",
                model: model)
        }
    }

    String renderInitialConditions(Map conditions) {
        def result = []
        conditions.keySet().each { c ->
            result.add convertToMathML(c, conditions[c].initialValue?.assign)
        }
        return groovyPageRenderer.render(template: "/templates/0.2/initialConditions",
            model: [conditions: result])
    }

    String displayProbability(String l, LogicBinOp logicBinOp) {
        StringBuilder result = oprand(l)
        result.append(op("("))
        result.append(logicBinOp.op)
        result.append(op(")"))

        return  result.toString()
    }

    String renderDiscreteCountData(CountData data) {
        StringBuilder result = new StringBuilder()
        def simpleParameters = data.getListOfCommonParameterElement().findAll {
            it instanceof SimpleParameter
        }
        simpleParameters = simpleParams(simpleParameters, [:], true)
        if (simpleParameters) {
            result.append("<p><span class=\"bold\">Parameters").append(simpleParameters).append("</span></p>")
        }

        String countVariables = data.countVariable.symbId
        if (countVariables) {
            result.append("<p><span class=\"bold\">Count Variables:&nbsp;</span>").append(countVariables).append("</p>")
        }
        String pmfs = convertPMFsToMathML(data.listOfPMF)
        if (pmfs) {
            result.append("<p class=\"bold\">PMF</p>").append(pmfs)
        }

        return result.toString()
    }

    String renderDiscreteCategoricalData(CategoricalData data) {
        StringBuilder result = new StringBuilder()

        String ordered = data.ordered
        if (ordered) {
            result.append("<p><span class=\"bold\">Ordered:&nbsp;</span>").append(ordered).append("</p>")
        }

        if (data.variable) {
            result.append("<p class=\"bold\">Variables</p>")
            data.variable.each { VariableDefinition vd ->
                result.append(convertVariableDefinitionToMathML(vd))
            }
        }

        if (data.listOfCategories) {
            def categories = data.listOfCategories.collect { it.symbId }
            String categoryString = "{${categories.join(',&nbsp;')}}"
            result.append("<p><span class=\"bold\">List of Categories:&nbsp;</span>")
            result.append(categoryString).append("</p>")
        }

        String categoryVariable = data.categoryVariable.symbId
        if (categoryVariable) {
            result.append("<p><span class=\"bold\">Category Variable:&nbsp;</span>").append(categoryVariable).append("</p>")
        }

        if (data.listOfPMF) {
            String pmfs = ""
            pmfs = convertPMFsToMathML(data.listOfPMF)
            pmfs = categoryVariable.concat(op(" ~ ")).concat(pmfs)
            result.append("<p class=\"bold\">PMF</p>").append(pmfs)
        }

        if (data.listOfProbabilityAssignment) {
            result.append("<p class=\"bold\">Probability Assignment</p>")
            data.listOfProbabilityAssignment.each {
                StringBuilder strBuider = new StringBuilder()
                if (it.listOfProbability[0] != null) {
                    if (it.listOfProbability[0].symbId) {
                        strBuider.append(oprand(it.listOfProbability[0].symbId))
                        strBuider.append(op(" := "))
                    }
                    strBuider.append(op("P"))
                    convertEquation(it.listOfProbability[0].logicBinop, strBuider)
                }
                result.append(strBuider)

                def temp = ""
                if (it.assign != null) {
                    temp = convertToMathML("", it.assign)
                    if (it.assign.equation) {
                        temp.concat(convertToMathML(it.assign.equation).toString())
                    } else if (it.assign.symbRef) {
                        temp = it.assign.symbRef.asString()
                    } else {
                        log.error("Cannot display this equation.")
                    }
                    result.append(temp)
                    result.append("<br/>")
                }
            }
        }
        return result.toString()
    }

    String renderDiscreteTimeToEventData(TimeToEventData data) {
        StringBuilder result = new StringBuilder()
        if (data.eventVariable) {
            result.append("<p><span class=\"bold\">Event Variable:&nbsp;</span>")
            result.append("${data.eventVariable.symbId}</p>")
        }

        if (data.listOfHazardFunction) {
            result.append("<p><span class=\"bold\">Hazard function</span><br/>")
            // Should check the existence of Independant Variable in the next improvement
            data.listOfHazardFunction.each {
                result.append(convertToMathML("h(t)", it.assign))
            }
        }

        if (data.listOfSurvivalFunction) {
            result.append("<p><span class=\"bold\">Survival function</span><br/>")
            // Should check the existence of Independant Variable in the next improvement
            data.listOfSurvivalFunction.each {
                result.append(convertToMathML("S(t)", it.assign))
            }
        }

        if (data.listOfCensoring) {
            result.append("<p><span class=\"bold\">Censoring</span><br/>")
            def censors = data.listOfCensoring

            if (censors.censoringType) {
                result.append("Type:&nbsp;${censors[0].censoringType.toString()}<br/>")
            }

            if (censors[0].listOfLeftCensoringTime) {
                String leftCensoringTime = censors[0].listOfLeftCensoringTime[0].assign.scalar.value.toString()
                result.append("Left Censoring Time:&nbsp;${leftCensoringTime}<br/>")
            }

            if (censors[0].listOfRightCensoringTime) {
                String rightCensoringTime = censors[0].listOfRightCensoringTime[0].assign.scalar.value.toString()
                result.append("Right Censoring Time:&nbsp;${rightCensoringTime}<br/>")
            }

            if (censors[0].listOfIntervalLength) {
                String intervalLength = censors[0].listOfIntervalLength[0].assign.scalar.value.toString()
                result.append("Interval Length:&nbsp;${intervalLength}<br/>")
            }
        }

        if (data.listOfMaximumNumberEvents) {
            def nbEvents = data.listOfMaximumNumberEvents[0]
            result.append("<p><span class=\"bold\">Maximum Number Event:&nbsp;</span>${nbEvents.assign.scalar.value}")
        }

        return result.toString()
    }

    String renderContinuousDataModel(ContinuousObservationModel data) {
        StringBuilder result = new StringBuilder()

        def simpleParameters = data.commonParameterElement.findAll {
            it instanceof SimpleParameter
        }
        def rv = data.commonParameterElement.findAll {
            it instanceof ParameterRandomVariable
        }
        def individualParameters = data.commonParameterElement.findAll {
            it instanceof IndividualParameter
        }
        result.append(simpleParams(simpleParameters))

        Map<String, List<String>> obsRandomVariableMap = [:]

        String randoms = randomVariables(rv, obsRandomVariableMap)
        if (randoms) {
            result.append(randoms)
        }
        StringBuilder individuals = individualParams(individualParameters, rv, covariates, [:])
        if (individuals) {
            result.append(individuals)
        }
        if (data.correlation) {
            def processor = new PharmMl0_3AwareCorrelationProcessor()
            List<CorrelationMatrix> matrices = processor.convertToStringMatrix(
                data.correlation, obsRandomVariableMap)
            if (matrices) {
                displayCorrelationMatrices(matrices, result)
            }
        }
        if (data.observationError) {
            if (data.observationError.symbol?.value) {
                result.append(data.observationError.symbol.value)
            }
            if (data.observationError instanceof GaussianObsError) {
                result.append(gaussianObsErr(data.observationError)).append(" ")
            } else { // can only be GeneralObsError
                result.append(generalObsErr(data.observationError)).append(" ")
            }
        }

        return result.toString()
    }
    /**
     * @param observationModels a list of
     * {@link eu.ddmore.libpharmml.dom.modeldefn.ObservationModel}s.
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderObservationModel")
    String renderObservationModel(List<ObservationModel> observations,
                                  List<CovariateModel> covariates) {
        StringBuilder result = new StringBuilder()
        result.append("<h3>Observation Model</h3>")

        try {
            observations.each { om ->
                result.append("<h4>")

                if (om.discrete) {
                    def discreteObs = om.discrete

                    if (discreteObs.countData) {
                        result.append("Observation ${discreteObs.countData.countVariable.symbId}<br/>")
                        result.append("<span class='italic'>Discrete / Count Data")
                        result.append("</span>")
                        result.append("</h4>")
                        def data = discreteObs.countData
                        result.append(renderDiscreteCountData(data))
                    } else if (discreteObs.categoricalData) {
                        result.append("Observation ${discreteObs.categoricalData.categoryVariable.symbId}<br/>")
                        result.append("<span class='italic'>Discrete / Categorical Data")
                        result.append("</span>")
                        result.append("</h4>")
                        def data = discreteObs.categoricalData
                        result.append(renderDiscreteCategoricalData(data))
                    } else if (discreteObs.timeToEventData) {
                        result.append("Observation ${discreteObs.timeToEventData.eventVariable.symbId}<br/>")
                        result.append("<span class='italic'>Discrete / Time-to-Event Data")
                        result.append("</span>")
                        result.append("</h4>")
                        def data = discreteObs.timeToEventData
                        result.append(renderDiscreteTimeToEventData(data))
                    }
                } else if (om.continuousData) {
                    def data = om.continuousData

                    def obsErr = data.observationError
                    if (obsErr) {
                        result.append("Observation ${obsErr.symbId}<br/>")
                        result.append("<span class='italic'>").append("Continuous / Residual Data</span>")
                    }
                    result.append("</h4>")
                    result.append("<span class=\"bold\">Parameters </span>")

                    def simpleParameters = data.commonParameterElement.findAll {
                        it instanceof SimpleParameter
                    }
                    def rv = data.commonParameterElement.findAll {
                        it instanceof ParameterRandomVariable
                    }
                    def individualParameters = data.commonParameterElement.findAll {
                        it instanceof IndividualParameter
                    }
                    result.append(simpleParams(simpleParameters))

                    Map<String, List<String>> obsRandomVariableMap = [:]

                    String randoms = randomVariables(rv, obsRandomVariableMap)
                    if (randoms) {
                        result.append(randoms)
                    }
                    StringBuilder individuals = individualParams(individualParameters, rv, covariates, [:])
                    if (individuals) {
                        result.append(individuals)
                    }
                    if (data.correlation) {
                        def processor = new PharmMl0_3AwareCorrelationProcessor()
                        List<CorrelationMatrix> matrices = processor.convertToStringMatrix(
                            data.correlation, obsRandomVariableMap)
                        if (matrices) {
                            displayCorrelationMatrices(matrices, result)
                        }
                    }
                    if (data.observationError) {
                        if (data.observationError.symbol?.value) {
                            result.append(data.observationError.symbol.value)
                        }
                        if (data.observationError instanceof GaussianObsError) {
                            result.append(gaussianObsErr(data.observationError)).append(" ")
                        } else { // can only be GeneralObsError
                            result.append(generalObsErr(data.observationError)).append(" ")
                        }
                    }

                    //result.append(renderContinuousDataModel(data))

                } else {
                    println "This model has not supported by the current version of PharmML."
                }
            }
        } catch (Exception e) {
            log.error("Error rendering the observations ${observations.inspect()}: ${e.message}", e)
            result.append("<br/>Sorry, something went wrong while rendering the observations.")
        } finally {
            return result.toString()
        }
    }

    /**
     * @param trialDesign an instance of {@link eu.ddmore.libpharmml.dom.trialdesign.TrialDesign}
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderTrialDesign")
    String renderTrialDesign(TrialDesign trialDesign) {}

    /**
     * @param structure - an instance of {@link eu.ddmore.libpharmml.dom.trialdesign.TrialStructure}
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderTrialDesignStructure")
    String renderTrialDesignStructure(TrialStructure structure) {
        def result = new StringBuilder()
        TrialDesignStructure tds
        def segmentActivitiesMap
        result.append("<h3>Structure overview</h3>\n")
        StringBuilder structureBuilder = new StringBuilder();
        try {
            tds = new TrialDesignStructure(structure.arm, structure.epoch,
                structure.cell, structure.segment)
            def armRefs = new ArrayList(tds.getArmRefs())
            def epochRefs = new ArrayList(tds.getEpochRefs())
            /* arm-epoch matrix*/
            structureBuilder.append("<table><thead><tr><th class='bold'>Arm/Epoch</th>")
            for (String e : epochRefs) {
                structureBuilder.append("<th class='bold'>").append(e).append("</th>")
            }
            structureBuilder.append("</tr></thead><tbody>\n")
            for (String a : armRefs) {
                structureBuilder.append("<tr><th class='bold'>").append(a).append("</th>")
                tds.findSegmentRefsByArm(a).each { s ->
                    structureBuilder.append("<td>").append(s).append("</td>")
                }
                structureBuilder.append("</tr>\n")
            }
            structureBuilder.append("</tbody></table>\n")
            result.append(structureBuilder.toString());
        } catch (Exception e) {
            result.append("Cannot display the arm-epoch matrix.")
            def errMsg = new StringBuilder("Error encountered while rendering the arm-epoch matrix of")
            errMsg.append("trial design structure ${structure.properties} ")
            errMsg.append("using helper ${tds.trialDesignStructure.inspect()}: ")
            log.error(errMsg, e)
        }
        StringBuilder segActBuilder = new StringBuilder()
        try {
            /* segments and activities */
            List activities = structure.activity
            // avoid the need to increase the size of the map, because re-hashing is expensive
            segmentActivitiesMap = new HashMap(activities.size() + 1, 1.0)
            structure.segment.each { s ->
                segmentActivitiesMap[s.oid] = s.activityRef.collect { a ->
                    structure.activity.find { a.oidRef.equals(it.oid) }
                }
            }
            boolean showDosingFootnote = false
            segActBuilder.append("<h4>Segment-Activity definition</h4>\n")
            segActBuilder.append("<table style='margin-bottom:0px;'><thead><tr><th class='bold'>Segment</th><th class='bold'>Activity</th>")
            segActBuilder.append("<th class='bold'>Treatment</th><th class='bold'>Dose time</th>")
            segActBuilder.append("<th class='bold'>Dose size</th><th class='bold'>Target variable</th></tr></thead><tbody>")
            if (segmentActivitiesMap) {
                segmentActivitiesMap.entrySet().each {
                    def activityList = it.value
                    if (!activityList) {
                        segActBuilder.append("<tr><td colspan='6'></td></tr>")
                    } else {
                        final int ACTIVITY_COUNT = activityList.size()
                        segActBuilder.append("<tr><td")
                        if (ACTIVITY_COUNT > 1) {
                            segActBuilder.append(" rowspan='").append(ACTIVITY_COUNT).append("'")
                        }
                        segActBuilder.append(">").append(it.key).append("</td>")
                        boolean toShowFootnote = activity(activityList[0], true, segActBuilder)
                        if ((!showDosingFootnote) && toShowFootnote) {
                            showDosingFootnote = toShowFootnote
                        }
                        if (ACTIVITY_COUNT > 1) {
                            for (int i = 1; i < ACTIVITY_COUNT; i++) {
                                toShowFootnote = activity(activityList[i], false, segActBuilder)
                                if (!showDosingFootnote && toShowFootnote) {
                                    showDosingFootnote = toShowFootnote
                                }
                            }
                        }
                    }
                }
                segActBuilder.append("</tbody></table>\n")
            }
            if (showDosingFootnote) {
                segActBuilder.append("<span>* &ndash; Element defined in the Individual dosing section.</span>")
            }
            result.append(segActBuilder.toString());
        } catch (Exception e) {
            result.append("Cannot display the segment-activity overview.")
            def errMsg = new StringBuilder("Cannot display the segment-activity overview for structure ")
            errMsg.append(structure.properties).append(" using helper map ")
            errMsg.append(segmentActivitiesMap.inspect())
            log.error(errMsg, e)
        }
        ObservationEventsMap oem
        StringBuilder epOccBuilder = new StringBuilder();
        /* epochs and occasions */
        try {
            if (structure.studyEvent) {
                oem = new ObservationEventsMap(structure.studyEvent)
                def arms = oem.getArms()
                def epochs = oem.getEpochs()
                epOccBuilder.append("\n<h4>Epoch-Occasion definition</h4>\n")
                epOccBuilder.append("<table><thead><tr><th class='bold'>Arm/Epoch</th>")
                for (String e : epochs) {
                    epOccBuilder.append("<th class='bold'>").append(e).append("</th>")
                }
                epOccBuilder.append("</tr></thead><tbody>\n")
                arms.each { a ->
                    epOccBuilder.append("<tr><th class='bold'>").append(a).append("</th>")
                    def occ = oem.findOccasionsByArm(a)
                    occ.each {
                        //TODO OCCASIONS CAN HAVE MANY ENTRIES
                        def o = it.firstEntry()
                        epOccBuilder.append("<td>")
                        epOccBuilder.append("<div>").append(o.key).append("</div><div><span class='bold'>")
                        epOccBuilder.append(o.value).append("</span> variability</div></td>")
                    }
                    epOccBuilder.append("</tr>")
                }
                epOccBuilder.append("</tbody></table>\n")
            }
            result.append(epOccBuilder.toString());
        } catch (Exception e) {
            result.append("<p>Cannot display the epoch-occasion overview.</p>")
            def errMsg = new StringBuilder("Cannot display the epoch-occasion overview for structure ")
            errMsg.append(structure.properties).append(" using helper map ")
            errMsg.append(oem.inspect())
            log.error(errMsg, e)
        } finally {
            return result.toString()
        }
    }

    /**
     * @param dosing - a list of {@link eu.ddmore.libpharmml.dom.trialdesign.IndividualDosing}
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderIndividualDosing")
    String renderIndividualDosing(List<IndividualDosing> dosing, RevisionTransportCommand rev, String downloadLink) {
        def result = new StringBuilder()
        try {
            result.append("<h4>Individual dosing</h4>\n")
            dosing.each { d ->
                if (d.dataSet) {
                    dataSet(d.dataSet, null, result, rev, downloadLink)
                }
            }
        } catch (Exception e) {
            result.append("Cannot display the trial dosing.")
            def errMsg = new StringBuilder("Cannot display the trial dosing ")
            errMsg.append(d.properties)
            log.error(errMsg, e)
        } finally {
            return result.toString()
        }
    }

    /**
     * @param population an instance of {@link eu.ddmore.libpharmml.dom.trialdesign.Population}
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderPopulation")
    String renderPopulation(Population pop, RevisionTransportCommand rev, String downloadLink) {
        def result = new StringBuilder("<h4>Population</h4>\n")
        if (pop.variabilityReference) {
            result.append("<span><strong>Variability level: </strong>")
            result.append(pop.variabilityReference.symbRef.symbIdRef).append("</span>")
        }
        try {
            if (pop.dataSet) {
                dataSet(pop.dataSet, null, result, rev, downloadLink)
            }
        } catch (Exception e) {
            result.append("Cannot display population data set.")
            def errMsg = new StringBuilder()
            errMsg.append("Cannot display population data set ")
            errMsg.append(pop.dataSet.properties).append("for population ")
            errMsg.append(pop.properties)
            log.error(errMsg, e)
        } finally {
            return result.toString()
        }
    }

    /**
     * @param steps an instance of {@link eu.ddmore.libpharmml.dom.modellingsteps.ModellingSteps}
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderModellingSteps")
    String renderModellingSteps(ModellingSteps steps) {}

    /**
     * @param steps a list of {@link eu.ddmore.libpharmml.dom.modellingsteps.CommonModellingStep}s.
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderCommonModellingSteps")
    String renderCommonModellingSteps(List<CommonModellingStep> steps) {}

    /**
     * @param steps a list of {@link eu.ddmore.libpharmml.dom.modellingsteps.Simulation}s.
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderSimulationSteps")
    String renderSimulationSteps(List<Simulation> steps, String iv) {
        def result = new StringBuilder("<h3>Simulation Steps</h3>\n")
        steps.each { s ->
            result.append("<h4>Simulation step <span class='italic'>${s.oid}</span></h4>")
            if (s.variableAssignment) {
                result.append(variableAssignments(s.variableAssignment, "<h5>Variable assignments</h5>\n"))
            }
            if (s.observations) {
                s.observations.each { o ->
                    result.append("\n<h5>Observation</h5>\n")
                    if (o.continuous) {
                        result.append("<p><span class=\"bold\">Type:</span>Continuous</p>\n")
                        if (o.continuous.symbRef) {
                            result.append("<p><span class=\"bold\">Variables:</span>")
                            //check for symbIdRef clashes and display the blkIdRef accordingly
                            List vars = o.continuous.symbRef
                            List allVars = []
                            List uniqueVars = []
                            vars.each {
                                uniqueVars << it.symbIdRef
                                allVars << [it.blkIdRef, it.symbIdRef].join('.')
                            }
                            boolean needBlk = allVars.size() != uniqueVars.unique().size()
                            vars.inject(result) { r, v ->
                                r.append("${needBlk ? v.blkIdRef + '.' : ''}${v.symbIdRef} ")
                            }
                            result.append("</p>\n")
                        }
                    }
                    if (o.timepoints) {
                        result.append("<p><span class=\"bold\">Independent variable")
                        if (iv) {
                            result.append("(").append(iv).append(")")
                        }
                        result.append(":</span>\n")
                        // put all timepoints here, output them separated by commas
                        List<String> observationTimepoints = []
                        o.timepoints.arrays.each { a ->
                            //JAXBElement parameterised with Sequence or CTVector
                            if (a.value instanceof CTVector) {
                                observationTimepoints << jaxbVector(a.value).toString()
                            } else if (a.value instanceof Sequence) {
                                observationTimepoints << sequence(a.value).toString()
                            }
                        }
                        result.append(
                            observationTimepoints.size() == 1 ? observationTimepoints[0] :
                                "[${observationTimepoints.join(', ')}]")
                        result.append("</p>\n")
                    }
                }
            }
            if (s.operation) {
                result.append formatOperations(s.operation, false)
            }
        }
        return result
    }

    @Override
    protected StringBuilder jaxbVector(CTVector vector) {
        if (!vector) {
            return new StringBuilder()
        }
        def result = new StringBuilder("")
        def values = []
        vector.getVectorElements().elements.inject(result) { r, e ->
            switch (e) {
                case Sequence:
                    values << sequence(e)
                    break
                case IntValue:
                case RealValue:
                case StringValue:
                case IdValue:
                case BooleanValue:
                    values << scalar(e)
                    break
                default:
                    log.error("Funny element ${e.dump()} in ${vector.vectorElements.elements}")
                    values << scalar(e)
            }
        }
        if (values) {
            result.append(values.size() > 1 ? "[${values.join(', ')}]" : values.first())
        }
        return result
    }


    @Override
    protected StringBuilder dataSet(DataSet dataSet, Map variableMap,
                                    StringBuilder sb, RevisionTransportCommand rev, String downloadLink) {
        List tables = dataSet.getListOfColumnDefinition()
        if (tables) {
            def columnOrder = tables.inject([:]) { order, colDef ->
                order << [(colDef.columnNum): (colDef.columnId)]
            }
            sb.append("\n<table><thead><tr>")

            tables.inject(sb) { txt, d ->
                def key = columnOrder[d.columnNum]
                if (key && variableMap && variableMap[key]) {
                    txt.append(["<th>", "</th>"].join(variableMap[key]))
                } else {
                    txt.append(["<th>", "</th>"].join(d.columnId))
                }
            }
            sb.append("</tr></thead><tbody>")
            dataSet.getListOfRow().each { i ->
                sb.append("\n<tr>")
                i.getListOfValue().each { td ->
                    sb.append(["<td class='default'>", "</td>"].join(scalar(td)))
                }
                sb.append("</tr>")
            }
            sb.append("</tbody></table>\n")
        }
        if (dataSet.externalFile) {
            sb.append("<div class='spaced-top-bottom'>")
            def rftc = rev.files.find {
                File file = new File(it.path)
                return file.getName() == dataSet.externalFile.path
            }
            if (rftc) {
                sb.append("This model refers to an external data file: <a href='");
                sb.append(downloadLink)
                sb.append("?filename=")
                sb.append(new File(rftc.path).getName())
                sb.append("' title='Download ")
                sb.append(rftc.mimeType)
                sb.append(" file'>Download</a>")
            } else {
                sb.append("This model refers to an external data file named '")
                sb.append(dataSet.externalFile.name)
                sb.append("', but the file is not available in the repository. ")
            }
            sb.append("</div>")
        }
        return sb
    }

    /**
     * @param steps a list of {@link eu.ddmore.libpharmml.dom.modellingsteps.Estimation}s.
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderEstimationSteps")
    String renderEstimationSteps(List<Estimation> steps, RevisionTransportCommand rev, String downloadLink) {
        def result = new StringBuilder("<h3>Estimation Steps</h3>\n")
        steps.each { s ->
            result.append("<h4>Estimation Step ${s.oid}</h4>\n")
            if (s.variableAssignment) {
                result.append(variableAssignments(s.variableAssignment, "<h5>Variable assignments</h5>"))
            }
            if (s.parametersToEstimate) {
                result.append(paramsToEstimate(s.parametersToEstimate))
            }
            if (s.operation) {
                result.append(formatOperations(s.operation))
            }
            if (s.objectiveDataSet) {
                result.append(objectiveDataSetMapping(s.objectiveDataSet, rev, downloadLink))
            }
        }
        return result.toString()
    }

    /**
     * @param dependencies a list of {@link eu.ddmore.libpharmml.dom.modellingsteps.StepDependency}s.
     */
    @Profiled(tag = "pharmMl0_6AwareRenderer.renderStepDependencies")
    String renderStepDependencies(StepDependency dependencies) {
        StringBuilder result = new StringBuilder()
        if (!dependencies || !dependencies.step) {
            return result
        }
        result.append("<h3>Step Dependencies</h3>")
        result.append("\n<ul>")
        dependencies.step.inject(result) { r, s ->
            StringBuilder dep = new StringBuilder(s.oidRef.oidRef)
            if (s.dependents) {
                dep.append(": ").append(transitiveStepDeps(s.dependents))
            }
            r.append(["<li>", "</li>\n"].join(dep.toString()))
        }
        return result.append("</ul>").toString()
    }

    /*
     * Override the default implementation because Correlation contains a pairwise element
     * where randomVariable1 and randomVariable2 are defined.
     */

    @Override
    protected void buildCorrelationMap(Correlation c, Map correlationsMap) {
        try {
            Pairwise p = c.pairwise
            final ScalarRhs VALUE = p.covariance ?: p.correlationCoefficient

            String var = c.variabilityReference.symbRef?.symbIdRef ?:
                c.variabilityReference.symbRef?.blkIdRef ?: "undefined"
            String r1 = p.randomVariable1.symbRef?.symbIdRef
            String r2 = p.randomVariable2.symbRef?.symbIdRef
            final String KEY = "$var|$r1|$r2"
            correlationsMap[KEY] = VALUE.symbRef.symbIdRef
            final String KEY_REV = "$var|$r2|$r1"
            correlationsMap[KEY_REV] = VALUE.symbRef.symbIdRef
        } catch (Exception e) {
            log.error(e.message, e)
        }
    }

    /* EquationType no longer has scalarOrSymbRefOrBinop list, just one element*/

    @Override
    protected StringBuilder gaussianObsErr(GaussianObsError e) {
        def result = new StringBuilder("<div class='spaced-top-bottom'>")

        // could be an Equation or just a String
        def lhs
        def lhsSymb = new SymbolRef()
        lhsSymb.symbIdRef = e.symbId
        def prediction
        def predictionSymb = e.output.symbRef
        def residualErrorSymb = e.residualError.symbRef

        if (e.transformation) {
            final String tr = e.transformation.value()
            def lhsUniop = new Uniop()
            lhsUniop.operator = Unioperator.fromString(tr)
            lhsUniop.symbRef = lhsSymb
            lhs = new Equation()
            lhs.uniop = lhsUniop
            def predUniop = new Uniop()
            predUniop.operator = Unioperator.fromString(tr)
            predUniop.symbRef = predictionSymb
            prediction = wrapJaxb(predUniop)
        } else {
            lhs = lhsSymb.symbIdRef
            prediction = wrapJaxb(predictionSymb)
        }

        def errModelAssign = e.errorModel.assign
        assert errModelAssign != null
        def errModel
        def errModelTimesResidualErr
        def rhsEquation
        if (errModelAssign.equation) {
            def errModelAssignEq = errModelAssign.equation
            if (errModelAssignEq.uniop) {
                errModel = wrapJaxb(errModelAssignEq.uniop)
            } else if (errModelAssignEq.binop) {
                errModel = wrapJaxb(errModelAssignEq.binop)
            } else if (errModelAssignEq.symbRef) {
                errModel = wrapJaxb(errModelAssignEq.symbRef)
            } else if (errModelAssignEq.scalar) {
                errModel = errModelAssignEq.scalar
            } else if (errModelAssignEq.functionCall) {
                errModel = wrapJaxb(errModelAssignEq.functionCall)
            } else if (errModelAssignEq.piecewise) {
                errModel = wrapJaxb(errModelAssignEq.piecewise)
            }
            assert errModel != null
        } else if (errModelAssign.scalar) {
            errModel = errModelAssign.scalar
        } else if (errModelAssign.symbRef) {
            errModel = wrapJaxb(errModelAssign.symbRef)
        }
        def em_re = new Binop()
        em_re.operator = Binoperator.TIMES
        em_re.operand1 = errModel.value
        em_re.operand2 = residualErrorSymb
        errModelTimesResidualErr = wrapJaxb(em_re)
        def sum = new Binop()
        sum.operator = Binoperator.PLUS
        sum.operand1 = prediction.value
        sum.operand2 = errModelTimesResidualErr.value
        rhsEquation = new Equation()
        rhsEquation.binop = sum
        return result.append(convertToMathML(lhs, rhsEquation)).append("</div>")
    }

    @Override
    protected StringBuilder individualParams(List<IndividualParameter> parameters,
                                             List<ParameterRandomVariable> rv, List<CovariateDefinition> covariates,
                                             Map<String, Equation> transfMap) {
        def output = new StringBuilder("<div class='spaced-top-bottom'>")
        try {
            parameters.each { p ->
                if (p.assign) {
                    String converted = convertToMathML(p.symbId, p.assign)
                    output.append("<div>")
                    output.append(converted)
                    output.append("</div>")
                }
                if (p.gaussianModel) {
                    output.append("\n")
                    def gaussianModel = p.gaussianModel
                    //RANDOM EFFECTS
                    def randomEffects = []
                    if (gaussianModel.randomEffects) {
                        gaussianModel.randomEffects.each { gmre ->
                            def randomEffectSymbol = new SymbolRef()
                            //ASSUME THERE IS ONLY ONE SYMBREF HERE
                            randomEffectSymbol.symbIdRef = gmre.symbRef[0].symbIdRef
                            randomEffects << wrapJaxb(randomEffectSymbol)
                        }
                    }
                    if (gaussianModel.linearCovariate) {
                        def linearCovariate = gaussianModel.linearCovariate
                        final String TRANSFORMATION = gaussianModel.transformation?.value()
                        final boolean APPLY_TRANSFORMATION = null != TRANSFORMATION
                        // left hand side can be equation or string
                        def lhsEquation
                        //LHS
                        if (!APPLY_TRANSFORMATION) {
                            lhsEquation = p.symbId
                        } else {
                            Uniop indivParam = new Uniop()
                            indivParam.operator = Unioperator.fromString(TRANSFORMATION)
                            def paramSymbRef = new SymbolRef()
                            paramSymbRef.symbIdRef = p.symbId
                            indivParam.symbRef = paramSymbRef
                            lhsEquation = new Equation()
                            lhsEquation.uniop = indivParam
                        }
                        //POPULATION
                        def popParam
                        Rhs popParamAssign = linearCovariate.populationParameter.assign
                        if (popParamAssign) {
                            SymbolRef popSymb = popParamAssign.symbRef ?:
                                popParamAssign.equation.symbRef
                            if (!popSymb) {
                                log.warn "\
Could not extract the population parameter of individual parameter ${p.symbId}."
                            }
                            if (APPLY_TRANSFORMATION) {
                                popParam = new Uniop()
                                popParam.operator = Unioperator.fromString(TRANSFORMATION)
                                popParam.symbRef = popSymb
                            } else {
                                popParam = popSymb
                            }
                        }
                        def fixedEffectsCovMap = [:]
                        linearCovariate.covariate.each { c ->
                            //fixed effects
                            if (!c.fixedEffect) {
                                return
                            }
                            def fixedEffects = []
                            def covEffectKey
                            def fe = c.fixedEffect
                            if (fe) {
                                if (fe.category) {
                                    def catIdSymbRef = new SymbolRef()
                                    def trickReference = new StringBuilder("<msub><mi>")
                                    trickReference.append(c.symbRef.symbIdRef).append("</mi><mi>")
                                    trickReference.append(fe.category.catId).append("</mi></msub>")
                                    catIdSymbRef.symbIdRef = trickReference.toString()
                                    covEffectKey = catIdSymbRef
                                } else {
                                    //RESOLVE REFERENCE TO CONT COV TRANSF
                                    final EquationType transfEq = resolveSymbolReference(c.symbRef, transfMap)
                                    if (transfEq) {
                                        covEffectKey = transfEq
                                    } else {
                                        covEffectKey = c.symbRef
                                    }
                                }
                                fixedEffects << fe.symbRef
                            }
                            fixedEffectsCovMap[covEffectKey] = fixedEffects
                        }
                        def fixedEffectsTimesCovariateList = []
                        if (fixedEffectsCovMap) {
                            fixedEffectsCovMap.each {
                                def thisCov = []
                                def key = it.key
                                if (key instanceof Equation) {
                                    def thisKey = key.scalar ?: wrapJaxb(key.uniop ?:
                                        key.binop ?: key.symbRef ?: key.functionCall ?:
                                            key.piecewise)
                                    thisCov.add thisKey
                                } else if (key instanceof JAXBElement) {
                                    thisCov.add key
                                } else {
                                    thisCov.add(wrapJaxb(key))
                                }
                                it.value.collect { v -> thisCov.add(wrapJaxb(v)) }
                                fixedEffectsTimesCovariateList.add(
                                    applyBinopToList(thisCov, Binoperator.TIMES))
                            }
                        }
                        def sumElements = []
                        sumElements.add(wrapJaxb(popParam))
                        if (fixedEffectsTimesCovariateList) {
                            sumElements.addAll(fixedEffectsTimesCovariateList)
                        }
                        sumElements.addAll(randomEffects)

                        Equation rhsEquation = new Equation()
                        rhsEquation.binop = applyBinopToList(sumElements, Binoperator.PLUS).value
                        output.append(convertToMathML(lhsEquation, rhsEquation))
                        output.append("\n")
                    } else if (gaussianModel.generalCovariate) {
                        def rhsEquation
                        def covModel
                        def covariateModelAssign = gaussianModel.generalCovariate.assign
                        assert covariateModelAssign != null
                        if (!randomEffects) {
                            rhsEquation = covariateModelAssign
                        } else {
                            if (covariateModelAssign.equation) {
                                Equation eq = covariateModelAssign.equation
                                covModel = eq.scalar ?: wrapJaxb(eq.uniop ?: eq.binop ?:
                                    eq.symbRef ?: eq.functionCall ?: eq.piecewise)
                            } else if (covariateModelAssign.scalar) {
                                covModel = wrapJaxb(covariateModelAssign.scalar)
                            } else if (covariateModelAssign.symbRef) {
                                covModel = wrapJaxb(covariateModelAssign.symbRef)
                            }
                        }
                        def cm_re = []
                        cm_re.add(covModel)
                        cm_re.addAll(randomEffects)
                        rhsEquation = new Equation()
                        rhsEquation.binop = applyBinopToList(cm_re, Binoperator.PLUS).value
                        String converted = convertToMathML(p.symbId, rhsEquation)
                        output.append("<div>")
                        output.append(converted)
                        output.append("</div>")
                    }
                }
            }
        } catch (Exception e) {
            output = new StringBuilder("<div class='spaced-top-bottom'>")
            output.append("Cannot display individual parameters.")
            log.error("Error encountered while rendering individual parameters ${parameters.inspect()} using random variables ${rv.inspect()} and covariates ${covariates.inspect()}: ${e.message}", e)
        }
        return output.append("</div>")
    }

    @Override
    protected JAXBElement expandNestedSymbRefs(JAXBElement<SymbolRef> symbRef,
                                               Map<String, Equation> transformations) {
        final EquationType TRANSF_EQ = resolveSymbolReference(symbRef.value, transformations)
        if (TRANSF_EQ) {
            final def ELEM = wrapJaxb(extractAttributeFromEquation(TRANSF_EQ))
            final Class ELEM_CLASS = ELEM.value.getClass()
            switch (ELEM_CLASS) {
                case Binop:
                    break
                case Uniop:
                    break
                case SymbolRef:
                    break
                case Constant:
                    break
                case FunctionCallType:
                    break
                case IdValue:
                    break
                case StringValue:
                    break
                case IntValue:
                    break
                case RealValue:
                    break
                default:
                    assert false, "Cannot have ${ELEM_CLASS.name} inside a transformation."
                    break
            }
            return ELEM
        } else {
            return symbRef
        }
    }

    @Override
    protected JAXBElement expandNestedUniop(JAXBElement<Uniop> jaxbUniop,
                                            Map<String, Equation> transfMap) {
        Uniop uniop = jaxbUniop.value
        Uniop replacement
        if (uniop.symbRef) {
            final EquationType TRANSF_EQ = resolveSymbolReference(uniop.symbRef, transfMap)
            if (TRANSF_EQ) {
                final def ELEM = extractAttributeFromEquation(TRANSF_EQ)
                final Class ELEM_CLASS = ELEM.getClass()
                replacement = new Uniop()
                replacement.operator = uniop.operator
                switch (ELEM_CLASS) {
                    case Binop:
                        replacement.binop = ELEM
                        break
                    case Uniop:
                        replacement.uniop = ELEM
                        break
                    case SymbolRef:
                        replacement.symbRef = ELEM
                        break
                    case Constant:
                        replacement.constant = ELEM
                        break
                    case FunctionCallType:
                        replacement.functionCall = ELEM
                        break
                    case IdValue:
                        replacement.scalar = ELEM
                        break
                    case StringValue:
                        break
                    case IntValue:
                        replacement.scalar = ELEM
                        break
                    case RealValue:
                        replacement.scalar = ELEM
                        break
                    default:
                        assert false, "Cannot have ${ELEM_CLASS.name} inside a unary operator."
                        replacement = null
                        break
                }
            }
        } else if (uniop.uniop) {
            def expanded = expandNestedUniop(wrapJaxb(uniop.uniop), transfMap)?.value
            if (expanded && !(expanded.equals(uniop.uniop))) {
                uniop.uniop = expanded
            }
        } else if (uniop.binop) {
            def expanded = expandNestedBinop(wrapJaxb(uniop.binop), transfMap)?.value
            if (expanded && !(expanded.equals(uniop.binop))) {
                uniop.binop = expanded
            }
        }
        if (replacement) {
            return wrapJaxb(replacement)
        }
        return jaxbUniop
    }

    @Override
    protected EquationType expandEquation(EquationType equation, Map<String, Equation> transfMap) {
        def eqTerms = extractAttributeFromEquation(equation)
        JAXBElement expandedTerms
        switch (eqTerms) {
            case Binop:
                expandedTerms = expandNestedBinop(wrapJaxb(eqTerms), transfMap)
                break
            case Uniop:
                expandedTerms = expandNestedUniop(wrapJaxb(eqTerms), transfMap)
                break
            case SymbolRef:
                expandedTerms = expandNestedSymbRefs(wrapJaxb(eqTerms), transfMap)
                break
            default:
                expandedTerms = wrapJaxb(eqTerms)
                break
        }
        def unwrappedExpandedTerms = expandedTerms.value

        if (!eqTerms.equals(unwrappedExpandedTerms)) {
            def newEquation = new EquationType()
            switch (unwrappedExpandedTerms) {
                case Uniop:
                    newEquation.uniop = unwrappedExpandedTerms
                    break
                case Binop:
                    newEquation.binop = unwrappedExpandedTerms
                    break
                case FunctionCallType:
                    newEquation.functionCall = unwrappedExpandedTerms
                    break
                case Piecewise:
                    newEquation.piecewise = unwrappedExpandedTerms
                    break
                case SymbolRef:
                    newEquation.symbRef = unwrappedExpandedTerms
                    break
                default: // scalar
                    newEquation.scalar = unwrappedExpandedTerms
                    break
            }
            return newEquation
        }
        return equation
    }

    protected def extractAttributeFromEquation(EquationType eq) {
        if (!eq) {
            return
        }
        return eq.uniop ?: eq.binop ?: eq.symbRef ?: eq.scalar ?: eq.functionCall ?: eq.piecewise
    }

    protected StringBuilder convertPMFsToMathML(List<CountPMF> pmfList) {
        def result = new StringBuilder()

        pmfList.each { pmf ->
            if (pmf.distribution instanceof PoissonDistributionType){
                PoissonDistributionType psd = pmf.distribution
                result.append("<math display='inline'><mstyle><mtext>")
                result.append("Poisson(rate = ${psd.rate.var.varId})")
                result.append("</mtext></mstyle></math>")
                result.append("<br/>")
            } else if (pmf.distribution instanceof  BinomialDistributionType) {
                BinomialDistribution bmd = pmf.distribution
                result.append("<math display='inline'><mstyle><mtext>")
                //StringBuilder binomialDistribution = renderBinomialDistribution()
                result.append("Binomial(numberOfTrials=${bmd.numberOfTrials.NVal}, probabilityOfSuccess=${bmd.probabilityOfSuccess.var.varId})")
                result.append("</mtext></mstyle></math>")
                result.append("<br/>")
            } else if (pmf.assign) {

                //result.append("<math display='inline'><mstyle><mtext>")
                result.append(oprand("P"))
                //convertEquation(pmf.logicBinop, result)
                result.append(convertToMathML(pmf.logicBinop)).append(op(" = ")).append(convertToMathML(pmf.assign.equation))
                //result.append("</mtext></mstyle></math>")
                result.append("<br/>")
            }else {
                log.error ("The function has not supported in the current version of PharmML.")
            }
        }

        return result
    }

    @Override
    protected String convertToMathML(String lhs, Rhs rhs, Map<String, Equation> transfMap = [:]) {
        if (rhs.equation) {
            return convertToMathML(lhs, rhs.equation, transfMap)
        }
        if (rhs.symbRef) {
            return convertToMathML(lhs, rhs.symbRef, transfMap)
        }
        StringBuilder builder = new StringBuilder("<math display='inline'><mstyle>")
        builder.append(oprand(lhs))
        builder.append(op("="))
        if (rhs.getScalar()) {
            builder.append(oprand(scalar(rhs.scalar.value)))
        } else if (rhs.getSequence()) {
            builder.append(sequenceAsMathML(rhs.sequence))
        } else if (rhs.getVector()) {
            builder.append(vectorAsMathML(rhs))
        } else if (rhs.interpolation) {
            builder.append(oprand(renderInterpolation(rhs.interpolation)))
        }
        builder.append("</mstyle></math>")
        return builder.toString()
    }

    String renderPKMacros(PKMacroList pkMacroList){
        StringBuilder builder=new StringBuilder()

        for(PKMacro pkMacro : pkMacroList.getListOfMacro()){
            builder.append("<div>")
            builder.append("<math display='inline'><mstyle>")
            builder.append(oprand(pkMacro.name))
            builder.append(op("("))
            String prefix = ""
            for (MacroValue macroValue: pkMacro.getListOfValue()){
                builder.append(oprand(prefix))
                prefix = ","

                if(macroValue.argument) {
                    builder.append(oprand(macroValue.argument))
                    builder.append(op("="))
                    Rhs rhs = macroValue.assign
                    if (rhs.symbRef)
                        builder.append(oprand(rhs.symbRef.asString()))
                    if (rhs.scalar)
                        builder.append(oprand(scalar(rhs.scalar.value)))
                    if (rhs.equation) {
                        Equation equation = rhs.equation
                        if (equation.symbRef)
                            builder.append(oprand(rhs.equation.symbRef.asString()))
                        else
                            builder.append(convertToMathML(equation))
                    }
                }
                else{
                    if (macroValue.symbRef)
                        builder.append(oprand(macroValue.symbRef.asString()))
                }

            }
            builder.append(op(")"))
            builder.append("</mstyle></math>")
            builder.append("</div>")
        }

        return builder.toString()
    }

}
