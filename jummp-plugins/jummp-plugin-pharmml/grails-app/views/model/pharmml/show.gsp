<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 
 You should have received a copy of the GNU Affero General Public License along 
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>

<head>
    <meta name="layout" content="modelDisplay"/>
    <style>
        td, th {
            padding: 5px;
        }
        th {
            text-align: center;
            vertical-align: middle;
        }
        .default {
            margin:0;
        }
        .bold {
            font-weight: bold;
        }
        .italic {
            font-style: italic;
        }
    </style>
</head>
<content tag="genericAnnotations">
    <pharmml:renderGenericAnnotations revision="${revision}"/>
</content>
<content tag="modelspecifictabs">
    <pharmml:decideTabs iv="${independentVar}" fd="${functionDefs}" md="${modelDefinition}"
            td="${trialDesign}" est="${estSteps}" sim="${simSteps}" />
</content>
<content tag="modelspecifictabscontent">
    <pharmml:handleModelDefinitionTab version="${version}" iv="${independentVar}"
            fd="${functionDefs}" sm="${structuralModel}" vm="${variabilityModel}"
            cm="${covariateModel}" pm="${parameterModel}" om="${observationModel}" />

    <pharmml:handleTrialDesignTab version="${version}" ts="${structure}"
            td="${dosing}" tp="${population}" rev="${revision}"/>

    <pharmml:handleModellingStepsTabs estimation="${estSteps}" simulation="${simSteps}"
        independentVariable="${independentVar}" deps="${stepDeps}" version="${version}"
        rev="${revision}"/>
</content>
