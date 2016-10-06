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

import net.biomodels.jummp.core.model.RevisionTransportCommand
import eu.ddmore.libpharmml.dom.PharmML
import eu.ddmore.libpharmml.dom.modeldefn.ModelDefinition
import eu.ddmore.libpharmml.dom.modellingsteps.ModellingSteps
import eu.ddmore.libpharmml.dom.trialdesign.TrialDesign

/**
 * Controller for handling Model files in the PharmML format.
 * @author  Raza Ali <raza.ali@ebi.ac.uk>
 * @author  Mihai Glonț <mihai.glont@ebi.ac.uk>
 */
class PharmMlController {
    def metadataDelegateService
    def pharmMlService

    def show = {
        def model = flash.genericModel
        final RevisionTransportCommand REVISION = model.revision
        model["genericAnnotations"] = metadataDelegateService.fetchGenericAnnotations REVISION
        PharmML dom = AbstractPharmMlHandler.getDomFromRevision(REVISION)
        final String VERSION = dom?.writtenVersion
        if (dom) {
            model["independentVar"] = pharmMlService.getIndependentVariable(dom, VERSION)
            model["functionDefs"] = pharmMlService.getFunctionDefinitions(dom, VERSION)
            model["version"] = VERSION

            ModelDefinition modelDefinition = pharmMlService.getModelDefinition(dom, VERSION)
            model["modelDefinition"] = modelDefinition
            model["structuralModel"] = pharmMlService.getStructuralModel(modelDefinition, VERSION)
            model["variabilityModel"] = pharmMlService.getVariabilityModel(modelDefinition, VERSION)
            model["covariateModel"] = pharmMlService.getCovariateModel(modelDefinition, VERSION)
            model["parameterModel"] = pharmMlService.getParameterModel(modelDefinition, VERSION)
            model["observationModel"] = pharmMlService.getObservationModel(modelDefinition, VERSION)

            TrialDesign design = pharmMlService.getTrialDesign(dom, VERSION)
            model["trialDesign"] = design
            model["structure"] = pharmMlService.getTrialDesignStructure(design, VERSION)
            model["population"] = pharmMlService.getPopulation(design, VERSION)
            model["dosing"] = pharmMlService.getIndividualDosing(design, VERSION)

            ModellingSteps steps = pharmMlService.getModellingSteps(dom, VERSION)
            model["estSteps"] = pharmMlService.getEstimationSteps(steps, VERSION)
            model["simSteps"] = pharmMlService.getSimulationSteps(steps, VERSION)
            model["stepDeps"] = pharmMlService.getStepDependencies(steps, VERSION)
        }
        render(view:"/model/pharmml/show", model: model)
    }
}
