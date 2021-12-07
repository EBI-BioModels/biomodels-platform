<%--
Copyright (C) 2010-2020 EMBL-European Bioinformatics Institute (EMBL-EBI),
Deutsches Krebsforschungszentrum (DKFZ)

This file is part of Jummp.

Jummp is free software; you can redistribute it and/or modify it under the
terms of the GNU Affero General Public License as published by the Free
Software Foundation; either version 3 of the License, or (at your option) any
later version.

Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
details.

You should have received a copy of the GNU Affero General Public License along
with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>

<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 01/04/2020
  Time: 12:27
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${layout}"/>
    <title>${title}</title>
</head>

<body>
    <div class="row">
        <div class="small-12 medium-12 large-12 columns">
            <h1>Update Redis Server</h1>
            <ul>
                <li><a href="${hpStatisticsDataForFeatures}">
                    Refresh the statistics data for the feature widgets</a></li>
                <li><a href="${hpStatisticsDataForCharts}">
                    Refresh statistics for the charts on Home Page (once click for updating the whole)</a><br/>Or click
                on each individual link from the list below
                    <ul style="list-style-type: square">
                        <li><a href="${hpStatisticsModellingApproaches}">Refresh statistics for Modelling
                                                                            Approaches</a></li>
                        <li><a href="${hpStatisticsOrganisms}">Refresh statistics for Organisms</a></li>
                        <li><a href="${hpStatisticsJournals}">Refresh statistics for Journals</a></li>
                    </ul>
                </li>
                <li><a href="${recentlyAccessedModels}">Refresh the recently accessed models</a></li>
                <li><a href="${recentlyPublishedModels}">Refresh the recently published models</a></li>
                <li><a href="${hpDataNewsWidget}">Refresh News</a></li>
                <li><a href="${latestMomEntry}">Refresh the latest Model Of The Month Entry</a></li>
            </ul>
        </div>
    </div>
    <div class="row">
        <div class="columns small-12 medium-12 large-12">
            <a class="button" href="${createLink(controller: "admin", action: "dashboard")}">
                Admin Dashboard
            </a>
            <a class="button" href="${createLink(controller: "curator", action: "dashboard")}">
                Curator Dashboard
            </a>
        </div>
    </div>
</body>
</html>
