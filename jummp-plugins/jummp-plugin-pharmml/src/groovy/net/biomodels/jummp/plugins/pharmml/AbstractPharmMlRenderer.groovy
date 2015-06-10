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
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Tika, Apache Commons,
* LibPharmml, Perf4j used as well as that of the covered work.}
**/

package net.biomodels.jummp.plugins.pharmml

import eu.ddmore.libpharmml.dom.maths.Binoperator
import eu.ddmore.libpharmml.dom.maths.Unioperator
import net.biomodels.jummp.core.model.RevisionTransportCommand
import eu.ddmore.libpharmml.dom.commontypes.DerivativeVariable
import eu.ddmore.libpharmml.dom.commontypes.FalseBoolean
import eu.ddmore.libpharmml.dom.commontypes.IdValue
import eu.ddmore.libpharmml.dom.commontypes.IntValue
import eu.ddmore.libpharmml.dom.commontypes.RealValue
import eu.ddmore.libpharmml.dom.commontypes.Rhs
import eu.ddmore.libpharmml.dom.commontypes.ScalarRhs
import eu.ddmore.libpharmml.dom.commontypes.Sequence
import eu.ddmore.libpharmml.dom.commontypes.StringValue
import eu.ddmore.libpharmml.dom.commontypes.SymbolRef
import eu.ddmore.libpharmml.dom.commontypes.TrueBoolean
import eu.ddmore.libpharmml.dom.commontypes.VariableAssignment
import eu.ddmore.libpharmml.dom.commontypes.Vector as CTVector
import eu.ddmore.libpharmml.dom.dataset.ColumnDefinition
import eu.ddmore.libpharmml.dom.dataset.DataSetTableDefnType
import eu.ddmore.libpharmml.dom.dataset.DataSetTableType
import eu.ddmore.libpharmml.dom.dataset.DataSet
import eu.ddmore.libpharmml.dom.maths.Binop
import eu.ddmore.libpharmml.dom.maths.Constant
import eu.ddmore.libpharmml.dom.maths.Equation
import eu.ddmore.libpharmml.dom.maths.EquationType
import eu.ddmore.libpharmml.dom.maths.FunctionCallType
import eu.ddmore.libpharmml.dom.maths.Uniop
import eu.ddmore.libpharmml.dom.modeldefn.CovariateDefinition
import eu.ddmore.libpharmml.dom.modeldefn.GaussianObsError
import eu.ddmore.libpharmml.dom.modeldefn.GeneralObsError
import eu.ddmore.libpharmml.dom.modeldefn.IndividualParameter
import eu.ddmore.libpharmml.dom.modeldefn.ParameterRandomVariable
import eu.ddmore.libpharmml.dom.modeldefn.SimpleParameter
import eu.ddmore.libpharmml.dom.modellingsteps.DatasetMapping
import eu.ddmore.libpharmml.dom.modellingsteps.OperationProperty
import eu.ddmore.libpharmml.dom.modellingsteps.ParameterEstimate
import eu.ddmore.libpharmml.dom.modellingsteps.ToEstimate
import eu.ddmore.libpharmml.dom.modellingsteps.VariableMapping
import eu.ddmore.libpharmml.dom.trialdesign.Activity
import eu.ddmore.libpharmml.dom.trialdesign.Bolus
import eu.ddmore.libpharmml.dom.trialdesign.Infusion
import eu.ddmore.libpharmml.dom.uncertml.NormalDistribution
import javax.xml.bind.JAXBElement
import javax.xml.namespace.QName
import net.biomodels.jummp.core.IPharmMlRenderer
import net.biomodels.jummp.plugins.pharmml.maths.FunctionSymbol
import net.biomodels.jummp.plugins.pharmml.maths.MathsSymbol
import net.biomodels.jummp.plugins.pharmml.maths.MathsUtil
import net.biomodels.jummp.plugins.pharmml.maths.OperatorSymbol
import net.biomodels.jummp.plugins.pharmml.maths.PieceSymbol
import net.biomodels.jummp.plugins.pharmml.maths.PiecewiseSymbol
import net.biomodels.jummp.plugins.pharmml.util.correlation.CorrelationMatrix
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

abstract class AbstractPharmMlRenderer implements IPharmMlRenderer {
    /* the class logger */
    private static final Log log = LogFactory.getLog(this)
    private static final String IS_DEBUG_ENABLED = log.isDebugEnabled()
    private static final String IS_INFO_ENABLED = log.isInfoEnabled()

    /*
     * Parses an activity and writes it to a StringBuilder.
     * Returns whether to display a dosing footnote or not.
     */
    protected boolean activity(Activity activity, boolean isFirst, StringBuilder result) {
        boolean showDosingFootnote = false
        if (!isFirst) {
            result.append("<tr>")
        }
        result.append("<td>")
        result.append(activity.oid).append("</td>")
        if (activity.washout) {
            result.append("<td>washout</td><td>&mdash;</td><td>&mdash;</td><td>&mdash;</td>")
        } else {
            /* dosingRegimen is a JAXBElement */
            def regimen = activity.dosingRegimen.value
            switch(regimen) {
                case Bolus:
                    result.append("<td>bolus</td>")
                    //fall through
                case Infusion:
                    if (regimen instanceof Infusion) {
                        result.append("<td>infusion</td>")
                    }
                    if (regimen.dosingTimes) {
                        rhs(regimen.dosingTimes.assign, result.append("<td>"))
                        result.append("</td>")
                    } else if (regimen.steadyState) {
                        result.append("<td>")
                        result.append(steadyState(regimen.steadyState))
                        result.append("</td>")
                    } else {
                        result.append("<td>*</td>")
                        showDosingFootnote = true
                    }
                    def amt = regimen.doseAmount
                    if (amt.assign) {
                        rhs(amt.assign, result.append("<td>")).append("</td>")
                    } else {
                        result.append("<td>*</td>")
                        showDosingFootnote = true
                    }
                    result.append("<td>").append(amt.symbRef.symbIdRef).append("</td>")
                    break
               default:
                    result.append("<td colspan='4'>Unknown</td>")
                    break
            }
        }
        result.append("</tr>\n")
        return showDosingFootnote
    }

    protected StringBuilder transitiveStepDeps(def ds) {
        StringBuilder result = new StringBuilder()
        if (!ds) {
            return result
        }
        def deps = []
        ds.each { deps << it.oidRef }

        result.append(deps.join(", "))
        return deps.size() == 1 ? result :
                new StringBuilder("[").append(result).append("]")
    }

    protected StringBuilder randomVariables(List<ParameterRandomVariable> rv, Map rvMap) {
        def output = new StringBuilder()
        try {
            rv.inject(output) { o, i ->
                populateRandomVariableMap(i.symbId, i.variabilityReference.symbRef.symbIdRef, rvMap)
                if (i.abstractContinuousUnivariateDistribution) {
                    o.append("<div>")
                    o.append(distributionAssignment(i.symbId, i.abstractContinuousUnivariateDistribution))
                    o.append("&nbsp;&mdash;&nbsp;").append(i.variabilityReference.symbRef.symbIdRef)
                    o.append("</div>\n")
                }
            }
        } catch(Exception e) {
            output.append("Cannot display random variables.")
            log.error("Error encountered while rendering random variables ${rv.inspect()}: ${e.message}", e)
        }
        return output
    }

    protected StringBuilder individualParams(List<IndividualParameter> parameters,
                List<ParameterRandomVariable> rv, List<CovariateDefinition> covariates,
                Map<String, Equation> transfMap) {
        def output = new StringBuilder()
        if (!parameters) {
            return output
        }
        output.append "<div class='spaced-top-bottom'>"
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
                        gaussianModel.randomEffects.each { re ->
                            def randomEffectSymbol = new SymbolRef()
                            randomEffectSymbol.symbIdRef = re.symbRef[0].symbIdRef
                            randomEffects << wrapJaxb(randomEffectSymbol)
                        }
                    }
                    if (gaussianModel.linearCovariate) {
                        def linearCovariate = gaussianModel.linearCovariate
                        if (gaussianModel.transformation) {
                            final String TRANSFORMATION = gaussianModel.transformation.value()
                            //LHS
                            Uniop indivParam = new Uniop()
                            indivParam.operator = Unioperator.valueOf(TRANSFORMATION)
                            def paramSymbRef = new SymbolRef()
                            paramSymbRef.symbIdRef = p.symbId
                            indivParam.symbRef = paramSymbRef
                            def lhsEquation = new Equation()
                            lhsEquation.scalarOrSymbRefOrBinop.add(wrapJaxb(indivParam))
                            //POPULATION
                            def popParam
                            if (linearCovariate.populationParameter.assign.symbRef) {
                                popParam = new Uniop()
                                popParam.operator = TRANSFORMATION
                                popParam.symbRef = linearCovariate.populationParameter.assign.symbRef
                            }
                            def fixedEffectsCovMap = [:]
                            linearCovariate.covariate.each { c ->
                                //fixed effects
                                if (!c.fixedEffect) {
                                    return
                                }
                                def fixedEffects = []
                                def covEffectKey
                                c.fixedEffect.each { fe ->
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
                                fixedEffectsCovMap.each{
                                    def thisCov = []
                                    def key = it.key
                                    if ( key instanceof Equation) {
                                        thisCov.addAll(key.scalarOrSymbRefOrBinop)
                                    } else {
                                        thisCov.add(wrapJaxb(key))
                                    }
                                    it.value.collect{ v -> thisCov.add(wrapJaxb(v)) }
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
                            rhsEquation.scalarOrSymbRefOrBinop.add(
                                applyBinopToList(sumElements, Binoperator.PLUS))
                            output.append(convertToMathML(lhsEquation, rhsEquation))
                            output.append("\n")
                        }
                    } else if (gaussianModel.generalCovariate) {
                        def rhsEquation
                        def covModel
                        def covariateModelAssign = gaussianModel.generalCovariate.assign
                        assert covariateModelAssign != null
                        if (!randomEffects) {
                            rhsEquation = covariateModelAssign
                        } else {
                            if (covariateModelAssign.equation) {
                                covModel = covariateModelAssign.equation.scalarOrSymbRefOrBinop.first()
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
                        rhsEquation.scalarOrSymbRefOrBinop.add(
                            applyBinopToList(cm_re, Binoperator.PLUS))
                        String converted = convertToMathML(p.symbId, rhsEquation)
                        output.append("<div>")
                        output.append(converted)
                        output.append("</div>")
                    }
                }
            }
        } catch(Exception e) {
            output = new StringBuilder("<div class='spaced-top-bottom'>")
            output.append("Cannot display individual parameters.")
            log.error("Error encountered while rendering individual parameters ${parameters.inspect()} using random variables ${rv.inspect()} and covariates ${covariates.inspect()}: ${e.message}", e)
        }
        return output.append("</div>")
    }

    protected void displayCorrelationMatrices(List<CorrelationMatrix> m, StringBuilder sb) {
        m.each { convertToMathML(it, sb) }
    }

    protected StringBuilder gaussianObsErr(GaussianObsError e) {
        def result = new StringBuilder()

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
            lhsUniop.operator = Unioperator.valueOf(tr)
            lhsUniop.symbRef = lhsSymb
            lhs = new Equation()
            lhs.scalarOrSymbRefOrBinop.add(wrapJaxb(lhsUniop))
            def predUniop = new Uniop()
            predUniop.operator = Unioperator.valueOf(tr)
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
            errModel = errModelAssign.equation.scalarOrSymbRefOrBinop.first()
        } else if (errModelAssign.scalar) {
            errModel = wrapJaxb(errModelAssign.scalar)
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
        rhsEquation.scalarOrSymbRefOrBinop.add(wrapJaxb(sum))
        if (lhs && rhsEquation) {
            result.append("<div class='spaced-top-bottom'>")
            return result.append(convertToMathML(lhs, rhsEquation)).append("</div>")
        } else {
            return result
        }
    }

    protected StringBuilder generalObsErr(GeneralObsError e) {
        def result = new StringBuilder()
        if (!e) {
            return result
        }
        if (e.assign) {
            result.append("<p>").append(convertToMathML(e.symbId, e.assign)).append("</p>")
        }
        return result
    }

    //TODO REMOVE THIS
    protected StringBuilder simpleParams(List<SimpleParameter> parameters,
                Map<String, Equation> transfMap = [:]) {
        def outcome = new StringBuilder()
        if (!parameters) {
            return outcome
        }
        outcome.append("<div class='spaced-top-bottom'>")
        try {
            parameters.inject(outcome) { o, p ->
                String thisParam
                if (p.assign) {
                    thisParam = convertToMathML(p.symbId, p.assign, transfMap)
                } else {
                    thisParam = ["<math display='inline'><mstyle>", "</mstyle></math>"].join(op(p.symbId))
                }
                o.append("<span>")
                o.append(thisParam).append(";&nbsp;")
                o.append("</span>\n")
            }
        } catch(Exception e) {
            outcome.append("<p>Cannot display simple parameters.<p>")
            log.error("Error encountered while rendering simple params ${parameters.inspect()}: ${e.message}", e)
        } finally {
            return outcome.append("</div>")
        }
    }

    protected StringBuilder jaxbVector(CTVector vector) {
        if (!vector) {
            return new StringBuilder()
        }
        def result = new StringBuilder("")
        def values = []
        vector.sequenceOrScalar.inject(result) { r, ss ->
            switch(ss.value) {
               case Sequence:
                    values << sequence(ss.value)
                    break
                default:
                    values << scalar(ss.value)
            }
        }
        result.append(values.size() > 1 ? "[${values.join(', ')}]" : values.first())
        return result
    }

    protected StringBuilder variableAssignments(List<VariableAssignment> assignments, String heading) {
        def result = new StringBuilder("\n${heading}\n")
        assignments.inject(result){r,v ->
            r.append("<p>").append(convertToMathML(v.symbRef.symbIdRef, v.assign)).append("</p>")
        }
        return result
    }

    protected StringBuilder paramsToEstimate(ToEstimate params) {
        def result = new StringBuilder("<div><h5>Estimation parameters</h5>\n")
        def fixedParams = params.parameterEstimation.findAll{ it.initialEstimate?.fixed }
        if (fixedParams) {
            result.append(estimParamsWithInitialEstimate(fixedParams, "Fixed parameters"))
        }
        def estimatedParams = params.parameterEstimation - fixedParams
        if (estimatedParams) {
            result.append(estimParamsWithInitialEstimate(estimatedParams, "Initial estimates for non-fixed parameters"))
        }
        return result.append("</div>")
    }

    protected StringBuilder estimParamsWithInitialEstimate(List<ParameterEstimate> params,
                String heading) {
        def result = new StringBuilder("<p class=\"bold\">${heading}</p>\n")
        if (params.size() > 1) {
            result.append("<ul>")
            params.inject(result) { r, p ->
                r.append("<li>")
                //how do we display lowerBound and upperBound?
                r.append(convertToMathML(p.symbRef.symbIdRef, p.initialEstimate))
                r.append("</li>\n")
            }
            result.append("</ul>\n")
        } else {
            result.append("&nbsp;<span>")
            result.append(convertToMathML(params[0].symbRef.symbIdRef, params[0].initialEstimate))
            result.append("</span>\n")
        }
        return result
    }

    protected StringBuilder formatOperations(def operations, boolean isEstimation = true) {
        if (!operations) {
            return new StringBuilder()
        }
        final String TYPE = isEstimation ? "Estimation" : "Simulation"
        def result = new StringBuilder("<h5>").append(TYPE).append(" operations</h5>\n")
        // It is nicer to display the long description than the enum value.
        def operationMeaningMap = [
                "estFIM"   : "Calculate the Fisher Information Matrix",
                "estIndiv" : "Estimate the individual parameters",
                "estPop"   : "Estimate the population parameters",
                "simulatePK" : "Simulate PK profile(s)",
                "simulatePD" : "Simulate PD profile(s)"
        ]
        operations.each { o ->
            result.append("<div><span class=\"bold\">")
            result.append(o.order).append(") ")
            result.append(o.name ? o.name.value : operationMeaningMap[o.opType])
            result.append("</span>\n")
            if (o.description || o.algorithm || o.property) {
                result.append("<div>")
                if (o.description) {
                    result.append(o.description.value)
                }
                result.append("</div>")
                if (o.property || o.algorithm) {
                    result.append("\n<ul>")
                    o.property.inject(result) { r, p ->
                        r.append("<li>").append(operationProperty(p)).append("</li>")
                    }
                    result.append("</ul>\n")
                    if (o.algorithm) {
                        result.append("\n<span>Algorithm ")
                        result.append(o.algorithm.name ? o.algorithm.name.value :
                                (o.algorithm.definition ? o.algorithm.definition : ""))
                        result.append("</span>\n")
                        if (o.algorithm.property) {
                            result.append("<ul>")
                            o.algorithm.property.inject(result) { r, p ->
                                r.append("<li>").append(operationProperty(p)).append("</li>")
                            }
                            result.append("</ul>")
                        }
                    }
                }
            }
            result.append("</div>")
        }
        return result
    }

    StringBuilder objectiveDataSetMapping(List<DatasetMapping> mappings, RevisionTransportCommand rev, String downloadLink) {
        def result = new StringBuilder("<h5>Dataset mapping")
        if (mappings.size() > 1) {
            result.append("s")
        }
        result.append("</h5>\n")
        def variableMap = [:]

        mappings.each { dsm ->
            if (dsm.variableAssignment) {
                result.append(variableAssignments(dsm.variableAssignment,
                            "<span class=\"bold\">Variable assignments</span>"))
            }
            dsm.mapping.each {
                //keep track of variableMappings so that we know how to name the columns
                //deal with JAXBElement
                if (it.value instanceof VariableMapping) {
                    variableMap << [ (it.value.columnRef.columnIdRef) : (it.value.symbRef.symbIdRef)]
                }
            }
            if (dsm.dataSet) {
                dataSet(dsm.dataSet, variableMap, result, rev, downloadLink)
            }
        }
        return result
    }

    protected StringBuilder operationProperty(OperationProperty prop) {
        return new StringBuilder().append(convertToMathML(prop.name, prop.assign))
    }

    protected StringBuilder dataSet(DataSet dataSet, Map variableMap,
            StringBuilder sb, RevisionTransportCommand rev, String downloadLink) {
        List tables = dataSet.getListOfColumnDefinition()
        if (tables) {
            tables.each {
                def columnOrder = tables.inject([:]) { order, colDef ->
                    order << [(colDef.columnNum) : (colDef.columnId)]
                }
                sb.append("\n<table><thead><tr>")

                tables.inject(sb) { txt, d ->
                    def key = columnOrder[d.columnNum]
                    if (key && variableMap && variableMap[key]) {
                        txt.append(["<th>", "</th>"].join(variableMap[key]))
                    } else if (d instanceof ColumnDefinition) {
                        txt.append(["<th>", "</th>"].join(d.columnId))
                    } else if (d instanceof DataSetTableDefnType) {
                        txt.append(["<th>", "</th>"].join(d.tableId))
                    }
                }
                sb.append("</tr></thead><tbody>")
                dataSet.getListOfRow().each { i ->
                    sb.append("\n<tr>")
                    i.getListOfValue().each { td ->
                        if (td.value instanceof DataSetTableType) {
                            def content = new StringBuilder("<table class='default'>")
                            td.value.row.inject(content) { cont, r ->
                                cont.append("<tr class='default'>")
                                r.getListOfValue().inject(cont) { s, val ->
                                    s.append("<td class='default'>")
                                    if (val instanceof DataSetTableType) {
                                        s.append("*")
                                    } else {
                                        s.append(scalar(val.value))
                                    }
                                    s.append("</td>")
                                }
                                cont.append("</tr>")
                            }
                            String ready = content.append("</table>").toString()
                            sb.append(["<td class='default'>", "</td>"].join(ready))
                        } else {
                            sb.append(["<td class='default'>", "</td>"].join(scalar(td.value)))
                        }
                    }
                    sb.append("</tr>")
                }
                sb.append("</tbody></table>\n")
            }
        }
        if (dataSet.externalFile) {
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
            }
            else {
                sb.append("This model refers to an external data file named '")
                sb.append(dataSet.externalFile.name)
                sb.append("', but the file is not available in the repository. ")
            }
        }
        return sb
    }

     protected StringBuilder steadyState(def ss) {
        def result = new StringBuilder()
        try {
            result.append("<strong>Steady state</strong>")
            def i = ss.interval
            def end = ss.endTime
            result.append("<div>Interval: ").append(convertToMathML(i.symbRef.symbIdRef, i.assign))
            result.append("</div><div>")
            result.append("End time: ").append(convertToMathML(end.symbRef.symbIdRef, end.assign))
            result.append("</div>")
        } catch(Exception e) {
            result = new StringBuilder()
            result.append("Cannot display steady state.")
            log.error("Cannot display steady state ${ss.properties}", e)
        }
        return result
    }

    protected StringBuilder occasions(def occasions, StringBuilder text) {
        text.append("[")
        occasions.each { o ->
            StringBuilder sb = new StringBuilder("(level:")
            sb.append(o.levelId).append(", symbolIdentifier:").append(o.symbId).append(")")
            text.append(sb)
        }
        return text.append("]")
    }

    protected StringBuilder distributionAssignment(String l, def d) {
        StringBuilder builder=new StringBuilder("<math display='inline'><mstyle>")
        builder.append(oprand(l))
        builder.append(op("&sim;"))
        builder.append(distribution(d))
        builder.append("</mstyle></math>")
        return builder
    }

    protected StringBuilder distribution(def d) {
        if (!d) {
            return
        }
        def StringBuilder result = new StringBuilder(oprand("N"))
        result.append(op("("))
        def distributionType = d.value
        if (distributionType instanceof NormalDistribution) {
            result.append(normalDistribution(d))
        }
        result.append(op(")"))
        return result
    }

    protected String normalDistribution(def dist) {
        StringBuilder result = new StringBuilder()
        NormalDistribution d = dist.value
        String mean = (d.mean.var?.varId) ? (d.mean.var.varId) : (d.mean.rVal)
        result.append(oprand(mean))
        String stdDev = d.stddev?.var?.varId ? d.stddev.var.varId : d.stddev?.prVal
        if (stdDev) {
            result.append(op(",")).append(oprand(stdDev))
        }
        String variance = d.variance?.var?.varId ? d.variance.var.varId : d.variance?.prVal
        if (variance) {
            result.append(op(",")).append(oprand(variance))
        }

        return result.toString()
    }

    protected StringBuilder scalarRhs(def r) {
        StringBuilder text = new StringBuilder()
        if (r.scalar) {
            text.append(scalar(r.scalar.value))
        } else if (r.equation) {
            text.append(convertToMathML(r.equation))
        } else if (r.symbRef) {
            text.append(r.symbRef.symbIdRef)
        }
        return text
    }

    protected StringBuilder rhs(Rhs r, StringBuilder text) {
        if (r.equation) {
            return text.append(convertToMathML(r.equation))
        }
        if (r.sequence) {
            return text.append(sequence(r.sequence))
        }
        if (r.vector) {
            return text.append(jaxbVector(r.vector))
        }
        if (r.symbRef) {
            return text.append(r.symbRef.symbIdRef)
        }
        if (r.scalar) {
            return text.append(scalar(r.scalar.value))
        }
    }

    protected String scalar(def s) {
        switch(s) {
            case RealValue:
            case IntValue:
            case StringValue:
            case IdValue:
                return s.value as String
            case TrueBoolean:
                return "true"
            case FalseBoolean:
                return "false"
            default:
                return s.toString()
        }
    }

    protected String sequence(Sequence s) {
        return [s.begin, s.stepSize, s.end].collect{rhs(it, new StringBuilder())}.join(":")
    }

    protected StringBuilder sequenceAsMathML(def s) {
        return new StringBuilder(op("[")).append(oprand(s.begin)).append(op(":")).
                   append(oprand(s.stepSize)).append(op(":")).
                   append(oprand(s.end)).append(op("]"))
    }

    StringBuilder vectorAsMathML(def v) {
        def result = new StringBuilder()
        if (!v) {
            return result.append(" ")
        }
        result.append(op("["))
        def iterator = v.vector.sequenceOrScalar.iterator()
        while (iterator.hasNext()) {
            //can be a scalar or a sequence
            def vectorElement = iterator.next().value
            switch (vectorElement) {
                case Sequence:
                    result.append(sequence(vectorElement))
                    break
                case IdValue:
                case StringValue:
                case IntValue:
                case RealValue:
                case TrueBoolean:
                case FalseBoolean:
                    result.append(oprand(vectorElement.value.toString()))
                    break
                default:
                    assert false, "Vectors can only contain scalars or sequences."
                    log.error("Vectors cant contain ${vectorElement.inspect()} ${vectorElement.properties}")
                    break
            }
            if (iterator.hasNext()) {
                result.append(op(","))
            }
        }
        result.append(op("]"))
    }

    StringBuilder vector(def v) {
        def result = new StringBuilder()
        if (!v) {
            return result.append(" ")
        }
        result.append("[")
        def iterator = v.sequenceOrScalar.iterator()
        while (iterator.hasNext()) {
            //can be a scalar or a sequence
            def vectorElement = iterator.next()
            def item
            try {
                item = vectorElement as ScalarRhs
                result.append(item.value.toPlainString())
            } catch (ClassCastException ignored) {
                item = vectorElement as Sequence
                result.append(sequence(item))
            }
            if (iterator.hasNext()) {
                result.append(",")
            }
        }
        result.append("]")
    }

    protected void prefixToInfix(StringBuilder builder, List<MathsSymbol> stack) {
        if (stack.isEmpty()) {
            return;
        }
        MathsSymbol symbol=stack.pop()
        if (symbol instanceof OperatorSymbol) {
            OperatorSymbol operator=symbol as OperatorSymbol
            if (operator.type==OperatorSymbol.OperatorType.BINARY) {
                builder.append(operator.getOpening())
                if (operator.needsTermSeparation) {
                    builder.append("<mrow>")
                }
                prefixToInfix(builder,stack)
                if (operator.needsTermSeparation) {
                    builder.append("</mrow>")
                }
                builder.append(operator.getMapping())
                if (operator.needsTermSeparation) {
                    builder.append("<mrow>")
                }
                prefixToInfix(builder,stack)
                if (operator.needsTermSeparation) {
                    builder.append("</mrow>")
                }
                builder.append(operator.getClosing())
            }
            else if (operator.type == OperatorSymbol.OperatorType.MINMAX) {
            	builder.append(operator.getMapping())
            	builder.append(operator.getOpening())
            	prefixToInfix(builder, stack)
            	builder.append(op(","))
            	prefixToInfix(builder, stack)
                builder.append(operator.getClosing())
            }
            else if (operator.type == OperatorSymbol.OperatorType.GAMMALN) {
            	builder.append(op("ln"));
            	builder.append(operator.getOpening())
            	builder.append(operator.getMapping())
                builder.append(operator.getOpening())
            	prefixToInfix(builder, stack)
                builder.append(operator.getClosing())
                builder.append(operator.getClosing())
            }
            else if (operator.type == OperatorSymbol.OperatorType.FACTLN) {
            	builder.append(op("ln"));
            	builder.append(operator.getOpening())
            	prefixToInfix(builder, stack)
                builder.append(operator.getMapping())
                builder.append(operator.getClosing())
            }
            else if (operator.type == OperatorSymbol.OperatorType.SQROOT) {
            	builder.append("<msqrt>")
            	prefixToInfix(builder, stack)
            	builder.append("</msqrt>")
            }
            // Special case of root/square root, handled differently from other
            // operators.
            else if (operator.type==OperatorSymbol.OperatorType.ROOT) {
                StringBuilder operandBuilder=new StringBuilder()
                prefixToInfix(operandBuilder, stack)
                StringBuilder rootBuilder=new StringBuilder()
                prefixToInfix(rootBuilder, stack)
                boolean isSquareRoot=false
                try {
                    String rootValue=rootBuilder.toString().replace("<mi>","").replace("</mi>","")
                    double value = Double.parseDouble(rootValue)
                    if (value==2.0) {
                        isSquareRoot=true
                    }
                } catch(NumberFormatException notANumber) { }
                if (!isSquareRoot) {
                    builder.append("<mroot><mrow>")
                    builder.append(operandBuilder)
                    builder.append("</mrow><mrow>")
                    builder.append(rootBuilder)
                    builder.append("</mrow></mroot>")
                }
                else {
                    builder.append("<msqrt>")
                    builder.append(operandBuilder)
                    builder.append("</msqrt>")
                }
            }
            // Special case of power, handled differently from other
            // operators.
            else if (operator.type==OperatorSymbol.OperatorType.POWER) {
                StringBuilder operandBuilder=new StringBuilder()
                prefixToInfix(operandBuilder, stack)
                StringBuilder powerBuilder=new StringBuilder()
                prefixToInfix(powerBuilder, stack)
                boolean isSquareRoot=false
                builder.append("<msup><mrow>")
                builder.append(operandBuilder)
                builder.append("</mrow><mrow>")
                builder.append(powerBuilder)
                builder.append("</mrow></msup>")
            }
            else {
                builder.append(operator.getMapping())
                builder.append(operator.getOpening())
                prefixToInfix(builder,stack)
                builder.append(operator.getClosing())
            }
        }
        else if (symbol instanceof FunctionSymbol) {
            FunctionSymbol function=symbol as FunctionSymbol
            builder.append(function.getMapping())
                builder.append(function.getOpening())
                for (int i=0; i<function.getArgCount(); i++) {
                    prefixToInfix(builder, stack)
                    if (i!=function.getArgCount()-1) {
                        builder.append(function.getArgSeparator())
                    }
                }
                builder.append(function.getClosing())
        }
        else if (symbol instanceof PiecewiseSymbol) {
            PiecewiseSymbol piecewise=symbol as PiecewiseSymbol
            builder.append(piecewise.getOpening())
            for (int i=0; i<piecewise.getPieceCount(); i++) {
                prefixToInfix(builder, stack)
            }
            builder.append(piecewise.getClosing())
        }
        else if (symbol instanceof PieceSymbol) {
            PieceSymbol piece=symbol as PieceSymbol
            builder.append(piece.getOpening())
            builder.append(piece.getTermStarter())
            prefixToInfix(builder, stack)
            builder.append(piece.getTermEnder())
            if (piece.type==PieceSymbol.ConditionType.OTHERWISE) {
                builder.append(piece.getOtherwiseText())
            }
            else {
                builder.append(piece.getIfText())
                builder.append(piece.getTermStarter())
                prefixToInfix(builder, stack)
                builder.append(piece.getTermEnder())
            }
            builder.append(piece.getClosing())
        }
        else {
            builder.append(symbol.getMapping())
        }
       // prefixToInfix(builder, stack)
    }

    protected JAXBElement expandNestedSymbRefs(JAXBElement<SymbolRef> symbRef,
            Map<String, Equation> transformations) {
        final EquationType TRANSF_EQ = resolveSymbolReference(symbRef.value, transformations)
        if (TRANSF_EQ) {
            final def FIRST_ELEM = TRANSF_EQ.scalarOrSymbRefOrBinop.first()
            final Class ELEM_CLASS = FIRST_ELEM.value.getClass()
            switch(ELEM_CLASS) {
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
            return FIRST_ELEM
        } else {
            return symbRef
        }
    }

    protected JAXBElement expandNestedUniop(JAXBElement<Uniop> jaxbUniop,
            Map<String, Equation> transfMap) {
        Uniop uniop = jaxbUniop.value
        Uniop replacement
        if (uniop.symbRef) {
            final EquationType TRANSF_EQ = resolveSymbolReference(uniop.symbRef, transfMap)
            if (TRANSF_EQ) {
                final def FIRST_ELEM = TRANSF_EQ.scalarOrSymbRefOrBinop.first().value
                final Class ELEM_CLASS = FIRST_ELEM.getClass()
                replacement = new Uniop()
                replacement.operator = uniop.operator
                switch(ELEM_CLASS) {
                    case Binop:
                        replacement.binop = FIRST_ELEM
                        break
                    case Uniop:
                        replacement.uniop = FIRST_ELEM
                        break
                    case SymbolRef:
                        replacement.symbRef = FIRST_ELEM
                        break
                    case Constant:
                        replacement.constant = FIRST_ELEM
                        break
                    case FunctionCallType:
                        replacement.functionCall = FIRST_ELEM
                        break
                    case IdValue:
                        replacement.scalar = FIRST_ELEM
                        break
                    case StringValue:
                        break
                    case IntValue:
                        replacement.scalar = FIRST_ELEM
                        break
                    case RealValue:
                        replacement.scalar = FIRST_ELEM
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

    protected JAXBElement expandNestedBinop(JAXBElement<Binop> jaxbBinop,
            Map<String, Equation> transfMap) {
        Binop binop = jaxbBinop.value
        List<JAXBElement> terms = [binop.operand1, binop.operand2].collect {it.toJAXBElement()}
        def expandedTerms = terms.collect { c ->
            switch (c.value) {
                case SymbolRef:
                    return expandNestedSymbRefs(c, transfMap)
                    break
                case Binop:
                    return expandNestedBinop(c, transfMap)
                    break
                case Uniop:
                    return expandNestedUniop(c, transfMap)
                    break
                default:
                    return c
                    break
            }
        }
        if (expandedTerms.equals(terms)) {
            return jaxbBinop
        }
        Binop expanded = new Binop()
        expanded.operator = binop.operator
        expanded.operand1 = expandedTerms[0].value
        expanded.operand2 = expandedTerms[1].value
        return wrapJaxb(expanded)
    }

    protected EquationType expandEquation(EquationType equation, Map<String, Equation> transfMap) {
        List<JAXBElement> eqTerms = equation.scalarOrSymbRefOrBinop
        List<JAXBElement> expandedTerms = eqTerms.collect {
            switch(it.value) {
                case Binop:
                    return expandNestedBinop(it, transfMap)
                    break
                case Uniop:
                    return expandNestedUniop(it, transfMap)
                    break
                case SymbolRef:
                    return expandNestedSymbRefs(it, transfMap)
                    break
                default:
                    return it
                    break
            }
        }
        if (!eqTerms.equals(expandedTerms)) {
            def newEquation = new EquationType()
            newEquation.scalarOrSymbRefOrBinop = expandedTerms
            return newEquation
        }
        return equation
    }

    protected void convertEquation(def equation, StringBuilder builder, Map<String, Equation> transfMap = [:]) {
        def equationToProcess
        if (!transfMap) {
            equationToProcess = equation
        } else if ((equation instanceof EquationType) || (equation instanceof Equation)) {
            equationToProcess = expandEquation(equation, transfMap)
        } else {
            equationToProcess = equation
        }
        List<MathsSymbol> symbols = MathsUtil.convertToSymbols(equationToProcess).reverse()
        List<String> stack = new LinkedList<String>()
        symbols.each {
           stack.push(it)
        }
        prefixToInfix(builder, stack)
    }

    protected String convertToMathML(def equation, Map<String, Equation> transfMap = [:]) {
        StringBuilder builder=new StringBuilder("<math display='inline'><mstyle>")
        convertEquation(equation, builder, transfMap)
        builder.append("</mstyle></math>")
        return builder.toString()
    }

    protected String convertToMathML(String lhs, def equation, Map<String, Equation> transfMap = [:]) {
        StringBuilder builder=new StringBuilder("<math display='inline'><mstyle>")
        builder.append(oprand(lhs))
        builder.append(op("="))
        convertEquation(equation, builder, transfMap)
        builder.append("</mstyle></math>")
        return builder.toString()
    }

    //works with EquationType and Equation as well
    protected String convertToMathML(EquationType lhs, EquationType rhs, Map<String, Equation> transfMap = [:]) {
        StringBuilder output = new StringBuilder("<div>")
        output.append("<math display='inline'><mstyle>")
        convertEquation(lhs, output)
        output.append(op("="))
        convertEquation(rhs, output, transfMap)
        output.append("</mstyle></math>")
        return output.append("</div>").toString()
    }

    protected String convertToMathML(String lhs, ScalarRhs srhs, Map<String, Equation> transfMap = [:]) {
         if (srhs.equation) {
            return convertToMathML(lhs, srhs.equation, transfMap)
        }
        if (srhs.symbRef) {
            return convertToMathML(lhs, srhs.symbRef, transfMap)
        }
        StringBuilder result = new StringBuilder("<math display='inline'><mstyle>")
        result.append(oprand(lhs)).append(op("="))
        if (srhs.scalar) {
            result.append(oprand(scalar(srhs.scalar.value)))
        }
        return result.append("</mstyle></math>").toString()
    }

    protected String convertToMathML(String lhs, Rhs rhs, Map<String, Equation> transfMap = [:]) {
        if (rhs.equation) {
            return convertToMathML(lhs, rhs.equation, transfMap)
        }
        if (rhs.symbRef) {
            return convertToMathML(lhs, rhs.symbRef, transfMap)
        }
        StringBuilder builder=new StringBuilder("<math display='inline'><mstyle>")
        builder.append(oprand(lhs))
        builder.append(op("="))
        if (rhs.getScalar()) {
            builder.append(oprand(scalar(rhs.scalar.value)))
        } else if (rhs.getSequence()) {
            builder.append(sequenceAsMathML(rhs.sequence))
        } else if (rhs.getVector()) {
            builder.append(vectorAsMathML(rhs))
        }
        builder.append("</mstyle></math>")
        return builder.toString()
    }

    protected String convertToMathML(DerivativeVariable derivative, def iv) {
        String independentVariable = derivative.independentVariable?.symbRef?.symbIdRef ?: (iv ?: "t")
        String derivTerm="d${derivative.symbId}<DIVIDEDBY>d${independentVariable}"
        return convertToMathML(derivTerm, derivative.getAssign())
    }

    protected String convertToMathML(String lhs, List arguments, def equation) {
        StringBuilder builder=new StringBuilder("<math display='inline'><mstyle>")
        builder.append(oprand(lhs))
        builder.append(op("("))
        for (int i=0; i<arguments.size(); i++) {
            builder.append(oprand(arguments.get(i).symbId))
            if (i<arguments.size()-1) {
                builder.append(op(","))
            }
        }
        builder.append(op(")"))
        builder.append(op("="))
        convertEquation(equation, builder)
        builder.append("</mstyle></math>")
        return builder.toString()
    }

    protected void convertToMathML(CorrelationMatrix corrMatrix, StringBuilder output) {
        final String TYPE = corrMatrix.type
        final String LVL = corrMatrix.variabilityLevel
        final List<String> EFFECTS = corrMatrix.randomEffects as List
        def matrix = corrMatrix.matrix
        if (!matrix) {
            log.info("$corrMatrix contains undefined correlation matrix.")
            return
        }

        output.append("<div class='spaced-top-bottom'>")
        output.append("<span class='bold'>").append(TYPE)
        output.append(" matrix for level <span class='italic'>")
        output.append(LVL).append("</span> and random effects: ")
        output.append(EFFECTS.join(", ")).append("</span></div>\n")
        output.append("<math display='inline'><mstyle>\n")
        output.append("<mrow>").append(op("(")).append("<mtable>\n")
        final int N = matrix.size()
        for (int i = 0; i < N; i++) {
            output.append("<mtr>")
            for (int j = 0; j < N; j++) {
                output.append("<mtd><mi>")
                output.append(matrix[i][j])
                output.append("</mi></mtd>\n")
            }
            output.append("</mtr>\n")
        }
        output.append("</mtable>\n").append(op(")")).append("</mrow>")
        output.append("</mstyle></math>")
    }

    protected String op(String o) {
        return "<mo>${o}</mo>"
    }

    protected String oprand(String o) {
        if (o.contains("<DIVIDEDBY>")) {
            String[] parts=o.split("<DIVIDEDBY>")
            return "<mfrac><mi>${parts[0]}</mi><mi>${parts[1]}</mi></mfrac>"
        }
        return "<mi>${o}</mi>"
    }

    protected JAXBElement wrapJaxb(def elem) {
        return elem instanceof JAXBElement ? elem : new JAXBElement(new QName(""), elem.getClass(), elem)
    }

    protected JAXBElement applyBinopToList(List elements, Binoperator operator) {
        if (elements.size() == 1) {
            // just return the element
            return wrapJaxb(elements.first())
        } else {
            def result = new Binop()
            result.operator = operator
            result.operand1 = elements.first().value
            result.operand2 = applyBinopToList(elements[1..-1], operator)?.value
            return wrapJaxb(result)
        }
    }

    /*
     * Looks up a symbol reference in continuousCovariateTransformations map.
     * If the given @p ref contains both a blkIdRef and a symbIdRef, then it returns
     * the equation corresponding to that continuous covariate, or null if there is no
     * covariate with that @ref defined in the map.
     *
     * If @ref only has a symbIdRef, then it will return the first element from the map
     * that matches, or null if there were no matches.
     */
    protected EquationType resolveSymbolReference(SymbolRef ref, Map<String, Equation> transfMap) {
        EquationType transfEq
        if (ref.blkIdRef) {
            String transfRef = "${ref.blkIdRef}_${ref.symbIdRef}"
            transfEq = transfMap[transfRef]
        } else {
            String transfRef = ref.symbIdRef
            transfEq = transfMap.find{ it.key.contains("_${transfRef}")}?.value
        }
        return transfEq
    }

    protected void populateRandomVariableMap(final String id, final String level, Map rv) {
        def currentRVs = rv[level]
        if (!currentRVs) {
            currentRVs = []
        }
        currentRVs.add(id)
        rv[level] = currentRVs
    }

}
